#pragma once

#include <FastLED.h>

// ============================================================================
// Hardware Pin Configuration
// STOP: Do not assume pins without physical verification (Task B01)
// ============================================================================
#ifndef DATA_PIN
#define DATA_PIN 27
#endif

#ifndef BUZZER_PIN
#define BUZZER_PIN 18
#endif

#ifndef BOOT_BUTTON_PIN
#define BOOT_BUTTON_PIN 0
#endif

// ============================================================================
// Matrix Dimensions & Layout
// ============================================================================
#define MATRIX_WIDTH 9
#define MATRIX_HEIGHT 9
#define NUM_LEDS (MATRIX_WIDTH * MATRIX_HEIGHT) // 81 LEDs

// Serpentine wiring:
// True: Even rows go left->right (0..8), odd rows go right->left (17..9).
// False: All rows go left->right.
#ifndef MATRIX_SERPENTINE
#define MATRIX_SERPENTINE true
#endif

// Color Order:
// Common WS2812B order is GRB. Verify with the color cycle test.
#ifndef COLOR_ORDER
#define COLOR_ORDER GRB
#endif

// ============================================================================
// Power & Brightness Caps (DO NOT DISABLE)
// Safe limits: 81 x 60 mA = 4.86 A max. Capped to prevent overheating or brownouts.
// ============================================================================
#define LED_VOLTS 5
#define LED_MAX_MA 2500
#define DEFAULT_BRIGHTNESS 30 // Low brightness for initial testing (0-255)
#define MAX_BRIGHTNESS 255
