#include "effects.h"
#include "matrix.h"

// Table of registered effects (first 10 base effects)
static EffectEntry effectTable[10];
static size_t registeredCount = 0;

void effectsInit() {
    registeredCount = 0;
    effectTable[registeredCount++] = {1,  "Solid",         renderSolid};
    effectTable[registeredCount++] = {2,  "Breathing",     renderBreathing};
    effectTable[registeredCount++] = {3,  "Pulse",         renderPulse};
    effectTable[registeredCount++] = {4,  "Heartbeat",     renderHeartbeat};
    effectTable[registeredCount++] = {5,  "Candle",        renderCandle};
    effectTable[registeredCount++] = {6,  "Rainbow Wave",  renderRainbowWave};
    effectTable[registeredCount++] = {7,  "Rainbow Cycle", renderRainbowCycle};
    effectTable[registeredCount++] = {8,  "Scanner",       renderScanner};
    effectTable[registeredCount++] = {9,  "Sparkle",       renderSparkle};
    effectTable[registeredCount++] = {10, "Fire",          renderFire};
}

const EffectEntry* getEffectById(uint8_t id) {
    for (size_t i = 0; i < registeredCount; i++) {
        if (effectTable[i].id == id) {
            return &effectTable[i];
        }
    }
    return nullptr;
}

const EffectEntry* getEffectByIndex(size_t index) {
    if (index < registeredCount) {
        return &effectTable[index];
    }
    return nullptr;
}

size_t getEffectCount() {
    return registeredCount;
}

void effectsRender(uint32_t t_ms, const EffectParams& params, CRGB* leds) {
    const EffectEntry* entry = getEffectById(params.base);
    if (entry && entry->render) {
        entry->render(t_ms, params, leds);
    } else {
        // Fallback: simple solid c1
        renderSolid(t_ms, params, leds);
    }
}

// 1. Solid
void renderSolid(uint32_t t_ms, const EffectParams& params, CRGB* leds) {
    CRGB col = (params.c1 == CRGB(0,0,0)) ? CRGB::White : params.c1;
    if (params.colorMode == 3) {
        uint8_t spd = params.speed ? params.speed : 50;
        col = CHSV((t_ms * spd / 40) & 0xFF, 240, 255);
    }
    for (int i = 0; i < NUM_LEDS; i++) {
        leds[i] = col;
    }
}

// 2. Breathing
void renderBreathing(uint32_t t_ms, const EffectParams& params, CRGB* leds) {
    uint8_t spd = params.speed ? params.speed : 50;
    uint8_t breath = (sin8((uint8_t)(t_ms * spd / 300)) / 2) + 30; // 30..157
    CRGB col = (params.c1 == CRGB(0,0,0)) ? CRGB(255, 60, 180) : params.c1;
    if (params.colorMode == 1) {
        uint8_t blendRatio = sin8((uint8_t)(t_ms * spd / 500));
        col = blend(params.c1, params.c2, blendRatio);
    } else if (params.colorMode == 3) {
        col = CHSV((t_ms * spd / 50) & 0xFF, 240, 255);
    }
    col.nscale8_video(breath);
    for (int i = 0; i < NUM_LEDS; i++) {
        leds[i] = col;
    }
}

// 3. Pulse
void renderPulse(uint32_t t_ms, const EffectParams& params, CRGB* leds) {
    uint8_t spd = params.speed ? params.speed : 60;
    uint16_t period = 12000 / spd;
    if (period == 0) period = 100;
    uint16_t phase = t_ms % period;
    uint8_t bright = 0;
    if (phase < period / 5) {
        bright = map(phase, 0, period / 5, 20, 255);
    } else {
        bright = map(phase, period / 5, period, 255, 10);
    }
    CRGB col = (params.c1 == CRGB(0,0,0)) ? CRGB::Cyan : params.c1;
    if (params.colorMode == 3) {
        col = CHSV((t_ms * spd / 60) & 0xFF, 240, 255);
    }
    col.nscale8_video(bright);
    for (int i = 0; i < NUM_LEDS; i++) {
        leds[i] = col;
    }
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
    for (int i = 0; i < NUM_LEDS; i++) {
        leds[i] = col;
    }
}

// 5. Candle
void renderCandle(uint32_t t_ms, const EffectParams& params, CRGB* leds) {
    uint8_t spd = params.speed ? params.speed : 40;
    for (uint8_t y = 0; y < MATRIX_HEIGHT; y++) {
        for (uint8_t x = 0; x < MATRIX_WIDTH; x++) {
            uint8_t noise = inoise8(x * 40, (y * 40) + (t_ms * spd / 20));
            // Candle palette: warm red/orange/yellow
            CRGB col = CRGB(noise, scale8(noise, 140), scale8(noise, 20));
            matrixSetPixel(x, y, col);
        }
    }
}

