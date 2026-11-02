#include <Arduino.h>
#include <unity.h>
#include "packet_parser.h"
#include "../src/packet_parser.cpp"
#include "font.h"
#include "../src/font.cpp"

void setUp(void) {}
void tearDown(void) {}

// Parser Tests (B06)
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

// Font & Clock & Timer Tests (B08)
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
