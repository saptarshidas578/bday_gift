package com.pixelgift.app.transport

import com.pixelgift.app.protocol.Command
import com.pixelgift.app.protocol.DeviceNotification
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.StateFlow

sealed interface ConnectionState {
    data object Disconnected : ConnectionState
    data object Connecting : ConnectionState
    data class Connected(
        val deviceName: String,
        val address: String
    ) : ConnectionState
    data object Disconnecting : ConnectionState
}

/**
 * Abstraction for device transport layer over BLE or simulated in-memory.
 */
interface DeviceTransport {
    val connectionState: StateFlow<ConnectionState>
    val status: StateFlow<DeviceNotification.StatusSnapshot>
    val notifications: Flow<DeviceNotification>

    suspend fun connect(address: String = "FA:KE:DE:VI:CE:00")
    suspend fun disconnect()
    suspend fun send(command: Command): Result<Unit>
}
