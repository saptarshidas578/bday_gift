#include "effects.h"
#include "matrix.h"

// 9. Sparkle
void renderSparkle(uint32_t t_ms, const EffectParams& params, CRGB* leds) {
    uint8_t spd = params.speed ? params.speed : 55;
    for (int i = 0; i < NUM_LEDS; i++) leds[i].nscale8(180);

    uint8_t count = map(spd, 0, 255, 1, 4);
    for (uint8_t c = 0; c < count; c++) {
        uint8_t rx = random8(MATRIX_WIDTH), ry = random8(MATRIX_HEIGHT);
        CRGB col = getEffectColor(params, t_ms, random8(), CRGB::White);
        matrixSetPixel(rx, ry, col);
    }
}

// 31. Twinkle
void renderTwinkle(uint32_t t_ms, const EffectParams& params, CRGB* leds) {
    uint8_t spd = params.speed ? params.speed : 40;
    for (uint8_t y = 0; y < MATRIX_HEIGHT; y++) {
        for (uint8_t x = 0; x < MATRIX_WIDTH; x++) {
            uint8_t n = inoise8(x * 90, (y * 90) + (t_ms * spd / 50));
            uint8_t b = (n > 180) ? map(n, 180, 255, 0, 255) : 0;
            CRGB col = (params.c1 == CRGB(0,0,0)) ? CRGB::Gold : params.c1;
            col.nscale8_video(b);
            matrixSetPixel(x, y, col);
        }
    }
}

// 32. Fireflies
void renderFireflies(uint32_t t_ms, const EffectParams& params, CRGB* leds) {
    uint8_t spd = params.speed ? params.speed : 40;
    for (int i = 0; i < NUM_LEDS; i++) leds[i].nscale8(190);

    float t = t_ms * spd * 0.001f;
    for (uint8_t f = 0; f < 4; f++) {
        int8_t x = (int8_t)(4.5f + sinf(t * 1.2f + f * 1.8f) * 3.8f);
        int8_t y = (int8_t)(4.5f + cosf(t * 0.9f + f * 2.3f) * 3.8f);
        if (x >= 0 && x < MATRIX_WIDTH && y >= 0 && y < MATRIX_HEIGHT) {
            uint8_t glow = sin8((uint8_t)(t * 100 + f * 60));
            CRGB col = CRGB(glow, glow, 20);
            matrixSetPixel(x, y, col);
        }
    }
}

// 33. Fireworks
void renderFireworks(uint32_t t_ms, const EffectParams& params, CRGB* leds) {
    uint8_t spd = params.speed ? params.speed : 50;
    for (int i = 0; i < NUM_LEDS; i++) leds[i].nscale8(180);

    uint16_t cycle = 2000;
    uint16_t phase = (t_ms * spd / 40) % cycle;
    if (phase < 600) {
        // Rocket rising
        uint8_t y = map(phase, 0, 600, MATRIX_HEIGHT - 1, 3);
        matrixSetPixel(4, y, CRGB::White);
    } else {
        // Explosion burst
        float r = map(phase, 600, cycle, 0, 45) / 10.0f;
        uint8_t fade = map(phase, 600, cycle, 255, 0);
        for (uint8_t a = 0; a < 8; a++) {
            float ang = a * 0.7853f;
            int8_t x = (int8_t)(4.5f + cosf(ang) * r);
            int8_t y = (int8_t)(3.5f + sinf(ang) * r);
            if (x >= 0 && x < MATRIX_WIDTH && y >= 0 && y < MATRIX_HEIGHT) {
                CRGB col = CHSV(a * 32, 220, fade);
                matrixSetPixel(x, y, col);
            }
        }
    }
}

