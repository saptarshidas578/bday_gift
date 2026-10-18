package com.pixelgift.app.transport

import com.pixelgift.app.protocol.Command
import com.pixelgift.app.protocol.DeviceNotification
import com.pixelgift.app.protocol.ProtocolConstants
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlin.random.Random

/**
 * In-memory simulator of PixelGift ESP32 hardware for testing and UI development.
 */
class FakeDevice(
    private val scope: CoroutineScope = CoroutineScope(Dispatchers.Default),
    var connectDelayMs: Long = 1000L
) : DeviceTransport {

    private val _connectionState = MutableStateFlow<ConnectionState>(ConnectionState.Disconnected)
    override val connectionState: StateFlow<ConnectionState> = _connectionState.asStateFlow()

    private val _status = MutableStateFlow(
        DeviceNotification.StatusSnapshot(
            protoVer = ProtocolConstants.PROTOCOL_VERSION,
            mode = ProtocolConstants.MODE_SOLID,
            brightness = 60,
            r = 255,
            g = 122,
            b = 0,
            effectBase = 1,
            speed = 50,
            flags = 0,
            fwMajor = 1,
            fwMinor = 0
        )
    )
    override val status: StateFlow<DeviceNotification.StatusSnapshot> = _status.asStateFlow()

    private val _notifications = MutableSharedFlow<DeviceNotification>(replay = 16, extraBufferCapacity = 64)
    override val notifications: SharedFlow<DeviceNotification> = _notifications.asSharedFlow()

    private var currentSeq: Int = 0
    private var randomDisconnectJob: Job? = null
    var randomDisconnectsEnabled: Boolean = false
        private set

    fun setRandomDisconnects(enabled: Boolean) {
        randomDisconnectsEnabled = enabled
        if (enabled && _connectionState.value is ConnectionState.Connected) {
            startRandomDisconnectLoop()
        } else {
            randomDisconnectJob?.cancel()
            randomDisconnectJob = null
        }
    }

    override suspend fun connect(address: String) {
        if (_connectionState.value is ConnectionState.Connected) return

        _connectionState.value = ConnectionState.Connecting
        delay(connectDelayMs)
        _connectionState.value = ConnectionState.Connected(
            deviceName = "PixelGift-Sim",
            address = address
        )

        // Emit initial status snapshot on connection
        _notifications.emit(_status.value)

        if (randomDisconnectsEnabled) {
            startRandomDisconnectLoop()
        }
    }

    override suspend fun disconnect() {
        randomDisconnectJob?.cancel()
        randomDisconnectJob = null

        _connectionState.value = ConnectionState.Disconnecting
        delay(50)
        _connectionState.value = ConnectionState.Disconnected
    }

    override suspend fun send(command: Command): Result<Unit> {
        val currentState = _connectionState.value
        if (currentState !is ConnectionState.Connected) {
            return Result.failure(IllegalStateException("Device is not connected"))
        }

        // Increment sequence number (0..255)
        currentSeq = (currentSeq + 1) and 0xFF

        // Update simulated hardware state
        val prev = _status.value
        var updated = prev

        when (command) {
            is Command.SetColor -> {
                updated = prev.copy(
                    mode = ProtocolConstants.MODE_SOLID,
                    r = command.r,
                    g = command.g,
                    b = command.b
                )
            }
            is Command.SetBrightness -> {
                updated = prev.copy(brightness = command.brightness.coerceIn(0, 255))
            }
            is Command.SetEffect -> {
                updated = prev.copy(
                    mode = ProtocolConstants.MODE_EFFECT,
                    effectBase = command.base,
                    speed = command.speed,
                    r = command.c1.r,
                    g = command.c1.g,
                    b = command.c1.b
                )
            }
            is Command.SetMode -> {
                updated = prev.copy(mode = command.mode)
            }
            is Command.SyncTime -> {
                updated = prev.copy(flags = prev.flags or ProtocolConstants.FLAG_TIME_SYNCED)
            }
            is Command.StartTimer -> {
                updated = prev.copy(
                    mode = ProtocolConstants.MODE_TIMER,
                    flags = prev.flags or ProtocolConstants.FLAG_TIMER_RUNNING
                )
            }
            is Command.CancelTimer -> {
                updated = prev.copy(flags = prev.flags and ProtocolConstants.FLAG_TIMER_RUNNING.inv())
            }
            is Command.PlayBirthday -> {
                updated = prev.copy(
                    mode = ProtocolConstants.MODE_BIRTHDAY,
                    flags = prev.flags or ProtocolConstants.FLAG_SHOW_RUNNING
                )
            }
            is Command.StopShow -> {
                updated = prev.copy(flags = prev.flags and ProtocolConstants.FLAG_SHOW_RUNNING.inv())
            }
            is Command.SetMute -> {
                val newFlags = if (command.muted) {
                    prev.flags or ProtocolConstants.FLAG_MUTED
                } else {
                    prev.flags and ProtocolConstants.FLAG_MUTED.inv()
                }
                updated = prev.copy(flags = newFlags)
            }
            is Command.GetStatus -> {
                // Just emit current snapshot
            }
            else -> {
                // Other commands processed with OK ack
            }
        }

        // Emit ACK first
        val ack = DeviceNotification.Ack(
            seq = currentSeq,
            errorCode = ProtocolConstants.ERROR_OK
        )
        _notifications.emit(ack)

        // If snapshot changed or GetStatus requested, update state and notify
        if (updated != prev || command is Command.GetStatus) {
            _status.value = updated
            _notifications.emit(updated)
        }

        return Result.success(Unit)
    }

    private fun startRandomDisconnectLoop() {
        randomDisconnectJob?.cancel()
        randomDisconnectJob = scope.launch {
            while (isActive && _connectionState.value is ConnectionState.Connected) {
                // Random delay between 1.5 and 4.0 seconds
                val waitMs = Random.nextLong(1500, 4000)
                delay(waitMs)
                if (randomDisconnectsEnabled && _connectionState.value is ConnectionState.Connected) {
                    _connectionState.value = ConnectionState.Disconnected
                    break
                }
            }
        }
    }
}
