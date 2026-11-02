#pragma once

#include <Arduino.h>
#include <FastLED.h>
#include "config.h"

// Initialize FastLED, power limits, and clear matrix
void matrixInit();

// Coordinate mapping: converts (x, y) into linear 1D strip index
// Returns NUM_LEDS if (x, y) is out of bounds
uint16_t XY(uint8_t x, uint8_t y);

// Set pixel color with bounds check
void matrixSetPixel(uint8_t x, uint8_t y, CRGB color);

// Get pixel color with bounds check (returns CRGB::Black if out of bounds)
CRGB matrixGetPixel(uint8_t x, uint8_t y);

// Set raw pixel by 1D index [0..NUM_LEDS-1]
void matrixSetPixelRaw(uint16_t index, CRGB color);

// Fill entire matrix with a single color
void matrixFill(CRGB color);

// Clear all pixels (sets to black)
void matrixClear();

// Push pixel buffer to LEDs via FastLED.show()
void matrixShow();

// Set brightness safely clamped to [0, 255]
void matrixSetBrightness(uint8_t brightness);

// Get current brightness
uint8_t matrixGetBrightness();

// Direct pointer to the CRGB buffer
CRGB* matrixGetLeds();

