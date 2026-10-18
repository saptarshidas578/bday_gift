package com.pixelgift.app.data.model

/**
 * Discovered BLE Device during scanning.
 */
data class DiscoveredDevice(
    val name: String,
    val address: String,
    val rssi: Int, // in dBm, e.g. -45, -70
    val isBonded: Boolean = false
) {
    val signalStrengthBars: Int
        get() = when {
            rssi >= -60 -> 4 // Excellent
            rssi >= -70 -> 3 // Good
            rssi >= -80 -> 2 // Fair
            else -> 1        // Weak
        }
}

/**
 * Bluetooth / Location permission status.
 */
enum class PermissionStatus {
    NOT_REQUESTED,
    GRANTED,
    DENIED,
    PERMANENTLY_DENIED
}

/**
 * State machine for BLE scanning and pairing process.
 */
sealed interface PairingUiState {
    data object Idle : PairingUiState
    data object Scanning : PairingUiState
    data class Discovered(val devices: List<DiscoveredDevice>) : PairingUiState
    data class Connecting(val device: DiscoveredDevice) : PairingUiState
    data class Success(val device: DiscoveredDevice) : PairingUiState
    data class Failed(val device: DiscoveredDevice?, val reason: String) : PairingUiState
}
