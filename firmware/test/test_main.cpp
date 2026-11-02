#include <Arduino.h>
#include <unity.h>
#include "packet_parser.h"
#include "../src/packet_parser.cpp"
#include "font.h"
#include "../src/font.cpp"
#include "scenes_scheduler.h"
#include "ota_manager.h"

// Stubs for scenes_scheduler unit testing
DeviceState state;
static bool mockTimeSynced = false;
bool isTimeSynced() { return mockTimeSynced; }
void storageMarkDirty() {}
void matrixSetBrightness(uint8_t b) { state.brightness = b; }
void matrixClear() {}
uint16_t XY(uint8_t x, uint8_t y) { return y * 10 + x; }

#include "../src/scenes_scheduler.cpp"
#include "../src/ota_manager.cpp"

void setUp(void) {}
void tearDown(void) {}

// =============================================================
// Parser Tests (B06)
// =============================================================
void test_set_color_valid() {
    uint8_t packet[] = { OP_SET_COLOR, 0x01, 255, 0, 128 };
    ParsedCommand cmd;
    uint8_t res = parseCommandPacket(packet, sizeof(packet), cmd);
    TEST_ASSERT_EQUAL_UINT8(ERR_OK, res);
    TEST_ASSERT_EQUAL_UINT8(OP_SET_COLOR, cmd.opcode);
    TEST_ASSERT_EQUAL_UINT8(0x01, cmd.seq);
    TEST_ASSERT_EQUAL_UINT8(3, cmd.payloadLen);
    TEST_ASSERT_EQUAL_UINT8(255, cmd.payload[0]);
    TEST_ASSERT_EQUAL_UINT8(0, cmd.payload[1]);
    TEST_ASSERT_EQUAL_UINT8(128, cmd.payload[2]);
}

void test_set_color_bad_len() {
    uint8_t packet[] = { OP_SET_COLOR, 0x01, 255, 0 }; // Missing 'b'
    ParsedCommand cmd;
    uint8_t res = parseCommandPacket(packet, sizeof(packet), cmd);
    TEST_ASSERT_EQUAL_UINT8(ERR_BAD_LEN, res);
}

void test_set_brightness() {
    uint8_t packet[] = { OP_SET_BRIGHTNESS, 0x02, 120 };
    ParsedCommand cmd;
    uint8_t res = parseCommandPacket(packet, sizeof(packet), cmd);
    TEST_ASSERT_EQUAL_UINT8(ERR_OK, res);
    TEST_ASSERT_EQUAL_UINT8(120, cmd.payload[0]);
}

void test_set_effect_valid() {
    uint8_t packet[] = { OP_SET_EFFECT, 0x03, 6, 50, 0, 0, 3, 255, 60, 180, 0, 200, 255 };
    ParsedCommand cmd;
    uint8_t res = parseCommandPacket(packet, sizeof(packet), cmd);
    TEST_ASSERT_EQUAL_UINT8(ERR_OK, res);
    TEST_ASSERT_EQUAL_UINT8(6, cmd.payload[0]); // Base
    TEST_ASSERT_EQUAL_UINT8(50, cmd.payload[1]); // Speed
}

void test_set_effect_bad_value() {
    // dir = 9 (> 5 invalid)
    uint8_t packet[] = { OP_SET_EFFECT, 0x03, 6, 50, 9, 0, 3, 255, 60, 180, 0, 200, 255 };
    ParsedCommand cmd;
    uint8_t res = parseCommandPacket(packet, sizeof(packet), cmd);
    TEST_ASSERT_EQUAL_UINT8(ERR_BAD_VALUE, res);
}

void test_set_mode_valid() {
    uint8_t packet[] = { OP_SET_MODE, 0x04, 1 };
    ParsedCommand cmd;
    uint8_t res = parseCommandPacket(packet, sizeof(packet), cmd);
    TEST_ASSERT_EQUAL_UINT8(ERR_OK, res);
    TEST_ASSERT_EQUAL_UINT8(1, cmd.payload[0]);
}

