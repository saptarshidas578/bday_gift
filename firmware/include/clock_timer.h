#pragma once

#include <Arduino.h>
#include <FastLED.h>

void clockTimerInit();

// Non-blocking background time & beacon checks
void clockTimerLoop(uint32_t now);

// Time sync (OP_SYNC_TIME)
void syncTime(uint32_t epochUtc, int16_t tzOffsetMinutes);
bool isTimeSynced();

// Render clock mode on 10x10
void renderClock(uint32_t now, CRGB* leds);

// Timer control (OP_START_TIMER, OP_CANCEL_TIMER)
void startTimer(uint32_t seconds);
void cancelTimer();
bool isTimerRunning();
bool isTimerAlerting();
uint32_t getTimerRemainingSeconds();

// Render timer mode on 10x10
void renderTimer(uint32_t now, CRGB* leds);

