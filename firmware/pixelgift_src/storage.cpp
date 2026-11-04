#include "storage.h"
#include <Preferences.h>

static Preferences prefs;
DeviceState state;

static bool isDirty = false;
static uint32_t lastDirtyTime = 0;
static const uint32_t DEBOUNCE_DELAY_MS = 2000; // 2 seconds debounce per SPEC

void setDefaults() {
    state.mode = MODE_SOLID;
    state.color = CRGB(210, 4, 45); // Cherry Red (#D2042D)
    state.brightness = DEFAULT_BRIGHTNESS;

    state.effect.base = 6; // Rainbow Wave
    state.effect.speed = 50;
    state.effect.dir = 0;
    state.effect.mirror = 0;
    state.effect.colorMode = 3; // Rainbow
    state.effect.c1 = CRGB(210, 4, 45); // Cherry Red
    state.effect.c2 = CRGB(255, 215, 0); // Gold

    state.paletteCount = 4;
    state.palette[0] = CRGB(210, 4, 45);  // Cherry Red
    state.palette[1] = CRGB(255, 215, 0); // Gold
    state.palette[2] = CRGB(255, 120, 20); // Amber
    state.palette[3] = CRGB(255, 60, 140); // Rose
    for (int i = 4; i < 8; i++) state.palette[i] = CRGB::Black;

    state.clockStyle = 0; // Stacked 3x4
    state.tzOffsetMinutes = 330; // IST (+5:30) default
    state.isMuted = 0;
    state.sleepDefaultMinutes = 30;

    strncpy(state.customMessage, "HAPPY 19TH BIRTHDAY MADHUMITHA!", sizeof(state.customMessage));
    strncpy(state.birthdayName, "MADHUMITHA", sizeof(state.birthdayName));
    strncpy(state.birthdayGreeting, "HAPPY 19TH \x01 24 JAN \x02 WISHING YOU JOY & LOVE !", sizeof(state.birthdayGreeting));

    for (int i = 0; i < 8; i++) {
        state.scenes[i].isValid = 0;
    }
    for (int i = 0; i < 4; i++) {
        state.schedules[i].enabled = 0;
    }
}

void storageInit() {
    setDefaults();

    if (!prefs.begin("pgift", false)) {
        Serial.println("[STORAGE] Warning: Failed to open Preferences namespace 'pgift'. Using defaults.");
        return;
    }

    if (prefs.isKey("magic")) {
        // Stored settings exist
        uint8_t savedMode = prefs.getUChar("mode", MODE_SOLID);
        // Birthday, Timer, Draw, Music, Message are not persisted as boot modes
        if (savedMode == MODE_SOLID || savedMode == MODE_EFFECT || savedMode == MODE_CLOCK || savedMode == MODE_OFF) {
            state.mode = savedMode;
        } else {
            state.mode = MODE_SOLID;
        }

        uint32_t col = prefs.getUInt("color", 0xFF3CB4);
        state.color = CRGB((col >> 16) & 0xFF, (col >> 8) & 0xFF, col & 0xFF);

        state.brightness = prefs.getUChar("bright", DEFAULT_BRIGHTNESS);
        if (state.brightness == 0) state.brightness = DEFAULT_BRIGHTNESS;

        state.effect.base = prefs.getUChar("eff_base", 6);
        state.effect.speed = prefs.getUChar("eff_spd", 50);
        state.effect.dir = prefs.getUChar("eff_dir", 0);
        state.effect.mirror = prefs.getUChar("eff_mir", 0);
        state.effect.colorMode = prefs.getUChar("eff_cmode", 3);

        uint32_t effC1 = prefs.getUInt("eff_c1", 0xFF3CB4);
        state.effect.c1 = CRGB((effC1 >> 16) & 0xFF, (effC1 >> 8) & 0xFF, effC1 & 0xFF);
        uint32_t effC2 = prefs.getUInt("eff_c2", 0x00C8FF);
        state.effect.c2 = CRGB((effC2 >> 16) & 0xFF, (effC2 >> 8) & 0xFF, effC2 & 0xFF);

        state.clockStyle = prefs.getUChar("clk_style", 0);
        state.tzOffsetMinutes = prefs.getShort("tz_offset", 0);
        state.isMuted = prefs.getUChar("muted", 0);
        state.sleepDefaultMinutes = prefs.getUShort("sleep_def", 30);

        prefs.getString("msg", state.customMessage, sizeof(state.customMessage));
        prefs.getString("bday_name", state.birthdayName, sizeof(state.birthdayName));
        prefs.getString("bday_greet", state.birthdayGreeting, sizeof(state.birthdayGreeting));

        // Load 8 scenes
        prefs.getBytes("scenes", state.scenes, sizeof(state.scenes));
        // Load 4 schedules
        prefs.getBytes("scheds", state.schedules, sizeof(state.schedules));

        // Prevent white-screen lock or black boot from previous bug
        if ((state.mode == MODE_SOLID && (state.color.r > 240 && state.color.g > 240 && state.color.b > 240)) ||
            (state.color.r == 0 && state.color.g == 0 && state.color.b == 0) ||
            state.mode == MODE_OFF) {
            state.mode = MODE_EFFECT;
            state.effect.base = 6; // Rainbow Wave
            state.effect.colorMode = 3; // Rainbow
            state.color = CRGB(210, 4, 45); // Cherry Red
        }

        Serial.printf("[STORAGE] Loaded state from NVS. Mode=%u, Brightness=%u, Effect=%u (\"%s\")\n",
                      state.mode, state.brightness, state.effect.base, state.customMessage);
    } else {
        Serial.println("[STORAGE] First boot detected. Initializing NVS defaults...");
        storageForceSave();
    }
}