void test_set_mode_bad_value() {
    uint8_t packet[] = { OP_SET_MODE, 0x04, 15 }; // Mode > 8
    ParsedCommand cmd;
    uint8_t res = parseCommandPacket(packet, sizeof(packet), cmd);
    TEST_ASSERT_EQUAL_UINT8(ERR_BAD_VALUE, res);
}

void test_unknown_opcode() {
    uint8_t packet[] = { 0xFE, 0x05, 0x00 };
    ParsedCommand cmd;
    uint8_t res = parseCommandPacket(packet, sizeof(packet), cmd);
    TEST_ASSERT_EQUAL_UINT8(ERR_UNKNOWN_OP, res);
}

void test_get_status() {
    uint8_t packet[] = { OP_GET_STATUS, 0x06 };
    ParsedCommand cmd;
    uint8_t res = parseCommandPacket(packet, sizeof(packet), cmd);
    TEST_ASSERT_EQUAL_UINT8(ERR_OK, res);
    TEST_ASSERT_EQUAL_UINT8(0, cmd.payloadLen);
}

void test_clear_bonds() {
    uint8_t packet[] = { OP_CLEAR_BONDS, 0x07 };
    ParsedCommand cmd;
    uint8_t res = parseCommandPacket(packet, sizeof(packet), cmd);
    TEST_ASSERT_EQUAL_UINT8(ERR_OK, res);
    TEST_ASSERT_EQUAL_UINT8(0, cmd.payloadLen);
}

// =============================================================
// Font & Clock & Timer Tests (B08)
// =============================================================
void test_digits_3x4_dimensions_and_masks() {
    for (int d = 0; d < 10; d++) {
        for (int col = 0; col < 3; col++) {
            uint8_t mask = DIGITS_3x4[d][col];
            TEST_ASSERT_BITS_LOW(0xF0, mask);
        }
    }

    TEST_ASSERT_EQUAL_HEX8(0x0F, DIGITS_3x4[0][0]);
    TEST_ASSERT_EQUAL_HEX8(0x09, DIGITS_3x4[0][1]);
    TEST_ASSERT_EQUAL_HEX8(0x0F, DIGITS_3x4[0][2]);

    TEST_ASSERT_EQUAL_HEX8(0x0F, DIGITS_3x4[1][2]);
}

void test_font_5x7_ascii_and_glyphs() {
    const uint8_t* space = getFont5x7(' ');
    for (int i = 0; i < 5; i++) {
        TEST_ASSERT_EQUAL_HEX8(0x00, space[i]);
    }

    const uint8_t* excl = getFont5x7('!');
    TEST_ASSERT_EQUAL_HEX8(0x5F, excl[2]);

    // '\x01' Heart
    const uint8_t* heart = getFont5x7('\x01');
    uint8_t heartLit = 0;
    for (int i = 0; i < 5; i++) {
        TEST_ASSERT_BITS_LOW(0x80, heart[i]);
        heartLit |= heart[i];
    }
    TEST_ASSERT_NOT_EQUAL(0, heartLit);

    // '\x02' Star
    const uint8_t* star = getFont5x7('\x02');
    uint8_t starLit = 0;
    for (int i = 0; i < 5; i++) {
        TEST_ASSERT_BITS_LOW(0x80, star[i]);
        starLit |= star[i];
    }
    TEST_ASSERT_NOT_EQUAL(0, starLit);

    // '\x03' Smile
    const uint8_t* smile = getFont5x7('\x03');
    uint8_t smileLit = 0;
    for (int i = 0; i < 5; i++) {
        TEST_ASSERT_BITS_LOW(0x80, smile[i]);
        smileLit |= smile[i];
    }
    TEST_ASSERT_NOT_EQUAL(0, smileLit);
}

void test_sync_time_packet_parser() {
    uint8_t packet[] = { OP_SYNC_TIME, 0x10, 0x00, 0xE4, 0x53, 0x65, 0xD4, 0xFE };
    ParsedCommand cmd;
    uint8_t res = parseCommandPacket(packet, sizeof(packet), cmd);
    TEST_ASSERT_EQUAL_UINT8(ERR_OK, res);
    TEST_ASSERT_EQUAL_UINT8(OP_SYNC_TIME, cmd.opcode);
    TEST_ASSERT_EQUAL_UINT8(6, cmd.payloadLen);

    uint8_t badPacket[] = { OP_SYNC_TIME, 0x10, 0x00, 0xE4, 0x53, 0x65, 0xD4 };
    res = parseCommandPacket(badPacket, sizeof(badPacket), cmd);
    TEST_ASSERT_EQUAL_UINT8(ERR_BAD_LEN, res);
}

