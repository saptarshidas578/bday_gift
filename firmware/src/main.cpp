#include <Arduino.h>
#include <FastLED.h>
#include "config.h"
#include "matrix.h"
#include "effects.h"
#include "storage.h"
#include "ble_server.h"
#include "audio.h"
#include "birthday.h"
#include "font.h"
#include "clock_timer.h"
#include "scenes_scheduler.h"

static uint32_t lastRenderTime = 0;
static const uint32_t FRAME_INTERVAL_MS = 16; // ~60 FPS

// Boot button detection for factory/bond reset (hold 5s)
static uint32_t bootBtnPressStart = 0;
static bool bootBtnPressed = false;

void checkBootButton() {
    bool isDown = (digitalRead(BOOT_BUTTON_PIN) == LOW);
    uint32_t now = millis();

    if (isDown && !bootBtnPressed) {
        bootBtnPressed = true;
        bootBtnPressStart = now;
    } else if (isDown && bootBtnPressed) {
        if (now - bootBtnPressStart >= 5000) {
            Serial.println("\n[RECOVERY] BOOT button held 5s! Clearing BLE bonds...");
            bleClearBonds();
            bootBtnPressStart = now + 10000;
        }
    } else if (!isDown && bootBtnPressed) {
        bootBtnPressed = false;
    }
}

void checkSerialCommands() {
    if (Serial.available()) {
        String cmd = Serial.readStringUntil('\n');
        cmd.trim();
        if (cmd.equalsIgnoreCase("bday") || cmd == "b") {
            Serial.println("[SERIAL] Starting Birthday Show...");
            birthdayStart();
        } else if (cmd.equalsIgnoreCase("stop") || cmd == "s") {
            Serial.println("[SERIAL] Stopping Birthday Show / Timer...");
            if (isBirthdayRunning()) birthdayStop();
            if (isTimerRunning() || isTimerAlerting()) cancelTimer();
        } else if (cmd.equalsIgnoreCase("clock") || cmd == "c") {
            state.mode = MODE_CLOCK;
            Serial.println("[SERIAL] Switched to CLOCK mode.");
        } else if (cmd.equalsIgnoreCase("sync")) {
            // Example UTC sync to 12:34:56
            syncTime(1700000000, 330); // IST (+5:30 = 330 mins)
            state.mode = MODE_CLOCK;
            Serial.println("[SERIAL] Synced time to sample epoch.");
        } else if (cmd.startsWith("timer")) {
            startTimer(10); // 10 second test countdown
            Serial.println("[SERIAL] Started 10-second test timer.");
        } else if (cmd.startsWith("scene save ")) {
            uint8_t slot = cmd.substring(11).toInt();
            saveScene(slot);
        } else if (cmd.startsWith("scene load ")) {
            uint8_t slot = cmd.substring(11).toInt();
            loadScene(slot);
        } else if (cmd.startsWith("sleep ")) {
            uint16_t m = cmd.substring(6).toInt();
            startSleepTimer(m);
        } else if (cmd.equalsIgnoreCase("music")) {
            // Demo burst of 10 levels
            uint8_t demo[10] = { 40, 90, 150, 220, 255, 230, 180, 120, 70, 30 };
            feedMusicLevels(demo, 10);
            Serial.println("[SERIAL] Fed demo music levels.");
        }
    }
}

