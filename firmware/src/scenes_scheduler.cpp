#include "scenes_scheduler.h"
#include "matrix.h"
#include "clock_timer.h"
#include <time.h>
#include <string.h>

// Sunrise Alarm State
static bool isSunriseRunning = false;
static uint32_t sunriseStartMs = 0;
static uint32_t sunriseDurationMs = 15 * 60000UL;

// Sleep Timer State
static bool isSleepRunning = false;
static uint32_t sleepStartMs = 0;
static uint32_t sleepDurationMs = 0;
static uint8_t sleepSavedBrightness = DEFAULT_BRIGHTNESS;

// Draw Canvas Buffers
static uint8_t stagedDraw[DRAW_BUFFER_BYTES];
static uint8_t activeDraw[DRAW_BUFFER_BYTES];

// Music Visualizer State
static uint8_t targetMusicLevels[MATRIX_WIDTH];
static float currentMusicLevels[MATRIX_WIDTH];
static uint32_t lastMusicTimeMs = 0;
static uint8_t modeBeforeMusic = MODE_SOLID;

void scenesSchedulerInit() {
    isSunriseRunning = false;
    isSleepRunning = false;
    memset(stagedDraw, 0, sizeof(stagedDraw));
    memset(activeDraw, 0, sizeof(activeDraw));
    memset(targetMusicLevels, 0, sizeof(targetMusicLevels));
    for (int i = 0; i < MATRIX_WIDTH; i++) {
        currentMusicLevels[i] = 0.0f;
    }
    lastMusicTimeMs = 0;
    modeBeforeMusic = MODE_SOLID;
}

// -------------------------------------------------------------
// Scenes (8 slots)
// -------------------------------------------------------------
bool saveScene(uint8_t slot) {
    if (slot >= 8) return false;

    state.scenes[slot].isValid = 1;
    state.scenes[slot].mode = state.mode;
    state.scenes[slot].color = state.color;
    state.scenes[slot].brightness = state.brightness;
    state.scenes[slot].effect = state.effect;

    storageMarkDirty();
    Serial.printf("[SCENES] Saved scene to slot %u (mode=%u, brightness=%u, effect=%u)\n",
                  slot, state.mode, state.brightness, state.effect.base);
    return true;
}

bool loadScene(uint8_t slot) {
    if (slot >= 8) return false;
    if (!state.scenes[slot].isValid) {
        Serial.printf("[SCENES] Cannot load empty scene slot %u\n", slot);
        return false;
    }

    state.mode = state.scenes[slot].mode;
    state.color = state.scenes[slot].color;
    state.brightness = state.scenes[slot].brightness;
    state.effect = state.scenes[slot].effect;

    matrixSetBrightness(state.brightness);
    storageMarkDirty();
    Serial.printf("[SCENES] Loaded scene from slot %u (mode=%u, brightness=%u)\n",
                  slot, state.mode, state.brightness);
    return true;
}

// -------------------------------------------------------------
// Schedules (4 slots)
// -------------------------------------------------------------
bool setSchedule(uint8_t slot, uint8_t enabled, uint8_t dayMask, uint8_t hh, uint8_t mm, uint8_t action, uint8_t arg) {
    if (slot >= 4) return false;

    state.schedules[slot].enabled = enabled;
    state.schedules[slot].dayMask = dayMask;
    state.schedules[slot].hh = hh;
    state.schedules[slot].mm = mm;
    state.schedules[slot].action = action;
    state.schedules[slot].arg = arg;

    storageMarkDirty();
    Serial.printf("[SCHED] Slot %u set: enabled=%u, days=0x%02X, time=%02u:%02u, action=%u, arg=%u\n",
                  slot, enabled, dayMask, hh, mm, action, arg);
    return true;
}

static void startSunriseAlarm(uint8_t durationMinutes) {
    isSunriseRunning = true;
    sunriseStartMs = millis();
    sunriseDurationMs = (durationMinutes == 0 ? 15 : durationMinutes) * 60000UL;
    state.mode = MODE_EFFECT; // Sunrise operates visually
    Serial.printf("[SCHED] Sunrise alarm started: %lu ms\n", (unsigned long)sunriseDurationMs);
}

bool isSunriseAlarmRunning() {
    return isSunriseRunning;
}

