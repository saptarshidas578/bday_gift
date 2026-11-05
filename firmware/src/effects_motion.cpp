#include "effects.h"
#include "matrix.h"

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

    uint16_t head = (t_ms * spd / 40) % NUM_LEDS;
    CRGB col = getEffectColor(params, t_ms, head, CRGB::Cyan);
    matrixSetPixelRaw(head, col);
}

// 21. Wipe
void renderWipe(uint32_t t_ms, const EffectParams& params, CRGB* leds) {
    uint8_t spd = params.speed ? params.speed : 40;
    uint16_t step = (t_ms * spd / 80) % (MATRIX_WIDTH * 2);
    CRGB c1 = (params.c1 == CRGB(0,0,0)) ? CRGB::Green : params.c1;
    CRGB c2 = (params.c2 == CRGB(0,0,0)) ? CRGB::DarkBlue : params.c2;

    for (uint8_t y = 0; y < MATRIX_HEIGHT; y++) {
        for (uint8_t x = 0; x < MATRIX_WIDTH; x++) {
            if (step < MATRIX_WIDTH) {
                matrixSetPixel(x, y, (x <= step) ? c1 : c2);
            } else {
                matrixSetPixel(x, y, (x <= (step - MATRIX_WIDTH)) ? c2 : c1);
            }
        }
    }
}

// 22. Bounce
void renderBounce(uint32_t t_ms, const EffectParams& params, CRGB* leds) {
    uint8_t spd = params.speed ? params.speed : 50;
    for (int i = 0; i < NUM_LEDS; i++) leds[i].nscale8(180);

    uint16_t xCycle = (MATRIX_WIDTH - 1) * 2;
    uint16_t yCycle = (MATRIX_HEIGHT - 1) * 2;
    uint16_t xStep = (t_ms * spd / 45) % xCycle;
    uint16_t yStep = (t_ms * spd / 38) % yCycle;

    uint8_t bx = (xStep < MATRIX_WIDTH) ? xStep : (xCycle - xStep);
    uint8_t by = (yStep < MATRIX_HEIGHT) ? yStep : (yCycle - yStep);

    CRGB col = (params.c1 == CRGB(0,0,0)) ? CRGB::Yellow : params.c1;
    matrixSetPixel(bx, by, col);
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
            matrixSetPixel(x, y, CHSV((uint8_t)(r * 40 + t * 20), 240, 255));
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
                CRGB col = CRGB(0, b, b / 4);
                matrixSetPixel(x, y, col);
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
            matrixSetPixel(x, y, (d == 0) ? CRGB::White : CRGB(255, 120, 0));
        }
    }
}

// 29. Theater Chase
void renderTheaterChase(uint32_t t_ms, const EffectParams& params, CRGB* leds) {
    uint8_t spd = params.speed ? params.speed : 50;
    uint8_t step = (t_ms * spd / 80) % 3;
    CRGB col = (params.c1 == CRGB(0,0,0)) ? CRGB::Orange : params.c1;

    for (int i = 0; i < NUM_LEDS; i++) {
        leds[i] = ((i % 3) == step) ? col : CRGB::Black;
    }
}

// 30. Row Sweep
void renderRowSweep(uint32_t t_ms, const EffectParams& params, CRGB* leds) {
    uint8_t spd = params.speed ? params.speed : 50;
    for (int i = 0; i < NUM_LEDS; i++) leds[i].nscale8(190);

    uint8_t row = (t_ms * spd / 80) % MATRIX_HEIGHT;
    CRGB col = (params.colorMode == 3) ? CHSV((t_ms * spd / 30) & 0xFF, 240, 255) : params.c1;
    for (uint8_t x = 0; x < MATRIX_WIDTH; x++) {
        matrixSetPixel(x, row, col);
    }
}
