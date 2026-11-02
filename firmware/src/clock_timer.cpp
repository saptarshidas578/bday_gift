#include "clock_timer.h"
#include "matrix.h"
#include "font.h"
#include "storage.h"
#include "audio.h"
#include <sys/time.h>
#include <time.h>

static bool timeSynced = false;

// Timer State
static bool timerActive = false;
static bool timerAlerting = false;
static uint32_t timerDurationSec = 0;
static uint32_t timerStartMillis = 0;
static uint32_t alertStartMillis = 0;
static uint8_t preTimerMode = MODE_SOLID;

void clockTimerInit() {
    timeSynced = false;
    timerActive = false;
    timerAlerting = false;
}

void syncTime(uint32_t epochUtc, int16_t tzOffsetMinutes) {
    struct timeval tv;
    tv.tv_sec = epochUtc;
    tv.tv_usec = 0;
    settimeofday(&tv, nullptr);
    timeSynced = true;
    state.tzOffsetMinutes = tzOffsetMinutes;
    storageMarkDirty();
    Serial.printf("[TIME] Synced via settimeofday: epoch=%u, tzOffset=%d min\n", epochUtc, tzOffsetMinutes);
}

bool isTimeSynced() {
    return timeSynced;
}

static void drawDigit3x4(uint8_t digit, uint8_t startX, uint8_t startY, CRGB color) {
    if (digit > 9) digit = 0;
    for (uint8_t c = 0; c < 3; c++) {
        uint8_t colBits = DIGITS_3x4[digit][c];
        for (uint8_t r = 0; r < 4; r++) {
            if (colBits & (1 << r)) {
                matrixSetPixel(startX + c, startY + r, color);
            }
        }
    }
}

void renderClock(uint32_t now, CRGB* leds) {
    if (!timeSynced) {
        drawSyncNeededGlyph(now);
        return;
    }

    matrixClear();

    time_t rawSec = time(nullptr) + (state.tzOffsetMinutes * 60);
    struct tm* ti = gmtime(&rawSec);
    uint8_t hh = ti ? ti->tm_hour : 0;
    uint8_t mm = ti ? ti->tm_min : 0;
    uint8_t ss = ti ? ti->tm_sec : 0;

    CRGB hourColor = CRGB(255, 120, 20); // Warm amber
    CRGB minColor  = CRGB(0, 180, 255);  // Electric cyan
    CRGB dotColor  = CRGB::White;

    // Hours (Rows 0..3)
    drawDigit3x4(hh / 10, 1, 0, hourColor); // Tens (cols 1..3)
    drawDigit3x4(hh % 10, 5, 0, hourColor); // Units (cols 5..7)

    // Blinking seconds colon on row 4
    if ((ss % 2) == 0) {
        matrixSetPixel(4, 4, dotColor);
        matrixSetPixel(5, 4, dotColor);
    }

    // Minutes (Rows 5..8)
    drawDigit3x4(mm / 10, 1, 5, minColor); // Tens (cols 1..3)
    drawDigit3x4(mm % 10, 5, 5, minColor); // Units (cols 5..7)
}

void startTimer(uint32_t seconds) {
    if (seconds == 0) {
        cancelTimer();
        return;
    }
    preTimerMode = state.mode;
    timerDurationSec = seconds;
    timerStartMillis = millis();
    timerActive = true;
    timerAlerting = false;
    state.mode = MODE_TIMER;
    Serial.printf("[TIMER] Started %u seconds countdown.\n", seconds);
}

void cancelTimer() {
    if (timerActive || timerAlerting) {
        timerActive = false;
        timerAlerting = false;
        state.mode = preTimerMode;
        Serial.println("[TIMER] Cancelled.");
    }
}

bool isTimerRunning() {
    return timerActive;
}

bool isTimerAlerting() {
    return timerAlerting;
}

uint32_t getTimerRemainingSeconds() {
    if (!timerActive) return 0;
    uint32_t elapsed = (millis() - timerStartMillis) / 1000;
    return (elapsed < timerDurationSec) ? (timerDurationSec - elapsed) : 0;
}

void renderTimer(uint32_t now, CRGB* leds) {
    if (timerAlerting) {
        // Visual alert: flashing Red / White for 5 seconds
        uint32_t alertElapsed = now - alertStartMillis;
        if (alertElapsed >= 5000) {
            timerAlerting = false;
            state.mode = preTimerMode;
            Serial.println("[TIMER] Alert finished. Returned to previous mode.");
            return;
        }

        bool flash = ((now / 200) % 2) == 0;
        matrixFill(flash ? CRGB::Red : CRGB::White);
        return;
    }

    if (!timerActive) return;

    uint32_t remaining = getTimerRemainingSeconds();
    if (remaining == 0) {
        // Timer done!
        timerActive = false;
        timerAlerting = true;
        alertStartMillis = now;
        playTimerDoneBeeps(); // Audio stub (beeps when buzzer arrives)
        Serial.println("[TIMER] Time's up! Alerting...");
        return;
    }

    matrixClear();

    // 1. Perimeter countdown progress ring (36 total border pixels)
    static const uint8_t borderX[] = {
        0,1,2,3,4,5,6,7,8,9,
        9,9,9,9,9,9,9,9,
        9,8,7,6,5,4,3,2,1,0,
        0,0,0,0,0,0,0,0
    };
    static const uint8_t borderY[] = {
        0,0,0,0,0,0,0,0,0,0,
        1,2,3,4,5,6,7,8,
        9,9,9,9,9,9,9,9,9,9,
        8,7,6,5,4,3,2,1
    };

    uint8_t litCount = map(remaining, 0, timerDurationSec, 0, 36);
    for (uint8_t i = 0; i < litCount; i++) {
        matrixSetPixel(borderX[i], borderY[i], CRGB(0, 180, 255));
    }

    // 2. Remaining seconds / minutes inside border
    if (remaining < 60) {
        // Display seconds
        drawDigit3x4(remaining / 10, 2, 3, CRGB::Yellow);
        drawDigit3x4(remaining % 10, 5, 3, CRGB::Yellow);
    } else {
        // Display minutes
        uint32_t mins = remaining / 60;
        if (mins > 99) mins = 99;
        drawDigit3x4(mins / 10, 2, 3, CRGB::Green);
        drawDigit3x4(mins % 10, 5, 3, CRGB::Green);
    }
}

