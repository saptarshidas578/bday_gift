#pragma once

#include <Arduino.h>
#include <FastLED.h>
#include "config.h"

// Effect parameter descriptor (mirrors protocol.json SET_EFFECT opcode)
struct EffectParams {
    uint8_t base;       // Effect ID (1..10)
    uint8_t speed;      // 0..255 (relative speed)
    uint8_t dir;        // 0: default, 1: reverse/alt, 2: vertical, 3: diagonal
    uint8_t mirror;     // 0: standard, 1: mirrored across center
    uint8_t colorMode;  // 0: c1, 1: c1-c2 blend, 2: palette, 3: rainbow
    CRGB c1;            // Primary color
    CRGB c2;            // Secondary color
};

typedef void (*EffectRenderFn)(uint32_t t_ms, const EffectParams& params, CRGB* leds);

struct EffectEntry {
    uint8_t id;
    const char* name;
    EffectRenderFn render;
};

// Initialize the effect engine and register the first 10 base effects
void effectsInit();

// Lookup registered effect by ID
const EffectEntry* getEffectById(uint8_t id);

// Lookup registered effect by index (0..count-1)
const EffectEntry* getEffectByIndex(size_t index);

// Total registered effects count
size_t getEffectCount();

// Render the requested effect to the matrix
void effectsRender(uint32_t t_ms, const EffectParams& params, CRGB* leds);

// The first 10 built-in effect renderers:
void renderSolid(uint32_t t_ms, const EffectParams& params, CRGB* leds);
void renderBreathing(uint32_t t_ms, const EffectParams& params, CRGB* leds);
void renderPulse(uint32_t t_ms, const EffectParams& params, CRGB* leds);
void renderHeartbeat(uint32_t t_ms, const EffectParams& params, CRGB* leds);
void renderCandle(uint32_t t_ms, const EffectParams& params, CRGB* leds);
void renderRainbowWave(uint32_t t_ms, const EffectParams& params, CRGB* leds);
void renderRainbowCycle(uint32_t t_ms, const EffectParams& params, CRGB* leds);
void renderScanner(uint32_t t_ms, const EffectParams& params, CRGB* leds);
void renderSparkle(uint32_t t_ms, const EffectParams& params, CRGB* leds);
void renderFire(uint32_t t_ms, const EffectParams& params, CRGB* leds);

