#include "packet_parser.h"
#include <string.h>

uint8_t parseCommandPacket(const uint8_t* data, size_t len, ParsedCommand& out) {
    if (!data || len < 2) {
        return ERR_BAD_LEN;
    }

    uint8_t opcode = data[0];
    uint8_t seq = data[1];
    size_t payloadLen = len - 2;

    out.opcode = opcode;
    out.seq = seq;
    out.payloadLen = (uint8_t)payloadLen;

    // Validate opcode range
    if (opcode < 1 || opcode > 28) {
        return ERR_UNKNOWN_OP;
    }

    // Validate payload length and constraints per opcode
    switch (opcode) {
        case OP_SET_COLOR:
            if (payloadLen != 3) return ERR_BAD_LEN;
            break;

        case OP_SET_BRIGHTNESS:
            if (payloadLen != 1) return ERR_BAD_LEN;
            break;

        case OP_SET_EFFECT:
            if (payloadLen != 11) return ERR_BAD_LEN;
            // Dir in [0..5], colorMode in [0..3]
            if (data[4] > 5) return ERR_BAD_VALUE; // dir
            if (data[6] > 3) return ERR_BAD_VALUE; // colorMode
            break;

        case OP_SET_PALETTE: {
            if (payloadLen < 1) return ERR_BAD_LEN;
            uint8_t count = data[2];
            if (count < 1 || count > 8) return ERR_BAD_VALUE;
            if (payloadLen != (size_t)(1 + count * 3)) return ERR_BAD_LEN;
            break;
        }

        case OP_SET_MODE:
            if (payloadLen != 1) return ERR_BAD_LEN;
            if (data[2] > 8) return ERR_BAD_VALUE; // Mode in 0..8
            break;

        case OP_SYNC_TIME:
            if (payloadLen != 6) return ERR_BAD_LEN;
            break;

        case OP_SET_CLOCK_STYLE:
            if (payloadLen != 1) return ERR_BAD_LEN;
            break;

        case OP_START_TIMER:
            if (payloadLen != 4) return ERR_BAD_LEN;
            break;

        case OP_CANCEL_TIMER:
        case OP_PLAY_BIRTHDAY:
        case OP_STOP_SHOW:
            if (payloadLen != 0) return ERR_BAD_LEN;
            break;

        case OP_SET_TEXT:
            if (payloadLen < 3 || payloadLen > 17) return ERR_BAD_LEN;
            if (data[2] > 2) return ERR_BAD_VALUE; // target in 0..2
            break;

        case OP_SHOW_MESSAGE:
            if (payloadLen != 1) return ERR_BAD_LEN;
            break;

        case OP_SAVE_SCENE:
        case OP_LOAD_SCENE:
            if (payloadLen != 1) return ERR_BAD_LEN;
            if (data[2] > 7) return ERR_BAD_VALUE; // Slot 0..7
            break;

        case OP_SET_SCHEDULE:
            if (payloadLen != 7) return ERR_BAD_LEN;
            if (data[2] > 3) return ERR_BAD_VALUE; // Slot 0..3
            break;

        case OP_SET_SLEEP_TIMER:
            if (payloadLen != 2) return ERR_BAD_LEN;
            break;

        case OP_DRAW_CHUNK:
            if (payloadLen < 2 || payloadLen > 16) return ERR_BAD_LEN;
            break;

        case OP_DRAW_COMMIT:
            if (payloadLen != 0) return ERR_BAD_LEN;
            break;

        case OP_MUSIC_LEVELS:
            if (payloadLen != 9 && payloadLen != 10) return ERR_BAD_LEN;
            break;

        case OP_SET_MUTE:
            if (payloadLen != 1) return ERR_BAD_LEN;
            break;

        case OP_GET_STATUS:
        case OP_CLEAR_BONDS:
            if (payloadLen != 0) return ERR_BAD_LEN;
            break;

        case OP_FACTORY_RESET:
            if (payloadLen != 2) return ERR_BAD_LEN;
            if (data[2] != 0xA5 || data[3] != 0x5A) return ERR_BAD_VALUE;
            break;

        case OP_OTA_BEGIN:
            if (payloadLen != 7) return ERR_BAD_LEN;
            break;

        case OP_OTA_CHUNK:
            if (payloadLen < 2) return ERR_BAD_LEN;
            break;

        case OP_OTA_COMMIT:
            if (payloadLen != 0 && payloadLen != 4) return ERR_BAD_LEN;
            break;

        case OP_OTA_ABORT:
            if (payloadLen != 0) return ERR_BAD_LEN;
            break;

        default:
            return ERR_UNKNOWN_OP;
    }

    if (payloadLen > 0 && payloadLen <= sizeof(out.payload)) {
        memcpy(out.payload, data + 2, payloadLen);
    }

    return ERR_OK;
}

