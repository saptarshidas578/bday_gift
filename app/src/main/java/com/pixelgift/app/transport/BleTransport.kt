package com.pixelgift.app.transport

import android.annotation.SuppressLint
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothGatt
import android.bluetooth.BluetoothGattCharacteristic
import android.bluetooth.BluetoothManager
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.Build
import android.os.ParcelUuid
import android.util.Log
import com.pixelgift.app.data.model.DiscoveredDevice
import com.pixelgift.app.protocol.Command
import com.pixelgift.app.protocol.DeviceNotification
import com.pixelgift.app.protocol.ProtocolCodec
import com.pixelgift.app.protocol.ProtocolConstants
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.TimeoutCancellationException
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withTimeout
import no.nordicsemi.android.ble.BleManager
import no.nordicsemi.android.ble.observer.BondingObserver
import no.nordicsemi.android.ble.observer.ConnectionObserver
import no.nordicsemi.android.support.v18.scanner.BluetoothLeScannerCompat
import no.nordicsemi.android.support.v18.scanner.ScanCallback
import no.nordicsemi.android.support.v18.scanner.ScanFilter
import no.nordicsemi.android.support.v18.scanner.ScanResult
import no.nordicsemi.android.support.v18.scanner.ScanSettings
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.atomic.AtomicInteger

private const val TAG = "BleTransport"

/**
 * Real Bluetooth Low Energy transport using the Nordic Android BLE library.
 * Complies with SPEC 7.1, 7.2, and 9.1:
 * - Scans by service UUID (7a1c0001-5d3e-4b8a-9c21-8f6d2e4a1b00)
 * - Connects and bonds with passkey / Just Works
 * - Requests MTU 185
 * - Subscribes to STATUS characteristic notifications
 * - Maintains write queue with sequence numbers and ACK timeouts (2000ms)
 * - Auto-reconnects to bonded address with exponential backoff
 * - Handles bond loss and phone Bluetooth turned off
 * - On reconnect, fetches device snapshot via GET_STATUS without overwriting device state
 */
