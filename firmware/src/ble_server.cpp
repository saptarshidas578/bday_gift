#include "ble_server.h"
#include <NimBLEDevice.h>
#include "secrets.h"
#include "storage.h"
#include "config.h"
#include <esp_mac.h>

static NimBLEServer* pServer = nullptr;
static NimBLECharacteristic* pCmdChar = nullptr;
static NimBLECharacteristic* pStatusChar = nullptr;
static bool deviceConnected = false;
static QueueHandle_t cmdQueue = nullptr;

class ServerCallbacks : public NimBLEServerCallbacks {
    void onConnect(NimBLEServer* pServer, ble_gap_conn_desc* desc) override {
        deviceConnected = true;
        Serial.printf("[BLE] Client connected (%s)\n",
                      desc ? NimBLEAddress(desc->peer_ota_addr).toString().c_str() : "peer");
        bleSendSnapshot();
    }

    void onDisconnect(NimBLEServer* pServer) override {
        deviceConnected = false;
        Serial.println("[BLE] Client disconnected. Re-advertising...");
        NimBLEDevice::startAdvertising();
    }

    void onAuthenticationComplete(ble_gap_conn_desc* desc) override {
        if (!desc->sec_state.encrypted) {
            Serial.println("[BLE] Security failed: not encrypted");
        } else {
            Serial.printf("[BLE] Authenticated & Bonded! (Encrypted: %d, Authenticated: %d, Bonded: %d)\n",
                          desc->sec_state.encrypted, desc->sec_state.authenticated, desc->sec_state.bonded);
        }
    }
};

class CmdCallbacks : public NimBLECharacteristicCallbacks {
    void onWrite(NimBLECharacteristic* pChar) override {
        std::string val = pChar->getValue();
        if (val.length() < 2) {
            bleSendAck(0, ERR_BAD_LEN);
            return;
        }

        ParsedCommand cmd;
        uint8_t res = parseCommandPacket((const uint8_t*)val.data(), val.length(), cmd);
        bleSendAck(cmd.seq, res);

        if (res == ERR_OK && cmdQueue) {
            xQueueSend(cmdQueue, &cmd, 0);
        }
    }
};

void bleInit() {
    // Generate device name: PixelGift-XXXX from last two MAC bytes
    uint8_t mac[6];
    esp_read_mac(mac, ESP_MAC_BT);
    char devName[32];
    snprintf(devName, sizeof(devName), "PixelGift-%02X%02X", mac[4], mac[5]);

    Serial.printf("[BLE] Initializing NimBLE with name: %s\n", devName);
    NimBLEDevice::init(devName);

    // FreeRTOS queue for thread-safe command dispatch to main loop
    if (!cmdQueue) {
        cmdQueue = xQueueCreate(16, sizeof(ParsedCommand));
    }

    // Security configuration: LE Secure Connections + Bonding + Static Passkey
    NimBLEDevice::setSecurityAuth(true, true, true); // bonding, mitm/authen, sc
    NimBLEDevice::setSecurityIOCap(ESP_IO_CAP_OUT);   // Display only
    NimBLEDevice::setSecurityInitKey(ESP_BLE_ENC_KEY_MASK | ESP_BLE_ID_KEY_MASK);
    NimBLEDevice::setSecurityRespKey(ESP_BLE_ENC_KEY_MASK | ESP_BLE_ID_KEY_MASK);
    NimBLEDevice::setSecurityPasskey(BLE_PASSKEY);

    pServer = NimBLEDevice::createServer();
    pServer->setCallbacks(new ServerCallbacks());

    NimBLEService* pService = pServer->createService(PIXELGIFT_SERVICE_UUID);

    // CMD Characteristic: Write / Write Without Response, Encrypted + Authenticated
    pCmdChar = pService->createCharacteristic(
        PIXELGIFT_CHAR_CMD_UUID,
        NIMBLE_PROPERTY::WRITE | NIMBLE_PROPERTY::WRITE_NR |
        NIMBLE_PROPERTY::WRITE_ENC | NIMBLE_PROPERTY::WRITE_AUTHEN
    );
    pCmdChar->setCallbacks(new CmdCallbacks());

    // STATUS Characteristic: Read / Notify, Encrypted + Authenticated
    pStatusChar = pService->createCharacteristic(
        PIXELGIFT_CHAR_STATUS_UUID,
        NIMBLE_PROPERTY::READ | NIMBLE_PROPERTY::NOTIFY |
        NIMBLE_PROPERTY::READ_ENC | NIMBLE_PROPERTY::READ_AUTHEN
    );

    pService->start();

    // Advertising setup
    NimBLEAdvertising* pAdvertising = NimBLEDevice::getAdvertising();
    pAdvertising->addServiceUUID(PIXELGIFT_SERVICE_UUID);
    pAdvertising->setScanResponse(true);
    pAdvertising->start();

    Serial.printf("[BLE] Service %s started. Advertising with passkey: %06u\n",
                  PIXELGIFT_SERVICE_UUID, BLE_PASSKEY);
}

void bleSendAck(uint8_t seq, uint8_t code) {
    if (!pStatusChar) return;
    uint8_t ack[3] = { HEADER_ACK, seq, code };
    pStatusChar->setValue(ack, sizeof(ack));
    if (deviceConnected) {
        pStatusChar->notify();
    }
}

void bleSendSnapshot() {
    if (!pStatusChar || !deviceConnected) return;

    uint8_t snapshot[12];
    snapshot[0] = HEADER_SNAPSHOT; // 0xA1
    snapshot[1] = 1;              // Proto version
    snapshot[2] = state.mode;
    snapshot[3] = state.brightness;
    snapshot[4] = state.color.r;
    snapshot[5] = state.color.g;
    snapshot[6] = state.color.b;
    snapshot[7] = state.effect.base;
    snapshot[8] = state.effect.speed;

    uint8_t flags = 0;
    if (state.isMuted) flags |= FLAG_MUTED;
    flags |= FLAG_POWER_CAPPED; // FastLED 2500mA hardware cap active

    snapshot[9] = flags;
    snapshot[10] = 1; // fwMajor
    snapshot[11] = 0; // fwMinor

    pStatusChar->setValue(snapshot, sizeof(snapshot));
    pStatusChar->notify();
}

bool bleHasCommand() {
    return cmdQueue && (uxQueueMessagesWaiting(cmdQueue) > 0);
}

bool blePopCommand(ParsedCommand& out) {
    if (!cmdQueue) return false;
    return (xQueueReceive(cmdQueue, &out, 0) == pdTRUE);
}

bool bleIsConnected() {
    return deviceConnected;
}

void bleClearBonds() {
    Serial.println("[BLE] Clearing all bonded devices...");
    NimBLEDevice::deleteAllBonds();
}