void test_set_clock_style_packet_parser() {
    uint8_t p0[] = { OP_SET_CLOCK_STYLE, 0x11, 0x00 };
    ParsedCommand cmd;
    uint8_t res = parseCommandPacket(p0, sizeof(p0), cmd);
    TEST_ASSERT_EQUAL_UINT8(ERR_OK, res);
    TEST_ASSERT_EQUAL_UINT8(0, cmd.payload[0]);

    uint8_t p1[] = { OP_SET_CLOCK_STYLE, 0x11, 0x01 };
    res = parseCommandPacket(p1, sizeof(p1), cmd);
    TEST_ASSERT_EQUAL_UINT8(ERR_OK, res);
    TEST_ASSERT_EQUAL_UINT8(1, cmd.payload[0]);

    uint8_t pBadLen[] = { OP_SET_CLOCK_STYLE, 0x11 };
    res = parseCommandPacket(pBadLen, sizeof(pBadLen), cmd);
    TEST_ASSERT_EQUAL_UINT8(ERR_BAD_LEN, res);
}

void test_timer_packet_parser() {
    uint8_t pStart[] = { OP_START_TIMER, 0x12, 0x3C, 0x00, 0x00, 0x00 };
    ParsedCommand cmd;
    uint8_t res = parseCommandPacket(pStart, sizeof(pStart), cmd);
    TEST_ASSERT_EQUAL_UINT8(ERR_OK, res);
    TEST_ASSERT_EQUAL_UINT8(OP_START_TIMER, cmd.opcode);
    TEST_ASSERT_EQUAL_UINT8(4, cmd.payloadLen);

    uint8_t pBadStart[] = { OP_START_TIMER, 0x12, 0x3C, 0x00 };
    res = parseCommandPacket(pBadStart, sizeof(pBadStart), cmd);
    TEST_ASSERT_EQUAL_UINT8(ERR_BAD_LEN, res);

    uint8_t pCancel[] = { OP_CANCEL_TIMER, 0x13 };
    res = parseCommandPacket(pCancel, sizeof(pCancel), cmd);
    TEST_ASSERT_EQUAL_UINT8(ERR_OK, res);
    TEST_ASSERT_EQUAL_UINT8(OP_CANCEL_TIMER, cmd.opcode);
    TEST_ASSERT_EQUAL_UINT8(0, cmd.payloadLen);

    uint8_t pBadCancel[] = { OP_CANCEL_TIMER, 0x13, 0x01 };
    res = parseCommandPacket(pBadCancel, sizeof(pBadCancel), cmd);
    TEST_ASSERT_EQUAL_UINT8(ERR_BAD_LEN, res);
}

// =============================================================
// Scenes, Schedules, Sleep, Draw & Music Tests (B09)
// =============================================================
void test_scenes_save_and_load() {
    scenesSchedulerInit();
    state.mode = MODE_EFFECT;
    state.color = CRGB(100, 150, 200);
    state.brightness = 180;
    state.effect.base = 12;

    // Slot out of bounds
    TEST_ASSERT_FALSE(saveScene(8));
    TEST_ASSERT_FALSE(loadScene(8));

    // Save to slot 3
    TEST_ASSERT_TRUE(saveScene(3));
    TEST_ASSERT_EQUAL_UINT8(1, state.scenes[3].isValid);
    TEST_ASSERT_EQUAL_UINT8(MODE_EFFECT, state.scenes[3].mode);
    TEST_ASSERT_EQUAL_UINT8(180, state.scenes[3].brightness);

    // Modify state and load from slot 3
    state.mode = MODE_SOLID;
    state.brightness = 50;
    TEST_ASSERT_TRUE(loadScene(3));
    TEST_ASSERT_EQUAL_UINT8(MODE_EFFECT, state.mode);
    TEST_ASSERT_EQUAL_UINT8(180, state.brightness);

    // Load empty slot 4 fails
    TEST_ASSERT_FALSE(loadScene(4));
}

