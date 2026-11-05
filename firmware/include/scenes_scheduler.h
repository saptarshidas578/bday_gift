#pragma once

#include <Arduino.h>
#include <FastLED.h>
#include "config.h"
#include "storage.h"

// Initialize scenes, scheduler, sleep timer, draw and music visualizer
void scenesSchedulerInit();

// Scenes (8 slots: 0..7)
bool saveScene(uint8_t slot);
bool loadScene(uint8_t slot);

// Schedules (4 slots: 0..3)
bool setSchedule(uint8_t slot, uint8_t enabled, uint8_t dayMask, uint8_t hh, uint8_t mm, uint8_t action, uint8_t arg);
void schedulerLoop(uint32_t now);
bool isSunriseAlarmRunning();
void stopSunriseAlarm();

// Sleep Timer (fade-out to OFF)
void startSleepTimer(uint16_t minutes);
void cancelSleepTimer();
bool isSleepTimerRunning();
uint16_t getSleepTimerRemainingMinutes();
void sleepTimerLoop(uint32_t now);

// Draw Canvas (staged chunking with bounds check)
#define DRAW_BUFFER_BYTES (NUM_LEDS * 3)
bool drawChunk(uint16_t offset, const uint8_t* chunk, uint8_t len);
void drawCommit();
void renderDraw(CRGB* leds);

// Custom Message & Text Targets
void setTextChunk(uint8_t target, uint8_t idx, uint8_t total, const uint8_t* data, uint8_t len);
void showMessage(uint8_t speed);
void renderMessage(uint32_t now, CRGB* leds);

// Music Levels (reactive visualizer with smoothing & timeout)
void feedMusicLevels(const uint8_t* levels, uint8_t count);
void musicLoop(uint32_t now);
void renderMusic(uint32_t now, CRGB* leds);

