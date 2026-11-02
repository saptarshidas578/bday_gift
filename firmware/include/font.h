#pragma once

#include <stdint.h>
#include <stddef.h>

// 3x4 Digits: 3 columns x 4 rows
extern const uint8_t DIGITS_3x4[10][3];

// Returns 5 columns for a 5x7 character (ASCII + special glyphs: '\x01' Heart, '\x02' Star, '\x03' Smile)
const uint8_t* getFont5x7(char c);

// Renders "Sync Needed" glyph onto a 10x10 matrix (blinking icon)
void drawSyncNeededGlyph(uint32_t now);