void storageMarkDirty() {
    isDirty = true;
    lastDirtyTime = millis();
}

void storageForceSave() {
    prefs.putUChar("magic", 0xA5);
    prefs.putUChar("mode", state.mode);

    uint32_t col = ((uint32_t)state.color.r << 16) | ((uint32_t)state.color.g << 8) | state.color.b;
    prefs.putUInt("color", col);

    prefs.putUChar("bright", state.brightness);

    prefs.putUChar("eff_base", state.effect.base);
    prefs.putUChar("eff_spd", state.effect.speed);
    prefs.putUChar("eff_dir", state.effect.dir);
    prefs.putUChar("eff_mir", state.effect.mirror);
    prefs.putUChar("eff_cmode", state.effect.colorMode);

    uint32_t effC1 = ((uint32_t)state.effect.c1.r << 16) | ((uint32_t)state.effect.c1.g << 8) | state.effect.c1.b;
    prefs.putUInt("eff_c1", effC1);
    uint32_t effC2 = ((uint32_t)state.effect.c2.r << 16) | ((uint32_t)state.effect.c2.g << 8) | state.effect.c2.b;
    prefs.putUInt("eff_c2", effC2);

    prefs.putUChar("clk_style", state.clockStyle);
    prefs.putShort("tz_offset", state.tzOffsetMinutes);
    prefs.putUChar("muted", state.isMuted);
    prefs.putUShort("sleep_def", state.sleepDefaultMinutes);

    prefs.putString("msg", state.customMessage);
    prefs.putString("bday_name", state.birthdayName);
    prefs.putString("bday_greet", state.birthdayGreeting);

    prefs.putBytes("scenes", state.scenes, sizeof(state.scenes));
    prefs.putBytes("scheds", state.schedules, sizeof(state.schedules));

    isDirty = false;
    Serial.println("[STORAGE] Persisted debounced state to NVS successfully.");
}

void storageLoop() {
    if (isDirty && (millis() - lastDirtyTime >= DEBOUNCE_DELAY_MS)) {
        storageForceSave();
    }
}

void storageFactoryReset() {
    Serial.println("[STORAGE] Performing Factory Reset...");
    prefs.clear();
    setDefaults();
    storageForceSave();
}
