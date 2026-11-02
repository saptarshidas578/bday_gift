#pragma once

#include <stdint.h>
#include <stddef.h>

// Error codes matching protocol/protocol.json
enum ProtocolErrorCode : uint8_t {
    ERR_OK = 0,
    ERR_UNKNOWN_OP = 1,
    ERR_BAD_LEN = 2,
    ERR_BAD_VALUE = 3,
    ERR_BUSY = 4,
    ERR_UNSUPPORTED = 5,
    ERR_STORAGE_ERR = 6
};

// Opcodes matching protocol/protocol.json
enum ProtocolOpcode : uint8_t {
    OP_SET_COLOR = 1,
    OP_SET_BRIGHTNESS = 2,
    OP_SET_EFFECT = 3,
    OP_SET_PALETTE = 4,
    OP_SET_MODE = 5,
    OP_SYNC_TIME = 6,
    OP_SET_CLOCK_STYLE = 7,
    OP_START_TIMER = 8,
    OP_CANCEL_TIMER = 9,
    OP_PLAY_BIRTHDAY = 10,
    OP_STOP_SHOW = 11,
    OP_SET_TEXT = 12,
    OP_SHOW_MESSAGE = 13,
    OP_SAVE_SCENE = 14,
    OP_LOAD_SCENE = 15,
    OP_SET_SCHEDULE = 16,
    OP_SET_SLEEP_TIMER = 17,
    OP_DRAW_CHUNK = 18,
    OP_DRAW_COMMIT = 19,
    OP_MUSIC_LEVELS = 20,
    OP_SET_MUTE = 21,
    OP_GET_STATUS = 22,
    OP_CLEAR_BONDS = 23,
    OP_FACTORY_RESET = 24
};

struct ParsedCommand {
    uint8_t opcode;
    uint8_t seq;
    uint8_t payloadLen;
    uint8_t payload[24];
};

// Parses and validates a raw packet: [opcode u8][seq u8][payload...]
// Returns ERR_OK on valid packet, or an error code from ProtocolErrorCode
uint8_t parseCommandPacket(const uint8_t* data, size_t len, ParsedCommand& out);