void test_schedules_bounds_and_setup() {
    // Valid slot 0..3
    TEST_ASSERT_TRUE(setSchedule(0, 1, 0x7F, 7, 30, 3, 15));
    TEST_ASSERT_EQUAL_UINT8(1, state.schedules[0].enabled);
    TEST_ASSERT_EQUAL_UINT8(0x7F, state.schedules[0].dayMask);
    TEST_ASSERT_EQUAL_UINT8(7, state.schedules[0].hh);
    TEST_ASSERT_EQUAL_UINT8(30, state.schedules[0].mm);
    TEST_ASSERT_EQUAL_UINT8(3, state.schedules[0].action); // SUNRISE_ALARM
    TEST_ASSERT_EQUAL_UINT8(15, state.schedules[0].arg);

    // Slot 4 out of bounds
    TEST_ASSERT_FALSE(setSchedule(4, 1, 0x7F, 8, 0, 0, 0));
}

void test_sleep_timer_bounds_and_lifecycle() {
    scenesSchedulerInit();
    state.brightness = 200;

    startSleepTimer(45);
    TEST_ASSERT_TRUE(isSleepTimerRunning());
    TEST_ASSERT_EQUAL_UINT16(45, getSleepTimerRemainingMinutes());

    // 0 cancels
    startSleepTimer(0);
    TEST_ASSERT_FALSE(isSleepTimerRunning());

    startSleepTimer(10);
    cancelSleepTimer();
    TEST_ASSERT_FALSE(isSleepTimerRunning());
}

void test_draw_chunk_bounds() {
    scenesSchedulerInit();
    uint8_t data[14] = { 1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 11, 12, 13, 14 };

    // Valid chunk within bounds
    TEST_ASSERT_TRUE(drawChunk(0, data, 14));
    TEST_ASSERT_TRUE(drawChunk(100, data, 14));

    // Near boundary: offset 295 on 300-byte buffer (clamped safely)
    TEST_ASSERT_TRUE(drawChunk(295, data, 14));

    // At or beyond boundary
    TEST_ASSERT_FALSE(drawChunk(DRAW_BUFFER_BYTES, data, 5));
    TEST_ASSERT_FALSE(drawChunk(DRAW_BUFFER_BYTES + 50, data, 5));

    // Invalid parameters
    TEST_ASSERT_FALSE(drawChunk(0, nullptr, 10));
    TEST_ASSERT_FALSE(drawChunk(0, data, 0));

    // Commit sets mode to DRAW
    drawCommit();
    TEST_ASSERT_EQUAL_UINT8(MODE_DRAW, state.mode);
}

void test_music_levels_feed_and_timeout() {
    scenesSchedulerInit();
    state.mode = MODE_SOLID;

    uint8_t levels[9] = { 20, 60, 100, 150, 200, 255, 180, 90, 40 };
    feedMusicLevels(levels, 9);
    TEST_ASSERT_EQUAL_UINT8(MODE_MUSIC, state.mode);

    // Immediate loop does not timeout
    uint32_t now = millis();
    musicLoop(now);
    TEST_ASSERT_EQUAL_UINT8(MODE_MUSIC, state.mode);

    // Timeout after 2600ms without new packets reverts back to MODE_SOLID
    musicLoop(now + 2600);
    TEST_ASSERT_EQUAL_UINT8(MODE_SOLID, state.mode);
}