// 34. Snow
void renderSnow(uint32_t t_ms, const EffectParams& params, CRGB* leds) {
    uint8_t spd = params.speed ? params.speed : 40;
    for (int i = 0; i < NUM_LEDS; i++) leds[i].nscale8(160);

    static uint32_t lastFlake = 0;
    if (t_ms - lastFlake >= (2000 / spd)) {
        lastFlake = t_ms;
        matrixSetPixel(random8(MATRIX_WIDTH), 0, CRGB(200, 220, 255));
    }
    // Drift down slowly
    static uint32_t lastMove = 0;
    if (t_ms - lastMove >= (1500 / spd)) {
        lastMove = t_ms;
        for (int y = MATRIX_HEIGHT - 1; y > 0; y--) {
            for (uint8_t x = 0; x < MATRIX_WIDTH; x++) {
                CRGB c = matrixGetPixel(x, y - 1);
                if (c.r > 20 || c.g > 20 || c.b > 20) {
                    matrixSetPixel(x, y, c);
                    matrixSetPixel(x, y - 1, CRGB::Black);
                }
            }
        }
    }
}

// 35. Confetti
void renderConfetti(uint32_t t_ms, const EffectParams& params, CRGB* leds) {
    for (int i = 0; i < NUM_LEDS; i++) leds[i].nscale8(190);

    if (random8() < 90) {
        matrixSetPixel(random8(MATRIX_WIDTH), random8(MATRIX_HEIGHT), CHSV(random8(), 200, 255));
    }
}

// 36. Meteor
void renderMeteor(uint32_t t_ms, const EffectParams& params, CRGB* leds) {
    uint8_t spd = params.speed ? params.speed : 60;
    for (int i = 0; i < NUM_LEDS; i++) leds[i].nscale8(175);

    uint16_t step = (t_ms * spd / 35) % (MATRIX_WIDTH + MATRIX_HEIGHT + 4);
    for (uint8_t k = 0; k < 3; k++) {
        int8_t x = step - k;
        int8_t y = step - k;
        if (x >= 0 && x < MATRIX_WIDTH && y >= 0 && y < MATRIX_HEIGHT) {
            matrixSetPixel(x, y, (k == 0) ? CRGB::White : CRGB(0, 180, 255));
        }
    }
}

// 37. Bubbles
void renderBubbles(uint32_t t_ms, const EffectParams& params, CRGB* leds) {
    uint8_t spd = params.speed ? params.speed : 40;
    for (int i = 0; i < NUM_LEDS; i++) leds[i].nscale8(180);

    float t = t_ms * spd * 0.002f;
    for (uint8_t b = 0; b < 3; b++) {
        int8_t x = (int8_t)(3.0f + b * 2.5f + sinf(t + b) * 1.2f);
        int8_t y = (int8_t)(MATRIX_HEIGHT - 1 - fmodf(t * 2.0f + b * 3.0f, (float)MATRIX_HEIGHT));
        if (x >= 0 && x < MATRIX_WIDTH && y >= 0 && y < MATRIX_HEIGHT) {
            matrixSetPixel(x, y, CRGB(100, 200, 255));
        }
    }
}

// 38. Glitter
void renderGlitter(uint32_t t_ms, const EffectParams& params, CRGB* leds) {
    CRGB baseCol = (params.c1 == CRGB(0,0,0)) ? CRGB(120, 20, 80) : params.c1;
    fill_solid(leds, NUM_LEDS, baseCol);

    if (random8() < 120) {
        matrixSetPixel(random8(MATRIX_WIDTH), random8(MATRIX_HEIGHT), CRGB::White);
    }
}

// 39. Stardust
void renderStardust(uint32_t t_ms, const EffectParams& params, CRGB* leds) {
    uint8_t spd = params.speed ? params.speed : 45;
    for (int i = 0; i < NUM_LEDS; i++) leds[i].nscale8(185);

    for (uint8_t s = 0; s < 2; s++) {
        uint8_t rx = random8(MATRIX_WIDTH);
        uint8_t ry = random8(MATRIX_HEIGHT);
        uint8_t hue = (t_ms * spd / 60 + s * 40) & 0xFF;
        matrixSetPixel(rx, ry, CHSV(hue, 180, 255));
    }
}
