package com.pixelgift.app.ui.screens

import android.Manifest
import android.content.pm.PackageManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.automirrored.filled.Undo
import androidx.compose.material.icons.filled.Cake
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.GraphicEq
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.MicOff
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Save
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Slider
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import com.pixelgift.app.audio.MicrophoneAudioSource
import com.pixelgift.app.audio.SimulatedAudioSource
import com.pixelgift.app.data.model.PixelDrawing
import com.pixelgift.app.protocol.Rgb
import com.pixelgift.app.ui.color.ColorMath
import com.pixelgift.app.ui.magic.MagicViewModel
import com.pixelgift.app.ui.theme.PixelGiftSpacing
import kotlinx.coroutines.delay

@Composable
fun MagicScreen(
    viewModel: MagicViewModel,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val haptic = LocalHapticFeedback.current

    var selectedTabIndex by remember { mutableIntStateOf(0) }
    val tabTitles = listOf("Birthday", "Message", "Draw (9×9)", "Music")

    // Birthday state
    val isPlayingShow by viewModel.isPlayingShow.collectAsState()
    val birthdayName by viewModel.birthdayName.collectAsState()
    val birthdayGreeting by viewModel.birthdayGreeting.collectAsState()
    val birthdayFeedback by viewModel.birthdayFeedback.collectAsState()
    var editNameText by remember(birthdayName) { mutableStateOf(birthdayName) }
    var editGreetingText by remember(birthdayGreeting) { mutableStateOf(birthdayGreeting) }

    // Message state
    val messageText by viewModel.messageText.collectAsState()
    val messageSpeed by viewModel.messageSpeed.collectAsState()
    val isMessageAsciiValid by viewModel.isMessageAsciiValid.collectAsState()
    val messageFeedback by viewModel.messageFeedback.collectAsState()

    // Draw state
    val canvasPixels by viewModel.canvasPixels.collectAsState()
    val selectedDrawColor by viewModel.selectedDrawColor.collectAsState()
    val savedDrawings by viewModel.savedDrawings.collectAsState()
    val drawFeedback by viewModel.drawFeedback.collectAsState()
    var showSaveDrawingDialog by remember { mutableStateOf(false) }
    var newDrawingName by remember { mutableStateOf("") }

    // Music state
    val isMusicActive by viewModel.isMusicActive.collectAsState()
    val liveLevels by viewModel.liveLevels.collectAsState()

    val permissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        if (isGranted) {
            viewModel.startMusicReactive(MicrophoneAudioSource())
        }
    }

    // Auto-dismiss feedbacks
    LaunchedEffect(birthdayFeedback, messageFeedback, drawFeedback) {
        if (birthdayFeedback != null || messageFeedback != null || drawFeedback != null) {
            delay(3500)
            viewModel.clearFeedback()
        }
    }

    Column(modifier = modifier.fillMaxSize()) {
        TabRow(selectedTabIndex = selectedTabIndex) {
            tabTitles.forEachIndexed { index, title ->
                Tab(
                    selected = selectedTabIndex == index,
                    onClick = { selectedTabIndex = index },
                    text = { Text(title, style = MaterialTheme.typography.labelMedium) }
                )
            }
        }

        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(PixelGiftSpacing.lg),
            verticalArrangement = Arrangement.spacedBy(PixelGiftSpacing.lg)
        ) {
            when (selectedTabIndex) {
                // 1. Birthday Show Tab
                0 -> {
                    BirthdayShowTab(
                        isPlayingShow = isPlayingShow,
                        name = editNameText,
                        greeting = editGreetingText,
                        feedback = birthdayFeedback,
                        onNameChanged = { editNameText = it },
                        onGreetingChanged = { editGreetingText = it },
                        onPlayShow = {
                            haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                            viewModel.onPlayBirthday()
                        },
                        onStopShow = {
                            haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                            viewModel.onStopShow()
                        },
                        onSaveContent = {
                            viewModel.onSaveBirthdayContent(editNameText, editGreetingText)
                        }
                    )
                }

                // 2. Custom Message Tab
                1 -> {
                    CustomMessageTab(
                        text = messageText,
                        speed = messageSpeed,
                        isAsciiValid = isMessageAsciiValid,
                        feedback = messageFeedback,
                        onTextChanged = { viewModel.onMessageTextChanged(it) },
                        onSpeedChanged = { viewModel.onMessageSpeedChanged(it) },
                        onSendMessage = {
                            haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                            viewModel.onSendMessage()
                        }
                    )
                }

                // 3. Draw Mode (9x9) Tab
                2 -> {
                    DrawModeTab(
                        canvasPixels = canvasPixels,
                        selectedColor = selectedDrawColor,
                        savedDrawings = savedDrawings,
                        feedback = drawFeedback,
                        onPixelClicked = { viewModel.onPixelClicked(it) },
                        onColorSelected = { viewModel.onSelectDrawColor(it) },
                        onUndo = { viewModel.onUndo() },
                        onClear = { viewModel.onClearCanvas() },
                        onCommit = {
                            haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                            viewModel.onCommitDrawing()
                        },
                        onPromptSave = {
                            newDrawingName = ""
                            showSaveDrawingDialog = true
                        },
                        onLoadDrawing = { viewModel.onLoadDrawing(it) }
                    )
                }

                // 4. Music Reactive Visualizer Tab
                3 -> {
                    MusicReactiveTab(
                        isMusicActive = isMusicActive,
                        liveLevels = liveLevels,
                        onToggleMusic = {
                            if (isMusicActive) {
                                viewModel.stopMusicReactive()
                            } else {
                                val hasPermission = ContextCompat.checkSelfPermission(
                                    context,
                                    Manifest.permission.RECORD_AUDIO
                                ) == PackageManager.PERMISSION_GRANTED

                                if (hasPermission) {
                                    viewModel.startMusicReactive(MicrophoneAudioSource())
                                } else {
                                    permissionLauncher.launch(Manifest.permission.RECORD_AUDIO)
                                }
                            }
                        },
                        onSimulateMusic = {
                            if (isMusicActive) {
                                viewModel.stopMusicReactive()
                            } else {
                                viewModel.startMusicReactive(SimulatedAudioSource())
                            }
                        }
                    )
                }
            }
        }
    }

    // Save Drawing Dialog
    if (showSaveDrawingDialog) {
        AlertDialog(
            onDismissRequest = { showSaveDrawingDialog = false },
            title = { Text("Save Drawing to Favorites") },
            text = {
                OutlinedTextField(
                    value = newDrawingName,
                    onValueChange = { newDrawingName = it },
                    label = { Text("Drawing Name") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        viewModel.onSaveDrawing(newDrawingName)
                        showSaveDrawingDialog = false
                    }
                ) {
                    Text("Save")
                }
            },
            dismissButton = {
                TextButton(onClick = { showSaveDrawingDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }
}

// -------------------------------------------------------------------------
// Sub-Tab 1: Birthday Show
// -------------------------------------------------------------------------
@Composable
private fun BirthdayShowTab(
    isPlayingShow: Boolean,
    name: String,
    greeting: String,
    feedback: String?,
    onNameChanged: (String) -> Unit,
    onGreetingChanged: (String) -> Unit,
    onPlayShow: () -> Unit,
    onStopShow: () -> Unit,
    onSaveContent: () -> Unit
) {
    Card(
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
        shape = MaterialTheme.shapes.large,
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(PixelGiftSpacing.lg),
            verticalArrangement = Arrangement.spacedBy(PixelGiftSpacing.md),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Box(
                modifier = Modifier
                    .size(68.dp)
                    .clip(CircleShape)
                    .background(
                        Brush.radialGradient(
                            listOf(Color(0xFFFF2D55), Color(0xFFFF7A00))
                        )
                    ),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.Cake,
                    contentDescription = null,
                    tint = Color.White,
                    modifier = Modifier.size(36.dp)
                )
            }

            Text(
                text = "Birthday Show Mode",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold
            )
            Text(
                text = "Plays intro sparkle, name reveal, heart & balloon animations, fireworks, and rainbow celebration (Lights-only, speaker not connected).",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            // Play / Stop Button
            if (isPlayingShow) {
                Button(
                    onClick = onStopShow,
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error),
                    modifier = Modifier
                        .fillMaxWidth()
                        .defaultMinSize(minHeight = 48.dp)
                ) {
                    Icon(Icons.Default.Stop, contentDescription = null)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Stop Birthday Show")
                }
            } else {
                Button(
                    onClick = onPlayShow,
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary),
                    modifier = Modifier
                        .fillMaxWidth()
                        .defaultMinSize(minHeight = 48.dp)
                ) {
                    Icon(Icons.Default.PlayArrow, contentDescription = null)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Start Birthday Show Now")
                }
            }

            feedback?.let {
                Text(it, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Bold)
            }
        }
    }

    // Personalized Content Editor
    Card(
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
        shape = MaterialTheme.shapes.large,
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(PixelGiftSpacing.lg),
            verticalArrangement = Arrangement.spacedBy(PixelGiftSpacing.sm)
        ) {
            Text("Personalized Birthday Text (SET_TEXT Chunks)", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)

            OutlinedTextField(
                value = name,
                onValueChange = onNameChanged,
                label = { Text("Birthday Person's Name (Target 1)") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth()
            )

            OutlinedTextField(
                value = greeting,
                onValueChange = onGreetingChanged,
                label = { Text("Custom Greeting (Target 2)") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth()
            )

            OutlinedButton(
                onClick = onSaveContent,
                modifier = Modifier
                    .fillMaxWidth()
                    .defaultMinSize(minHeight = 48.dp)
            ) {
                Icon(Icons.Default.Save, contentDescription = null)
                Spacer(modifier = Modifier.width(8.dp))
                Text("Save Name & Greeting to Device")
            }
        }
    }
}

// -------------------------------------------------------------------------
// Sub-Tab 2: Custom Scrolling Message
// -------------------------------------------------------------------------
@Composable
private fun CustomMessageTab(
    text: String,
    speed: Int,
    isAsciiValid: Boolean,
    feedback: String?,
    onTextChanged: (String) -> Unit,
    onSpeedChanged: (Int) -> Unit,
    onSendMessage: () -> Unit
) {
    Card(
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
        shape = MaterialTheme.shapes.large,
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(PixelGiftSpacing.lg),
            verticalArrangement = Arrangement.spacedBy(PixelGiftSpacing.md)
        ) {
            Text("Custom Scrolling Message", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
            Text("Sends text in 14-byte chunks (target 0) and triggers SHOW_MESSAGE.", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)

            OutlinedTextField(
                value = text,
                onValueChange = onTextChanged,
                label = { Text("Message Text") },
                isError = !isAsciiValid,
                supportingText = {
                    if (!isAsciiValid) {
                        Text("Only ASCII characters (0x00..0x7F) are supported", color = MaterialTheme.colorScheme.error)
                    } else {
                        Text("${text.length} chars • ${if (text.isEmpty()) 1 else ((text.length + 13) / 14)} chunk(s)")
                    }
                },
                modifier = Modifier.fillMaxWidth()
            )

            Column {
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    Text("Scroll Speed", style = MaterialTheme.typography.labelMedium)
                    Text("$speed%", style = MaterialTheme.typography.bodySmall.copy(fontFamily = FontFamily.Monospace))
                }
                Slider(
                    value = speed.toFloat(),
                    onValueChange = { onSpeedChanged(it.toInt()) },
                    valueRange = 1f..100f
                )
            }

            Button(
                onClick = onSendMessage,
                enabled = isAsciiValid && text.isNotBlank(),
                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary),
                modifier = Modifier
                    .fillMaxWidth()
                    .defaultMinSize(minHeight = 48.dp)
            ) {
                Icon(Icons.AutoMirrored.Filled.Send, contentDescription = null)
                Spacer(modifier = Modifier.width(8.dp))
                Text("Send & Scroll Message")
            }

            feedback?.let {
                Text(it, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Bold)
            }
        }
    }
}

