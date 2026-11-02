#include <Arduino.h>
#include <unity.h>
#include "packet_parser.h"
#include "../src/packet_parser.cpp"

void setUp(void) {}
void tearDown(void) {}

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

int runParserTests() {
    UNITY_BEGIN();
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
    return UNITY_END();
}

#if defined(ARDUINO)
void setup() {
    delay(2000);
    runParserTests();
}
void loop() {}
#else
int main(int argc, char **argv) {
    return runParserTests();
}
#endif

