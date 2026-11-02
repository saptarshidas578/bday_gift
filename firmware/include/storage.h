#pragma once

#include <Arduino.h>
#include <FastLED.h>
#include "config.h"
#include "effects.h"

// System Modes (mirrors protocol.json)
enum DeviceMode : uint8_t {
    MODE_SOLID = 0,
    MODE_EFFECT = 1,
    MODE_CLOCK = 2,
    MODE_TIMER = 3,
    MODE_BIRTHDAY = 4,
    MODE_MESSAGE = 5,
    MODE_DRAW = 6,
    MODE_MUSIC = 7,
    MODE_OFF = 8
};

// Scene Slot (8 slots: 0..7)
struct SceneSlot {
    uint8_t isValid;
    uint8_t mode;
    CRGB color;
    uint8_t brightness;
    EffectParams effect;
};

// Schedule Slot (4 slots: 0..3)
struct ScheduleSlot {
    uint8_t enabled;
    uint8_t dayMask; // bit 0 = Sun, bit 1 = Mon ... bit 6 = Sat
    uint8_t hh;
    uint8_t mm;
    uint8_t action;  // 0: ON, 1: OFF, 2: LOAD_SCENE, 3: SUNRISE_ALARM
    uint8_t arg;     // scene slot index if action == 2
};

// Comprehensive Device State for live operation and NVS persistence
struct DeviceState {
    uint8_t mode;                  // Restorable: SOLID, EFFECT, CLOCK, OFF only
    CRGB color;                    // Primary solid color
    uint8_t brightness;            // 0..255
    EffectParams effect;           // Active effect params
    uint8_t paletteCount;          // 1..8
    CRGB palette[8];               // Custom palette colors
    uint8_t clockStyle;            // 0: stacked 3x4, 1: scrolling 5x7
    int16_t tzOffsetMinutes;       // Timezone offset in minutes
    uint8_t isMuted;               // Global mute flag (0 or 1)
    uint16_t sleepDefaultMinutes;  // Sleep timer default
    char customMessage[64];        // Message text
    char birthdayName[32];         // Birthday name
    char birthdayGreeting[64];     // Birthday greeting
    SceneSlot scenes[8];           // 8 scene slots
    ScheduleSlot schedules[4];     // 4 schedule slots
};

// Global state instance
extern DeviceState state;

// Initialize Preferences storage and load persisted state
void storageInit();

// Mark state as modified to trigger debounced save (2s window)
void storageMarkDirty();

// Periodic loop handler: executes write after 2s debounce delay
void storageLoop();

// Force immediate save to NVS
void storageForceSave();

// Factory reset: wipes NVS namespace and reloads defaults
void storageFactoryReset();