void test_b09_packet_parsers() {
    ParsedCommand cmd;

    // OP_SAVE_SCENE
    uint8_t pSave[] = { OP_SAVE_SCENE, 0x20, 0x05 };
    TEST_ASSERT_EQUAL_UINT8(ERR_OK, parseCommandPacket(pSave, sizeof(pSave), cmd));
    uint8_t pSaveBad[] = { OP_SAVE_SCENE, 0x20, 0x08 }; // Slot 8 > 7
    TEST_ASSERT_EQUAL_UINT8(ERR_BAD_VALUE, parseCommandPacket(pSaveBad, sizeof(pSaveBad), cmd));

    // OP_LOAD_SCENE
    uint8_t pLoad[] = { OP_LOAD_SCENE, 0x21, 0x02 };
    TEST_ASSERT_EQUAL_UINT8(ERR_OK, parseCommandPacket(pLoad, sizeof(pLoad), cmd));

    // OP_SET_SCHEDULE
    uint8_t pSched[] = { OP_SET_SCHEDULE, 0x22, 0x01, 0x01, 0x3E, 0x07, 0x1E, 0x02, 0x03 };
    TEST_ASSERT_EQUAL_UINT8(ERR_OK, parseCommandPacket(pSched, sizeof(pSched), cmd));
    uint8_t pSchedBadSlot[] = { OP_SET_SCHEDULE, 0x22, 0x04, 0x01, 0x3E, 0x07, 0x1E, 0x02, 0x03 }; // Slot 4 > 3
    TEST_ASSERT_EQUAL_UINT8(ERR_BAD_VALUE, parseCommandPacket(pSchedBadSlot, sizeof(pSchedBadSlot), cmd));

    // OP_SET_SLEEP_TIMER
    uint8_t pSleep[] = { OP_SET_SLEEP_TIMER, 0x23, 0x1E, 0x00 }; // 30 minutes
    TEST_ASSERT_EQUAL_UINT8(ERR_OK, parseCommandPacket(pSleep, sizeof(pSleep), cmd));

    // OP_DRAW_CHUNK
    uint8_t pChunk[] = { OP_DRAW_CHUNK, 0x24, 0x00, 0x00, 0xFF, 0x00, 0x00 };
    TEST_ASSERT_EQUAL_UINT8(ERR_OK, parseCommandPacket(pChunk, sizeof(pChunk), cmd));

    // OP_DRAW_COMMIT
    uint8_t pCommit[] = { OP_DRAW_COMMIT, 0x25 };
    TEST_ASSERT_EQUAL_UINT8(ERR_OK, parseCommandPacket(pCommit, sizeof(pCommit), cmd));

    // OP_MUSIC_LEVELS (9 or 10 bytes)
    uint8_t pMusic9[] = { OP_MUSIC_LEVELS, 0x26, 10, 20, 30, 40, 50, 60, 70, 80, 90 };
    TEST_ASSERT_EQUAL_UINT8(ERR_OK, parseCommandPacket(pMusic9, sizeof(pMusic9), cmd));
    uint8_t pMusic10[] = { OP_MUSIC_LEVELS, 0x26, 10, 20, 30, 40, 50, 60, 70, 80, 90, 100 };
    TEST_ASSERT_EQUAL_UINT8(ERR_OK, parseCommandPacket(pMusic10, sizeof(pMusic10), cmd));
}

// =============================================================
// OTA Tests (B10)
// =============================================================
void test_ota_version_and_space_checks() {
    otaInit();

    // Anti-rollback: attempt to downgrade from v1.0.0 to v0.9.0 -> ERR_OTA_VER
    uint8_t res = otaHandleBegin(100000, 0, 9, 0);
    TEST_ASSERT_EQUAL_UINT8(ERR_OTA_VER, res);

    // Attempt 0 bytes -> ERR_OTA_SPACE
    res = otaHandleBegin(0, 1, 1, 0);
    TEST_ASSERT_EQUAL_UINT8(ERR_OTA_SPACE, res);

    // Attempt size exceeding 1.9MB partition limit -> ERR_OTA_SPACE
    res = otaHandleBegin(2500000, 1, 1, 0);
    TEST_ASSERT_EQUAL_UINT8(ERR_OTA_SPACE, res);
}

void test_ota_chunk_validation() {
    otaInit();

    // Chunk before begin -> ERR_BUSY
    uint8_t chunk[10] = { 0 };
    uint8_t res = otaHandleChunk(0, chunk, 10);
    TEST_ASSERT_EQUAL_UINT8(ERR_BUSY, res);

    // Commit before begin -> ERR_BUSY
    res = otaHandleCommit(0);
    TEST_ASSERT_EQUAL_UINT8(ERR_BUSY, res);
}

