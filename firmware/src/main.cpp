#include <Arduino.h>
#include <FastLED.h>
#include "config.h"
#include "matrix.h"
#include "effects.h"

static EffectParams currentParams;
static uint32_t effectStartTime = 0;
static uint32_t lastRenderTime = 0;
static size_t currentEffectIndex = 0;

static const uint32_t EFFECT_DURATION_MS = 4000; // 4 seconds per effect
static const uint32_t FRAME_INTERVAL_MS = 16;   // ~60 FPS

void loadEffectByIndex(size_t index) {
    const EffectEntry* entry = getEffectByIndex(index);
    if (!entry) return;

    currentParams.base = entry->id;
    currentParams.speed = 55;
    currentParams.dir = 0;
    currentParams.mirror = 0;
    currentParams.colorMode = 0;
    currentParams.c1 = CRGB(255, 60, 180); // Neon Pink
    currentParams.c2 = CRGB(0, 200, 255);  // Electric Blue

    // Tailored defaults for visual demo
    if (entry->id == 1) currentParams.colorMode = 3;  // Rainbow solid
    if (entry->id == 4) currentParams.c1 = CRGB::Crimson;
    if (entry->id == 6 || entry->id == 7) currentParams.colorMode = 3;
    if (entry->id == 8) currentParams.c1 = CRGB::Red;
    if (entry->id == 9) currentParams.colorMode = 3;
    if (entry->id == 10) currentParams.speed = 65;

    Serial.printf("\n[Effect %u/%u] \"%s\" (ID %u, Category: %s)\n",
                  (unsigned int)(index + 1),
                  (unsigned int)getEffectCount(),
                  entry->name,
                  entry->id,
                  entry->category);
}

void setup() {
    Serial.begin(115200);
    unsigned long startWait = millis();
    while (!Serial && (millis() - startWait < 1000)) {
        // Wait up to 1 second for serial
    }

    Serial.println();
    Serial.println("==================================================");
    Serial.println("  PixelGift 66-Effect Engine Catalog (Task B04)  ");
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

    // Auto-cycle to next effect every 4 seconds
    if (now - effectStartTime >= EFFECT_DURATION_MS) {
        currentEffectIndex = (currentEffectIndex + 1) % getEffectCount();
        loadEffectByIndex(currentEffectIndex);
        effectStartTime = now;
        matrixClear();
    }

    // Smooth ~60 FPS render
    if (now - lastRenderTime >= FRAME_INTERVAL_MS) {
        lastRenderTime = now;
        effectsRender(now, currentParams, matrixGetLeds());
        matrixShow();
    }
}
