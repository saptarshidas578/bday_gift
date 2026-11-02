#pragma once

#include <Arduino.h>
#include "packet_parser.h"

// UUIDs matching SPEC section 7.2 and protocol.json
#define PIXELGIFT_SERVICE_UUID        "7a1c0001-5d3e-4b8a-9c21-8f6d2e4a1b00"
#define PIXELGIFT_CHAR_CMD_UUID       "7a1c0002-5d3e-4b8a-9c21-8f6d2e4a1b00"
#define PIXELGIFT_CHAR_STATUS_UUID    "7a1c0003-5d3e-4b8a-9c21-8f6d2e4a1b00"

// Status Notification Headers (protocol.json)
#define HEADER_ACK      0xA0 // 160
#define HEADER_SNAPSHOT 0xA1 // 161

// Flags bitmask
#define FLAG_MUTED          (1 << 0)
#define FLAG_TIME_SYNCED    (1 << 1)
#define FLAG_TIMER_RUNNING  (1 << 2)
#define FLAG_SHOW_RUNNING   (1 << 3)
#define FLAG_POWER_CAPPED   (1 << 4)

// Initialize NimBLE server, security, characteristics, and advertising
void bleInit();

// Send ACK notification to connected client: [0xA0][seq][code]
void bleSendAck(uint8_t seq, uint8_t code);

// Send full state snapshot notification to client: [0xA1][protoVer]...
void bleSendSnapshot();

// Queue access for main loop dispatch
bool bleHasCommand();
bool blePopCommand(ParsedCommand& out);

// Connection status
bool bleIsConnected();

// Clear all bonded devices
void bleClearBonds();

