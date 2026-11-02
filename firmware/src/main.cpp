#include <Arduino.h>
#include <FastLED.h>
#include "config.h"
#include "matrix.h"

enum B02State {
    STATE_CORNERS,
    STATE_ROW_SWEEP,
    STATE_PERIMETER_BOX,
    STATE_BRIGHTNESS_RAMP,
    STATE_PAUSE
};

static B02State currentState = STATE_CORNERS;
static uint32_t stateStartTime = 0;
static uint32_t lastStepTime = 0;
static uint8_t sweepX = 0;
static uint8_t sweepY = 0;
static uint8_t rampStep = 0;

void printB02Banner() {
    Serial.println();
    Serial.println("==================================================");
    Serial.println("  PixelGift Matrix Mapping & Power Cap (Task B02)");
    Serial.println("==================================================");
    Serial.printf("MCU:           ESP32 DevKit\n");
    Serial.printf("Data Pin:      GPIO %d (D5)\n", DATA_PIN);
    Serial.printf("Matrix Size:   %d x %d (%d LEDs)\n", MATRIX_WIDTH, MATRIX_HEIGHT, NUM_LEDS);
    Serial.printf("Layout:        Serpentine: %s\n", MATRIX_SERPENTINE ? "YES" : "NO");
    Serial.printf("Power Cap:     %d V, %d mA limit\n", LED_VOLTS, LED_MAX_MA);
    Serial.printf("Default Bright: %d / 255\n", DEFAULT_BRIGHTNESS);
    Serial.println("--------------------------------------------------");
    Serial.println("VERIFICATION PHASES:");
    Serial.println("1. Corners: (0,0)=Red, (9,0)=Green, (0,9)=Blue, (9,9)=Yellow");
    Serial.println("2. Serpentine Row Sweep: dot travels LEFT->RIGHT on ALL rows");
    Serial.println("3. Perimeter Box: Cyan border around 10x10 edges");
    Serial.println("4. Brightness Ramp: Full panel ramps 20->40->60 safely");
    Serial.println("==================================================");
}

void setup() {
    Serial.begin(115200);
    unsigned long startWait = millis();
    while (!Serial && (millis() - startWait < 1000)) {
        // Wait up to 1 second for serial
    }

    printB02Banner();

    // Initialize matrix with power cap and safe initial brightness
    matrixInit();

    pinMode(BUZZER_PIN, OUTPUT);
    digitalWrite(BUZZER_PIN, LOW);

    stateStartTime = millis();
    lastStepTime = millis();
    currentState = STATE_CORNERS;
    Serial.println("[B02] Phase 1: 4 Corners Check (TopLeft=R, TopRight=G, BottomLeft=B, BottomRight=Y)...");
}

void loop() {
    uint32_t now = millis();

    switch (currentState) {
        case STATE_CORNERS: {
            matrixClear();
            matrixSetPixel(0, 0, CRGB::Red);                                    // Top Left
            matrixSetPixel(MATRIX_WIDTH - 1, 0, CRGB::Green);                   // Top Right
            matrixSetPixel(0, MATRIX_HEIGHT - 1, CRGB::Blue);                   // Bottom Left
            matrixSetPixel(MATRIX_WIDTH - 1, MATRIX_HEIGHT - 1, CRGB::Yellow);  // Bottom Right
            matrixShow();

            if (now - stateStartTime >= 2500) {
                currentState = STATE_ROW_SWEEP;
                stateStartTime = now;
                lastStepTime = now;
                sweepX = 0;
                sweepY = 0;
                matrixClear();
                matrixShow();
                Serial.println("[B02] Phase 2: Serpentine Sweep (dot moves Left-to-Right on EVERY row)...");
            }
            break;
        }

        case STATE_ROW_SWEEP: {
            if (now - lastStepTime >= 40) { // 40ms per pixel
                lastStepTime = now;
                matrixClear();
                matrixSetPixel(sweepX, sweepY, CRGB::White);
                matrixShow();

                sweepX++;
                if (sweepX >= MATRIX_WIDTH) {
                    sweepX = 0;
                    sweepY++;
                    if (sweepY >= MATRIX_HEIGHT) {
                        sweepY = 0;
                        currentState = STATE_PERIMETER_BOX;
                        stateStartTime = now;
                        matrixClear();
                        matrixShow();
                        Serial.println("[B02] Phase 3: Perimeter Box (10x10 border check)...");
                    }
                }
            }
            break;
        }

        case STATE_PERIMETER_BOX: {
            matrixClear();
            for (uint8_t x = 0; x < MATRIX_WIDTH; x++) {
                matrixSetPixel(x, 0, CRGB::Cyan);
                matrixSetPixel(x, MATRIX_HEIGHT - 1, CRGB::Cyan);
            }
            for (uint8_t y = 0; y < MATRIX_HEIGHT; y++) {
                matrixSetPixel(0, y, CRGB::Cyan);
                matrixSetPixel(MATRIX_WIDTH - 1, y, CRGB::Cyan);
            }
            matrixShow();

            if (now - stateStartTime >= 2500) {
                currentState = STATE_BRIGHTNESS_RAMP;
                stateStartTime = now;
                lastStepTime = now;
                rampStep = 0;
                matrixFill(CRGB(255, 140, 20)); // Warm Amber fill
                matrixSetBrightness(20);
                matrixShow();
                Serial.println("[B02] Phase 4: Brightness Ramp (20 -> 40 -> 60 safe power limit)...");
            }
            break;
        }

        case STATE_BRIGHTNESS_RAMP: {
            if (now - lastStepTime >= 500) { // step every 500ms
                lastStepTime = now;
                rampStep++;
                uint8_t levels[] = {20, 35, 50, 65, 50, 35, 20};
                if (rampStep < sizeof(levels) / sizeof(levels[0])) {
                    matrixSetBrightness(levels[rampStep]);
                    matrixFill(CRGB(255, 140, 20));
                    matrixShow();
                    Serial.printf("  Brightness set to: %d / 255\n", levels[rampStep]);
                } else {
                    matrixSetBrightness(DEFAULT_BRIGHTNESS);
                    currentState = STATE_PAUSE;
                    stateStartTime = now;
                    matrixClear();
                    matrixShow();
                    Serial.println("[B02] Cycle complete. Pausing 1s before restarting test loop...");
                }
            }
            break;
        }

        case STATE_PAUSE: {
            if (now - stateStartTime >= 1000) {
                currentState = STATE_CORNERS;
                stateStartTime = now;
                Serial.println("\n[B02] Restarting Phase 1 (Corners)...");
            }
            break;
        }
    }
}
