#include "ota_manager.h"
#include "matrix.h"
#include <Update.h>
#include <esp_ota_ops.h>

static bool isOtaActive = false;
static uint32_t totalExpectedBytes = 0;
static uint32_t writtenBytes = 0;
static uint16_t expectedSeq = 0;

static bool rebootPending = false;
static uint32_t rebootTime = 0;

void otaInit() {
    isOtaActive = false;
    rebootPending = false;
    writtenBytes = 0;

    const esp_partition_t* running = esp_ota_get_running_partition();
    if (running) {
        Serial.printf("[OTA] Running partition: %s (address 0x%08X, size 0x%08X)\n",
                      running->label, running->address, running->size);

        esp_ota_img_states_t ota_state;
        if (esp_ota_get_state_partition(running, &ota_state) == ESP_OK) {
            if (ota_state == ESP_OTA_IMG_PENDING_VERIFY) {
                Serial.println("[OTA] First boot on new partition! Self-test passed. Cancelling rollback.");
                esp_ota_mark_app_valid_cancel_rollback();
            } else if (ota_state == ESP_OTA_IMG_VALID) {
                Serial.println("[OTA] Current image state: VALID.");
            }
        }
    }
    Serial.printf("[OTA] Firmware Version: v%u.%u.%u\n",
                  FW_VERSION_MAJOR, FW_VERSION_MINOR, FW_VERSION_PATCH);
}

bool isOtaRunning() {
    return isOtaActive;
}

uint8_t getOtaProgressPercent() {
    if (!isOtaActive || totalExpectedBytes == 0) return 0;
    return (uint8_t)((writtenBytes * 100ULL) / totalExpectedBytes);
}

uint8_t otaHandleBegin(uint32_t totalBytes, uint8_t major, uint8_t minor, uint8_t patch) {
    // 1. Version check (anti-rollback: reject older firmware version)
    uint32_t newVer = ((uint32_t)major << 16) | ((uint32_t)minor << 8) | patch;
    uint32_t currVer = ((uint32_t)FW_VERSION_MAJOR << 16) | ((uint32_t)FW_VERSION_MINOR << 8) | FW_VERSION_PATCH;
    if (newVer < currVer) {
        Serial.printf("[OTA] Version check failed: v%u.%u.%u < v%u.%u.%u\n",
                      major, minor, patch, FW_VERSION_MAJOR, FW_VERSION_MINOR, FW_VERSION_PATCH);
        return ERR_OTA_VER;
    }

    // 2. Partition space check
    if (totalBytes == 0 || totalBytes > 1900000) {
        Serial.printf("[OTA] Size check failed: %lu bytes exceeds limit\n", (unsigned long)totalBytes);
        return ERR_OTA_SPACE;
    }

    // 3. Begin flash update
    if (!Update.begin(totalBytes, U_FLASH)) {
        Serial.printf("[OTA] Update.begin failed. Error: %u\n", Update.getError());
        return ERR_OTA_SPACE;
    }

    totalExpectedBytes = totalBytes;
    writtenBytes = 0;
    expectedSeq = 0;
    isOtaActive = true;
    Serial.printf("[OTA] Started OTA update: %lu bytes, target v%u.%u.%u\n",
                  (unsigned long)totalBytes, major, minor, patch);
    return ERR_OK;
}

uint8_t otaHandleChunk(uint16_t chunkSeq, const uint8_t* data, size_t len) {
    if (!isOtaActive) return ERR_BUSY;

    if (chunkSeq != expectedSeq) {
        Serial.printf("[OTA] Chunk sequence mismatch! Got %u, expected %u\n", chunkSeq, expectedSeq);
        return ERR_BAD_VALUE;
    }

    size_t written = Update.write((uint8_t*)data, len);
    if (written != len) {
        Serial.printf("[OTA] Write error: wrote %u of %u bytes\n", (unsigned int)written, (unsigned int)len);
        Update.abort();
        isOtaActive = false;
        return ERR_OTA_WRITE;
    }

    expectedSeq++;
    writtenBytes += len;

    if ((expectedSeq % 50) == 0 || writtenBytes >= totalExpectedBytes) {
        Serial.printf("[OTA] Progress: %lu / %lu bytes (%u%%)\n",
                      (unsigned long)writtenBytes, (unsigned long)totalExpectedBytes, getOtaProgressPercent());
    }
    return ERR_OK;
}

uint8_t otaHandleCommit(uint32_t expectedCrc) {
    if (!isOtaActive) return ERR_BUSY;

    if (!Update.end(true)) {
        Serial.printf("[OTA] Update.end() failed: error %u\n", Update.getError());
        Update.abort();
        isOtaActive = false;
        return ERR_OTA_WRITE;
    }

    if (!Update.isFinished()) {
        Serial.println("[OTA] Update not finished properly!");
        Update.abort();
        isOtaActive = false;
        return ERR_OTA_WRITE;
    }

    isOtaActive = false;
    Serial.println("[OTA] Update committed successfully! Rebooting in 500ms...");
    rebootPending = true;
    rebootTime = millis() + 500;
    return ERR_OK;
}

void otaHandleAbort() {
    if (isOtaActive) {
        Update.abort();
        isOtaActive = false;
        writtenBytes = 0;
        Serial.println("[OTA] Update aborted by request.");
    }
}

void otaRender(uint32_t now, CRGB* leds) {
    matrixClear();

    uint8_t pct = getOtaProgressPercent();
    // Fill bottom row(s) according to percent
    int litLeds = (pct * NUM_LEDS) / 100;
    if (litLeds > NUM_LEDS) litLeds = NUM_LEDS;

    // Pulse brightness
    uint8_t pulse = beatsin8(60, 80, 255);
    CRGB otaColor = CRGB(0, 150, 255);
    otaColor.nscale8_video(pulse);

    for (int i = 0; i < litLeds; i++) {
        leds[i] = otaColor;
    }
}

void otaLoop() {
    if (rebootPending && millis() >= rebootTime) {
        rebootPending = false;
        Serial.println("[OTA] Rebooting now into new firmware...");
        Serial.flush();
        ESP.restart();
    }
}

