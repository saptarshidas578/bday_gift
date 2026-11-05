package com.pixelgift.app.ui.magic

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.pixelgift.app.audio.AudioFftProcessor
import com.pixelgift.app.audio.AudioSampleSource
import com.pixelgift.app.data.model.PixelDrawing
import com.pixelgift.app.protocol.Command
import com.pixelgift.app.protocol.ProtocolCodec
import com.pixelgift.app.protocol.Rgb
import com.pixelgift.app.transport.DeviceTransport
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class MagicViewModel(
    val transport: DeviceTransport,
    private val externalScope: CoroutineScope? = null
) : ViewModel() {

    private val scope: CoroutineScope
        get() = externalScope ?: viewModelScope

    // --- 1. Birthday Show State ---
    private val _isPlayingShow = MutableStateFlow(false)
    val isPlayingShow: StateFlow<Boolean> = _isPlayingShow.asStateFlow()

    private val _birthdayName = MutableStateFlow("Alice")
    val birthdayName: StateFlow<String> = _birthdayName.asStateFlow()

    private val _birthdayGreeting = MutableStateFlow("Happy Birthday!")
    val birthdayGreeting: StateFlow<String> = _birthdayGreeting.asStateFlow()

    private val _birthdayFeedback = MutableStateFlow<String?>(null)
    val birthdayFeedback: StateFlow<String?> = _birthdayFeedback.asStateFlow()

    // --- 2. Custom Message State ---
    private val _messageText = MutableStateFlow("Happy Birthday!")
    val messageText: StateFlow<String> = _messageText.asStateFlow()

    private val _messageSpeed = MutableStateFlow(50)
    val messageSpeed: StateFlow<Int> = _messageSpeed.asStateFlow()

    private val _isMessageAsciiValid = MutableStateFlow(true)
    val isMessageAsciiValid: StateFlow<Boolean> = _isMessageAsciiValid.asStateFlow()

    private val _messageFeedback = MutableStateFlow<String?>(null)
    val messageFeedback: StateFlow<String?> = _messageFeedback.asStateFlow()

    // --- 3. Draw Canvas State (10x10 = 100 pixels) ---
    private val _canvasPixels = MutableStateFlow(PixelDrawing.BLANK_CANVAS)
    val canvasPixels: StateFlow<List<Rgb>> = _canvasPixels.asStateFlow()

    private val _selectedDrawColor = MutableStateFlow(Rgb(255, 45, 85)) // Vibrant pink
    val selectedDrawColor: StateFlow<Rgb> = _selectedDrawColor.asStateFlow()

    private val undoHistory = ArrayDeque<List<Rgb>>()

    private val _savedDrawings = MutableStateFlow(
        listOf(
            PixelDrawing.HEART_PRESET,
            PixelDrawing.STAR_PRESET,
            PixelDrawing.SMILE_PRESET,
            PixelDrawing.GIFT_PRESET
        )
    )
    val savedDrawings: StateFlow<List<PixelDrawing>> = _savedDrawings.asStateFlow()

    private val _drawFeedback = MutableStateFlow<String?>(null)
    val drawFeedback: StateFlow<String?> = _drawFeedback.asStateFlow()

    // --- 4. Music Reactive State ---
    private val _isMusicActive = MutableStateFlow(false)
    val isMusicActive: StateFlow<Boolean> = _isMusicActive.asStateFlow()

    private val _liveLevels = MutableStateFlow(ByteArray(com.pixelgift.app.protocol.ProtocolConstants.MATRIX_WIDTH))
    val liveLevels: StateFlow<ByteArray> = _liveLevels.asStateFlow()

    private val fftProcessor = AudioFftProcessor(sampleRate = 44100, fftSize = 512)
    private var activeAudioSource: AudioSampleSource? = null
    private var lastMusicSendTimeMs = 0L

    var sendCountForTesting: Int = 0
        private set

    init {
        _isPlayingShow.value = transport.status.value.isShowRunning
    }

    // --- Birthday Actions ---
    fun onPlayBirthday() {
        _isPlayingShow.value = true
        _birthdayFeedback.value = "Playing Birthday Show"
        sendCountForTesting++
        scope.launch {
            transport.send(Command.PlayBirthday)
        }
    }

    fun onStopShow() {
        _isPlayingShow.value = false
        _birthdayFeedback.value = "Show Stopped"
        sendCountForTesting++
        scope.launch {
            transport.send(Command.StopShow)
        }
    }

    fun onSaveBirthdayContent(name: String, greeting: String): Boolean {
        if (!validateAscii(name) || !validateAscii(greeting)) {
            _birthdayFeedback.value = "Error: only printable ASCII characters allowed"
            return false
        }

        _birthdayName.value = name
        _birthdayGreeting.value = greeting
        _birthdayFeedback.value = "Birthday content saved to device"

        scope.launch {
            // Target 1: Birthday Name
            val nameChunks = ProtocolCodec.chunkText(target = 1, name)
            for (chunk in nameChunks) {
                sendCountForTesting++
                transport.send(chunk)
            }

            // Target 2: Birthday Greeting
            val greetingChunks = ProtocolCodec.chunkText(target = 2, greeting)
            for (chunk in greetingChunks) {
                sendCountForTesting++
                transport.send(chunk)
            }
        }
        return true
    }

    // Feature #16: "Thinking of You" Tap (Local BLE)
    fun onSendHeartbeat() {
        _birthdayFeedback.value = "Sending glowing Heartbeat to matrix..."
        scope.launch {
            sendCountForTesting++
            transport.send(
                Command.SetEffect(
                    base = 4, // Heartbeat effect
                    speed = 60,
                    dir = 0,
                    mirror = 0,
                    colorMode = 0,
                    c1 = Rgb(255, 45, 85),
                    c2 = Rgb(255, 120, 150)
                )
            )
            _birthdayFeedback.value = "Heartbeat glowing on matrix ❤️"
        }
    }

    // Feature #4: Daily Secret Gift Notes
    fun onSendSecretNote(noteText: String) {
        _birthdayFeedback.value = "Sending secret note..."
        scope.launch {
            val chunks = ProtocolCodec.chunkText(target = 0, noteText)
            for (chunk in chunks) {
                sendCountForTesting++
                transport.send(chunk)
            }
            sendCountForTesting++
            transport.send(Command.ShowMessage(45))
            _birthdayFeedback.value = "Secret note scrolling on matrix ✨"
        }
    }

    // --- Custom Message Actions ---
    fun onMessageTextChanged(text: String) {
        _messageText.value = text
        _isMessageAsciiValid.value = validateAscii(text)
    }

    fun onMessageSpeedChanged(speed: Int) {
        _messageSpeed.value = speed.coerceIn(1, 100)
    }

    fun onSendMessage(): Boolean {
        val text = _messageText.value
        if (!validateAscii(text)) {
            _messageFeedback.value = "Error: only printable ASCII characters allowed"
            return false
        }

        _messageFeedback.value = "Sending message '${text}'…"
        scope.launch {
            // Target 0: Custom Message chunks
            val chunks = ProtocolCodec.chunkText(target = 0, text)
            for (chunk in chunks) {
                sendCountForTesting++
                transport.send(chunk)
            }
            // Trigger SHOW_MESSAGE
            sendCountForTesting++
            transport.send(Command.ShowMessage(_messageSpeed.value))
            _messageFeedback.value = "Message scrolling on matrix"
        }
        return true
    }

    // --- Draw Mode Actions ---
    fun onSelectDrawColor(rgb: Rgb) {
        _selectedDrawColor.value = rgb
    }

    fun onPixelClicked(index: Int) {
        if (index !in 0 until com.pixelgift.app.protocol.ProtocolConstants.MATRIX_LED_COUNT) return
        val current = _canvasPixels.value
        val color = _selectedDrawColor.value

        // Don't record undo if color doesn't change
        if (current[index] == color) return

        undoHistory.addLast(current)
        if (undoHistory.size > 20) undoHistory.removeFirst()

        val updated = current.toMutableList()
        updated[index] = color
        _canvasPixels.value = updated
    }

    fun onUndo() {
        if (undoHistory.isNotEmpty()) {
            _canvasPixels.value = undoHistory.removeLast()
        }
    }

    fun onClearCanvas() {
        val current = _canvasPixels.value
        if (current.any { it != Rgb.BLACK }) {
            undoHistory.addLast(current)
            _canvasPixels.value = PixelDrawing.BLANK_CANVAS
        }
    }

    fun onCommitDrawing() {
        val drawing = PixelDrawing("active_draw", "Custom Drawing", _canvasPixels.value)
        val frameBytes = drawing.toFrameBytes()
        _drawFeedback.value = "Sending drawing to matrix…"

        scope.launch {
            val chunks = ProtocolCodec.chunkDrawFrame(frameBytes)
            for (chunk in chunks) {
                sendCountForTesting++
                transport.send(chunk)
            }
            sendCountForTesting++
            transport.send(Command.DrawCommit)
            _drawFeedback.value = "Drawing active on device"
        }
    }

    fun onSaveDrawing(name: String) {
        val safeName = name.ifBlank { "Drawing ${_savedDrawings.value.size + 1}" }
        val newDrawing = PixelDrawing(
            id = "draw_${System.currentTimeMillis()}",
            name = safeName,
            pixels = _canvasPixels.value
        )
        _savedDrawings.value = listOf(newDrawing) + _savedDrawings.value
        _drawFeedback.value = "Saved '$safeName' to favorites"
    }

    fun onLoadDrawing(drawing: PixelDrawing) {
        undoHistory.addLast(_canvasPixels.value)
        _canvasPixels.value = drawing.pixels
        onCommitDrawing()
    }

    // Feature #5: Animated Memories Reel
    fun onPlayMemoriesReel() {
        _drawFeedback.value = "Playing Animated Memories Reel..."
        scope.launch {
            val frames = listOf(
                PixelDrawing.HEART_PRESET,
                PixelDrawing.STAR_PRESET,
                PixelDrawing.SMILE_PRESET,
                PixelDrawing.GIFT_PRESET
            )
            for (frame in frames) {
                undoHistory.addLast(_canvasPixels.value)
                _canvasPixels.value = frame.pixels
                onCommitDrawing()
                kotlinx.coroutines.delay(1200)
            }
            _drawFeedback.value = "Memories Reel complete"
        }
    }

    // Feature #12: Interactive Life Sandbox
    fun onEvolveLife() {
        onCommitDrawing()
        scope.launch {
            kotlinx.coroutines.delay(400)
            transport.send(
                Command.SetEffect(
                    base = 57, // Game of Life
                    speed = 40,
                    dir = 0,
                    mirror = 0,
                    colorMode = 0,
                    c1 = _selectedDrawColor.value,
                    c2 = Rgb(0, 229, 255)
                )
            )
            _drawFeedback.value = "Conway's Life evolving on matrix ✨"
        }
    }

    // --- Music Reactive Actions ---
    fun startMusicReactive(source: AudioSampleSource) {
        if (_isMusicActive.value) return
        _isMusicActive.value = true
        activeAudioSource = source

        source.start { samples ->
            val levels = fftProcessor.processSamples(samples)
            _liveLevels.value = levels

            // Throttle sending to <= 20 Hz (50 ms interval)
            val now = System.currentTimeMillis()
            if (now - lastMusicSendTimeMs >= 50L) {
                lastMusicSendTimeMs = now
                sendCountForTesting++
                scope.launch {
                    transport.send(Command.MusicLevels(levels))
                }
            }
        }
    }

    fun stopMusicReactive() {
        _isMusicActive.value = false
        activeAudioSource?.stop()
        activeAudioSource = null
        _liveLevels.value = ByteArray(com.pixelgift.app.protocol.ProtocolConstants.MATRIX_WIDTH)
    }

    fun clearFeedback() {
        _birthdayFeedback.value = null
        _messageFeedback.value = null
        _drawFeedback.value = null
    }

    private fun validateAscii(text: String): Boolean {
        for (c in text) {
            if (c.code !in 0..127) return false
        }
        return true
    }

    override fun onCleared() {
        super.onCleared()
        stopMusicReactive()
    }
}
