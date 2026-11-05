#include "effects.h"
#include "matrix.h"
#include "storage.h"

// 10. Fire
void renderFire(uint32_t t_ms, const EffectParams& params, CRGB* leds) {
    static uint8_t heat[MATRIX_WIDTH][MATRIX_HEIGHT] = {0};
    static uint32_t lastFireUpdate = 0;

    uint8_t spd = params.speed ? params.speed : 60;
    uint32_t interval = 800 / (spd + 10);
    if (t_ms - lastFireUpdate >= interval) {
        lastFireUpdate = t_ms;

        for (uint8_t x = 0; x < MATRIX_WIDTH; x++) {
            for (uint8_t y = 0; y < MATRIX_HEIGHT; y++) {
                uint8_t cd = random8(0, ((55 * 10) / MATRIX_HEIGHT) + 2);
                heat[x][y] = (heat[x][y] > cd) ? (heat[x][y] - cd) : 0;
            }
        }
        for (uint8_t x = 0; x < MATRIX_WIDTH; x++) {
            for (int y = 0; y < MATRIX_HEIGHT - 1; y++) {
                heat[x][y] = (heat[x][y + 1] + heat[x][y + 2 < MATRIX_HEIGHT ? y + 2 : y + 1]) / 2;
            }
        }
        for (uint8_t x = 0; x < MATRIX_WIDTH; x++) {
            if (random8() < 160) {
                heat[x][MATRIX_HEIGHT - 1] = qadd8(heat[x][MATRIX_HEIGHT - 1], random8(160, 255));
            }
        }
    }

    for (uint8_t y = 0; y < MATRIX_HEIGHT; y++) {
        for (uint8_t x = 0; x < MATRIX_WIDTH; x++) {
            uint8_t h = heat[x][y];
            CRGB pixelCol;
            if (params.colorMode == 1) {
                CRGB c1 = (params.c1 == CRGB(0,0,0)) ? CRGB(255, 30, 0) : params.c1;
                CRGB c2 = (params.c2 == CRGB(0,0,0)) ? CRGB(255, 200, 0) : params.c2;
                pixelCol = blend(c1, c2, h);
                pixelCol.nscale8_video(h);
            } else if (params.colorMode == 2 && state.paletteCount > 0) {
                uint16_t total = (uint16_t)(h * state.paletteCount);
                uint8_t step = (total >> 8) % state.paletteCount;
                uint8_t nextStep = (step + 1) % state.paletteCount;
                uint8_t frac = total & 0xFF;
                pixelCol = blend(state.palette[step], state.palette[nextStep], frac);
                pixelCol.nscale8_video(h);
            } else if (params.colorMode == 3) {
                pixelCol = CHSV((h + (t_ms * spd / 30)) & 0xFF, 240, h);
            } else if (params.c1 != CRGB(0,0,0)) {
                pixelCol = params.c1;
                pixelCol.nscale8_video(h);
            } else {
                pixelCol = HeatColor(h);
            }
            matrixSetPixel(x, y, pixelCol);
        }
    }
}

// 40. Lava
void renderLava(uint32_t t_ms, const EffectParams& params, CRGB* leds) {
    uint8_t spd = params.speed ? params.speed : 30;
    for (uint8_t y = 0; y < MATRIX_HEIGHT; y++) {
        for (uint8_t x = 0; x < MATRIX_WIDTH; x++) {
            uint8_t n = inoise8(x * 50, (y * 50) + (t_ms * spd / 30));
            CRGB col;
            if (params.colorMode == 1) {
                CRGB c1 = (params.c1 == CRGB(0,0,0)) ? CRGB(180, 20, 0) : params.c1;
                CRGB c2 = (params.c2 == CRGB(0,0,0)) ? CRGB(255, 230, 50) : params.c2;
                col = blend(c1, c2, n);
            } else if (params.colorMode == 2 && state.paletteCount > 0) {
                uint16_t total = (uint16_t)(n * state.paletteCount);
                uint8_t step = (total >> 8) % state.paletteCount;
                uint8_t nextStep = (step + 1) % state.paletteCount;
                col = blend(state.palette[step], state.palette[nextStep], total & 0xFF);
            } else if (params.colorMode == 3) {
                col = CHSV((n + (t_ms * spd / 35)) & 0xFF, 240, 255);
            } else if (params.c1 != CRGB(0,0,0)) {
                col = blend(CRGB(20, 0, 0), params.c1, n);
            } else {
                if (n < 130) {
                    col = blend(CRGB(20, 0, 0), CRGB(180, 20, 0), map(n, 0, 130, 0, 255));
                } else if (n < 200) {
                    col = blend(CRGB(180, 20, 0), CRGB(255, 120, 0), map(n, 130, 200, 0, 255));
                } else {
                    col = blend(CRGB(255, 120, 0), CRGB(255, 230, 50), map(n, 200, 255, 0, 255));
                }
            }
            matrixSetPixel(x, y, col);
        }
    }
}

