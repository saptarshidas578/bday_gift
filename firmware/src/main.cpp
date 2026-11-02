#include <Arduino.h>
#include <FastLED.h>
#include "config.h"
#include "matrix.h"
#include "effects.h"
#include "storage.h"

static uint32_t lastRenderTime = 0;
static const uint32_t FRAME_INTERVAL_MS = 16; // ~60 FPS

// Demo state machine for Task B05 verification
enum DemoPhase {
    PHASE_BOOT_RESTORE,
    PHASE_RAPID_CHANGES,
    PHASE_WAIT_DEBOUNCE,
    PHASE_NORMAL_RUN
};

static DemoPhase demoPhase = PHASE_BOOT_RESTORE;
static uint32_t phaseStartTime = 0;
static uint8_t changeCount = 0;

void setup() {
    Serial.begin(115200);
    unsigned long startWait = millis();
    while (!Serial && (millis() - startWait < 1000)) {
        // Wait up to 1 second for serial
    }

    Serial.println();
    Serial.println("==================================================");
    Serial.println("  PixelGift NVS Persistence & Debounce (Task B05) ");
    Serial.println("==================================================");

    matrixInit();
    effectsInit();
    storageInit(); // Loads state from NVS

    pinMode(BUZZER_PIN, OUTPUT);
    digitalWrite(BUZZER_PIN, LOW);

    // Apply restored state to hardware
    matrixSetBrightness(state.brightness);

    demoPhase = PHASE_BOOT_RESTORE;
    phaseStartTime = millis();
    lastRenderTime = millis();
    Serial.println("[B05] Initial state active. Beginning debounce verification in 3s...");
}

void loop() {
    uint32_t now = millis();

    // 1. Non-blocking storage debounced flush (runs continuously)
    storageLoop();

    // 2. Automated Task B05 Debounce Demonstration
    switch (demoPhase) {
        case PHASE_BOOT_RESTORE: {
            if (now - phaseStartTime >= 3000) {
                demoPhase = PHASE_RAPID_CHANGES;
                phaseStartTime = now;
                changeCount = 0;
                Serial.println("\n[B05] Simulating rapid parameter adjustments (should NOT write to NVS immediately)...");
            }
            break;
        }

        case PHASE_RAPID_CHANGES: {
            static uint32_t lastChangeTime = 0;
            if (now - lastChangeTime >= 350) { // change every 350ms (faster than 2s debounce)
                lastChangeTime = now;
                changeCount++;

                if (changeCount == 1) {
                    state.brightness = 35;
                    matrixSetBrightness(state.brightness);
                    storageMarkDirty();
                    Serial.println("  [Change 1] Brightness set to 35. storageMarkDirty() called.");
                } else if (changeCount == 2) {
                    state.color = CRGB::Cyan;
                    storageMarkDirty();
                    Serial.println("  [Change 2] Color set to Cyan. storageMarkDirty() called.");
                } else if (changeCount == 3) {
                    state.effect.base = 2; // Breathing
                    state.effect.c1 = CRGB(255, 40, 180);
                    state.effect.speed = 60;
                    storageMarkDirty();
                    Serial.println("  [Change 3] Effect set to Breathing (ID 2). storageMarkDirty() called.");
                } else if (changeCount == 4) {
                    state.mode = MODE_EFFECT;
                    storageMarkDirty();
                    Serial.println("  [Change 4] Mode set to EFFECT. Changes complete. Awaiting 2s debounce window...");
                    demoPhase = PHASE_WAIT_DEBOUNCE;
                    phaseStartTime = now;
                }
            }
            break;
        }

        case PHASE_WAIT_DEBOUNCE: {
            // Exactly 2000 ms after change 4, storageLoop() will trigger and print:
            // "[STORAGE] Persisted debounced state to NVS successfully."
            if (now - phaseStartTime >= 3000) {
                demoPhase = PHASE_NORMAL_RUN;
                Serial.println("[B05] Debounce verification finished! Operating normally in restored mode.\n");
            }
            break;
        }

        case PHASE_NORMAL_RUN: {
            // Normal operation rendering active effect / mode
            break;
        }
    }

    // 3. Render loop (~60 FPS non-blocking)
    if (now - lastRenderTime >= FRAME_INTERVAL_MS) {
        lastRenderTime = now;

        if (state.mode == MODE_SOLID) {
            matrixFill(state.color);
        } else if (state.mode == MODE_EFFECT) {
            effectsRender(now, state.effect, matrixGetLeds());
        } else if (state.mode == MODE_OFF) {
            matrixClear();
        } else {
            // Default render
            effectsRender(now, state.effect, matrixGetLeds());
        }

        matrixShow();
    }
}
