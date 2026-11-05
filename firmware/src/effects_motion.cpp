#include "effects.h"
#include "matrix.h"
#include "storage.h"

// 8. Scanner
void renderScanner(uint32_t t_ms, const EffectParams& params, CRGB* leds) {
    uint8_t spd = params.speed ? params.speed : 65;
    for (int i = 0; i < NUM_LEDS; i++) leds[i].nscale8(195);

    uint16_t cycle = (MATRIX_WIDTH - 1) * 2;
    uint16_t step = (t_ms * spd / 50) % cycle;
    uint8_t pos = (step < MATRIX_WIDTH) ? step : (cycle - step);

    CRGB col = getEffectColor(params, t_ms, pos, CRGB::Red);
    if (params.dir == 2) {
        for (uint8_t x = 0; x < MATRIX_WIDTH; x++) matrixSetPixel(x, pos, col);
    } else {
        for (uint8_t y = 0; y < MATRIX_HEIGHT; y++) matrixSetPixel(pos, y, col);
    }
}

// 20. Chase
void renderChase(uint32_t t_ms, const EffectParams& params, CRGB* leds) {
    uint8_t spd = params.speed ? params.speed : 50;
    for (int i = 0; i < NUM_LEDS; i++) leds[i].nscale8(170);

    uint16_t head1 = (t_ms * spd / 40) % NUM_LEDS;
    if (params.colorMode == 1) {
        uint16_t head2 = (head1 + (NUM_LEDS / 2)) % NUM_LEDS;
        CRGB c1 = (params.c1 == CRGB(0,0,0)) ? CRGB::Cyan : params.c1;
        CRGB c2 = (params.c2 == CRGB(0,0,0)) ? CRGB::Magenta : params.c2;
        matrixSetPixelRaw(head1, c1);
        matrixSetPixelRaw(head2, c2);
    } else {
        CRGB col = getEffectColor(params, t_ms, head1, CRGB::Cyan);
        matrixSetPixelRaw(head1, col);
    }
}

// 21. Wipe
void renderWipe(uint32_t t_ms, const EffectParams& params, CRGB* leds) {
    uint8_t spd = params.speed ? params.speed : 40;
    uint16_t step = (t_ms * spd / 80) % (MATRIX_WIDTH * 2);

    for (uint8_t y = 0; y < MATRIX_HEIGHT; y++) {
        for (uint8_t x = 0; x < MATRIX_WIDTH; x++) {
            CRGB col;
            if (params.colorMode == 3) {
                uint8_t hue = (t_ms * spd / 30) + (x * 20);
                col = CHSV(hue, 240, 255);
                if (step < MATRIX_WIDTH) {
                    if (x > step) col = CRGB::Black;
                } else {
                    if (x <= (step - MATRIX_WIDTH)) col = CRGB::Black;
                }
            } else if (params.colorMode == 2 && state.paletteCount > 0) {
                uint8_t pIdx1 = ((t_ms * spd / 2000)) % state.paletteCount;
                uint8_t pIdx2 = (pIdx1 + 1) % state.paletteCount;
                CRGB c1 = state.palette[pIdx1];
                CRGB c2 = state.palette[pIdx2];
                col = (step < MATRIX_WIDTH) ? ((x <= step) ? c1 : c2) : ((x <= (step - MATRIX_WIDTH)) ? c2 : c1);
            } else {
                CRGB c1 = (params.c1 == CRGB(0,0,0)) ? CRGB::Green : params.c1;
                CRGB c2 = (params.c2 == CRGB(0,0,0)) ? CRGB::DarkBlue : params.c2;
                col = (step < MATRIX_WIDTH) ? ((x <= step) ? c1 : c2) : ((x <= (step - MATRIX_WIDTH)) ? c2 : c1);
            }
            matrixSetPixel(x, y, col);
        }
    }
}

