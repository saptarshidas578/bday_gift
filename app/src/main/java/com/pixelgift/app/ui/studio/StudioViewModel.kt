package com.pixelgift.app.ui.studio

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.pixelgift.app.protocol.Command
import com.pixelgift.app.protocol.ProtocolConstants
import com.pixelgift.app.protocol.Rgb
import com.pixelgift.app.transport.DeviceTransport
import com.pixelgift.app.ui.color.ColorMath
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class StudioViewModel(
    val transport: DeviceTransport,
    private val externalScope: CoroutineScope? = null
) : ViewModel() {

    private val scope: CoroutineScope
        get() = externalScope ?: viewModelScope

    // Initial default: Amber
    private val defaultInitialColor = Rgb(255, 122, 0)

    private val _committedColor = MutableStateFlow(defaultInitialColor)
    val committedColor: StateFlow<Rgb> = _committedColor.asStateFlow()

    private val _selectedColor = MutableStateFlow(defaultInitialColor)
    val selectedColor: StateFlow<Rgb> = _selectedColor.asStateFlow()

    private val _recentColors = MutableStateFlow(
        listOf(
            Rgb(255, 122, 0),   // Amber
            Rgb(0, 229, 255),   // Cyan
            Rgb(158, 119, 237), // Violet
            Rgb(255, 45, 85),   // Magenta/Pink
            Rgb(0, 230, 118),   // Spring Green
            Rgb(255, 214, 0),   // Golden Yellow
            Rgb(41, 121, 255),  // Deep Azure
            Rgb(255, 255, 255)  // Warm White
        )
    )
    val recentColors: StateFlow<List<Rgb>> = _recentColors.asStateFlow()

    private val _hexInput = MutableStateFlow(ColorMath.rgbToHex(defaultInitialColor))
    val hexInput: StateFlow<String> = _hexInput.asStateFlow()

    private val _rInput = MutableStateFlow(defaultInitialColor.r.toString())
    val rInput: StateFlow<String> = _rInput.asStateFlow()

    private val _gInput = MutableStateFlow(defaultInitialColor.g.toString())
    val gInput: StateFlow<String> = _gInput.asStateFlow()

    private val _bInput = MutableStateFlow(defaultInitialColor.b.toString())
    val bInput: StateFlow<String> = _bInput.asStateFlow()

    // 10 Hz throttling state
    private var lastSendTimeMs: Long = 0L
    private var pendingSendJob: Job? = null
    var sendCountForTesting: Int = 0
        private set

    init {
        // Sync with device initial color if already available
        val deviceColor = transport.status.value.color
        if (deviceColor.r != 0 || deviceColor.g != 0 || deviceColor.b != 0) {
            _committedColor.value = deviceColor
            _selectedColor.value = deviceColor
            updateTextInputs(deviceColor)
        }
    }

    /**
     * Called continuously as the user drags on the color wheel.
     * Throttled to at most 10 sends per second (100ms interval) per SPEC 9.3.
     */
    fun onColorDragged(rgb: Rgb) {
        _selectedColor.value = rgb
        updateTextInputs(rgb)

        val now = System.currentTimeMillis()
        val elapsed = now - lastSendTimeMs

        if (elapsed >= 100L) {
            lastSendTimeMs = now
            pendingSendJob?.cancel()
            pendingSendJob = null
            dispatchColorCommand(rgb)
        } else {
            // Trailing edge debounce guarantees the last touch position is always dispatched
            pendingSendJob?.cancel()
            pendingSendJob = scope.launch {
                delay(100L - elapsed)
                lastSendTimeMs = System.currentTimeMillis()
                dispatchColorCommand(rgb)
            }
        }
    }

    fun onHexChanged(hex: String) {
        _hexInput.value = hex
        val parsed = ColorMath.hexToRgb(hex)
        if (parsed != null) {
            _selectedColor.value = parsed
            _rInput.value = parsed.r.toString()
            _gInput.value = parsed.g.toString()
            _bInput.value = parsed.b.toString()
            onColorDragged(parsed)
        }
    }

    fun onRgbChannelChanged(rStr: String, gStr: String, bStr: String) {
        _rInput.value = rStr
        _gInput.value = gStr
        _bInput.value = bStr

        val r = rStr.toIntOrNull()
        val g = gStr.toIntOrNull()
        val b = bStr.toIntOrNull()

        if (r in 0..255 && g in 0..255 && b in 0..255) {
            val parsed = Rgb(r!!, g!!, b!!)
            _selectedColor.value = parsed
            _hexInput.value = ColorMath.rgbToHex(parsed)
            onColorDragged(parsed)
        }
    }

    fun onRecentColorSelected(rgb: Rgb) {
        _selectedColor.value = rgb
        updateTextInputs(rgb)
        onColorDragged(rgb)
    }

    /**
     * Cancel reverts color to the last committed color.
     */
    fun onCancel(): Rgb {
        val reverted = _committedColor.value
        _selectedColor.value = reverted
        updateTextInputs(reverted)
        dispatchColorCommand(reverted)
        return reverted
    }

    /**
     * OK commits the color, persists it into recents list, and sends final SET_COLOR.
     */
    fun onCommitOk(): Rgb {
        val committed = _selectedColor.value
        _committedColor.value = committed

        // Add to front of recent colors without duplicates
        val updatedRecents = ArrayList<Rgb>()
        updatedRecents.add(committed)
        for (c in _recentColors.value) {
            if (c != committed && updatedRecents.size < 12) {
                updatedRecents.add(c)
            }
        }
        _recentColors.value = updatedRecents

        dispatchColorCommand(committed)
        return committed
    }

    private fun dispatchColorCommand(rgb: Rgb) {
        sendCountForTesting++
        scope.launch {
            transport.send(Command.SetColor(rgb))
            transport.send(Command.SetMode(ProtocolConstants.MODE_SOLID))
        }
    }

    private fun updateTextInputs(rgb: Rgb) {
        _hexInput.value = ColorMath.rgbToHex(rgb)
        _rInput.value = rgb.r.toString()
        _gInput.value = rgb.g.toString()
        _bInput.value = rgb.b.toString()
    }
}
