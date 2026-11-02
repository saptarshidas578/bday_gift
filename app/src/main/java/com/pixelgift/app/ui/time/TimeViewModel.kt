package com.pixelgift.app.ui.time

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.pixelgift.app.data.model.ScheduleItem
import com.pixelgift.app.protocol.Command
import com.pixelgift.app.protocol.DeviceNotification
import com.pixelgift.app.protocol.ProtocolConstants
import com.pixelgift.app.transport.DeviceTransport
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import java.util.TimeZone

/**
 * ViewModel managing Time tab state per SPEC 6.5, 6.6, 7.4.
 * Covers: Clock mode & styles, auto & manual SYNC_TIME, timer with live countdown,
 * 8 schedule slots, and sleep timer.
 */
class TimeViewModel(
    val transport: DeviceTransport,
    private val externalScope: CoroutineScope? = null
) : ViewModel() {

    private val scope: CoroutineScope
        get() = externalScope ?: viewModelScope

    // --- 1. Clock Mode & Sync State ---
    private val _isTimeSynced = MutableStateFlow(false)
    val isTimeSynced: StateFlow<Boolean> = _isTimeSynced.asStateFlow()

    private val _isClockMode = MutableStateFlow(false)
    val isClockMode: StateFlow<Boolean> = _isClockMode.asStateFlow()

    private val _clockStyle = MutableStateFlow(0) // 0: Stacked 3x4, 1: Scrolling 5x7
    val clockStyle: StateFlow<Int> = _clockStyle.asStateFlow()

    private val _lastSyncTimestamp = MutableStateFlow<Long?>(null)
    val lastSyncTimestamp: StateFlow<Long?> = _lastSyncTimestamp.asStateFlow()

    // --- 2. Timer State ---
    private val _isTimerRunning = MutableStateFlow(false)
    val isTimerRunning: StateFlow<Boolean> = _isTimerRunning.asStateFlow()

    private val _timerTotalSeconds = MutableStateFlow(300L) // default 5 min
    val timerTotalSeconds: StateFlow<Long> = _timerTotalSeconds.asStateFlow()

    private val _timerRemainingSeconds = MutableStateFlow(300L)
    val timerRemainingSeconds: StateFlow<Long> = _timerRemainingSeconds.asStateFlow()

    private val _customTimerMinutes = MutableStateFlow(5)
    val customTimerMinutes: StateFlow<Int> = _customTimerMinutes.asStateFlow()

    private var countdownJob: Job? = null

    // --- 3. Schedules State (Slots 0..7) ---
    private val _schedules = MutableStateFlow(ScheduleItem.DEFAULT_SCHEDULES)
    val schedules: StateFlow<List<ScheduleItem>> = _schedules.asStateFlow()

    // --- 4. Sleep Timer State ---
    private val _activeSleepMinutes = MutableStateFlow<Int?>(null)
    val activeSleepMinutes: StateFlow<Int?> = _activeSleepMinutes.asStateFlow()

    // --- Feedback ---
    private val _feedback = MutableStateFlow<String?>(null)
    val feedback: StateFlow<String?> = _feedback.asStateFlow()

    var sendCountForTesting: Int = 0
        private set

    init {
        // Initialize synchronously from current snapshot
        updateFromSnapshot(transport.status.value)
    }

    /**
     * Updates ViewModel state from incoming device StatusSnapshot.
     * Allows UI and tests to mirror hardware changes directly.
     */
    fun updateFromSnapshot(snapshot: DeviceNotification.StatusSnapshot) {
        _isTimeSynced.value = snapshot.isTimeSynced
        _isClockMode.value = (snapshot.mode == ProtocolConstants.MODE_CLOCK)

        val running = snapshot.isTimerRunning
        val wasRunning = _isTimerRunning.value
        _isTimerRunning.value = running

        if (running && !wasRunning) {
            // Started on hardware: start local countdown if not already counting down
            startLocalCountdown()
        } else if (!running && wasRunning) {
            // Cancelled or finished on hardware: stop countdown
            stopLocalCountdown()
            _timerRemainingSeconds.value = 0L
        }
    }

    // --- Clock Actions ---

    fun onToggleClockMode(enable: Boolean) {
        _isClockMode.value = enable
        val newMode = if (enable) ProtocolConstants.MODE_CLOCK else ProtocolConstants.MODE_SOLID
        sendCountForTesting++
        scope.launch {
            transport.send(Command.SetMode(newMode))
            _feedback.value = if (enable) "Clock mode activated" else "Clock mode turned off"
        }
    }

    fun onSelectClockStyle(style: Int) {
        require(style in 0..1) { "Clock style must be 0 or 1, got $style" }
        _clockStyle.value = style
        sendCountForTesting++
        scope.launch {
            transport.send(Command.SetClockStyle(style))
            _feedback.value = if (style == 0) "Style: Stacked 3×4 Digits" else "Style: Scrolling 5×7"
        }
    }

    fun onManualSyncTime() {
        performSyncTime(isManual = true)
    }

    fun autoSyncOnConnect() {
        performSyncTime(isManual = false)
    }

    private fun performSyncTime(isManual: Boolean) {
        val now = System.currentTimeMillis()
        val epochSeconds = now / 1000L
        val tzMinutes = (TimeZone.getDefault().getOffset(now) / 60000)

        _isTimeSynced.value = true
        _lastSyncTimestamp.value = now
        sendCountForTesting++

        scope.launch {
            transport.send(Command.SyncTime(epochSeconds, tzMinutes))
            if (isManual) {
                _feedback.value = "Device clock synced to local time"
            }
        }
    }

    // --- Timer Actions ---

    fun onCustomMinutesChanged(minutes: Int) {
        val clamped = minutes.coerceIn(1, 120)
        _customTimerMinutes.value = clamped
    }

    fun onStartTimer(seconds: Long) {
        require(seconds > 0) { "Timer seconds must be positive, got $seconds" }
        val clamped = seconds.coerceAtMost(0xFFFFFFFFL)
        _timerTotalSeconds.value = clamped
        _timerRemainingSeconds.value = clamped
        _isTimerRunning.value = true
        sendCountForTesting++

        startLocalCountdown()

        scope.launch {
            transport.send(Command.StartTimer(clamped))
            _feedback.value = "Timer started for ${formatSeconds(clamped)}"
        }
    }

    fun onCancelTimer() {
        _isTimerRunning.value = false
        stopLocalCountdown()
        _timerRemainingSeconds.value = _timerTotalSeconds.value
        sendCountForTesting++

        scope.launch {
            transport.send(Command.CancelTimer)
            _feedback.value = "Timer cancelled"
        }
    }

    private fun startLocalCountdown() {
        stopLocalCountdown()
        countdownJob = scope.launch {
            while (isActive && _isTimerRunning.value && _timerRemainingSeconds.value > 0) {
                delay(1000L)
                val next = _timerRemainingSeconds.value - 1
                _timerRemainingSeconds.value = next
                if (next <= 0) {
                    _isTimerRunning.value = false
                    _feedback.value = "Timer finished!"
                    break
                }
            }
        }
    }

    private fun stopLocalCountdown() {
        countdownJob?.cancel()
        countdownJob = null
    }

    // --- Schedule Actions ---

    fun onToggleSchedule(slot: Int) {
        val currentList = _schedules.value
        val item = currentList.find { it.slot == slot } ?: return
        val updated = item.copy(enabled = !item.enabled)
        updateScheduleItem(updated)
    }

    fun onSaveSchedule(item: ScheduleItem) {
        updateScheduleItem(item)
    }

    private fun updateScheduleItem(updated: ScheduleItem) {
        val currentList = _schedules.value.toMutableList()
        val index = currentList.indexOfFirst { it.slot == updated.slot }
        if (index >= 0) {
            currentList[index] = updated
        } else {
            currentList.add(updated)
        }
        _schedules.value = currentList
        sendCountForTesting++

        scope.launch {
            transport.send(updated.toCommand())
            _feedback.value = "Schedule slot ${updated.slot} updated (${if (updated.enabled) "enabled" else "disabled"})"
        }
    }

    // --- Sleep Timer Actions ---

    fun onSetSleepTimer(minutes: Int) {
        require(minutes >= 0) { "Minutes must be non-negative" }
        _activeSleepMinutes.value = if (minutes == 0) null else minutes
        sendCountForTesting++

        scope.launch {
            transport.send(Command.SetSleepTimer(minutes))
            _feedback.value = if (minutes == 0) "Sleep timer turned off" else "Sleep timer set for $minutes min"
        }
    }

    fun onCancelSleepTimer() {
        onSetSleepTimer(0)
    }

    fun clearFeedback() {
        _feedback.value = null
    }

    private fun formatSeconds(totalSecs: Long): String {
        val mins = totalSecs / 60
        val secs = totalSecs % 60
        return if (mins > 0 && secs == 0L) "${mins}m" else "%02d:%02d".format(mins, secs)
    }

    override fun onCleared() {
        super.onCleared()
        stopLocalCountdown()
    }
}