// 22. Bounce
void renderBounce(uint32_t t_ms, const EffectParams& params, CRGB* leds) {
    uint8_t spd = params.speed ? params.speed : 50;
    for (int i = 0; i < NUM_LEDS; i++) leds[i].nscale8(180);

    uint16_t xCycle = (MATRIX_WIDTH - 1) * 2;
    uint16_t yCycle = (MATRIX_HEIGHT - 1) * 2;
    uint16_t xStep1 = (t_ms * spd / 45) % xCycle;
    uint16_t yStep1 = (t_ms * spd / 38) % yCycle;

    uint8_t bx1 = (xStep1 < MATRIX_WIDTH) ? xStep1 : (xCycle - xStep1);
    uint8_t by1 = (yStep1 < MATRIX_HEIGHT) ? yStep1 : (yCycle - yStep1);

    if (params.colorMode == 1) {
        uint16_t xStep2 = ((t_ms * spd / 45) + (xCycle / 2)) % xCycle;
        uint16_t yStep2 = ((t_ms * spd / 38) + (yCycle / 2)) % yCycle;
        uint8_t bx2 = (xStep2 < MATRIX_WIDTH) ? xStep2 : (xCycle - xStep2);
        uint8_t by2 = (yStep2 < MATRIX_HEIGHT) ? yStep2 : (yCycle - yStep2);

        CRGB c1 = (params.c1 == CRGB(0,0,0)) ? CRGB::Yellow : params.c1;
        CRGB c2 = (params.c2 == CRGB(0,0,0)) ? CRGB::Cyan : params.c2;
        matrixSetPixel(bx1, by1, c1);
        matrixSetPixel(bx2, by2, c2);
    } else {
        CRGB col = getEffectColor(params, t_ms, bx1 + by1, CRGB::Yellow);
        matrixSetPixel(bx1, by1, col);
    }
}

// 23. Spiral
void renderSpiral(uint32_t t_ms, const EffectParams& params, CRGB* leds) {
    uint8_t spd = params.speed ? params.speed : 40;
    float t = t_ms * spd * 0.003f;
    for (int i = 0; i < NUM_LEDS; i++) leds[i].nscale8(190);

    for (float r = 0.5f; r < 4.5f; r += 0.8f) {
        float angle = t + (r * 1.5f);
        int8_t x = (int8_t)(4.5f + cosf(angle) * r);
        int8_t y = (int8_t)(4.5f + sinf(angle) * r);
        if (x >= 0 && x < MATRIX_WIDTH && y >= 0 && y < MATRIX_HEIGHT) {
            CRGB col = getEffectColor(params, t_ms, (uint8_t)(r * 30), CHSV((uint8_t)(r * 40 + t * 20), 240, 255));
            matrixSetPixel(x, y, col);
        }
    }
}

// 24. Radar
void renderRadar(uint32_t t_ms, const EffectParams& params, CRGB* leds) {
    uint8_t spd = params.speed ? params.speed : 40;
    float sweep = (t_ms * spd * 0.004f);
    float cx = 4.5f, cy = 4.5f;

    for (uint8_t y = 0; y < MATRIX_HEIGHT; y++) {
        for (uint8_t x = 0; x < MATRIX_WIDTH; x++) {
            float angle = atan2f(y - cy, x - cx);
            float diff = fmodf(sweep - angle + 6.28318f, 6.28318f);
            if (diff < 1.2f) {
                uint8_t b = map((uint8_t)(diff * 200), 0, 240, 255, 10);
                CRGB baseCol = getEffectColor(params, t_ms, (uint8_t)(diff * 35), CRGB(0, 255, 60));
                baseCol.nscale8_video(b);
                matrixSetPixel(x, y, baseCol);
            } else {
                CRGB cur = matrixGetPixel(x, y);
                cur.nscale8(230);
                matrixSetPixel(x, y, cur);
            }
        }
    }
}

// 25. Rain
void renderRain(uint32_t t_ms, const EffectParams& params, CRGB* leds) {
    uint8_t spd = params.speed ? params.speed : 55;
    static uint32_t lastDrop = 0;

    for (int i = 0; i < NUM_LEDS; i++) leds[i].nscale8(180);

    if (t_ms - lastDrop >= (3000 / spd)) {
        lastDrop = t_ms;
        matrixSetPixel(random8(MATRIX_WIDTH), 0, CRGB(150, 220, 255));
    }
    // Shift pixels down
    static uint32_t lastShift = 0;
    if (t_ms - lastShift >= (1200 / spd)) {
        lastShift = t_ms;
        for (int y = MATRIX_HEIGHT - 1; y > 0; y--) {
            for (uint8_t x = 0; x < MATRIX_WIDTH; x++) {
                CRGB above = matrixGetPixel(x, y - 1);
                if (above.r > 10 || above.g > 10 || above.b > 10) {
                    matrixSetPixel(x, y, above);
                    matrixSetPixel(x, y - 1, CRGB::Black);
                }
            }
        }
    }
}

