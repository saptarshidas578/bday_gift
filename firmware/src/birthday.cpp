#include "birthday.h"
#include "matrix.h"
#include "storage.h"
#include "effects.h"
#include "audio.h"
#include "font.h"

static bool isRunning = false;
static uint32_t showStartTime = 0;
static uint8_t previousMode = MODE_SOLID;

static void drawScrollingText(const char* text, int16_t scrollX, CRGB color) {
    size_t len = strlen(text);
    int16_t cursorX = scrollX;

    for (size_t i = 0; i < len; i++) {
        const uint8_t* glyph = getFont5x7(text[i]);
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
    Serial.println("\n[BIRTHDAY] Starting full Birthday Show timeline (lights-only, ~98s)...");
}

void birthdayStop() {
    if (isRunning) {
        isRunning = false;
        stopMelody();
        // Return to soft ambient effect: breathing Cherry Red
        state.mode = MODE_EFFECT;
        state.effect.base = 2; // Breathing
        state.effect.c1 = CRGB(210, 4, 45); // Cherry Red
        state.effect.speed = 40;
        storageMarkDirty();
        Serial.println("[BIRTHDAY] Show finished. Settled into Cherry Red ambient glow.");
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
        snprintf(banner, sizeof(banner), "HAPPY BIRTHDAY %s ! \x01",
                 (strlen(state.birthdayName) > 0) ? state.birthdayName : "MADHUMITHA");

        uint32_t phaseTime = elapsed - 6000;
        int16_t scrollX = MATRIX_WIDTH - (int16_t)(phaseTime / 85); // 85ms per col
        uint8_t hue = (phaseTime / 30) & 0xFF;
        drawScrollingText(banner, scrollX, CHSV(hue, 220, 255));
    }
    // --- Phase 3: Hearts and Balloons (22..36s) ---
    else if (elapsed < 36000) {
        EffectParams p;
        p.speed = 55;
        p.c1 = CRGB(210, 4, 45); // Cherry Red
        p.c2 = CRGB(255, 215, 0); // Gold
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
    // --- Phase 6: Scrolling Custom Greeting (66..94s) ---
    else if (elapsed < 94000) {
        matrixClear();
        const char* greeting = (strlen(state.birthdayGreeting) > 0) ? state.birthdayGreeting : "HAPPY 19TH \x01 24 JAN \x02 WISHING YOU JOY & LOVE !";
        uint32_t phaseTime = elapsed - 66000;
        int16_t scrollX = MATRIX_WIDTH - (int16_t)(phaseTime / 85);
        drawScrollingText(greeting, scrollX, CRGB(255, 180, 50));
    }
    // --- Phase 7: Finale Glow into Cherry Red Ambient (94..98s) ---
    else if (elapsed < 98000) {
        uint32_t phaseTime = elapsed - 94000;
        uint8_t b = map(phaseTime, 0, 4000, 220, 60);
        CRGB cherryRed = CRGB(210, 4, 45);
        cherryRed.nscale8_video(b);
        matrixFill(cherryRed);
    }
    // --- End of Timeline ---
    else {
        birthdayStop();
    }
}

