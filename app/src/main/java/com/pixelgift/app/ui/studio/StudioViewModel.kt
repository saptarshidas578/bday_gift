package com.pixelgift.app.ui.studio

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.pixelgift.app.data.model.AppData
import com.pixelgift.app.data.model.AppDataJson
import com.pixelgift.app.data.model.NamedPalette
import com.pixelgift.app.data.repository.AppDataRepository
import com.pixelgift.app.data.repository.InMemoryAppDataRepository
import com.pixelgift.app.protocol.Command
import com.pixelgift.app.protocol.ProtocolConstants
import com.pixelgift.app.protocol.Rgb
import com.pixelgift.app.transport.DeviceTransport
import com.pixelgift.app.ui.color.ColorMath
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class StudioViewModel(
    val transport: DeviceTransport,
    val repository: AppDataRepository = InMemoryAppDataRepository(),
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

    private val _brightness = MutableStateFlow(128)
    val brightness: StateFlow<Int> = _brightness.asStateFlow()

    private val _favoriteColors = MutableStateFlow(AppData.DEFAULT_FAVORITES)
    val favoriteColors: StateFlow<List<Rgb>> = _favoriteColors.asStateFlow()

    private val _recentColors = MutableStateFlow(AppData.DEFAULT_RECENTS)
    val recentColors: StateFlow<List<Rgb>> = _recentColors.asStateFlow()

    private val _palettes = MutableStateFlow(AppData.DEFAULT_PALETTES)
    val palettes: StateFlow<List<NamedPalette>> = _palettes.asStateFlow()

    private val _selectedPalette = MutableStateFlow<NamedPalette?>(null)
    val selectedPalette: StateFlow<NamedPalette?> = _selectedPalette.asStateFlow()

    // Scenes (0..7)
    private val _activeScene = MutableStateFlow<Int?>(null)
    val activeScene: StateFlow<Int?> = _activeScene.asStateFlow()

    private val _sceneFeedback = MutableStateFlow<String?>(null)
    val sceneFeedback: StateFlow<String?> = _sceneFeedback.asStateFlow()

    private val _hexInput = MutableStateFlow(ColorMath.rgbToHex(defaultInitialColor))
    val hexInput: StateFlow<String> = _hexInput.asStateFlow()

    private val _rInput = MutableStateFlow(defaultInitialColor.r.toString())
    val rInput: StateFlow<String> = _rInput.asStateFlow()

    private val _gInput = MutableStateFlow(defaultInitialColor.g.toString())
    val gInput: StateFlow<String> = _gInput.asStateFlow()

    private val _bInput = MutableStateFlow(defaultInitialColor.b.toString())
    val bInput: StateFlow<String> = _bInput.asStateFlow()

    // 10 Hz throttling state for color drag
    private var lastColorSendTimeMs: Long = 0L
    private var pendingColorSendJob: Job? = null
    var sendCountForTesting: Int = 0
        private set

    // 10 Hz throttling state for brightness slider
    private var lastBrightnessSendTimeMs: Long = 0L
    private var pendingBrightnessSendJob: Job? = null
    var brightnessSendCountForTesting: Int = 0
        private set

    init {
        // Sync with device initial state if available
        val deviceColor = transport.status.value.color
        if (deviceColor.r != 0 || deviceColor.g != 0 || deviceColor.b != 0) {
            _committedColor.value = deviceColor
            _selectedColor.value = deviceColor
            updateTextInputs(deviceColor)
        }
        val deviceBrightness = transport.status.value.brightness
        if (deviceBrightness > 0) {
            _brightness.value = deviceBrightness
        }

        // Initialize state synchronously from repository
        val initData = repository.appData.value
        _favoriteColors.value = initData.favoriteColors
        _recentColors.value = initData.recentColors
        _palettes.value = initData.palettes
        if (initData.brightness != 128) {
            _brightness.value = initData.brightness
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
        val elapsed = now - lastColorSendTimeMs

        if (elapsed >= 100L) {
            lastColorSendTimeMs = now
            pendingColorSendJob?.cancel()
            pendingColorSendJob = null
            dispatchColorCommand(rgb)
        } else {
            pendingColorSendJob?.cancel()
            pendingColorSendJob = scope.launch {
                delay(100L - elapsed)
                lastColorSendTimeMs = System.currentTimeMillis()
                dispatchColorCommand(rgb)
            }
        }
    }

    /**
     * Called when the brightness slider changes (0..255).
     * Throttled to at most 10 sends per second (100ms interval).
     */
    fun onBrightnessChanged(newBrightness: Int) {
        val clamped = newBrightness.coerceIn(0, 255)
        _brightness.value = clamped

        val now = System.currentTimeMillis()
        val elapsed = now - lastBrightnessSendTimeMs

        if (elapsed >= 100L) {
            lastBrightnessSendTimeMs = now
            pendingBrightnessSendJob?.cancel()
            pendingBrightnessSendJob = null
            dispatchBrightnessCommand(clamped)
        } else {
            pendingBrightnessSendJob?.cancel()
            pendingBrightnessSendJob = scope.launch {
                delay(100L - elapsed)
                lastBrightnessSendTimeMs = System.currentTimeMillis()
                dispatchBrightnessCommand(clamped)
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

    fun onFavoriteSelected(rgb: Rgb) {
        _selectedColor.value = rgb
        updateTextInputs(rgb)
        onColorDragged(rgb)
    }

    fun onAddFavorite(color: Rgb = _selectedColor.value) {
        if (!_favoriteColors.value.contains(color)) {
            _favoriteColors.value = listOf(color) + _favoriteColors.value
        }
        scope.launch {
            repository.addFavorite(color)
        }
    }

    fun onRemoveFavorite(color: Rgb) {
        _favoriteColors.value = _favoriteColors.value.filter { it != color }
        scope.launch {
            repository.removeFavorite(color)
        }
    }

    fun isFavorite(color: Rgb = _selectedColor.value): Boolean {
        return _favoriteColors.value.contains(color)
    }

    fun onPaletteSelected(palette: NamedPalette) {
        _selectedPalette.value = palette
        scope.launch {
            transport.send(Command.SetPalette(palette.colors.take(8)))
        }
    }

    fun onCreatePalette(name: String, colors: List<Rgb>) {
        val safeColors = colors.take(8).ifEmpty { listOf(_selectedColor.value) }
        val palette = NamedPalette(
            id = "palette_${System.currentTimeMillis()}",
            name = name.ifBlank { "My Palette" },
            colors = safeColors
        )
        _palettes.value = listOf(palette) + _palettes.value
        scope.launch {
            repository.savePalette(palette)
        }
    }

    fun onRenamePalette(paletteId: String, newName: String) {
        val existing = _palettes.value.find { it.id == paletteId } ?: return
        val updated = existing.copy(name = newName.ifBlank { existing.name })
        _palettes.value = _palettes.value.map { if (it.id == paletteId) updated else it }
        scope.launch {
            repository.savePalette(updated)
        }
    }

    fun onDeletePalette(paletteId: String) {
        _palettes.value = _palettes.value.filter { it.id != paletteId }
        scope.launch {
            repository.deletePalette(paletteId)
        }
    }

    fun onReorderPalettes(palettes: List<NamedPalette>) {
        _palettes.value = palettes
        scope.launch {
            repository.reorderPalettes(palettes)
        }
    }

    fun onAddColorToPalette(paletteId: String, color: Rgb = _selectedColor.value) {
        val existing = _palettes.value.find { it.id == paletteId } ?: return
        if (existing.colors.size < 8 && !existing.colors.contains(color)) {
            val updated = existing.copy(colors = existing.colors + color)
            _palettes.value = _palettes.value.map { if (it.id == paletteId) updated else it }
            scope.launch {
                repository.savePalette(updated)
            }
        }
    }

    // Scenes (slots 0..7)
    fun onLoadScene(slot: Int) {
        require(slot in 0..7) { "Scene slot must be 0..7, got $slot" }
        _activeScene.value = slot
        _sceneFeedback.value = "Loaded Scene ${slot + 1}"
        scope.launch {
            transport.send(Command.LoadScene(slot))
        }
    }

    fun onSaveScene(slot: Int) {
        require(slot in 0..7) { "Scene slot must be 0..7, got $slot" }
        _activeScene.value = slot
        _sceneFeedback.value = "Saved Scene ${slot + 1}"
        scope.launch {
            transport.send(Command.SaveScene(slot))
        }
    }

    fun clearSceneFeedback() {
        _sceneFeedback.value = null
    }

    suspend fun exportJson(): String {
        val currentData = AppData(
            version = 1,
            brightness = _brightness.value,
            favoriteColors = _favoriteColors.value,
            recentColors = _recentColors.value,
            palettes = _palettes.value
        )
        return AppDataJson.toJson(currentData)
    }

    suspend fun importJson(json: String): Boolean {
        return try {
            val parsed = AppDataJson.fromJson(json)
            _favoriteColors.value = parsed.favoriteColors
            _recentColors.value = parsed.recentColors
            _palettes.value = parsed.palettes
            _brightness.value = parsed.brightness
            scope.launch {
                repository.update { parsed }
            }
            true
        } catch (_: Exception) {
            false
        }
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

        val list = mutableListOf(committed)
        for (c in _recentColors.value) {
            if (c != committed && list.size < 12) list.add(c)
        }
        _recentColors.value = list

        scope.launch {
            repository.addRecent(committed)
        }

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

    private fun dispatchBrightnessCommand(v: Int) {
        brightnessSendCountForTesting++
        scope.launch {
            transport.send(Command.SetBrightness(v))
            repository.setBrightness(v)
        }
    }

    private fun updateTextInputs(rgb: Rgb) {
        _hexInput.value = ColorMath.rgbToHex(rgb)
        _rInput.value = rgb.r.toString()
        _gInput.value = rgb.g.toString()
        _bInput.value = rgb.b.toString()
    }
}
