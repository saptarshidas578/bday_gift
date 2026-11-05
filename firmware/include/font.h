#pragma once

#include <stdint.h>
#include <stddef.h>

// 3x4 Digits: 3 columns x 4 rows
extern const uint8_t DIGITS_3x4[10][3];

// 4x5 Digits: 4 columns x 5 rows for 10x10 matrix clock
extern const uint8_t DIGITS_4x5[10][4];

// Returns 5 columns for a 5x7 character (ASCII + special glyphs: '\x01' Heart, '\x02' Star, '\x03' Smile)
const uint8_t* getFont5x7(char c);

// Renders "Sync Needed" glyph onto a 10x10 matrix (blinking icon)
void drawSyncNeededGlyph(uint32_t now);

