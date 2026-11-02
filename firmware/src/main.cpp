#include <Arduino.h>
#include <FastLED.h>
#include "config.h"
#include "matrix.h"
#include "effects.h"
#include "storage.h"
#include "ble_server.h"

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
            bootBtnPressStart = now + 10000; // prevent repeat
        }
    } else if (!isDown && bootBtnPressed) {
        bootBtnPressed = false;
    }
}

void processIncomingCommand(const ParsedCommand& cmd) {
    Serial.printf("[CMD] Dispatching opcode 0x%02X (seq %u, len %u)\n",
                  cmd.opcode, cmd.seq, cmd.payloadLen);

    switch (cmd.opcode) {
        case OP_SET_COLOR: {
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
            state.mode = cmd.payload[0];
            storageMarkDirty();
            Serial.printf("  SET_MODE: %u\n", state.mode);
            break;
        }

        case OP_GET_STATUS: {
            Serial.println("  GET_STATUS: Sending snapshot notification.");
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
    Serial.println("  PixelGift BLE Server & Dispatcher (Task B06)   ");
    Serial.println("==================================================");

    pinMode(BOOT_BUTTON_PIN, INPUT_PULLUP);
    pinMode(BUZZER_PIN, OUTPUT);
    digitalWrite(BUZZER_PIN, LOW);

    matrixInit();
    effectsInit();
    storageInit();

    // Set restored hardware brightness
    matrixSetBrightness(state.brightness);

    // Initialize BLE Server with LE Secure Connections & Passkey
    bleInit();

    lastRenderTime = millis();
    Serial.println("[SYSTEM] Ready! Awaiting BLE connections / commands.\n");
}

void loop() {
    uint32_t now = millis();

    // 1. Process boot button for clearing bonds (hold 5s)
    checkBootButton();

    // 2. Dispatch queued BLE commands to main loop
    ParsedCommand cmd;
    while (blePopCommand(cmd)) {
        processIncomingCommand(cmd);
    }

    // 3. Periodic NVS debounced flush
    storageLoop();

    // 4. Render loop (~60 FPS non-blocking)
    if (now - lastRenderTime >= FRAME_INTERVAL_MS) {
        lastRenderTime = now;

        if (state.mode == MODE_SOLID) {
            matrixFill(state.color);
        } else if (state.mode == MODE_EFFECT) {
            effectsRender(now, state.effect, matrixGetLeds());
        } else if (state.mode == MODE_OFF) {
            matrixClear();
        } else {
            effectsRender(now, state.effect, matrixGetLeds());
        }

        matrixShow();
    }
}
