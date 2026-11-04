#include "matrix.h"

static CRGB leds[NUM_LEDS];
static uint8_t currentBrightness = DEFAULT_BRIGHTNESS;

void matrixInit() {
    FastLED.addLeds<WS2812B, DATA_PIN, COLOR_ORDER>(leds, NUM_LEDS);
    FastLED.setMaxPowerInVoltsAndMilliamps(LED_VOLTS, LED_MAX_MA);
    FastLED.setBrightness(DEFAULT_BRIGHTNESS);
    FastLED.clear(true);
}

uint16_t XY(uint8_t x, uint8_t y) {
    if (x >= MATRIX_WIDTH || y >= MATRIX_HEIGHT) {
        return NUM_LEDS; // Sentinel for out-of-bounds
    }

#if MATRIX_SERPENTINE
    if (y & 0x01) {
        // Odd rows run backwards (right to left)
        return (y * MATRIX_WIDTH) + (MATRIX_WIDTH - 1 - x);
    } else {
        // Even rows run forward (left to right)
        return (y * MATRIX_WIDTH) + x;
    }
#else
    return (y * MATRIX_WIDTH) + x;
#endif
}

void matrixSetPixel(uint8_t x, uint8_t y, CRGB color) {
    uint16_t idx = XY(x, y);
    if (idx < NUM_LEDS) {
        leds[idx] = color;
    }
}

CRGB matrixGetPixel(uint8_t x, uint8_t y) {
    uint16_t idx = XY(x, y);
    if (idx < NUM_LEDS) {
        return leds[idx];
    }
    return CRGB::Black;
}

void matrixSetPixelRaw(uint16_t index, CRGB color) {
    if (index < NUM_LEDS) {
        leds[index] = color;
    }
}

void matrixFill(CRGB color) {
    fill_solid(leds, NUM_LEDS, color);
}

void matrixClear() {
    FastLED.clear();
}

void matrixShow() {
    FastLED.show();
}

void matrixSetBrightness(uint8_t brightness) {
    currentBrightness = brightness;
    FastLED.setBrightness(brightness);
}

uint8_t matrixGetBrightness() {
    return currentBrightness;
}

CRGB* matrixGetLeds() {
    return leds;
}
