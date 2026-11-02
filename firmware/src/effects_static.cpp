#include "effects.h"
#include "matrix.h"

// 1. Solid
void renderSolid(uint32_t t_ms, const EffectParams& params, CRGB* leds) {
    CRGB col = (params.c1 == CRGB(0,0,0)) ? CRGB::White : params.c1;
    if (params.colorMode == 3) {
        uint8_t spd = params.speed ? params.speed : 50;
        col = CHSV((t_ms * spd / 40) & 0xFF, 240, 255);
    }
    for (int i = 0; i < NUM_LEDS; i++) leds[i] = col;
}

// 2. Breathing
void renderBreathing(uint32_t t_ms, const EffectParams& params, CRGB* leds) {
    uint8_t spd = params.speed ? params.speed : 50;
    uint8_t breath = (sin8((uint8_t)(t_ms * spd / 300)) / 2) + 30;
    CRGB col = (params.c1 == CRGB(0,0,0)) ? CRGB(255, 60, 180) : params.c1;
    if (params.colorMode == 1) {
        col = blend(params.c1, params.c2, sin8((uint8_t)(t_ms * spd / 500)));
    } else if (params.colorMode == 3) {
        col = CHSV((t_ms * spd / 50) & 0xFF, 240, 255);
    }
    col.nscale8_video(breath);
    for (int i = 0; i < NUM_LEDS; i++) leds[i] = col;
}

// 3. Pulse
void renderPulse(uint32_t t_ms, const EffectParams& params, CRGB* leds) {
    uint8_t spd = params.speed ? params.speed : 60;
    uint16_t period = 12000 / spd;
    if (period == 0) period = 100;
    uint16_t phase = t_ms % period;
    uint8_t bright = (phase < period / 5) ? map(phase, 0, period / 5, 20, 255)
                                          : map(phase, period / 5, period, 255, 10);
    CRGB col = (params.c1 == CRGB(0,0,0)) ? CRGB::Cyan : params.c1;
    if (params.colorMode == 3) {
        col = CHSV((t_ms * spd / 60) & 0xFF, 240, 255);
    }
    col.nscale8_video(bright);
    for (int i = 0; i < NUM_LEDS; i++) leds[i] = col;
}

// 4. Heartbeat
void renderHeartbeat(uint32_t t_ms, const EffectParams& params, CRGB* leds) {
    uint8_t spd = params.speed ? params.speed : 70;
    uint32_t cycle = 120000 / spd;
    if (cycle == 0) cycle = 1000;
    uint32_t pos = t_ms % cycle;
    uint8_t b = 15;
    if (pos < 120) {
        b = sin8(map(pos, 0, 120, 0, 128));
    } else if (pos >= 200 && pos < 380) {
        b = sin8(map(pos, 200, 380, 0, 128));
    }
    b = qadd8(b, 15);
    CRGB col = (params.c1 == CRGB(0,0,0)) ? CRGB::Crimson : params.c1;
    col.nscale8_video(b);
    for (int i = 0; i < NUM_LEDS; i++) leds[i] = col;
}

// 5. Candle
void renderCandle(uint32_t t_ms, const EffectParams& params, CRGB* leds) {
    uint8_t spd = params.speed ? params.speed : 40;
    for (uint8_t y = 0; y < MATRIX_HEIGHT; y++) {
        for (uint8_t x = 0; x < MATRIX_WIDTH; x++) {
            uint8_t noise = inoise8(x * 40, (y * 40) + (t_ms * spd / 20));
            matrixSetPixel(x, y, CRGB(noise, scale8(noise, 140), scale8(noise, 20)));
        }
    }
}

// 11. Strobe-Soft
void renderStrobeSoft(uint32_t t_ms, const EffectParams& params, CRGB* leds) {
    uint8_t spd = params.speed ? params.speed : 80;
    uint16_t period = 6000 / spd;
    if (period == 0) period = 50;
    uint16_t phase = t_ms % period;
    uint8_t b = (phase < period / 3) ? map(phase, 0, period / 3, 255, 0) : 0;
    CRGB col = (params.c1 == CRGB(0,0,0)) ? CRGB::White : params.c1;
    col.nscale8_video(b);
    for (int i = 0; i < NUM_LEDS; i++) leds[i] = col;
}

// 12. Fade-Two
void renderFadeTwo(uint32_t t_ms, const EffectParams& params, CRGB* leds) {
    uint8_t spd = params.speed ? params.speed : 40;
    uint8_t ratio = sin8((uint8_t)(t_ms * spd / 400));
    CRGB c1 = (params.c1 == CRGB(0,0,0)) ? CRGB(255, 40, 100) : params.c1;
    CRGB c2 = (params.c2 == CRGB(0,0,0)) ? CRGB(0, 180, 255) : params.c2;
    CRGB col = blend(c1, c2, ratio);
    for (int i = 0; i < NUM_LEDS; i++) leds[i] = col;
}

// 13. Flicker
void renderFlicker(uint32_t t_ms, const EffectParams& params, CRGB* leds) {
    uint8_t spd = params.speed ? params.speed : 50;
    CRGB col = (params.c1 == CRGB(0,0,0)) ? CRGB(255, 180, 50) : params.c1;
    for (int i = 0; i < NUM_LEDS; i++) {
        uint8_t n = inoise8(i * 30, (t_ms * spd / 15));
        uint8_t b = map(n, 0, 255, 120, 255);
        CRGB p = col;
        p.nscale8_video(b);
        leds[i] = p;
    }
}
