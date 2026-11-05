#include "effects.h"
#include "matrix.h"
#include "storage.h"

// 47. Ripple
void renderRipple(uint32_t t_ms, const EffectParams& params, CRGB* leds) {
    uint8_t spd = params.speed ? params.speed : 50;
    float t = t_ms * spd * 0.005f;
    float cx = 4.5f, cy = 4.5f;

    for (uint8_t y = 0; y < MATRIX_HEIGHT; y++) {
        for (uint8_t x = 0; x < MATRIX_WIDTH; x++) {
            float dist = sqrtf((x - cx) * (x - cx) + (y - cy) * (y - cy));
            uint8_t wave = sin8((uint8_t)((dist * 40.0f) - (t * 40.0f)));
            CRGB col;
            if (params.colorMode == 1) {
                CRGB c1 = (params.c1 == CRGB(0,0,0)) ? CRGB::Cyan : params.c1;
                CRGB c2 = (params.c2 == CRGB(0,0,0)) ? CRGB::DarkBlue : params.c2;
                col = blend(c1, c2, wave);
            } else if (params.colorMode == 2 && state.paletteCount > 0) {
                uint16_t total = (uint16_t)(wave * state.paletteCount);
                uint8_t step = (total >> 8) % state.paletteCount;
                uint8_t nextStep = (step + 1) % state.paletteCount;
                col = blend(state.palette[step], state.palette[nextStep], total & 0xFF);
            } else if (params.colorMode == 3) {
                col = CHSV((uint8_t)(140 + (wave / 4) + (t_ms * spd / 30)), 220, 255);
            } else if (params.c1 != CRGB(0,0,0)) {
                col = params.c1;
            } else {
                col = CHSV(140 + (wave / 4), 220, 255);
            }
            col.nscale8_video(wave);
            matrixSetPixel(x, y, col);
        }
    }
}

// 48. Expanding Squares
void renderExpandingSquares(uint32_t t_ms, const EffectParams& params, CRGB* leds) {
    uint8_t spd = params.speed ? params.speed : 45;
    uint8_t step = (t_ms * spd / 80) % 5;

    for (uint8_t y = 0; y < MATRIX_HEIGHT; y++) {
        for (uint8_t x = 0; x < MATRIX_WIDTH; x++) {
            int8_t dx = abs((int8_t)x - 4);
            int8_t dy = abs((int8_t)y - 4);
            uint8_t d = max(dx, dy);
            CRGB col = (d == step) ? CRGB::Magenta : CRGB::Black;
            matrixSetPixel(x, y, col);
        }
    }
}

// 49. Plasma
void renderPlasma(uint32_t t_ms, const EffectParams& params, CRGB* leds) {
    uint8_t spd = params.speed ? params.speed : 40;
    float t = t_ms * spd * 0.002f;

    for (uint8_t y = 0; y < MATRIX_HEIGHT; y++) {
        for (uint8_t x = 0; x < MATRIX_WIDTH; x++) {
            float v1 = sinf(x * 0.6f + t);
            float v2 = sinf(y * 0.6f + t * 1.2f);
            float v3 = sinf((x + y) * 0.5f + t * 0.8f);
            uint8_t hue = (uint8_t)((v1 + v2 + v3 + 3.0f) * 42.0f);
            CRGB col;
            if (params.colorMode == 1) {
                CRGB c1 = (params.c1 == CRGB(0,0,0)) ? CRGB::Magenta : params.c1;
                CRGB c2 = (params.c2 == CRGB(0,0,0)) ? CRGB::Cyan : params.c2;
                col = blend(c1, c2, sin8(hue));
            } else if (params.colorMode == 2 && state.paletteCount > 0) {
                uint16_t total = (uint16_t)(hue * state.paletteCount);
                uint8_t step = (total >> 8) % state.paletteCount;
                uint8_t nextStep = (step + 1) % state.paletteCount;
                col = blend(state.palette[step], state.palette[nextStep], total & 0xFF);
            } else if (params.colorMode == 0 && params.c1 != CRGB(0,0,0)) {
                col = params.c1;
                col.nscale8_video(sin8(hue));
            } else {
                col = CHSV(hue, 220, 255);
            }
            matrixSetPixel(x, y, col);
        }
    }
}