// 6. Rainbow Wave
void renderRainbowWave(uint32_t t_ms, const EffectParams& params, CRGB* leds) {
    uint8_t spd = params.speed ? params.speed : 50;
    uint8_t offset = (t_ms * spd / 30) & 0xFF;
    if (params.dir == 1) offset = 255 - offset;

    for (uint8_t y = 0; y < MATRIX_HEIGHT; y++) {
        for (uint8_t x = 0; x < MATRIX_WIDTH; x++) {
            uint8_t hue;
            if (params.dir == 2) {
                hue = offset + (y * (255 / MATRIX_HEIGHT));
            } else if (params.dir == 3) {
                hue = offset + ((x + y) * (255 / (MATRIX_WIDTH + MATRIX_HEIGHT)));
            } else {
                uint8_t effX = x;
                if (params.mirror) {
                    effX = (x < MATRIX_WIDTH / 2) ? x : (MATRIX_WIDTH - 1 - x);
                }
                hue = offset + (effX * (255 / MATRIX_WIDTH));
            }
            matrixSetPixel(x, y, CHSV(hue, 240, 255));
        }
    }
}

// 7. Rainbow Cycle
void renderRainbowCycle(uint32_t t_ms, const EffectParams& params, CRGB* leds) {
    uint8_t spd = params.speed ? params.speed : 45;
    uint8_t offset = (t_ms * spd / 25) & 0xFF;
    float centerX = (MATRIX_WIDTH - 1) / 2.0f;
    float centerY = (MATRIX_HEIGHT - 1) / 2.0f;

    for (uint8_t y = 0; y < MATRIX_HEIGHT; y++) {
        for (uint8_t x = 0; x < MATRIX_WIDTH; x++) {
            float dx = x - centerX;
            float dy = y - centerY;
            uint8_t dist = (uint8_t)(sqrtf(dx * dx + dy * dy) * 35.0f);
            uint8_t hue = (params.dir == 1) ? (offset + dist) : (offset - dist);
            matrixSetPixel(x, y, CHSV(hue, 240, 255));
        }
    }
}

// 8. Scanner
void renderScanner(uint32_t t_ms, const EffectParams& params, CRGB* leds) {
    uint8_t spd = params.speed ? params.speed : 65;
    for (int i = 0; i < NUM_LEDS; i++) {
        leds[i].nscale8(195);
    }
    uint16_t cycle = (MATRIX_WIDTH - 1) * 2;
    uint16_t step = (t_ms * spd / 50) % cycle;
    uint8_t pos = (step < MATRIX_WIDTH) ? step : (cycle - step);

    CRGB col = (params.c1 == CRGB(0,0,0)) ? CRGB::Red : params.c1;
    if (params.colorMode == 3) {
        col = CHSV((t_ms * spd / 40) & 0xFF, 240, 255);
    }

    if (params.dir == 2 || params.dir == 3) {
        for (uint8_t x = 0; x < MATRIX_WIDTH; x++) {
            matrixSetPixel(x, pos, col);
        }
    } else {
        for (uint8_t y = 0; y < MATRIX_HEIGHT; y++) {
            matrixSetPixel(pos, y, col);
        }
    }
}

// 9. Sparkle
void renderSparkle(uint32_t t_ms, const EffectParams& params, CRGB* leds) {
    uint8_t spd = params.speed ? params.speed : 55;
    for (int i = 0; i < NUM_LEDS; i++) {
        leds[i].nscale8(180);
    }
    uint8_t count = map(spd, 0, 255, 1, 4);
    for (uint8_t c = 0; c < count; c++) {
        uint8_t rx = random8(MATRIX_WIDTH);
        uint8_t ry = random8(MATRIX_HEIGHT);
        CRGB col = params.c1;
        if (params.colorMode == 3) {
            col = CHSV(random8(), 200, 255);
        } else if (params.colorMode == 1) {
            col = (random8(2) == 0) ? params.c1 : params.c2;
        } else if (col == CRGB(0, 0, 0)) {
            col = CRGB::White;
        }
        matrixSetPixel(rx, ry, col);
    }
}

// 10. Fire
void renderFire(uint32_t t_ms, const EffectParams& params, CRGB* leds) {
    static uint8_t heat[MATRIX_WIDTH][MATRIX_HEIGHT] = {0};
    static uint32_t lastFireUpdate = 0;

    uint8_t spd = params.speed ? params.speed : 60;
    uint32_t interval = 800 / (spd + 10);
    if (t_ms - lastFireUpdate >= interval) {
        lastFireUpdate = t_ms;

        // Cool down
        for (uint8_t x = 0; x < MATRIX_WIDTH; x++) {
            for (uint8_t y = 0; y < MATRIX_HEIGHT; y++) {
                uint8_t cooldown = random8(0, ((55 * 10) / MATRIX_HEIGHT) + 2);
                heat[x][y] = (heat[x][y] > cooldown) ? (heat[x][y] - cooldown) : 0;
            }
        }

        // Heat drifts up
        for (uint8_t x = 0; x < MATRIX_WIDTH; x++) {
            for (int y = 0; y < MATRIX_HEIGHT - 1; y++) {
                heat[x][y] = (heat[x][y + 1] + heat[x][y + 2 < MATRIX_HEIGHT ? y + 2 : y + 1]) / 2;
            }
        }

        // Ignite new sparks at bottom
        for (uint8_t x = 0; x < MATRIX_WIDTH; x++) {
            if (random8() < 160) {
                heat[x][MATRIX_HEIGHT - 1] = qadd8(heat[x][MATRIX_HEIGHT - 1], random8(160, 255));
            }
        }
    }

    // Map heat to color
    for (uint8_t y = 0; y < MATRIX_HEIGHT; y++) {
        for (uint8_t x = 0; x < MATRIX_WIDTH; x++) {
            matrixSetPixel(x, y, HeatColor(heat[x][y]));
        }
    }
}

