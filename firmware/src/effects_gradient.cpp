#include "effects.h"
#include "matrix.h"

// 6. Rainbow Wave
void renderRainbowWave(uint32_t t_ms, const EffectParams& params, CRGB* leds) {
    uint8_t spd = params.speed ? params.speed : 50;
    uint8_t offset = (t_ms * spd / 30) & 0xFF;
    if (params.dir == 1) offset = 255 - offset;

    for (uint8_t y = 0; y < MATRIX_HEIGHT; y++) {
        for (uint8_t x = 0; x < MATRIX_WIDTH; x++) {
            uint8_t effX = params.mirror ? ((x < MATRIX_WIDTH / 2) ? x : (MATRIX_WIDTH - 1 - x)) : x;
            uint8_t pos = (params.dir == 2) ? y : effX;
            uint8_t hue = offset + (pos * 25);
            CRGB rainbowCol = CHSV(hue, 240, 255);
            CRGB pixelCol = (params.colorMode == 3) ? rainbowCol : getEffectColor(params, t_ms, pos, rainbowCol);
            matrixSetPixel(x, y, pixelCol);
        }
    }
}

// 7. Rainbow Cycle
void renderRainbowCycle(uint32_t t_ms, const EffectParams& params, CRGB* leds) {
    uint8_t spd = params.speed ? params.speed : 45;
    uint8_t offset = (t_ms * spd / 25) & 0xFF;
    float cx = 4.5f, cy = 4.5f;

    for (uint8_t y = 0; y < MATRIX_HEIGHT; y++) {
        for (uint8_t x = 0; x < MATRIX_WIDTH; x++) {
            float dx = x - cx, dy = y - cy;
            uint8_t dist = (uint8_t)(sqrtf(dx * dx + dy * dy) * 35.0f);
            uint8_t hue = (params.dir == 1) ? (offset + dist) : (offset - dist);
            CRGB rainbowCol = CHSV(hue, 240, 255);
            CRGB pixelCol = (params.colorMode == 3) ? rainbowCol : getEffectColor(params, t_ms, dist / 15, rainbowCol);
            matrixSetPixel(x, y, pixelCol);
        }
    }
}

// 14. Diagonal Gradient
void renderDiagonalGradient(uint32_t t_ms, const EffectParams& params, CRGB* leds) {
    uint8_t spd = params.speed ? params.speed : 50;
    uint8_t shift = (t_ms * spd / 35) & 0xFF;
    CRGB c1 = (params.c1 == CRGB(0,0,0)) ? CRGB::Blue : params.c1;
    CRGB c2 = (params.c2 == CRGB(0,0,0)) ? CRGB::Magenta : params.c2;

    for (uint8_t y = 0; y < MATRIX_HEIGHT; y++) {
        for (uint8_t x = 0; x < MATRIX_WIDTH; x++) {
            uint8_t ratio = sin8((uint8_t)(shift + (x + y) * 14));
            matrixSetPixel(x, y, blend(c1, c2, ratio));
        }
    }
}

// 15. Radial Gradient
void renderRadialGradient(uint32_t t_ms, const EffectParams& params, CRGB* leds) {
    uint8_t spd = params.speed ? params.speed : 40;
    uint8_t pulse = sin8((uint8_t)(t_ms * spd / 200));
    CRGB c1 = (params.c1 == CRGB(0,0,0)) ? CRGB::Yellow : params.c1;
    CRGB c2 = (params.c2 == CRGB(0,0,0)) ? CRGB::Purple : params.c2;
    float cx = 4.5f, cy = 4.5f;

    for (uint8_t y = 0; y < MATRIX_HEIGHT; y++) {
        for (uint8_t x = 0; x < MATRIX_WIDTH; x++) {
            float d = sqrtf((x - cx) * (x - cx) + (y - cy) * (y - cy));
            uint8_t ratio = (uint8_t)constrain((d * 36.0f) + (pulse / 4), 0, 255);
            matrixSetPixel(x, y, blend(c1, c2, ratio));
        }
    }
}

// 16. Aurora
void renderAurora(uint32_t t_ms, const EffectParams& params, CRGB* leds) {
    uint8_t spd = params.speed ? params.speed : 40;
    for (uint8_t y = 0; y < MATRIX_HEIGHT; y++) {
        for (uint8_t x = 0; x < MATRIX_WIDTH; x++) {
            uint8_t n = inoise8(x * 50, (y * 30) + (t_ms * spd / 25));
            CRGB col;
            if (n < 110) {
                col = blend(CRGB(10, 0, 40), CRGB(0, 180, 100), map(n, 0, 110, 0, 255));
            } else if (n < 190) {
                col = blend(CRGB(0, 180, 100), CRGB(0, 220, 220), map(n, 110, 190, 0, 255));
            } else {
                col = blend(CRGB(0, 220, 220), CRGB(180, 20, 240), map(n, 190, 255, 0, 255));
            }
            matrixSetPixel(x, y, col);
        }
    }
}

// 17. Sunset
void renderSunset(uint32_t t_ms, const EffectParams& params, CRGB* leds) {
    uint8_t spd = params.speed ? params.speed : 30;
    uint8_t shift = (t_ms * spd / 80) & 0xFF;

    for (uint8_t y = 0; y < MATRIX_HEIGHT; y++) {
        uint8_t pos = (y * 25) + (sin8(shift) / 8);
        CRGB col;
        if (pos < 60) {
            col = blend(CRGB(20, 0, 60), CRGB(120, 0, 80), map(pos, 0, 60, 0, 255));
        } else if (pos < 130) {
            col = blend(CRGB(120, 0, 80), CRGB(255, 50, 0), map(pos, 60, 130, 0, 255));
        } else if (pos < 200) {
            col = blend(CRGB(255, 50, 0), CRGB(255, 140, 0), map(pos, 130, 200, 0, 255));
        } else {
            col = blend(CRGB(255, 140, 0), CRGB(255, 230, 80), map(pos, 200, 255, 0, 255));
        }
        for (uint8_t x = 0; x < MATRIX_WIDTH; x++) {
            matrixSetPixel(x, y, col);
        }
    }
}

// 18. Palette Scroll
void renderPaletteScroll(uint32_t t_ms, const EffectParams& params, CRGB* leds) {
    uint8_t spd = params.speed ? params.speed : 50;
    uint8_t step = (t_ms * spd / 30) & 0xFF;
    for (uint8_t y = 0; y < MATRIX_HEIGHT; y++) {
        for (uint8_t x = 0; x < MATRIX_WIDTH; x++) {
            matrixSetPixel(x, y, ColorFromPalette(RainbowColors_p, step + (x * 12) + (y * 12)));
        }
    }
}

// 19. Color Wheel Spin
void renderColorWheelSpin(uint32_t t_ms, const EffectParams& params, CRGB* leds) {
    uint8_t spd = params.speed ? params.speed : 45;
    float angleOffset = (t_ms * spd * 0.003f);
    float cx = 4.5f, cy = 4.5f;

    for (uint8_t y = 0; y < MATRIX_HEIGHT; y++) {
        for (uint8_t x = 0; x < MATRIX_WIDTH; x++) {
            float angle = atan2f(y - cy, x - cx) + angleOffset;
            uint8_t hue = (uint8_t)(angle * (128.0f / 3.14159f));
            matrixSetPixel(x, y, CHSV(hue, 240, 255));
        }
    }
}