// 50. Kaleidoscope
void renderKaleidoscope(uint32_t t_ms, const EffectParams& params, CRGB* leds) {
    uint8_t spd = params.speed ? params.speed : 45;
    uint8_t t = (t_ms * spd / 40) & 0xFF;

    for (uint8_t y = 0; y < 5; y++) {
        for (uint8_t x = 0; x < 5; x++) {
            uint8_t hue = inoise8(x * 50, y * 50 + t);
            CRGB col = getEffectColor(params, t_ms, hue, CHSV(hue, 230, 255));
            matrixSetPixel(x, y, col);
            matrixSetPixel(MATRIX_WIDTH - 1 - x, y, col);
            matrixSetPixel(x, MATRIX_HEIGHT - 1 - y, col);
            matrixSetPixel(MATRIX_WIDTH - 1 - x, MATRIX_HEIGHT - 1 - y, col);
        }
    }
}

// 51. Tunnel
void renderTunnel(uint32_t t_ms, const EffectParams& params, CRGB* leds) {
    uint8_t spd = params.speed ? params.speed : 40;
    float cx = 4.5f, cy = 4.5f;
    uint8_t step = (t_ms * spd / 30) & 0xFF;

    for (uint8_t y = 0; y < MATRIX_HEIGHT; y++) {
        for (uint8_t x = 0; x < MATRIX_WIDTH; x++) {
            float d = sqrtf((x - cx) * (x - cx) + (y - cy) * (y - cy));
            uint8_t ring = (uint8_t)(d * 40.0f) - step;
            matrixSetPixel(x, y, CHSV(ring, 240, 255));
        }
    }
}

// 52. Checker
void renderChecker(uint32_t t_ms, const EffectParams& params, CRGB* leds) {
    uint8_t spd = params.speed ? params.speed : 40;
    uint8_t state = (t_ms * spd / 300) & 0x01;
    CRGB c1 = (params.c1 == CRGB(0,0,0)) ? CRGB::White : params.c1;
    CRGB c2 = (params.c2 == CRGB(0,0,0)) ? CRGB::DarkSlateGray : params.c2;

    for (uint8_t y = 0; y < MATRIX_HEIGHT; y++) {
        for (uint8_t x = 0; x < MATRIX_WIDTH; x++) {
            uint8_t isEven = (x + y + state) & 0x01;
            matrixSetPixel(x, y, isEven ? c1 : c2);
        }
    }
}

// 53. Diamonds
void renderDiamonds(uint32_t t_ms, const EffectParams& params, CRGB* leds) {
    uint8_t spd = params.speed ? params.speed : 50;
    uint8_t step = (t_ms * spd / 80) % 9;

    for (uint8_t y = 0; y < MATRIX_HEIGHT; y++) {
        for (uint8_t x = 0; x < MATRIX_WIDTH; x++) {
            int8_t dist = abs((int8_t)x - 4) + abs((int8_t)y - 4);
            CRGB col = (dist == step) ? CRGB::Gold : CRGB::Black;
            matrixSetPixel(x, y, col);
        }
    }
}

// 54. Pulse Rings
void renderPulseRings(uint32_t t_ms, const EffectParams& params, CRGB* leds) {
    uint8_t spd = params.speed ? params.speed : 45;
    for (int i = 0; i < NUM_LEDS; i++) leds[i].nscale8(180);

    float cx = (MATRIX_WIDTH - 1) / 2.0f, cy = (MATRIX_HEIGHT - 1) / 2.0f;
    float maxR = sqrtf(cx * cx + cy * cy) + 1.0f;
    float r = fmodf(t_ms * spd * 0.005f, maxR);

    uint8_t posIdx = (uint8_t)(r * 35.0f);
    CRGB col = getEffectColor(params, t_ms, posIdx, CRGB::Cyan);

    for (uint8_t y = 0; y < MATRIX_HEIGHT; y++) {
        for (uint8_t x = 0; x < MATRIX_WIDTH; x++) {
            float d = sqrtf((x - cx) * (x - cx) + (y - cy) * (y - cy));
            if (fabsf(d - r) < 0.6f) {
                matrixSetPixel(x, y, col);
            }
        }
    }
}

// 55. Noise Field
void renderNoiseField(uint32_t t_ms, const EffectParams& params, CRGB* leds) {
    uint8_t spd = params.speed ? params.speed : 35;
    for (uint8_t y = 0; y < MATRIX_HEIGHT; y++) {
        for (uint8_t x = 0; x < MATRIX_WIDTH; x++) {
            uint8_t n = inoise8(x * 55, y * 55, (t_ms * spd / 20));
            CRGB col = getEffectColor(params, t_ms, n, CHSV(n, 220, 255));
            matrixSetPixel(x, y, col);
        }
    }
}
