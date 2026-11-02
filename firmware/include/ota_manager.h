#pragma once

#include <Arduino.h>
#include <FastLED.h>
#include "config.h"
#include "packet_parser.h"

// Initialize OTA and validate newly booted partition to cancel rollback
void otaInit();

// Check if an OTA transfer is currently active
bool isOtaRunning();

// Returns OTA transfer progress in percent (0..100)
uint8_t getOtaProgressPercent();

// BLE OTA Handlers
uint8_t otaHandleBegin(uint32_t totalBytes, uint8_t major, uint8_t minor, uint8_t patch);
uint8_t otaHandleChunk(uint16_t chunkSeq, const uint8_t* data, size_t len);
uint8_t otaHandleCommit(uint32_t expectedCrc);
void otaHandleAbort();

// Render progress animation on matrix while OTA is running
void otaRender(uint32_t now, CRGB* leds);

// Check if pending reboot should occur
void otaLoop();