class BleTransport(
    private val context: Context,
    private val externalScope: CoroutineScope? = null
) : DeviceTransport {

    private val scope = externalScope ?: CoroutineScope(SupervisorJob() + Dispatchers.IO)

    private val _connectionState = MutableStateFlow<ConnectionState>(ConnectionState.Disconnected)
    override val connectionState: StateFlow<ConnectionState> = _connectionState.asStateFlow()

    private val _status = MutableStateFlow(
        DeviceNotification.StatusSnapshot(
            protoVer = ProtocolConstants.PROTOCOL_VERSION,
            mode = ProtocolConstants.MODE_SOLID,
            brightness = 40,
            r = 210, g = 4, b = 45, // Cherry Red default
            effectBase = 6,
            speed = 50,
            flags = ProtocolConstants.FLAG_TIME_SYNCED,
            fwMajor = 1, fwMinor = 0
        )
    )
    override val status: StateFlow<DeviceNotification.StatusSnapshot> = _status.asStateFlow()

    private val _notifications = MutableSharedFlow<DeviceNotification>(extraBufferCapacity = 64)
    override val notifications: Flow<DeviceNotification> = _notifications.asSharedFlow()

    // Diagnostics / Metrics
    private val _packetLossCount = AtomicInteger(0)
    val packetLossCount: Int get() = _packetLossCount.get()

    private val _lastAckSeq = MutableStateFlow(-1)
    val lastAckSeq: StateFlow<Int> = _lastAckSeq.asStateFlow()

    // Sequence counter: 0..255 wrapping
    private val seqCounter = AtomicInteger(0)

    // Write queue management
    private val writeMutex = Mutex()
    private val pendingAcks = ConcurrentHashMap<Int, CompletableDeferred<Int>>()

    // Bonding & Auto-reconnect state
    private var lastConnectedAddress: String? = null
    private var autoReconnectEnabled = true
    private var userRequestedDisconnect = false
    private var reconnectJob: Job? = null
    private var pendingPasskey: String? = null
    private var activeScanCallback: ScanCallback? = null
    private var connectReadyDeferred: CompletableDeferred<Unit>? = null

    private val bleManager = PixelGiftBleManager(context)

    init {
        setupObservers()
        registerBroadcastReceivers()
    }

    private fun getBluetoothAdapter(): BluetoothAdapter? {
        val manager = context.getSystemService(Context.BLUETOOTH_SERVICE) as? BluetoothManager
        return manager?.adapter
    }

    @SuppressLint("MissingPermission")
    fun getLastBondedAddress(): String? {
        if (!lastConnectedAddress.isNullOrBlank()) {
            return lastConnectedAddress
        }
        val adapter = getBluetoothAdapter() ?: return null
        return try {
            val bonded = adapter.bondedDevices ?: return null
            bonded.firstOrNull { it.name?.startsWith("PixelGift", ignoreCase = true) == true }?.address
        } catch (e: Exception) {
            null
        }
    }

    fun stopScanning() {
        val cb = activeScanCallback
        if (cb != null) {
            try {
                BluetoothLeScannerCompat.getScanner().stopScan(cb)
            } catch (e: Exception) {
                Log.w(TAG, "Failed to stop scanner: ${e.message}")
            }
            activeScanCallback = null
        }
    }

    fun setAutoReconnect(enabled: Boolean) {
        autoReconnectEnabled = enabled
        if (!enabled) cancelAutoReconnect()
    }

    fun setPendingPasskey(passkey: String?) {
        pendingPasskey = passkey
    }

    private fun getNextSeq(): Int {
        return seqCounter.getAndUpdate { (it + 1) and 0xFF }
    }

    // ========================================================================
    // DeviceTransport Implementation
    // ========================================================================

    @SuppressLint("MissingPermission")
    override suspend fun connect(address: String) {
        val adapter = getBluetoothAdapter()
            ?: throw IllegalStateException("Bluetooth is not available on this device")
        if (!adapter.isEnabled) {
            throw IllegalStateException("Bluetooth is turned off")
        }

        userRequestedDisconnect = false
        cancelAutoReconnect()
        stopScanning()
        _connectionState.value = ConnectionState.Connecting

        val device = try {
            adapter.getRemoteDevice(address)
        } catch (e: Exception) {
            _connectionState.value = ConnectionState.Disconnected
            throw IllegalArgumentException("Invalid Bluetooth address: $address", e)
        }

        val readyDeferred = CompletableDeferred<Unit>()
        connectReadyDeferred = readyDeferred

        bleManager.connect(device)
            .retry(3, 300)
            .useAutoConnect(false)
            .timeout(12000)
            .fail { _, status ->
                _connectionState.value = ConnectionState.Disconnected
                readyDeferred.completeExceptionally(Exception("GATT connection failed with status $status"))
                connectReadyDeferred = null
            }
            .enqueue()

        try {
            readyDeferred.await()
        } finally {
            connectReadyDeferred = null
        }
    }

    override suspend fun disconnect() {
        userRequestedDisconnect = true
        cancelAutoReconnect()
        if (_connectionState.value is ConnectionState.Connected || _connectionState.value is ConnectionState.Connecting) {
            _connectionState.value = ConnectionState.Disconnecting
            val deferred = CompletableDeferred<Unit>()
            bleManager.disconnect()
                .done { deferred.complete(Unit) }
                .fail { _, _ -> deferred.complete(Unit) }
                .enqueue()
            try {
                deferred.await()
            } catch (e: Exception) {
                // Ignore disconnect errors
            }
        }
        _connectionState.value = ConnectionState.Disconnected
    }

    override suspend fun send(command: Command): Result<Unit> = writeMutex.withLock {
        if (_connectionState.value !is ConnectionState.Connected) {
            return Result.failure(IllegalStateException("Device is not connected"))
        }

        val cmdChar = bleManager.cmdChar
            ?: return Result.failure(IllegalStateException("Command characteristic not available"))

        val seq = getNextSeq()
        val packet = ProtocolCodec.encodeCommand(command, seq)

        // MusicLevels uses write without response for 20Hz streaming and does not block on ACK
        if (command is Command.MusicLevels) {
            val deferred = CompletableDeferred<Unit>()
            bleManager.writeCmd(
                packet,
                BluetoothGattCharacteristic.WRITE_TYPE_NO_RESPONSE
            ).done { deferred.complete(Unit) }
             .fail { _, status -> deferred.completeExceptionally(Exception("Write failed with status $status")) }
             .enqueue()

            return try {
                deferred.await()
                Result.success(Unit)
            } catch (e: Exception) {
                Result.failure(e)
            }
        }

        // Standard commands: Write with response & await ACK notification
        val ackDeferred = CompletableDeferred<Int>()
        pendingAcks[seq] = ackDeferred

        val writeDeferred = CompletableDeferred<Unit>()
        bleManager.writeCmd(
            packet,
            BluetoothGattCharacteristic.WRITE_TYPE_DEFAULT
        ).done { writeDeferred.complete(Unit) }
         .fail { _, status -> writeDeferred.completeExceptionally(Exception("GATT write failed with status $status")) }
         .enqueue()

        try {
            writeDeferred.await()

            // Await ACK from device with 2000ms timeout
            val ackCode = withTimeout(2000L) {
                ackDeferred.await()
            }

            if (ackCode == ProtocolConstants.ERROR_OK) {
                Result.success(Unit)
            } else {
                Result.failure(Exception("Device rejected command (error code $ackCode)"))
            }
        } catch (e: TimeoutCancellationException) {
            _packetLossCount.incrementAndGet()
            Log.w(TAG, "ACK timeout for seq $seq (Total packet losses: ${_packetLossCount.get()})")
            Result.failure(Exception("ACK timeout for seq $seq (packet loss)"))
        } catch (e: Exception) {
            Result.failure(e)
        } finally {
            pendingAcks.remove(seq)
        }
    }

    // ========================================================================
    // Real BLE Scanning by Service UUID
    // ========================================================================

    fun scanDevices(): Flow<List<DiscoveredDevice>> = flow {
        val scanner = BluetoothLeScannerCompat.getScanner()
        val devices = ConcurrentHashMap<String, DiscoveredDevice>()
        val channel = Channel<List<DiscoveredDevice>>(Channel.CONFLATED)

        val settings = ScanSettings.Builder()
            .setScanMode(ScanSettings.SCAN_MODE_LOW_LATENCY)
            .setReportDelay(200)
            .build()

        val filters = listOf(
            ScanFilter.Builder()
                .setServiceUuid(ParcelUuid(ProtocolConstants.SERVICE_UUID))
                .build()
        )

        val callback = object : ScanCallback() {
            @SuppressLint("MissingPermission")
            override fun onScanResult(callbackType: Int, result: ScanResult) {
                val dev = processScanResult(result)
                if (dev != null) {
                    devices[dev.address] = dev
                    channel.trySend(devices.values.toList().sortedByDescending { it.rssi })
                }
            }

            @SuppressLint("MissingPermission")
            override fun onBatchScanResults(results: List<ScanResult>) {
                var updated = false
                for (r in results) {
                    val dev = processScanResult(r)
                    if (dev != null) {
                        devices[dev.address] = dev
                        updated = true
                    }
                }
                if (updated) {
                    channel.trySend(devices.values.toList().sortedByDescending { it.rssi })
                }
            }

            override fun onScanFailed(errorCode: Int) {
                Log.e(TAG, "BLE scan failed with error code $errorCode")
                channel.close(Exception("BLE scan failed with error code $errorCode"))
            }

            @SuppressLint("MissingPermission")
            private fun processScanResult(result: ScanResult): DiscoveredDevice? {
                val name = result.device.name ?: result.scanRecord?.deviceName ?: ""
                val hasService = result.scanRecord?.serviceUuids?.any {
                    it.uuid == ProtocolConstants.SERVICE_UUID
                } == true
                val matchesName = name.startsWith("PixelGift", ignoreCase = true)

                if (hasService || matchesName) {
                    val displayName = if (name.isNotBlank()) name else "PixelGift"
                    return DiscoveredDevice(
                        name = displayName,
                        address = result.device.address,
                        rssi = result.rssi,
                        isBonded = result.device.bondState == BluetoothDevice.BOND_BONDED
                    )
                }
                return null
            }
        }

        try {
            activeScanCallback = callback
            scanner.startScan(filters, settings, callback)
            for (list in channel) {
                emit(list)
            }
        } finally {
            activeScanCallback = null
            try {
                scanner.stopScan(callback)
            } catch (e: Exception) {
                // Ignore
            }
        }
    }.flowOn(Dispatchers.IO)

    // ========================================================================
    // Notifications & Snapshot Handling
    // ========================================================================

    private fun handleIncomingBytes(bytes: ByteArray) {
        try {
            val notification = ProtocolCodec.decodeNotification(bytes)
            _notifications.tryEmit(notification)

            when (notification) {
                is DeviceNotification.Ack -> {
                    _lastAckSeq.value = notification.seq
                    val deferred = pendingAcks.remove(notification.seq)
                    deferred?.complete(notification.errorCode)
                }
                is DeviceNotification.StatusSnapshot -> {
                    Log.d(TAG, "Received device snapshot: mode=${notification.mode}, brightness=${notification.brightness}")
                    _status.value = notification
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Failed to decode incoming notification packet: ${e.message}", e)
        }
    }

    private fun failAllPendingAcks(exception: Exception) {
        for ((_, deferred) in pendingAcks) {
            deferred.completeExceptionally(exception)
        }
        pendingAcks.clear()
    }

    // ========================================================================
    // Observers & Auto-Reconnect Logic
    // ========================================================================

    private fun setupObservers() {
        bleManager.setConnectionObserver(object : ConnectionObserver {
            override fun onDeviceConnecting(device: BluetoothDevice) {
                Log.d(TAG, "Device connecting: ${device.address}")
                _connectionState.value = ConnectionState.Connecting
            }

            override fun onDeviceConnected(device: BluetoothDevice) {
                Log.d(TAG, "Device connected at GATT layer: ${device.address}")
            }

            override fun onDeviceFailedToConnect(device: BluetoothDevice, reason: Int) {
                Log.w(TAG, "Device failed to connect: ${device.address}, reason: $reason")
                _connectionState.value = ConnectionState.Disconnected
                connectReadyDeferred?.completeExceptionally(Exception("Device failed to connect (reason $reason)"))
                connectReadyDeferred = null
                if (!userRequestedDisconnect) {
                    triggerAutoReconnect(device.address)
                }
            }

            @SuppressLint("MissingPermission")
            override fun onDeviceReady(device: BluetoothDevice) {
                val devName = try { device.name ?: "PixelGift" } catch (e: Exception) { "PixelGift" }
                Log.i(TAG, "Device ready! ($devName, ${device.address})")
                _connectionState.value = ConnectionState.Connected(devName, device.address)
                lastConnectedAddress = device.address
                connectReadyDeferred?.complete(Unit)
                connectReadyDeferred = null

                // SPEC 7.1: Fetch device status snapshot immediately on connect / reconnect
                scope.launch {
                    delay(150L) // Settle connection
                    send(Command.GetStatus)
                }
            }

            override fun onDeviceDisconnecting(device: BluetoothDevice) {
                Log.d(TAG, "Device disconnecting: ${device.address}")
                _connectionState.value = ConnectionState.Disconnecting
            }

            override fun onDeviceDisconnected(device: BluetoothDevice, reason: Int) {
                Log.i(TAG, "Device disconnected: ${device.address}, reason: $reason")
                _connectionState.value = ConnectionState.Disconnected
                connectReadyDeferred?.completeExceptionally(Exception("Device disconnected (code $reason)"))
                connectReadyDeferred = null
                failAllPendingAcks(Exception("Device disconnected (code $reason)"))

                if (!userRequestedDisconnect && autoReconnectEnabled) {
                    triggerAutoReconnect(device.address)
                }
            }
        })

        bleManager.setBondingObserver(object : BondingObserver {
            override fun onBondingRequired(device: BluetoothDevice) {
                Log.d(TAG, "Bonding required for ${device.address}")
            }

            override fun onBonded(device: BluetoothDevice) {
                Log.i(TAG, "Successfully bonded with ${device.address}")
            }

            override fun onBondingFailed(device: BluetoothDevice) {
                Log.w(TAG, "Bonding failed with ${device.address}")
            }
        })
    }

    private fun triggerAutoReconnect(address: String) {
        reconnectJob?.cancel()
        reconnectJob = scope.launch {
            var delayMs = 1000L
            Log.d(TAG, "Starting auto-reconnect loop for $address")

            while (autoReconnectEnabled && !userRequestedDisconnect && _connectionState.value !is ConnectionState.Connected) {
                delay(delayMs)
                if (!autoReconnectEnabled || userRequestedDisconnect || _connectionState.value is ConnectionState.Connected) {
                    break
                }

                val adapter = getBluetoothAdapter()
                if (adapter != null && adapter.isEnabled) {
                    try {
                        _connectionState.value = ConnectionState.Connecting
                        val device = adapter.getRemoteDevice(address)
                        val deferred = CompletableDeferred<Boolean>()

                        bleManager.connect(device)
                            .retry(1, 200)
                            .useAutoConnect(false)
                            .timeout(6000)
                            .done { deferred.complete(true) }
                            .fail { _, _ -> deferred.complete(false) }
                            .enqueue()

                        val ok = deferred.await()
                        if (ok) {
                            Log.i(TAG, "Auto-reconnect successful for $address")
                            break
                        }
                    } catch (e: Exception) {
                        Log.w(TAG, "Auto-reconnect attempt failed: ${e.message}")
                    }
                }

                delayMs = (delayMs * 2).coerceAtMost(10000L) // Exponential backoff up to 10s
            }
        }
    }

    private fun cancelAutoReconnect() {
        reconnectJob?.cancel()
        reconnectJob = null
    }

    // ========================================================================
    // Broadcast Receivers (Bluetooth Off / Bond Loss / Passkey)
    // ========================================================================

    private fun registerBroadcastReceivers() {
        val filter = IntentFilter().apply {
            addAction(BluetoothAdapter.ACTION_STATE_CHANGED)
            addAction(BluetoothDevice.ACTION_BOND_STATE_CHANGED)
            addAction(BluetoothDevice.ACTION_PAIRING_REQUEST)
        }

        val receiver = object : BroadcastReceiver() {
            @SuppressLint("MissingPermission")
            override fun onReceive(context: Context, intent: Intent) {
                when (intent.action) {
                    BluetoothAdapter.ACTION_STATE_CHANGED -> {
                        val state = intent.getIntExtra(BluetoothAdapter.EXTRA_STATE, BluetoothAdapter.ERROR)
                        when (state) {
                            BluetoothAdapter.STATE_TURNING_OFF, BluetoothAdapter.STATE_OFF -> {
                                Log.w(TAG, "Phone Bluetooth turned off!")
                                _connectionState.value = ConnectionState.Disconnected
                                cancelAutoReconnect()
                                failAllPendingAcks(Exception("Phone Bluetooth turned off"))
                            }
                            BluetoothAdapter.STATE_ON -> {
                                Log.i(TAG, "Phone Bluetooth turned on!")
                                if (autoReconnectEnabled && lastConnectedAddress != null && !userRequestedDisconnect) {
                                    triggerAutoReconnect(lastConnectedAddress!!)
                                }
                            }
                        }
                    }
                    BluetoothDevice.ACTION_BOND_STATE_CHANGED -> {
                        val device = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                            intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE, BluetoothDevice::class.java)
                        } else {
                            @Suppress("DEPRECATION")
                            intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE)
                        }
                        val bondState = intent.getIntExtra(BluetoothDevice.EXTRA_BOND_STATE, BluetoothDevice.BOND_NONE)
                        if (device != null && device.address == lastConnectedAddress && bondState == BluetoothDevice.BOND_NONE) {
                            Log.w(TAG, "Bond lost with device ${device.address}")
                        }
                    }
                    BluetoothDevice.ACTION_PAIRING_REQUEST -> {
                        val device = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                            intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE, BluetoothDevice::class.java)
                        } else {
                            @Suppress("DEPRECATION")
                            intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE)
                        }
                        val passkey = pendingPasskey
                        if (device != null && !passkey.isNullOrBlank()) {
                            try {
                                device.setPin(passkey.toByteArray())
                            } catch (e: Exception) {
                                Log.w(TAG, "Could not set PIN automatically: ${e.message}")
                            }
                        }
                    }
                }
            }
        }

        try {
            context.registerReceiver(receiver, filter)
        } catch (e: Exception) {
            Log.e(TAG, "Failed to register broadcast receiver: ${e.message}")
        }
    }

    // ========================================================================
    // Inner Nordic BleManager
    // ========================================================================

    private inner class PixelGiftBleManager(context: Context) : BleManager(context) {
        var cmdChar: BluetoothGattCharacteristic? = null
        var statusChar: BluetoothGattCharacteristic? = null

        fun writeCmd(data: ByteArray, writeType: Int) = writeCharacteristic(cmdChar, data, writeType)

        override fun isRequiredServiceSupported(gatt: BluetoothGatt): Boolean {
            val service = gatt.getService(ProtocolConstants.SERVICE_UUID) ?: return false
            cmdChar = service.getCharacteristic(ProtocolConstants.COMMAND_UUID)
            statusChar = service.getCharacteristic(ProtocolConstants.STATUS_UUID)
            return cmdChar != null && statusChar != null
        }

        override fun initialize() {
            // 1. Request MTU 185
            requestMtu(185).enqueue()

            // 2. Subscribe to STATUS characteristic notifications
            setNotificationCallback(statusChar).with { _, data ->
                val bytes = data.value ?: return@with
                handleIncomingBytes(bytes)
            }
            enableNotifications(statusChar).enqueue()
        }

        override fun onServicesInvalidated() {
            cmdChar = null
            statusChar = null
        }
    }
}

