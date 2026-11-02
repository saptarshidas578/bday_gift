package com.pixelgift.app.ui.device

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.pixelgift.app.data.model.DiscoveredDevice
import com.pixelgift.app.data.model.PairingUiState
import com.pixelgift.app.data.model.PermissionStatus
import com.pixelgift.app.protocol.Command
import com.pixelgift.app.protocol.DeviceNotification
import com.pixelgift.app.transport.ConnectionState
import com.pixelgift.app.transport.DeviceTransport
import com.pixelgift.app.transport.BleTransport
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class DeviceViewModel(
    val transport: DeviceTransport,
    private val externalScope: CoroutineScope? = null
) : ViewModel() {

    private val scope: CoroutineScope
        get() = externalScope ?: viewModelScope

    // Connection & Status from Transport
    val connectionState: StateFlow<ConnectionState> = transport.connectionState
    val statusSnapshot: StateFlow<DeviceNotification.StatusSnapshot> = transport.status

    // --- Permission State ---
    private val _permissionStatus = MutableStateFlow(PermissionStatus.NOT_REQUESTED)
    val permissionStatus: StateFlow<PermissionStatus> = _permissionStatus.asStateFlow()

    // --- Pairing State ---
    private val _pairingState = MutableStateFlow<PairingUiState>(PairingUiState.Idle)
    val pairingState: StateFlow<PairingUiState> = _pairingState.asStateFlow()

    private val _discoveredDevices = MutableStateFlow<List<DiscoveredDevice>>(emptyList())
    val discoveredDevices: StateFlow<List<DiscoveredDevice>> = _discoveredDevices.asStateFlow()

    private val _selectedDeviceForPasskey = MutableStateFlow<DiscoveredDevice?>(null)
    val selectedDeviceForPasskey: StateFlow<DiscoveredDevice?> = _selectedDeviceForPasskey.asStateFlow()

    // --- Device Tab Settings ---
    private val _autoReconnect = MutableStateFlow(true)
    val autoReconnect: StateFlow<Boolean> = _autoReconnect.asStateFlow()

    private val _feedback = MutableStateFlow<String?>(null)
    val feedback: StateFlow<String?> = _feedback.asStateFlow()

    private var scanJob: Job? = null

    var sendCountForTesting: Int = 0
        private set

    // Default simulated devices for discovery list
    companion object {
        val SIMULATED_SCAN_RESULTS = listOf(
            DiscoveredDevice(name = "PixelGift-Sim", address = "FA:KE:DE:VI:CE:00", rssi = -48),
            DiscoveredDevice(name = "PixelGift-781C", address = "78:1C:E4:9B:10:22", rssi = -64),
            DiscoveredDevice(name = "PixelGift-4A2B", address = "4A:2B:99:31:0A:7F", rssi = -79)
        )
    }

    // --- Permission Actions ---

    fun onPermissionResult(isGranted: Boolean, shouldShowRationale: Boolean) {
        _permissionStatus.value = when {
            isGranted -> PermissionStatus.GRANTED
            shouldShowRationale -> PermissionStatus.DENIED
            else -> PermissionStatus.PERMANENTLY_DENIED
        }
    }

    fun setPermissionStatusForTesting(status: PermissionStatus) {
        _permissionStatus.value = status
    }

    // --- Pairing Actions ---

    fun startScan() {
        scanJob?.cancel()
        _pairingState.value = PairingUiState.Scanning

        scanJob = scope.launch {
            if (transport is BleTransport) {
                try {
                    transport.scanDevices().collect { devices ->
                        _discoveredDevices.value = devices
                        _pairingState.value = PairingUiState.Discovered(devices)
                    }
                } catch (e: Exception) {
                    _pairingState.value = PairingUiState.Failed(null, e.message ?: "Scan failed")
                }
            } else {
                // Simulate scan delay (500ms) for FakeDevice or unit testing
                delay(500L)
                _discoveredDevices.value = SIMULATED_SCAN_RESULTS
                _pairingState.value = PairingUiState.Discovered(SIMULATED_SCAN_RESULTS)
            }
        }
    }

    fun stopScan() {
        scanJob?.cancel()
        scanJob = null
        if (_pairingState.value is PairingUiState.Scanning) {
            _pairingState.value = PairingUiState.Idle
        }
    }

    fun onSelectDeviceForPasskey(device: DiscoveredDevice) {
        if (device.isBonded) {
            confirmPairing(device, "")
        } else {
            _selectedDeviceForPasskey.value = device
        }
    }

    fun onDismissPasskeyDialog() {
        _selectedDeviceForPasskey.value = null
    }

    fun confirmPairing(device: DiscoveredDevice, passkey: String) {
        stopScan()
        _selectedDeviceForPasskey.value = null

        if (!device.isBonded && passkey.isNotBlank()) {
            if (passkey.length != 6 || !passkey.all { it.isDigit() }) {
                _pairingState.value = PairingUiState.Failed(device, "Passkey must be exactly 6 digits")
                return
            }
            if (transport is BleTransport) {
                transport.setPendingPasskey(passkey)
            }
        } else if (!device.isBonded && passkey.isBlank()) {
            _pairingState.value = PairingUiState.Failed(device, "Passkey is required for unbonded devices")
            return
        }

        _pairingState.value = PairingUiState.Connecting(device)

        scope.launch {
            try {
                transport.connect(device.address)
                _pairingState.value = PairingUiState.Success(device)
                _feedback.value = "Successfully connected with ${device.name}"
            } catch (e: Exception) {
                _pairingState.value = PairingUiState.Failed(device, e.message ?: "Failed to connect to device")
            }
        }
    }

    fun getScanJobForTesting(): Job? = scanJob

    fun setPairingStateForTesting(state: PairingUiState) {
        _pairingState.value = state
    }

    fun resetPairingState() {
        _pairingState.value = PairingUiState.Idle
        _selectedDeviceForPasskey.value = null
    }

    // --- Device Tab Actions ---

    fun onToggleConnect() {
        scope.launch {
            try {
                when (transport.connectionState.value) {
                    is ConnectionState.Disconnected -> {
                        val addr = if (transport is BleTransport) {
                            transport.getLastBondedAddress()
                        } else {
                            "FA:KE:DE:VI:CE:00"
                        }
                        if (!addr.isNullOrBlank()) {
                            _feedback.value = "Connecting to device…"
                            transport.connect(addr)
                        } else {
                            _feedback.value = "No bonded device found. Please tap 'Pair Device…' to pair."
                        }
                    }
                    is ConnectionState.Connected -> {
                        transport.disconnect()
                        _feedback.value = "Disconnected from device"
                    }
                    else -> {}
                }
            } catch (e: Exception) {
                _feedback.value = "Connection failed: ${e.message}"
            }
        }
    }

    fun onFactoryReset() {
        sendCountForTesting++
        scope.launch {
            try {
                transport.send(Command.FactoryReset)
                _feedback.value = "Factory reset signal sent to device"
            } catch (e: Exception) {
                _feedback.value = "Failed to send factory reset: ${e.message}"
            }
        }
    }

    fun onToggleAutoReconnect(enabled: Boolean) {
        _autoReconnect.value = enabled
        if (transport is BleTransport) {
            transport.setAutoReconnect(enabled)
        }
        _feedback.value = if (enabled) "Auto-reconnect enabled" else "Auto-reconnect disabled"
    }

    fun onToggleMute(muted: Boolean) {
        sendCountForTesting++
        scope.launch {
            transport.send(Command.SetMute(muted))
            _feedback.value = if (muted) "Piezo buzzer muted" else "Piezo buzzer unmuted"
        }
    }

    fun onClearBonds() {
        sendCountForTesting++
        scope.launch {
            transport.send(Command.ClearBonds)
            _feedback.value = "Bonding keys cleared on device"
        }
    }

    fun onBeginOta() {
        sendCountForTesting++
        scope.launch {
            transport.send(Command.OtaBegin)
            _feedback.value = "OTA firmware update mode initiated"
        }
    }

    fun onSaveScene(slot: Int) {
        require(slot in 0..7) { "Slot must be 0..7" }
        sendCountForTesting++
        scope.launch {
            transport.send(Command.SaveScene(slot))
            _feedback.value = "Current state saved to Scene $slot"
        }
    }

    fun onLoadScene(slot: Int) {
        require(slot in 0..7) { "Slot must be 0..7" }
        sendCountForTesting++
        scope.launch {
            transport.send(Command.LoadScene(slot))
            _feedback.value = "Scene $slot loaded"
        }
    }

    fun onRefreshStatus() {
        sendCountForTesting++
        scope.launch {
            transport.send(Command.GetStatus)
            _feedback.value = "Refreshed device status"
        }
    }

    fun clearFeedback() {
        _feedback.value = null
    }

    override fun onCleared() {
        super.onCleared()
        stopScan()
    }
}
