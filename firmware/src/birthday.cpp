#include "birthday.h"
#include "matrix.h"
#include "storage.h"
#include "effects.h"
#include "audio.h"

static bool isRunning = false;
static uint32_t showStartTime = 0;
static uint8_t previousMode = MODE_SOLID;

// Compact 5x7 ASCII font (Space, '!', '?', 0-9, A-Z)
static const uint8_t FONT5x7[][5] = {
    {0x00, 0x00, 0x00, 0x00, 0x00}, // 32 ' '
    {0x00, 0x00, 0x5F, 0x00, 0x00}, // 33 '!'
    {0x02, 0x01, 0x51, 0x09, 0x06}, // 63 '?'
    // Digits 0-9
    {0x3E, 0x51, 0x49, 0x45, 0x3E}, // '0'
    {0x00, 0x42, 0x7F, 0x40, 0x00}, // '1'
    {0x42, 0x61, 0x51, 0x49, 0x46}, // '2'
    {0x21, 0x41, 0x45, 0x4B, 0x31}, // '3'
    {0x18, 0x14, 0x12, 0x7F, 0x10}, // '4'
    {0x27, 0x45, 0x45, 0x45, 0x39}, // '5'
    {0x3C, 0x4A, 0x49, 0x49, 0x30}, // '6'
    {0x01, 0x71, 0x09, 0x05, 0x03}, // '7'
    {0x36, 0x49, 0x49, 0x49, 0x36}, // '8'
    {0x06, 0x49, 0x49, 0x29, 0x1E}, // '9'
    // Letters A-Z
    {0x7E, 0x11, 0x11, 0x11, 0x7E}, // 'A'
    {0x7F, 0x49, 0x49, 0x49, 0x36}, // 'B'
    {0x3E, 0x41, 0x41, 0x41, 0x22}, // 'C'
    {0x7F, 0x41, 0x41, 0x22, 0x1C}, // 'D'
    {0x7F, 0x49, 0x49, 0x49, 0x41}, // 'E'
    {0x7F, 0x09, 0x09, 0x09, 0x01}, // 'F'
    {0x3E, 0x41, 0x49, 0x49, 0x7A}, // 'G'
    {0x7F, 0x08, 0x08, 0x08, 0x7F}, // 'H'
    {0x00, 0x41, 0x7F, 0x41, 0x00}, // 'I'
    {0x20, 0x40, 0x41, 0x3F, 0x01}, // 'J'
    {0x7F, 0x08, 0x14, 0x22, 0x41}, // 'K'
    {0x7F, 0x40, 0x40, 0x40, 0x40}, // 'L'
    {0x7F, 0x02, 0x0C, 0x02, 0x7F}, // 'M'
    {0x7F, 0x04, 0x08, 0x10, 0x7F}, // 'N'
    {0x3E, 0x41, 0x41, 0x41, 0x3E}, // 'O'
    {0x7F, 0x09, 0x09, 0x09, 0x06}, // 'P'
    {0x3E, 0x41, 0x51, 0x21, 0x5E}, // 'Q'
    {0x7F, 0x09, 0x19, 0x29, 0x46}, // 'R'
    {0x46, 0x49, 0x49, 0x49, 0x31}, // 'S'
    {0x01, 0x01, 0x7F, 0x01, 0x01}, // 'T'
    {0x3F, 0x40, 0x40, 0x40, 0x3F}, // 'U'
    {0x1F, 0x20, 0x40, 0x20, 0x1F}, // 'V'
    {0x7F, 0x20, 0x18, 0x20, 0x7F}, // 'W'
    {0x63, 0x14, 0x08, 0x14, 0x63}, // 'X'
    {0x07, 0x08, 0x70, 0x08, 0x07}, // 'Y'
    {0x61, 0x51, 0x49, 0x45, 0x43}  // 'Z'
};

static const uint8_t* getFontColumn(char c) {
    if (c >= 'a' && c <= 'z') c -= 32; // Uppercase
    if (c >= 'A' && c <= 'Z') return FONT5x7[13 + (c - 'A')];
    if (c >= '0' && c <= '9') return FONT5x7[3 + (c - '0')];
    if (c == '!') return FONT5x7[1];
    if (c == '?') return FONT5x7[2];
    return FONT5x7[0]; // Space default
}

