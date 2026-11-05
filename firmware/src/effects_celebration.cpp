#include "effects.h"
#include "storage.h"
#include "matrix.h"

// 61. Hearts
void renderHearts(uint32_t t_ms, const EffectParams& params, CRGB* leds) {
    uint8_t spd = params.speed ? params.speed : 50;
    uint8_t b = (sin8((uint8_t)(t_ms * spd / 200)) / 2) + 120;

    // 10x10 heart mask
    static const uint8_t heartMask[10][10] = {
        {0,0,0,0,0,0,0,0,0,0},
        {0,1,1,0,0,0,0,1,1,0},
        {1,1,1,1,0,0,1,1,1,1},
        {1,1,1,1,1,1,1,1,1,1},
        {1,1,1,1,1,1,1,1,1,1},
        {0,1,1,1,1,1,1,1,1,0},
        {0,0,1,1,1,1,1,1,0,0},
        {0,0,0,1,1,1,1,0,0,0},
        {0,0,0,0,1,1,0,0,0,0},
        {0,0,0,0,0,0,0,0,0,0}
    };

        CRGB bgCol = CRGB::Black;
    if (params.colorMode == 1 && params.c2 != CRGB(0,0,0)) {
        bgCol = params.c2;
        bgCol.nscale8(50);
    }

    for (uint8_t y = 0; y < MATRIX_HEIGHT; y++) {
        for (uint8_t x = 0; x < MATRIX_WIDTH; x++) {
            if (heartMask[y][x]) {
                uint8_t posIdx = (x * 12 + y * 12);
                CRGB col = getEffectColor(params, t_ms, posIdx, CRGB::Crimson);
                col.nscale8_video(b);
                matrixSetPixel(x, y, col);
            } else {
                matrixSetPixel(x, y, bgCol);
            }
        }
    }
}

// 62. Balloons
void renderBalloons(uint32_t t_ms, const EffectParams& params, CRGB* leds) {
    uint8_t spd = params.speed ? params.speed : 40;
    for (int i = 0; i < NUM_LEDS; i++) leds[i].nscale8(180);

    float t = t_ms * spd * 0.002f;
    static const CRGB bCols[3] = {CRGB::Red, CRGB::DeepSkyBlue, CRGB::Gold};

    for (uint8_t b = 0; b < 3; b++) {
        int8_t x = (int8_t)(2.5f + b * 2.8f + sinf(t * 1.5f + b) * 0.8f);
        int8_t y = (int8_t)(MATRIX_HEIGHT - 1 - fmodf(t * 2.0f + b * 4.0f, (float)(MATRIX_HEIGHT + 2)));
        if (x >= 0 && x < MATRIX_WIDTH && y >= 0 && y < MATRIX_HEIGHT) {
            matrixSetPixel(x, y, bCols[b]);
            if (y + 1 < MATRIX_HEIGHT) matrixSetPixel(x, y + 1, CRGB(60, 60, 60)); // String tail
        }
    }
}

// 63. Candles
void renderCandles(uint32_t t_ms, const EffectParams& params, CRGB* leds) {
    matrixClear();

    // 3 Birthday candles at x=2, x=5, x=8
    static const uint8_t candleX[3] = {2, 5, 8};
    for (uint8_t i = 0; i < 3; i++) {
        uint8_t cx = candleX[i];
        CRGB bodyCol = getEffectColor(params, t_ms, i * 70, CRGB(0, 180, 220));
        // Candle body
        for (uint8_t y = 4; y < MATRIX_HEIGHT; y++) {
            matrixSetPixel(cx, y, bodyCol);
        }
        // Flickering flame at y=2..3
        uint8_t f = inoise8(cx * 40, (t_ms / 15) + (i * 50));
        matrixSetPixel(cx, 3, CRGB(255, scale8(f, 150), 20));
        if (f > 160) matrixSetPixel(cx, 2, CRGB(255, 220, 100));
    }
}

// 64. Confetti Burst
void renderConfettiBurst(uint32_t t_ms, const EffectParams& params, CRGB* leds) {
    uint8_t spd = params.speed ? params.speed : 55;
    for (int i = 0; i < NUM_LEDS; i++) leds[i].nscale8(180);

    float cx = (MATRIX_WIDTH - 1) / 2.0f, cy = (MATRIX_HEIGHT - 1) / 2.0f;
    uint16_t cycle = 1500;
    uint16_t phase = (t_ms * spd / 35) % cycle;
    float r = map(phase, 0, cycle, 0, 55) / 10.0f;

    for (uint8_t k = 0; k < 12; k++) {
        float ang = k * (6.28318f / 12.0f);
        int8_t x = (int8_t)(cx + cosf(ang) * r);
        int8_t y = (int8_t)(cy + sinf(ang) * r);
        if (x >= 0 && x < MATRIX_WIDTH && y >= 0 && y < MATRIX_HEIGHT) {
            CRGB confCol = getEffectColor(params, t_ms, k * 21, CHSV(k * 21, 240, 255));
            matrixSetPixel(x, y, confCol);
        }
    }
}

// 65. Party Strobe
void renderPartyStrobe(uint32_t t_ms, const EffectParams& params, CRGB* leds) {
    uint8_t spd = params.speed ? params.speed : 70;
    uint16_t period = 4000 / spd;
    if (period == 0) period = 60;
    uint16_t step = (t_ms / period) % 6;

    static const CRGB partyColors[6] = {
        CRGB::Red, CRGB::Blue, CRGB::Green, CRGB::Yellow, CRGB::Magenta, CRGB::Cyan
    };
    CRGB strobeCol = getEffectColor(params, t_ms, step * 42, partyColors[step]);
    fill_solid(leds, NUM_LEDS, strobeCol);
}

// 66. Star Burst
void renderStarBurst(uint32_t t_ms, const EffectParams& params, CRGB* leds) {
    uint8_t spd = params.speed ? params.speed : 45;
    float t = t_ms * spd * 0.003f;
    float cx = (MATRIX_WIDTH - 1) / 2.0f, cy = (MATRIX_HEIGHT - 1) / 2.0f;

    for (uint8_t y = 0; y < MATRIX_HEIGHT; y++) {
        for (uint8_t x = 0; x < MATRIX_WIDTH; x++) {
            float dx = x - cx, dy = y - cy;
            float angle = atan2f(dy, dx);
            float dist = sqrtf(dx * dx + dy * dy);
            // 6-pointed star ray equation
            float rays = cosf(6.0f * angle + t);
            if (dist < (2.0f + rays * 1.8f)) {
                uint8_t posIdx = (uint8_t)(t * 40.0f + dist * 25.0f);
                CRGB starCol = getEffectColor(params, t_ms, posIdx, CHSV(posIdx, 220, 255));
                matrixSetPixel(x, y, starCol);
            } else {
                matrixSetPixel(x, y, CRGB::Black);
            }
        }
    }
}
