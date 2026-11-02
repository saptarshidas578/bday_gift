#pragma once

#include <Arduino.h>
#include <FastLED.h>
#include "config.h"

// Effect parameter descriptor (matches protocol.json SET_EFFECT opcode)
struct EffectParams {
    uint8_t base;       // Effect ID (1..66)
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
    const char* category;
    EffectRenderFn render;
};

// Initialize effect registry with all 66 base effects
void effectsInit();

// Lookup registered effect
const EffectEntry* getEffectById(uint8_t id);
const EffectEntry* getEffectByIndex(size_t index);
size_t getEffectCount();

// Render current effect to LED array
void effectsRender(uint32_t t_ms, const EffectParams& params, CRGB* leds);

// --- Category 1: Static / Glow (1-5, 11-13) ---
void renderSolid(uint32_t t_ms, const EffectParams& p, CRGB* leds);
void renderBreathing(uint32_t t_ms, const EffectParams& p, CRGB* leds);
void renderPulse(uint32_t t_ms, const EffectParams& p, CRGB* leds);
void renderHeartbeat(uint32_t t_ms, const EffectParams& p, CRGB* leds);
void renderCandle(uint32_t t_ms, const EffectParams& p, CRGB* leds);
void renderStrobeSoft(uint32_t t_ms, const EffectParams& p, CRGB* leds);
void renderFadeTwo(uint32_t t_ms, const EffectParams& p, CRGB* leds);
void renderFlicker(uint32_t t_ms, const EffectParams& p, CRGB* leds);

// --- Category 2: Rainbow / Gradient (6-7, 14-19) ---
void renderRainbowWave(uint32_t t_ms, const EffectParams& p, CRGB* leds);
void renderRainbowCycle(uint32_t t_ms, const EffectParams& p, CRGB* leds);
void renderDiagonalGradient(uint32_t t_ms, const EffectParams& p, CRGB* leds);
void renderRadialGradient(uint32_t t_ms, const EffectParams& p, CRGB* leds);
void renderAurora(uint32_t t_ms, const EffectParams& p, CRGB* leds);
void renderSunset(uint32_t t_ms, const EffectParams& p, CRGB* leds);
void renderPaletteScroll(uint32_t t_ms, const EffectParams& p, CRGB* leds);
void renderColorWheelSpin(uint32_t t_ms, const EffectParams& p, CRGB* leds);

// --- Category 3: Motion (8, 20-30) ---
void renderScanner(uint32_t t_ms, const EffectParams& p, CRGB* leds);
void renderChase(uint32_t t_ms, const EffectParams& p, CRGB* leds);
void renderWipe(uint32_t t_ms, const EffectParams& p, CRGB* leds);
void renderBounce(uint32_t t_ms, const EffectParams& p, CRGB* leds);
void renderSpiral(uint32_t t_ms, const EffectParams& p, CRGB* leds);
void renderRadar(uint32_t t_ms, const EffectParams& p, CRGB* leds);
void renderRain(uint32_t t_ms, const EffectParams& p, CRGB* leds);
void renderWaterfall(uint32_t t_ms, const EffectParams& p, CRGB* leds);
void renderSnakePath(uint32_t t_ms, const EffectParams& p, CRGB* leds);
void renderComet(uint32_t t_ms, const EffectParams& p, CRGB* leds);
void renderTheaterChase(uint32_t t_ms, const EffectParams& p, CRGB* leds);
void renderRowSweep(uint32_t t_ms, const EffectParams& p, CRGB* leds);

// --- Category 4: Particles (9, 31-39) ---
void renderSparkle(uint32_t t_ms, const EffectParams& p, CRGB* leds);
void renderTwinkle(uint32_t t_ms, const EffectParams& p, CRGB* leds);
void renderFireflies(uint32_t t_ms, const EffectParams& p, CRGB* leds);
void renderFireworks(uint32_t t_ms, const EffectParams& p, CRGB* leds);
void renderSnow(uint32_t t_ms, const EffectParams& p, CRGB* leds);
void renderConfetti(uint32_t t_ms, const EffectParams& p, CRGB* leds);
void renderMeteor(uint32_t t_ms, const EffectParams& p, CRGB* leds);
void renderBubbles(uint32_t t_ms, const EffectParams& p, CRGB* leds);
void renderGlitter(uint32_t t_ms, const EffectParams& p, CRGB* leds);
void renderStardust(uint32_t t_ms, const EffectParams& p, CRGB* leds);

// --- Category 5: Nature / Fire (10, 40-46) ---
void renderFire(uint32_t t_ms, const EffectParams& p, CRGB* leds);
void renderLava(uint32_t t_ms, const EffectParams& p, CRGB* leds);
void renderOcean(uint32_t t_ms, const EffectParams& p, CRGB* leds);
void renderLightning(uint32_t t_ms, const EffectParams& p, CRGB* leds);
void renderNorthernLights(uint32_t t_ms, const EffectParams& p, CRGB* leds);
void renderEmbers(uint32_t t_ms, const EffectParams& p, CRGB* leds);
void renderStorm(uint32_t t_ms, const EffectParams& p, CRGB* leds);
void renderWind(uint32_t t_ms, const EffectParams& p, CRGB* leds);

// --- Category 6: Geometric (47-55) ---
void renderRipple(uint32_t t_ms, const EffectParams& p, CRGB* leds);
void renderExpandingSquares(uint32_t t_ms, const EffectParams& p, CRGB* leds);
void renderPlasma(uint32_t t_ms, const EffectParams& p, CRGB* leds);
void renderKaleidoscope(uint32_t t_ms, const EffectParams& p, CRGB* leds);
void renderTunnel(uint32_t t_ms, const EffectParams& p, CRGB* leds);
void renderChecker(uint32_t t_ms, const EffectParams& p, CRGB* leds);
void renderDiamonds(uint32_t t_ms, const EffectParams& p, CRGB* leds);
void renderPulseRings(uint32_t t_ms, const EffectParams& p, CRGB* leds);
void renderNoiseField(uint32_t t_ms, const EffectParams& p, CRGB* leds);

// --- Category 7: Retro (56-60) ---
void renderMatrixCode(uint32_t t_ms, const EffectParams& p, CRGB* leds);
void renderGameOfLife(uint32_t t_ms, const EffectParams& p, CRGB* leds);
void renderSnakeGameDemo(uint32_t t_ms, const EffectParams& p, CRGB* leds);
void renderPongDemo(uint32_t t_ms, const EffectParams& p, CRGB* leds);
void renderEqualizerDemo(uint32_t t_ms, const EffectParams& p, CRGB* leds);

// --- Category 8: Celebration (61-66) ---
void renderHearts(uint32_t t_ms, const EffectParams& p, CRGB* leds);
void renderBalloons(uint32_t t_ms, const EffectParams& p, CRGB* leds);
void renderCandles(uint32_t t_ms, const EffectParams& p, CRGB* leds);
void renderConfettiBurst(uint32_t t_ms, const EffectParams& p, CRGB* leds);
void renderPartyStrobe(uint32_t t_ms, const EffectParams& p, CRGB* leds);
void renderStarBurst(uint32_t t_ms, const EffectParams& p, CRGB* leds);