// 41. Ocean
void renderOcean(uint32_t t_ms, const EffectParams& params, CRGB* leds) {
    uint8_t spd = params.speed ? params.speed : 35;
    for (uint8_t y = 0; y < MATRIX_HEIGHT; y++) {
        for (uint8_t x = 0; x < MATRIX_WIDTH; x++) {
            uint8_t n = inoise8(x * 45, (y * 45) + (t_ms * spd / 25));
            CRGB col;
            if (params.colorMode == 1) {
                CRGB c1 = (params.c1 == CRGB(0,0,0)) ? CRGB(0, 20, 120) : params.c1;
                CRGB c2 = (params.c2 == CRGB(0,0,0)) ? CRGB(0, 180, 200) : params.c2;
                col = blend(c1, c2, n);
            } else if (params.colorMode == 2 && state.paletteCount > 0) {
                uint16_t total = (uint16_t)(n * state.paletteCount);
                uint8_t step = (total >> 8) % state.paletteCount;
                uint8_t nextStep = (step + 1) % state.paletteCount;
                col = blend(state.palette[step], state.palette[nextStep], total & 0xFF);
            } else if (params.colorMode == 3) {
                col = CHSV((n + (t_ms * spd / 30)) & 0xFF, 240, 255);
            } else if (params.c1 != CRGB(0,0,0)) {
                col = blend(CRGB(0, 5, 25), params.c1, n);
            } else {
                col = blend(CRGB(0, 20, 120), CRGB(0, 180, 200), n);
            }
            matrixSetPixel(x, y, col);
        }
    }
}

// 42. Lightning
void renderLightning(uint32_t t_ms, const EffectParams& params, CRGB* leds) {
    static uint32_t flashUntil = 0;
    static uint32_t nextFlash = 0;

    for (int i = 0; i < NUM_LEDS; i++) leds[i].nscale8(120);

    if (t_ms >= nextFlash) {
        flashUntil = t_ms + random8(60, 200);
        nextFlash = t_ms + random16(1500, 4000);
    }
    if (t_ms < flashUntil) {
        CRGB col = getEffectColor(params, t_ms, 0, CRGB(200, 220, 255));
        fill_solid(leds, NUM_LEDS, col);
    }
}

// 43. Northern Lights
void renderNorthernLights(uint32_t t_ms, const EffectParams& params, CRGB* leds) {
    uint8_t spd = params.speed ? params.speed : 30;
    for (uint8_t y = 0; y < MATRIX_HEIGHT; y++) {
        for (uint8_t x = 0; x < MATRIX_WIDTH; x++) {
            uint8_t n = inoise8(x * 35, (y * 25) + (t_ms * spd / 30));
            CRGB col;
            if (params.colorMode == 1) {
                CRGB c1 = (params.c1 == CRGB(0,0,0)) ? CRGB(0, 180, 80) : params.c1;
                CRGB c2 = (params.c2 == CRGB(0,0,0)) ? CRGB(160, 0, 200) : params.c2;
                col = blend(c1, c2, n);
            } else if (params.colorMode == 2 && state.paletteCount > 0) {
                uint16_t total = (uint16_t)(n * state.paletteCount);
                uint8_t step = (total >> 8) % state.paletteCount;
                uint8_t nextStep = (step + 1) % state.paletteCount;
                col = blend(state.palette[step], state.palette[nextStep], total & 0xFF);
            } else if (params.colorMode == 3) {
                col = CHSV((n + (t_ms * spd / 30)) & 0xFF, 240, 255);
            } else if (params.c1 != CRGB(0,0,0)) {
                col = blend(CRGB(0, 20, 10), params.c1, n);
            } else {
                col = blend(CRGB(0, 180, 80), CRGB(160, 0, 200), n);
            }
            col.nscale8_video(scale8(n, 200) + 55);
            matrixSetPixel(x, y, col);
        }
    }
}