// -------------------------------------------------------------------------
// Sub-Tab 3: Draw Mode (9x9 Canvas)
// -------------------------------------------------------------------------
@Composable
private fun DrawModeTab(
    canvasPixels: List<Rgb>,
    selectedColor: Rgb,
    savedDrawings: List<PixelDrawing>,
    feedback: String?,
    onPixelClicked: (Int) -> Unit,
    onColorSelected: (Rgb) -> Unit,
    onUndo: () -> Unit,
    onClear: () -> Unit,
    onCommit: () -> Unit,
    onPromptSave: () -> Unit,
    onLoadDrawing: (PixelDrawing) -> Unit
) {
    val paletteOptions = listOf(
        Rgb(255, 45, 85),   // Pink
        Rgb(255, 122, 0),  // Amber
        Rgb(255, 214, 0),  // Yellow
        Rgb(0, 230, 118),  // Green
        Rgb(0, 229, 255),  // Cyan
        Rgb(158, 119, 237),// Violet
        Rgb(255, 255, 255),// White
        Rgb.BLACK          // Eraser
    )

    Card(
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
        shape = MaterialTheme.shapes.large,
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(PixelGiftSpacing.md),
            verticalArrangement = Arrangement.spacedBy(PixelGiftSpacing.md),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text("9×9 Pixel Draw Canvas (243-Byte Frame)", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)

            // 9x9 Interactive Canvas Grid
            Box(
                modifier = Modifier
                    .size(240.dp)
                    .clip(RoundedCornerShape(16.dp))
                    .background(Color(0xFF0F1117))
                    .border(2.dp, MaterialTheme.colorScheme.outlineVariant, RoundedCornerShape(16.dp))
                    .padding(8.dp)
            ) {
                Canvas(
                    modifier = Modifier
                        .fillMaxSize()
                        .aspectRatio(1f)
                        .pointerInput(Unit) {
                            detectTapGestures { offset ->
                                val cellW = size.width / 9f
                                val cellH = size.height / 9f
                                val col = (offset.x / cellW).toInt().coerceIn(0, 8)
                                val row = (offset.y / cellH).toInt().coerceIn(0, 8)
                                onPixelClicked(row * 9 + col)
                            }
                        }
                        .pointerInput(Unit) {
                            detectDragGestures { change, _ ->
                                change.consume()
                                val cellW = size.width / 9f
                                val cellH = size.height / 9f
                                val col = (change.position.x / cellW).toInt().coerceIn(0, 8)
                                val row = (change.position.y / cellH).toInt().coerceIn(0, 8)
                                onPixelClicked(row * 9 + col)
                            }
                        }
                ) {
                    val cellW = size.width / 9f
                    val cellH = size.height / 9f
                    val radius = minOf(cellW, cellH) * 0.42f

                    for (r in 0 until 9) {
                        for (c in 0 until 9) {
                            val idx = r * 9 + c
                            val pixelColor = ColorMath.rgbToComposeColor(canvasPixels[idx])
                            val center = Offset((c + 0.5f) * cellW, (r + 0.5f) * cellH)

                            drawCircle(
                                color = if (canvasPixels[idx] == Rgb.BLACK) Color(0xFF1E222D) else pixelColor,
                                radius = radius,
                                center = center
                            )
                            drawCircle(
                                color = Color(0x33FFFFFF),
                                radius = radius,
                                center = center,
                                style = Stroke(width = 1f)
                            )
                        }
                    }
                }
            }

            // Draw Palette Row
            Row(
                modifier = Modifier.horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(PixelGiftSpacing.sm)
            ) {
                paletteOptions.forEach { rgb ->
                    val isSelected = selectedColor == rgb
                    Box(
                        modifier = Modifier
                            .size(36.dp)
                            .clip(CircleShape)
                            .background(ColorMath.rgbToComposeColor(rgb))
                            .border(
                                width = if (isSelected) 3.dp else 1.dp,
                                color = if (isSelected) MaterialTheme.colorScheme.primary else Color(0x44FFFFFF),
                                shape = CircleShape
                            )
                            .clickable { onColorSelected(rgb) },
                        contentAlignment = Alignment.Center
                    ) {
                        if (rgb == Rgb.BLACK) {
                            Icon(Icons.Default.Clear, contentDescription = "Eraser", tint = Color.Gray, modifier = Modifier.size(16.dp))
                        }
                    }
                }
            }

            // Actions Row: Undo, Clear, Save, Commit
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(PixelGiftSpacing.xs)
            ) {
                OutlinedButton(onClick = onUndo, modifier = Modifier.weight(1f).defaultMinSize(minHeight = 44.dp)) {
                    Icon(Icons.AutoMirrored.Filled.Undo, contentDescription = "Undo", modifier = Modifier.size(16.dp))
                }
                OutlinedButton(onClick = onClear, modifier = Modifier.weight(1f).defaultMinSize(minHeight = 44.dp)) {
                    Icon(Icons.Default.Clear, contentDescription = "Clear", modifier = Modifier.size(16.dp))
                }
                OutlinedButton(onClick = onPromptSave, modifier = Modifier.weight(1f).defaultMinSize(minHeight = 44.dp)) {
                    Icon(Icons.Default.Save, contentDescription = "Save Favorite", modifier = Modifier.size(16.dp))
                }
                Button(
                    onClick = onCommit,
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary),
                    modifier = Modifier.weight(1.5f).defaultMinSize(minHeight = 44.dp)
                ) {
                    Text("Display", style = MaterialTheme.typography.labelMedium)
                }
            }

            feedback?.let {
                Text(it, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Bold)
            }

            // Saved Favorites Row
            if (savedDrawings.isNotEmpty()) {
                Column(modifier = Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(PixelGiftSpacing.xs)) {
                    Text("Favorite & Preset Drawings", style = MaterialTheme.typography.labelMedium)
                    Row(
                        modifier = Modifier.horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(PixelGiftSpacing.sm)
                    ) {
                        savedDrawings.forEach { drawing ->
                            FilterChip(
                                selected = false,
                                onClick = { onLoadDrawing(drawing) },
                                label = { Text(drawing.name) }
                            )
                        }
                    }
                }
            }
        }
    }
}

