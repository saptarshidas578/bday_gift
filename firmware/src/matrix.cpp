#include "matrix.h"

static CRGB leds[NUM_LEDS];
static uint8_t currentBrightness = DEFAULT_BRIGHTNESS;

// Standard CIE 1931 perceptual lightness curve (scaled to 0..255)
// Converts linear input into logarithmic human-eye perceptual brightness
static const uint8_t PROGMEM CIE1931[256] = {
    0,   0,   0,   0,   0,   0,   0,   0,   0,   0,   1,   1,   1,   1,   1,   1,
    1,   1,   2,   2,   2,   2,   2,   2,   3,   3,   3,   3,   4,   4,   4,   4,
    5,   5,   5,   5,   6,   6,   6,   7,   7,   7,   8,   8,   8,   9,   9,  10,
   10,  10,  11,  11,  12,  12,  13,  13,  14,  14,  15,  15,  16,  16,  17,  17,
   18,  19,  19,  20,  20,  21,  22,  22,  23,  24,  24,  25,  26,  26,  27,  28,
   29,  29,  30,  31,  32,  33,  33,  34,  35,  36,  37,  38,  39,  39,  40,  41,
   42,  43,  44,  45,  46,  47,  48,  49,  50,  51,  52,  53,  54,  55,  56,  57,
   59,  60,  61,  62,  63,  64,  66,  67,  68,  69,  71,  72,  73,  75,  76,  77,
   79,  80,  81,  83,  84,  86,  87,  89,  90,  92,  93,  95,  96,  98,  99, 101,
  103, 104, 106, 108, 109, 111, 113, 115, 116, 118, 120, 122, 124, 126, 128, 130,
  131, 133, 135, 138, 140, 142, 144, 146, 148, 150, 153, 155, 157, 159, 162, 164,
  166, 169, 171, 173, 176, 178, 181, 183, 186, 188, 191, 193, 196, 198, 201, 204,
  206, 209, 212, 215, 217, 220, 223, 226, 229, 232, 235, 238, 241, 244, 247, 250,
  253, 255
};

uint8_t matrixCieGamma(uint8_t input) {
    uint8_t val = pgm_read_byte(&CIE1931[input]);
    if (input > 0 && val == 0) return 1;
    return val;
}

void matrixInit() {
    FastLED.addLeds<WS2812B, DATA_PIN, COLOR_ORDER>(leds, NUM_LEDS);
    FastLED.setMaxPowerInVoltsAndMilliamps(LED_VOLTS, LED_MAX_MA);
    matrixSetBrightness(DEFAULT_BRIGHTNESS);
    FastLED.clear(true);
}

uint16_t XY(uint8_t x, uint8_t y) {
    if (x >= MATRIX_WIDTH || y >= MATRIX_HEIGHT) {
        return NUM_LEDS; // Sentinel for out-of-bounds
    }

#if MATRIX_SERPENTINE
    if (y & 0x01) {
        // Odd rows run forward (left to right)
        return (y * MATRIX_WIDTH) + x;
    } else {
        // Even rows run backwards (right to left)
        return (y * MATRIX_WIDTH) + (MATRIX_WIDTH - 1 - x);
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
    uint8_t gammaBrightness = matrixCieGamma(brightness);
    FastLED.setBrightness(gammaBrightness);
}

uint8_t matrixGetBrightness() {
    return currentBrightness;
}

CRGB* matrixGetLeds() {
    return leds;
}