// 26. Waterfall
void renderWaterfall(uint32_t t_ms, const EffectParams& params, CRGB* leds) {
    uint8_t spd = params.speed ? params.speed : 50;
    for (uint8_t y = 0; y < MATRIX_HEIGHT; y++) {
        for (uint8_t x = 0; x < MATRIX_WIDTH; x++) {
            uint8_t n = inoise8(x * 60, (y * 40) - (t_ms * spd / 15));
            matrixSetPixel(x, y, CRGB(scale8(n, 40), scale8(n, 180), n));
        }
    }
}

// 27. Snake Path
void renderSnakePath(uint32_t t_ms, const EffectParams& params, CRGB* leds) {
    uint8_t spd = params.speed ? params.speed : 50;
    for (int i = 0; i < NUM_LEDS; i++) leds[i].nscale8(210);

    uint16_t pos = (t_ms * spd / 50) % NUM_LEDS;
    matrixSetPixelRaw(pos, CRGB::LimeGreen);
}

// 28. Comet
void renderComet(uint32_t t_ms, const EffectParams& params, CRGB* leds) {
    uint8_t spd = params.speed ? params.speed : 60;
    for (int i = 0; i < NUM_LEDS; i++) leds[i].nscale8(190);

    uint16_t step = (t_ms * spd / 40) % (MATRIX_WIDTH + MATRIX_HEIGHT);
    for (uint8_t d = 0; d < 3; d++) {
        int8_t x = step - d;
        int8_t y = step - d;
        if (x >= 0 && x < MATRIX_WIDTH && y >= 0 && y < MATRIX_HEIGHT) {
            CRGB col;
            if (params.colorMode == 1) {
                CRGB c1 = (params.c1 == CRGB(0,0,0)) ? CRGB::White : params.c1;
                CRGB c2 = (params.c2 == CRGB(0,0,0)) ? CRGB(255, 120, 0) : params.c2;
                col = (d == 0) ? c1 : c2;
            } else {
                col = getEffectColor(params, t_ms, d * 40, (d == 0) ? CRGB::White : CRGB(255, 120, 0));
            }
            matrixSetPixel(x, y, col);
        }
    }
}

// 29. Theater Chase
void renderTheaterChase(uint32_t t_ms, const EffectParams& params, CRGB* leds) {
    uint8_t spd = params.speed ? params.speed : 50;
    uint8_t step = (t_ms * spd / 80) % 3;

    for (int i = 0; i < NUM_LEDS; i++) {
        if ((i % 3) == step) {
            leds[i] = getEffectColor(params, t_ms, i, CRGB::Orange);
        } else if (params.colorMode == 1) {
            CRGB c2 = (params.c2 == CRGB(0,0,0)) ? CRGB(0, 40, 100) : params.c2;
            CRGB bg = c2;
            bg.nscale8_video(30);
            leds[i] = bg;
        } else {
            leds[i] = CRGB::Black;
        }
    }
}

// 30. Row Sweep
void renderRowSweep(uint32_t t_ms, const EffectParams& params, CRGB* leds) {
    uint8_t spd = params.speed ? params.speed : 50;
    for (int i = 0; i < NUM_LEDS; i++) leds[i].nscale8(190);

    uint8_t row = (t_ms * spd / 80) % MATRIX_HEIGHT;
    CRGB col;
    if (params.colorMode == 1) {
        CRGB c1 = (params.c1 == CRGB(0,0,0)) ? CRGB::Cyan : params.c1;
        CRGB c2 = (params.c2 == CRGB(0,0,0)) ? CRGB::Magenta : params.c2;
        col = (row % 2 == 0) ? c1 : c2;
    } else {
        col = getEffectColor(params, t_ms, row * 20, CRGB::Cyan);
    }
    for (uint8_t x = 0; x < MATRIX_WIDTH; x++) {
        matrixSetPixel(x, row, col);
    }
}