void processIncomingCommand(const ParsedCommand& cmd) {
    Serial.printf("[CMD] Dispatching opcode 0x%02X (seq %u, len %u)\n",
                  cmd.opcode, cmd.seq, cmd.payloadLen);

    switch (cmd.opcode) {
        case OP_SET_COLOR: {
            if (isBirthdayRunning()) birthdayStop();
            state.color = CRGB(cmd.payload[0], cmd.payload[1], cmd.payload[2]);
            state.mode = MODE_SOLID;
            storageMarkDirty();
            Serial.printf("  SET_COLOR: RGB(%u, %u, %u)\n", state.color.r, state.color.g, state.color.b);
            break;
        }

        case OP_SET_BRIGHTNESS: {
            state.brightness = cmd.payload[0];
            matrixSetBrightness(state.brightness);
            storageMarkDirty();
            Serial.printf("  SET_BRIGHTNESS: %u / 255\n", state.brightness);
            break;
        }

        case OP_SET_EFFECT: {
            if (isBirthdayRunning()) birthdayStop();
            state.effect.base = cmd.payload[0];
            state.effect.speed = cmd.payload[1];
            state.effect.dir = cmd.payload[2];
            state.effect.mirror = cmd.payload[3];
            state.effect.colorMode = cmd.payload[4];
            state.effect.c1 = CRGB(cmd.payload[5], cmd.payload[6], cmd.payload[7]);
            state.effect.c2 = CRGB(cmd.payload[8], cmd.payload[9], cmd.payload[10]);
            state.mode = MODE_EFFECT;
            storageMarkDirty();
            Serial.printf("  SET_EFFECT: Base=%u, Speed=%u, Dir=%u, CMode=%u\n",
                          state.effect.base, state.effect.speed, state.effect.dir, state.effect.colorMode);
            break;
        }

        case OP_SET_PALETTE: {
            state.paletteCount = cmd.payload[0];
            for (uint8_t i = 0; i < state.paletteCount && i < 8; i++) {
                state.palette[i] = CRGB(cmd.payload[1 + i * 3],
                                        cmd.payload[2 + i * 3],
                                        cmd.payload[3 + i * 3]);
            }
            storageMarkDirty();
            Serial.printf("  SET_PALETTE: %u colors updated\n", state.paletteCount);
            break;
        }

        case OP_SET_MODE: {
            if (isBirthdayRunning()) birthdayStop();
            state.mode = cmd.payload[0];
            storageMarkDirty();
            Serial.printf("  SET_MODE: %u\n", state.mode);
            break;
        }

        case OP_SYNC_TIME: {
            uint32_t epoch = (uint32_t)cmd.payload[0] |
                             ((uint32_t)cmd.payload[1] << 8) |
                             ((uint32_t)cmd.payload[2] << 16) |
                             ((uint32_t)cmd.payload[3] << 24);
            int16_t tz = (int16_t)((uint16_t)cmd.payload[4] | ((uint16_t)cmd.payload[5] << 8));
            syncTime(epoch, tz);
            break;
        }

        case OP_SET_CLOCK_STYLE: {
            state.clockStyle = cmd.payload[0];
            storageMarkDirty();
            Serial.printf("  SET_CLOCK_STYLE: %u\n", state.clockStyle);
            break;
        }

        case OP_START_TIMER: {
            uint32_t sec = (uint32_t)cmd.payload[0] |
                           ((uint32_t)cmd.payload[1] << 8) |
                           ((uint32_t)cmd.payload[2] << 16) |
                           ((uint32_t)cmd.payload[3] << 24);
            startTimer(sec);
            break;
        }

        case OP_CANCEL_TIMER: {
            cancelTimer();
            break;
        }

        case OP_PLAY_BIRTHDAY: {
            birthdayStart();
            break;
        }

        case OP_STOP_SHOW: {
            birthdayStop();
            break;
        }

        case OP_SAVE_SCENE: {
            saveScene(cmd.payload[0]);
            break;
        }

        case OP_LOAD_SCENE: {
            loadScene(cmd.payload[0]);
            break;
        }

        case OP_SET_SCHEDULE: {
            setSchedule(cmd.payload[0], cmd.payload[1], cmd.payload[2],
                        cmd.payload[3], cmd.payload[4], cmd.payload[5], cmd.payload[6]);
            break;
        }

        case OP_SET_SLEEP_TIMER: {
            uint16_t mins = (uint16_t)cmd.payload[0] | ((uint16_t)cmd.payload[1] << 8);
            startSleepTimer(mins);
            break;
        }

        case OP_DRAW_CHUNK: {
            uint16_t offset = (uint16_t)cmd.payload[0] | ((uint16_t)cmd.payload[1] << 8);
            const uint8_t* chunk = &cmd.payload[2];
            uint8_t len = cmd.payloadLen - 2;
            drawChunk(offset, chunk, len);
            break;
        }

        case OP_DRAW_COMMIT: {
            drawCommit();
            break;
        }

        case OP_MUSIC_LEVELS: {
            feedMusicLevels(cmd.payload, cmd.payloadLen);
            break;
        }

        case OP_GET_STATUS: {
            bleSendSnapshot();
            break;
        }

        case OP_CLEAR_BONDS: {
            bleClearBonds();
            break;
        }

        case OP_FACTORY_RESET: {
            storageFactoryReset();
            bleClearBonds();
            break;
        }

        default:
            Serial.printf("  Opcode 0x%02X acknowledged.\n", cmd.opcode);
            break;
    }
}

void setup() {
    Serial.begin(115200);
    unsigned long startWait = millis();
    while (!Serial && (millis() - startWait < 1000)) {
        // Wait up to 1 second for serial
    }

    Serial.println();
    Serial.println("==================================================");
    Serial.println("  PixelGift Clock & Timer Engine (Task B08)       ");
    Serial.println("==================================================");

    pinMode(BOOT_BUTTON_PIN, INPUT_PULLUP);
    pinMode(BUZZER_PIN, OUTPUT);
    digitalWrite(BUZZER_PIN, LOW);

    matrixInit();
    effectsInit();
    storageInit();
    audioInit();
    birthdayInit();
    clockTimerInit();
    scenesSchedulerInit();

    matrixSetBrightness(state.brightness);
    bleInit();

    lastRenderTime = millis();
    Serial.println("[SYSTEM] Ready! Type 'clock', 'sync', 'timer', or 'bday' in serial.\n");
}

void loop() {
    uint32_t now = millis();

    // 1. Check recovery boot button and serial commands
    checkBootButton();
    checkSerialCommands();

    // 2. Dispatch queued BLE commands
    ParsedCommand cmd;
    while (blePopCommand(cmd)) {
        processIncomingCommand(cmd);
    }

    // 3. Audio, Storage, Scheduler, Sleep & Music non-blocking loop steps
    audioLoop();
    storageLoop();
    schedulerLoop(now);
    sleepTimerLoop(now);
    musicLoop(now);

    // 4. Render loop (~60 FPS non-blocking)
    if (now - lastRenderTime >= FRAME_INTERVAL_MS) {
        lastRenderTime = now;

        if (isBirthdayRunning()) {
            birthdayRender(now, matrixGetLeds());
        } else if (isTimerRunning() || isTimerAlerting()) {
            renderTimer(now, matrixGetLeds());
        } else if (state.mode == MODE_CLOCK) {
            renderClock(now, matrixGetLeds());
        } else if (state.mode == MODE_SOLID) {
            matrixFill(state.color);
        } else if (state.mode == MODE_EFFECT) {
            effectsRender(now, state.effect, matrixGetLeds());
        } else if (state.mode == MODE_DRAW) {
            renderDraw(matrixGetLeds());
        } else if (state.mode == MODE_MUSIC) {
            renderMusic(now, matrixGetLeds());
        } else if (state.mode == MODE_OFF) {
            matrixClear();
        } else {
            effectsRender(now, state.effect, matrixGetLeds());
        }

        matrixShow();
    }
}
