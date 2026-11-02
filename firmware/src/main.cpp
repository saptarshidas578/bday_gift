#include <Arduino.h>
#include <FastLED.h>
#include "config.h"
#include "matrix.h"
#include "effects.h"

static EffectParams currentParams;
static uint32_t effectStartTime = 0;
static uint32_t lastRenderTime = 0;
static size_t currentEffectIndex = 0;

static const uint32_t EFFECT_DURATION_MS = 5000;
static const uint32_t FRAME_INTERVAL_MS = 16; // ~60 FPS

void loadEffectByIndex(size_t index) {
    const EffectEntry* entry = getEffectByIndex(index);
    if (!entry) return;

    currentParams.base = entry->id;
    currentParams.speed = 50;
    currentParams.dir = 0;
    currentParams.mirror = 0;
    currentParams.colorMode = 0;
    currentParams.c1 = CRGB(255, 60, 180); // Vibrant Pink default
    currentParams.c2 = CRGB(0, 200, 255);  // Cyan default

    // Effect-specific custom parameter tuning for demo
    if (entry->id == 1) { // Solid
        currentParams.colorMode = 3; // Rainbow cycling solid
    } else if (entry->id == 4) { // Heartbeat
        currentParams.c1 = CRGB::Crimson;
    } else if (entry->id == 6) { // Rainbow Wave
        currentParams.speed = 60;
        currentParams.colorMode = 3;
    } else if (entry->id == 7) { // Rainbow Cycle
        currentParams.speed = 50;
        currentParams.colorMode = 3;
    } else if (entry->id == 8) { // Scanner
        currentParams.c1 = CRGB::Red;
        currentParams.speed = 70;
    } else if (entry->id == 9) { // Sparkle
        currentParams.colorMode = 3;
        currentParams.speed = 65;
    } else if (entry->id == 10) { // Fire
        currentParams.speed = 65;
    }

    Serial.printf("\n[B03 Effect %u/%u] Running: \"%s\" (ID %u)...\n",
                  (unsigned int)(index + 1),
                  (unsigned int)getEffectCount(),
                  entry->name,
                  entry->id);
}

void setup() {
    Serial.begin(115200);
    unsigned long startWait = millis();
    while (!Serial && (millis() - startWait < 1000)) {
        // Wait up to 1 second for serial
    }

    Serial.println();
    Serial.println("==================================================");
    Serial.println("  PixelGift Effect Engine - First 10 Effects (B03)");
    Serial.println("==================================================");
    Serial.printf("Matrix: %dx%d (%d LEDs), Pin D5 (GPIO %d)\n", MATRIX_WIDTH, MATRIX_HEIGHT, NUM_LEDS, DATA_PIN);
    Serial.printf("Power limit: %d V, %d mA, Brightness: %d/255\n", LED_VOLTS, LED_MAX_MA, DEFAULT_BRIGHTNESS);
    Serial.println("--------------------------------------------------");

    matrixInit();
    effectsInit();

    pinMode(BUZZER_PIN, OUTPUT);
    digitalWrite(BUZZER_PIN, LOW);

    currentEffectIndex = 0;
    loadEffectByIndex(currentEffectIndex);
    effectStartTime = millis();
    lastRenderTime = millis();
}

void loop() {
    uint32_t now = millis();

    // Check if it's time to cycle to the next effect
    if (now - effectStartTime >= EFFECT_DURATION_MS) {
        currentEffectIndex = (currentEffectIndex + 1) % getEffectCount();
        loadEffectByIndex(currentEffectIndex);
        effectStartTime = now;
        matrixClear();
    }

    // Render at target ~60 FPS
    if (now - lastRenderTime >= FRAME_INTERVAL_MS) {
        lastRenderTime = now;
        effectsRender(now, currentParams, matrixGetLeds());
        matrixShow();
    }
}
