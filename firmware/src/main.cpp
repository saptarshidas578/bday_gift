#include <Arduino.h>
#include <FastLED.h>
#include "config.h"

// CRGB array for 81 LEDs (9x9)
CRGB leds[NUM_LEDS];

// Test state machine states
enum TestState {
    STATE_PIXEL_WALK,
    STATE_PAUSE_BEFORE_COLORS,
    STATE_COLOR_RED,
    STATE_COLOR_GREEN,
    STATE_COLOR_BLUE,
    STATE_PAUSE_BEFORE_WALK
};

static TestState currentState = STATE_PIXEL_WALK;
static uint32_t lastStepTime = 0;
static int currentPixel = 0;

// Timing constants (ms)
static const uint32_t PIXEL_WALK_INTERVAL_MS = 60;
static const uint32_t COLOR_DISPLAY_MS = 2000;
static const uint32_t PAUSE_INTERVAL_MS = 500;

void printBanner() {
    Serial.println();
    Serial.println("=================================================");
    Serial.println("  PixelGift Hardware Verification Test (Task B01)");
    Serial.println("=================================================");
    Serial.printf("MCU: ESP32 DevKit (board: esp32dev)\n");
    Serial.printf("LED Data Pin: GPIO %d\n", DATA_PIN);
    Serial.printf("Buzzer Pin:   GPIO %d\n", BUZZER_PIN);
    Serial.printf("Total LEDs:   %d (9x9 matrix)\n", NUM_LEDS);
    Serial.printf("Power Cap:    %d V, %d mA max\n", LED_VOLTS, LED_MAX_MA);
    Serial.printf("Brightness:   %d / 255 (Safe low level)\n", DEFAULT_BRIGHTNESS);
    Serial.println("-------------------------------------------------");
    Serial.println("TEST SEQUENCE:");
    Serial.println("1. Pixel walk: index 0 -> 80 (watch serpentine path)");
    Serial.println("2. Solid RED   (verify if true red or green/blue)");
    Serial.println("3. Solid GREEN (verify if true green or red/blue)");
    Serial.println("4. Solid BLUE  (verify if true blue)");
    Serial.println("=================================================");
}

void setup() {
    Serial.begin(115200);
    // Brief settle time for serial
    unsigned long startWait = millis();
    while (!Serial && (millis() - startWait < 1000)) {
        // wait up to 1 second
    }

    printBanner();

    // FastLED setup with hardware current cap & safe low brightness
    FastLED.addLeds<WS2812B, DATA_PIN, COLOR_ORDER>(leds, NUM_LEDS);
    FastLED.setMaxPowerInVoltsAndMilliamps(LED_VOLTS, LED_MAX_MA);
    FastLED.setBrightness(DEFAULT_BRIGHTNESS);
    FastLED.clear();
    FastLED.show();

    // Configure buzzer pin as output and silent
    pinMode(BUZZER_PIN, OUTPUT);
    digitalWrite(BUZZER_PIN, LOW);

    lastStepTime = millis();
    currentPixel = 0;
    currentState = STATE_PIXEL_WALK;
    Serial.println("[START] Beginning Pixel Walk...");
}

void loop() {
    uint32_t now = millis();

    switch (currentState) {
        case STATE_PIXEL_WALK: {
            if (now - lastStepTime >= PIXEL_WALK_INTERVAL_MS) {
                lastStepTime = now;

                // Clear previous pixel
                FastLED.clear();

                // Light current pixel with a distinct color (white / warm)
                if (currentPixel < NUM_LEDS) {
                    leds[currentPixel] = CRGB(200, 200, 255);
                    FastLED.show();

                    // Print milestone every row (every 9 pixels)
                    if (currentPixel % 9 == 0) {
                        Serial.printf("  Pixel row %d (index %d..%d)\n", currentPixel / 9, currentPixel, currentPixel + 8);
                    }

                    currentPixel++;
                } else {
                    currentPixel = 0;
                    currentState = STATE_PAUSE_BEFORE_COLORS;
                    lastStepTime = now;
                    FastLED.clear();
                    FastLED.show();
                    Serial.println("[WALK COMPLETE] Pausing before color tests...");
                }
            }
            break;
        }

        case STATE_PAUSE_BEFORE_COLORS: {
            if (now - lastStepTime >= PAUSE_INTERVAL_MS) {
                currentState = STATE_COLOR_RED;
                lastStepTime = now;
                fill_solid(leds, NUM_LEDS, CRGB::Red);
                FastLED.show();
                Serial.println("[COLOR CHECK] Solid RED -> Check: Are all 81 LEDs pure RED?");
            }
            break;
        }

        case STATE_COLOR_RED: {
            if (now - lastStepTime >= COLOR_DISPLAY_MS) {
                currentState = STATE_COLOR_GREEN;
                lastStepTime = now;
                fill_solid(leds, NUM_LEDS, CRGB::Green);
                FastLED.show();
                Serial.println("[COLOR CHECK] Solid GREEN -> Check: Are all 81 LEDs pure GREEN?");
            }
            break;
        }

        case STATE_COLOR_GREEN: {
            if (now - lastStepTime >= COLOR_DISPLAY_MS) {
                currentState = STATE_COLOR_BLUE;
                lastStepTime = now;
                fill_solid(leds, NUM_LEDS, CRGB::Blue);
                FastLED.show();
                Serial.println("[COLOR CHECK] Solid BLUE -> Check: Are all 81 LEDs pure BLUE?");
            }
            break;
        }

        case STATE_COLOR_BLUE: {
            if (now - lastStepTime >= COLOR_DISPLAY_MS) {
                currentState = STATE_PAUSE_BEFORE_WALK;
                lastStepTime = now;
                FastLED.clear();
                FastLED.show();
                Serial.println("[COLOR CHECK COMPLETE] Pausing before next walk cycle...");
            }
            break;
        }

        case STATE_PAUSE_BEFORE_WALK: {
            if (now - lastStepTime >= PAUSE_INTERVAL_MS) {
                currentState = STATE_PIXEL_WALK;
                lastStepTime = now;
                currentPixel = 0;
                Serial.println("\n[CYCLE RESTART] Beginning Pixel Walk...");
            }
            break;
        }
    }
}