void stopSunriseAlarm() {
    isSunriseRunning = false;
}

static void executeScheduleAction(uint8_t action, uint8_t arg) {
    Serial.printf("[SCHED] Executing schedule action %u (arg=%u)\n", action, arg);
    switch (action) {
        case 0: // ON
            if (state.mode == MODE_OFF) {
                state.mode = MODE_SOLID;
            }
            if (state.brightness == 0) {
                state.brightness = DEFAULT_BRIGHTNESS;
                matrixSetBrightness(state.brightness);
            }
            break;

        case 1: // OFF
            state.mode = MODE_OFF;
            matrixClear();
            break;

        case 2: // LOAD_SCENE
            loadScene(arg);
            break;

        case 3: // SUNRISE_ALARM
            startSunriseAlarm(arg);
            break;

        default:
            break;
    }
}

void schedulerLoop(uint32_t now) {
    // Schedules pause until valid time is synced (per SPEC)
    if (!isTimeSynced()) return;

    time_t localEpoch = time(nullptr) + (state.tzOffsetMinutes * 60);
    struct tm* tmLocal = gmtime(&localEpoch);
    if (!tmLocal) return;

    static int lastCheckedMinute = -1;
    if (tmLocal->tm_min != lastCheckedMinute) {
        lastCheckedMinute = tmLocal->tm_min;

        for (int i = 0; i < 4; i++) {
            if (!state.schedules[i].enabled) continue;

            // dayMask bit 0: Sunday, bit 1: Monday, ... bit 6: Saturday
            if ((state.schedules[i].dayMask & (1 << tmLocal->tm_wday)) != 0) {
                if (state.schedules[i].hh == tmLocal->tm_hour &&
                    state.schedules[i].mm == tmLocal->tm_min) {
                    executeScheduleAction(state.schedules[i].action, state.schedules[i].arg);
                }
            }
        }
    }

    // Sunrise progression update if active
    if (isSunriseRunning) {
        uint32_t elapsed = now - sunriseStartMs;
        if (elapsed >= sunriseDurationMs) {
            isSunriseRunning = false;
            // Leave on warm daylight ambient
            state.color = CRGB(255, 220, 180);
            state.brightness = MAX_BRIGHTNESS;
            matrixSetBrightness(state.brightness);
        } else {
            float progress = (float)elapsed / (float)sunriseDurationMs;
            // Warm progression: deep red -> orange -> golden daylight
            CRGB sunriseColor = blend(CRGB(255, 30, 0), CRGB(255, 230, 180), (uint8_t)(progress * 255));
            state.color = sunriseColor;
            uint8_t currentB = 5 + (uint8_t)(progress * (MAX_BRIGHTNESS - 5));
            matrixSetBrightness(currentB);
        }
    }
}

// -------------------------------------------------------------
// Sleep Timer
// -------------------------------------------------------------
void startSleepTimer(uint16_t minutes) {
    if (minutes == 0) {
        cancelSleepTimer();
        return;
    }
    isSleepRunning = true;
    sleepStartMs = millis();
    sleepDurationMs = (uint32_t)minutes * 60000UL;
    sleepSavedBrightness = state.brightness;
    Serial.printf("[SLEEP] Timer started for %u minutes\n", minutes);
}

void cancelSleepTimer() {
    if (isSleepRunning) {
        isSleepRunning = false;
        matrixSetBrightness(sleepSavedBrightness);
        state.brightness = sleepSavedBrightness;
        Serial.println("[SLEEP] Timer cancelled. Restored brightness.");
    }
}

bool isSleepTimerRunning() {
    return isSleepRunning;
}

uint16_t getSleepTimerRemainingMinutes() {
    if (!isSleepRunning) return 0;
    uint32_t elapsed = millis() - sleepStartMs;
    if (elapsed >= sleepDurationMs) return 0;
    return (uint16_t)((sleepDurationMs - elapsed + 59999UL) / 60000UL);
}