// -------------------------------------------------------------------------
// Sub-Tab 4: Music Reactive Visualizer
// -------------------------------------------------------------------------
@Composable
private fun MusicReactiveTab(
    isMusicActive: Boolean,
    liveLevels: ByteArray,
    onToggleMusic: () -> Unit,
    onSimulateMusic: () -> Unit
) {
    Card(
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
        shape = MaterialTheme.shapes.large,
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(PixelGiftSpacing.lg),
            verticalArrangement = Arrangement.spacedBy(PixelGiftSpacing.md),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(PixelGiftSpacing.sm)) {
                    Icon(Icons.Default.GraphicEq, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                    Text("9-Band FFT Visualizer", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                }
                Text("<= 20 Hz", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }

            Text(
                text = "Captures microphone audio, performs FFT into 9 frequency bands, and sends MUSIC_LEVELS packets at 20 Hz via write-without-response.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            // Equalizer Bars (9 Columns)
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(180.dp)
                    .clip(RoundedCornerShape(16.dp))
                    .background(Color(0xFF0F1117))
                    .border(1.dp, MaterialTheme.colorScheme.outlineVariant, RoundedCornerShape(16.dp))
                    .padding(16.dp),
                contentAlignment = Alignment.BottomCenter
            ) {
                Canvas(modifier = Modifier.fillMaxSize()) {
                    val cols = 9
                    val colWidth = size.width / (cols * 1.5f)
                    val spacing = (size.width - (cols * colWidth)) / (cols - 1)

                    for (i in 0 until cols) {
                        val level = if (i < liveLevels.size) (liveLevels[i].toInt() and 0xFF) / 255f else 0.05f
                        val barHeight = maxOf(4f, level * size.height)
                        val left = i * (colWidth + spacing)
                        val top = size.height - barHeight

                        drawRoundRect(
                            brush = Brush.verticalGradient(
                                colors = listOf(Color(0xFFFF2D55), Color(0xFFFF7A00), Color(0xFF00E5FF)),
                                startY = 0f,
                                endY = size.height
                            ),
                            topLeft = Offset(left, top),
                            size = Size(colWidth, barHeight),
                            cornerRadius = CornerRadius(4f, 4f)
                        )
                    }
                }
            }

            // Controls
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(PixelGiftSpacing.sm)
            ) {
                Button(
                    onClick = onToggleMusic,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = if (isMusicActive) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary
                    ),
                    modifier = Modifier
                        .weight(1.5f)
                        .defaultMinSize(minHeight = 48.dp)
                ) {
                    Icon(
                        imageVector = if (isMusicActive) Icons.Default.MicOff else Icons.Default.Mic,
                        contentDescription = null
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(if (isMusicActive) "Stop Mic" else "Listen Mic")
                }

                OutlinedButton(
                    onClick = onSimulateMusic,
                    modifier = Modifier
                        .weight(1f)
                        .defaultMinSize(minHeight = 48.dp)
                ) {
                    Text(if (isMusicActive) "Stop Sim" else "Simulate")
                }
            }
        }
    }
}