// 44. Embers
void renderEmbers(uint32_t t_ms, const EffectParams& params, CRGB* leds) {
    uint8_t spd = params.speed ? params.speed : 40;
    for (uint8_t y = 0; y < MATRIX_HEIGHT; y++) {
        for (uint8_t x = 0; x < MATRIX_WIDTH; x++) {
            uint8_t n = inoise8(x * 60, (y * 60) + (t_ms * spd / 20));
            CRGB col;
            if (params.colorMode == 1) {
                CRGB c1 = (params.c1 == CRGB(0,0,0)) ? CRGB(255, 60, 0) : params.c1;
                CRGB c2 = (params.c2 == CRGB(0,0,0)) ? CRGB(255, 200, 80) : params.c2;
                col = blend(c1, c2, n);
            } else if (params.colorMode == 2 && state.paletteCount > 0) {
                uint16_t total = (uint16_t)(n * state.paletteCount);
                uint8_t step = (total >> 8) % state.paletteCount;
                uint8_t nextStep = (step + 1) % state.paletteCount;
                col = blend(state.palette[step], state.palette[nextStep], total & 0xFF);
            } else if (params.colorMode == 3) {
                col = CHSV((n + (t_ms * spd / 30)) & 0xFF, 240, 255);
            } else if (params.c1 != CRGB(0,0,0)) {
                col = blend(CRGB(15, 0, 0), params.c1, n);
            } else {
                col = blend(CRGB(40, 5, 0), CRGB(255, 60, 0), n);
            }
            matrixSetPixel(x, y, col);
        }
    }
    if (random8() < 40) {
        CRGB sparkCol = getEffectColor(params, t_ms, random8(), CRGB(255, 200, 80));
        matrixSetPixel(random8(MATRIX_WIDTH), random8(MATRIX_HEIGHT), sparkCol);
    }
}

// 45. Storm
void renderStorm(uint32_t t_ms, const EffectParams& params, CRGB* leds) {
    uint8_t spd = params.speed ? params.speed : 50;
    for (uint8_t y = 0; y < MATRIX_HEIGHT; y++) {
        for (uint8_t x = 0; x < MATRIX_WIDTH; x++) {
            uint8_t n = inoise8(x * 40, (y * 40) + (t_ms * spd / 20));
            CRGB col;
            if (params.colorMode == 1) {
                CRGB c1 = (params.c1 == CRGB(0,0,0)) ? CRGB(20, 40, 80) : params.c1;
                CRGB c2 = (params.c2 == CRGB(0,0,0)) ? CRGB(80, 160, 220) : params.c2;
                col = blend(c1, c2, n);
            } else if (params.colorMode == 2 && state.paletteCount > 0) {
                uint16_t total = (uint16_t)(n * state.paletteCount);
                uint8_t step = (total >> 8) % state.paletteCount;
                uint8_t nextStep = (step + 1) % state.paletteCount;
                col = blend(state.palette[step], state.palette[nextStep], total & 0xFF);
            } else if (params.colorMode == 3) {
                col = CHSV((n + (t_ms * spd / 30)) & 0xFF, 240, 255);
            } else if (params.c1 != CRGB(0,0,0)) {
                col = blend(CRGB(5, 5, 10), params.c1, n);
            } else {
                col = CRGB(scale8(n, 20), scale8(n, 40), scale8(n, 80));
            }
            matrixSetPixel(x, y, col);
        }
    }
    if (random8() < 5) {
        matrixSetPixel(random8(MATRIX_WIDTH), random8(MATRIX_HEIGHT), CRGB::White);
    }
}

// 46. Wind
void renderWind(uint32_t t_ms, const EffectParams& params, CRGB* leds) {
    uint8_t spd = params.speed ? params.speed : 60;
    for (int i = 0; i < NUM_LEDS; i++) leds[i].nscale8(180);

    uint8_t step = (t_ms * spd / 40) & 0xFF;
    for (uint8_t y = 0; y < MATRIX_HEIGHT; y++) {
        uint8_t offset = inoise8(y * 40, step * 4);
        uint8_t x = map(offset, 0, 255, 0, MATRIX_WIDTH - 1);
        CRGB col = getEffectColor(params, t_ms, y * 25, CRGB(180, 220, 255));
        matrixSetPixel(x, y, col);
    }
}