void test_ota_packet_parsers() {
    ParsedCommand cmd;

    // OP_OTA_BEGIN: totalBytes u32, fwMajor u8, fwMinor u8, fwPatch u8 (7 bytes)
    uint8_t pBegin[] = { OP_OTA_BEGIN, 0x30, 0x00, 0x00, 0x05, 0x00, 0x01, 0x01, 0x00 };
    TEST_ASSERT_EQUAL_UINT8(ERR_OK, parseCommandPacket(pBegin, sizeof(pBegin), cmd));
    uint8_t pBeginBad[] = { OP_OTA_BEGIN, 0x30, 0x00, 0x00, 0x05, 0x00, 0x01 }; // 5 bytes (bad len)
    TEST_ASSERT_EQUAL_UINT8(ERR_BAD_LEN, parseCommandPacket(pBeginBad, sizeof(pBeginBad), cmd));

    // OP_OTA_CHUNK: chunkSeq u16 + data (>= 2 bytes)
    uint8_t pChunk[] = { OP_OTA_CHUNK, 0x31, 0x00, 0x00, 0xAA, 0xBB, 0xCC };
    TEST_ASSERT_EQUAL_UINT8(ERR_OK, parseCommandPacket(pChunk, sizeof(pChunk), cmd));
    uint8_t pChunkBad[] = { OP_OTA_CHUNK, 0x31, 0x00 }; // 1 byte (bad len)
    TEST_ASSERT_EQUAL_UINT8(ERR_BAD_LEN, parseCommandPacket(pChunkBad, sizeof(pChunkBad), cmd));

    // OP_OTA_COMMIT: crc32 u32 (4 bytes or 0 bytes)
    uint8_t pCommit[] = { OP_OTA_COMMIT, 0x32, 0x12, 0x34, 0x56, 0x78 };
    TEST_ASSERT_EQUAL_UINT8(ERR_OK, parseCommandPacket(pCommit, sizeof(pCommit), cmd));
    uint8_t pCommitZero[] = { OP_OTA_COMMIT, 0x32 };
    TEST_ASSERT_EQUAL_UINT8(ERR_OK, parseCommandPacket(pCommitZero, sizeof(pCommitZero), cmd));

    // OP_OTA_ABORT: 0 bytes
    uint8_t pAbort[] = { OP_OTA_ABORT, 0x33 };
    TEST_ASSERT_EQUAL_UINT8(ERR_OK, parseCommandPacket(pAbort, sizeof(pAbort), cmd));
}

int runAllTests() {
    UNITY_BEGIN();
    // B06 tests
    RUN_TEST(test_set_color_valid);
    RUN_TEST(test_set_color_bad_len);
    RUN_TEST(test_set_brightness);
    RUN_TEST(test_set_effect_valid);
    RUN_TEST(test_set_effect_bad_value);
    RUN_TEST(test_set_mode_valid);
    RUN_TEST(test_set_mode_bad_value);
    RUN_TEST(test_unknown_opcode);
    RUN_TEST(test_get_status);
    RUN_TEST(test_clear_bonds);
    // B08 tests
    RUN_TEST(test_digits_3x4_dimensions_and_masks);
    RUN_TEST(test_font_5x7_ascii_and_glyphs);
    RUN_TEST(test_sync_time_packet_parser);
    RUN_TEST(test_set_clock_style_packet_parser);
    RUN_TEST(test_timer_packet_parser);
    // B09 tests
    RUN_TEST(test_scenes_save_and_load);
    RUN_TEST(test_schedules_bounds_and_setup);
    RUN_TEST(test_sleep_timer_bounds_and_lifecycle);
    RUN_TEST(test_draw_chunk_bounds);
    RUN_TEST(test_music_levels_feed_and_timeout);
    RUN_TEST(test_b09_packet_parsers);
    // B10 tests
    RUN_TEST(test_ota_version_and_space_checks);
    RUN_TEST(test_ota_chunk_validation);
    RUN_TEST(test_ota_packet_parsers);
    return UNITY_END();
}

#if defined(ARDUINO)
void setup() {
    delay(2000);
    runAllTests();
}
void loop() {}
#else
int main(int argc, char **argv) {
    return runAllTests();
}
#endif