static void drawScrollingText(const char* text, int16_t scrollX, CRGB color) {
    size_t len = strlen(text);
    int16_t cursorX = scrollX;

    for (size_t i = 0; i < len; i++) {
        const uint8_t* glyph = getFontColumn(text[i]);
        for (uint8_t col = 0; col < 5; col++) {
            int16_t screenX = cursorX + col;
            if (screenX >= 0 && screenX < MATRIX_WIDTH) {
                uint8_t bits = glyph[col];
                for (uint8_t row = 0; row < 7; row++) {
                    if (bits & (1 << row)) {
                        matrixSetPixel(screenX, row + 1, color); // Centered vertically on rows 1..7
                    }
                }
            }
        }
        cursorX += 6; // 5 cols + 1 col space
    }
}

void birthdayInit() {
    isRunning = false;
}

void birthdayStart() {
    if (!isRunning) {
        previousMode = state.mode;
    }
    isRunning = true;
    showStartTime = millis();
    state.mode = MODE_BIRTHDAY;
    playMelodyHappyBirthday(); // Stubbed for lights-only
    Serial.println("\n[BIRTHDAY] Starting full Birthday Show timeline (lights-only, ~80s)...");
}

void birthdayStop() {
    if (isRunning) {
        isRunning = false;
        stopMelody();
        // Return to soft ambient effect
        state.mode = MODE_EFFECT;
        state.effect.base = 2; // Breathing
        state.effect.c1 = CRGB(255, 140, 20); // Warm Gold
        state.effect.speed = 40;
        storageMarkDirty();
        Serial.println("[BIRTHDAY] Show finished. Settled into warm ambient glow.");
    }
}

bool isBirthdayRunning() {
    return isRunning;
}

void birthdayRender(uint32_t now, CRGB* leds) {
    if (!isRunning) return;

    uint32_t elapsed = now - showStartTime;

    // --- Phase 1: Intro Sparkle (0..6s) ---
    if (elapsed < 6000) {
        for (int i = 0; i < NUM_LEDS; i++) leds[i].nscale8(180);
        uint8_t count = map(elapsed, 0, 6000, 1, 4);
        for (uint8_t c = 0; c < count; c++) {
            matrixSetPixel(random8(MATRIX_WIDTH), random8(MATRIX_HEIGHT),
                           CHSV(random8(30, 60), 220, 255)); // Warm gold sparkles
        }
    }
    // --- Phase 2: Name Reveal Scrolling (6..22s) ---
    else if (elapsed < 22000) {
        matrixClear();
        char banner[64];
        snprintf(banner, sizeof(banner), "HAPPY BIRTHDAY %s !",
                 (strlen(state.birthdayName) > 0) ? state.birthdayName : "FRIEND");

        uint32_t phaseTime = elapsed - 6000;
        int16_t scrollX = MATRIX_WIDTH - (int16_t)(phaseTime / 85); // 85ms per col
        uint8_t hue = (phaseTime / 30) & 0xFF;
        drawScrollingText(banner, scrollX, CHSV(hue, 220, 255));
    }
    // --- Phase 3: Hearts and Balloons (22..36s) ---
    else if (elapsed < 36000) {
        EffectParams p;
        p.speed = 55;
        p.c1 = CRGB::Crimson;
        p.c2 = CRGB::DeepSkyBlue;
        p.colorMode = 0;
        if (elapsed < 29000) {
            renderHearts(now, p, leds);
        } else {
            renderBalloons(now, p, leds);
        }
    }
    // --- Phase 4: Fireworks (36..52s) ---
    else if (elapsed < 52000) {
        EffectParams p;
        p.speed = 60;
        renderFireworks(now, p, leds);
    }
    // --- Phase 5: Rainbow Celebration (52..66s) ---
    else if (elapsed < 66000) {
        EffectParams p;
        p.speed = 50;
        renderRainbowCycle(now, p, leds);
        if (random8() < 90) {
            matrixSetPixel(random8(MATRIX_WIDTH), random8(MATRIX_HEIGHT), CRGB::White);
        }
    }
    // --- Phase 6: Scrolling Greeting (66..76s) ---
    else if (elapsed < 76000) {
        matrixClear();
        const char* greeting = (strlen(state.birthdayGreeting) > 0) ? state.birthdayGreeting : "BEST WISHES !";
        uint32_t phaseTime = elapsed - 66000;
        int16_t scrollX = MATRIX_WIDTH - (int16_t)(phaseTime / 85);
        drawScrollingText(greeting, scrollX, CRGB(255, 180, 50));
    }
    // --- Phase 7: Finale Glow into Ambient (76..80s) ---
    else if (elapsed < 80000) {
        uint32_t phaseTime = elapsed - 76000;
        uint8_t b = map(phaseTime, 0, 4000, 220, 60);
        CRGB warmGold = CRGB(255, 140, 20);
        warmGold.nscale8_video(b);
        matrixFill(warmGold);
    }
    // --- End of Timeline ---
    else {
        birthdayStop();
    }
}

