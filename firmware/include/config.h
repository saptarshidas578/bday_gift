#pragma once

#include <FastLED.h>

// ============================================================================
// Hardware Pin Configuration
// Confirmed working in B01: Data on GPIO 5 (D5)
// ============================================================================
#ifndef DATA_PIN
#define DATA_PIN 5
#endif

#ifndef BUZZER_PIN
#define BUZZER_PIN 18
#endif

#ifndef BOOT_BUTTON_PIN
#define BOOT_BUTTON_PIN 0
#endif

// ============================================================================
// Matrix Dimensions & Layout (10x10 = 100 LEDs)
// ============================================================================
#define MATRIX_WIDTH 10
#define MATRIX_HEIGHT 10
#define NUM_LEDS (MATRIX_WIDTH * MATRIX_HEIGHT) // 100 LEDs

// Serpentine wiring:
// True: Even rows go left->right (0..9), odd rows go right->left (19..10).
// False: All rows go left->right.
#ifndef MATRIX_SERPENTINE
#define MATRIX_SERPENTINE true
#endif

// Color Order:
// WS2812B GRB confirmed working.
#ifndef COLOR_ORDER
#define COLOR_ORDER GRB
#endif

// ============================================================================
// Power & Brightness Caps (DO NOT DISABLE)
// Safe limits: 100 x 60 mA = 6.0 A max theoretical.
// Capped at 2500 mA to prevent power supply overload or brownouts.
// ============================================================================
#define LED_VOLTS 5
#define LED_MAX_MA 2500
#define DEFAULT_BRIGHTNESS 40 // Safe low brightness (0-255)
#define MAX_BRIGHTNESS 255

// ============================================================================
// Firmware Version
// ============================================================================
#define FW_VERSION_MAJOR 1
#define FW_VERSION_MINOR 0
#define FW_VERSION_PATCH 0