void sleepTimerLoop(uint32_t now) {
    if (!isSleepRunning) return;

    uint32_t elapsed = now - sleepStartMs;
    if (elapsed >= sleepDurationMs) {
        isSleepRunning = false;
        state.mode = MODE_OFF;
        // Restore brightness setting for next power-on
        state.brightness = sleepSavedBrightness;
        matrixSetBrightness(sleepSavedBrightness);
        matrixClear();
        Serial.println("[SLEEP] Sleep timer finished: system powered OFF.");
    } else {
        float remainingFrac = 1.0f - ((float)elapsed / (float)sleepDurationMs);
        uint8_t fadedBrightness = (uint8_t)(sleepSavedBrightness * remainingFrac);
        matrixSetBrightness(fadedBrightness);
    }
}

// -------------------------------------------------------------
// Draw Canvas (with Bounds Checks)
// -------------------------------------------------------------
bool drawChunk(uint16_t offset, const uint8_t* chunk, uint8_t len) {
    if (offset >= DRAW_BUFFER_BYTES || len == 0 || chunk == nullptr) {
        return false;
    }

    uint8_t copyLen = len;
    if ((uint32_t)offset + copyLen > DRAW_BUFFER_BYTES) {
        copyLen = DRAW_BUFFER_BYTES - offset;
    }

    memcpy(&stagedDraw[offset], chunk, copyLen);
    return true;
}

void drawCommit() {
    memcpy(activeDraw, stagedDraw, DRAW_BUFFER_BYTES);
    state.mode = MODE_DRAW;
    Serial.println("[DRAW] Frame committed. Mode set to DRAW.");
}

void renderDraw(CRGB* leds) {
    for (int i = 0; i < NUM_LEDS; i++) {
        int idx = i * 3;
        leds[i] = CRGB(activeDraw[idx], activeDraw[idx + 1], activeDraw[idx + 2]);
    }
}

// -------------------------------------------------------------
// Music Visualizer (Smoothing + Timeout)
// -------------------------------------------------------------
void feedMusicLevels(const uint8_t* levels, uint8_t count) {
    lastMusicTimeMs = millis();

    if (state.mode != MODE_MUSIC) {
        modeBeforeMusic = state.mode;
        state.mode = MODE_MUSIC;
    }

    // Map input levels (e.g. 9 or 10 bytes) to 10 matrix columns
    for (int x = 0; x < MATRIX_WIDTH; x++) {
        uint8_t srcIdx = (x * count) / MATRIX_WIDTH;
        if (srcIdx >= count) srcIdx = count - 1;
        targetMusicLevels[x] = levels[srcIdx];
    }
}

void musicLoop(uint32_t now) {
    if (state.mode != MODE_MUSIC) return;

    // Timeout: if no packets arrive within 2500ms, revert to previous mode
    if (now - lastMusicTimeMs > 2500) {
        Serial.println("[MUSIC] Packet stream stopped. Returning to previous mode.");
        state.mode = modeBeforeMusic;
        return;
    }

    // Smooth falloff per frame
    const float decayStep = 8.0f;
    for (int x = 0; x < MATRIX_WIDTH; x++) {
        float target = (float)targetMusicLevels[x];
        if (target >= currentMusicLevels[x]) {
            currentMusicLevels[x] = target; // Instant fast attack
        } else {
            if (currentMusicLevels[x] > decayStep) {
                currentMusicLevels[x] -= decayStep;
            } else {
                currentMusicLevels[x] = 0.0f;
            }
        }
    }
}

void renderMusic(uint32_t now, CRGB* leds) {
    matrixClear();

    for (int x = 0; x < MATRIX_WIDTH; x++) {
        int barHeight = (int)((currentMusicLevels[x] * MATRIX_HEIGHT) / 255.0f);
        if (barHeight > MATRIX_HEIGHT) barHeight = MATRIX_HEIGHT;

        for (int y = 0; y < barHeight; y++) {
            // y=0 is bottom row (row 9), y=9 is top row (row 0)
            int displayRow = MATRIX_HEIGHT - 1 - y;
            uint16_t idx = XY(x, displayRow);
            if (idx < NUM_LEDS) {
                CRGB col;
                if (y < 4) {
                    col = CRGB(0, 255, 60);    // Green base
                } else if (y < 7) {
                    col = CRGB(255, 200, 0);   // Yellow mid
                } else {
                    col = CRGB(255, 30, 40);   // Red peak
                }
                leds[idx] = col;
            }
        }
    }
}

