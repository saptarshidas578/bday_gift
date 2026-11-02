package com.pixelgift.app.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.pixelgift.app.protocol.Command
import com.pixelgift.app.protocol.DeviceNotification
import com.pixelgift.app.transport.ConnectionState
import com.pixelgift.app.transport.DeviceTransport
import com.pixelgift.app.transport.FakeDevice
import com.pixelgift.app.ui.components.DeviceConnectionStatus
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class MainViewModel(
    val transport: DeviceTransport = FakeDevice()
) : ViewModel() {

    val connectionState: StateFlow<ConnectionState> = transport.connectionState
    val statusSnapshot: StateFlow<DeviceNotification.StatusSnapshot> = transport.status

    val uiConnectionStatus: StateFlow<DeviceConnectionStatus> = transport.connectionState
        .map { state ->
            when (state) {
                is ConnectionState.Connected -> DeviceConnectionStatus.CONNECTED
                is ConnectionState.Connecting -> DeviceConnectionStatus.CONNECTING
                is ConnectionState.Disconnecting -> DeviceConnectionStatus.RECONNECTING
                is ConnectionState.Disconnected -> DeviceConnectionStatus.OFFLINE
            }
        }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = DeviceConnectionStatus.OFFLINE
        )

    fun onConnectionChipClicked() {
        viewModelScope.launch {
            try {
                when (transport.connectionState.value) {
                    is ConnectionState.Disconnected -> {
                        val addr = if (transport is com.pixelgift.app.transport.BleTransport) {
                            transport.getLastBondedAddress()
                        } else {
                            "FA:KE:DE:VI:CE:00"
                        }
                        if (!addr.isNullOrBlank()) {
                            transport.connect(addr)
                        }
                    }
                    is ConnectionState.Connected -> {
                        transport.disconnect()
                    }
                    else -> {
                        // Connecting or Disconnecting in progress
                    }
                }
            } catch (e: Exception) {
                android.util.Log.w("MainViewModel", "Connection chip action failed: ${e.message}")
            }
        }
    }

    fun sendTestColor(r: Int, g: Int, b: Int) {
        viewModelScope.launch {
            transport.send(Command.SetColor(r, g, b))
        }
    }

    fun toggleRandomDisconnects(enabled: Boolean) {
        if (transport is FakeDevice) {
            transport.setRandomDisconnects(enabled)
        }
    }
}
