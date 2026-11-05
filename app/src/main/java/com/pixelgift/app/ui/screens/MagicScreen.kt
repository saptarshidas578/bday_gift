package com.pixelgift.app.ui.screens

import android.Manifest
import android.content.pm.PackageManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
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
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.core.content.ContextCompat
import com.pixelgift.app.audio.MicrophoneAudioSource
import com.pixelgift.app.audio.SimulatedAudioSource
import com.pixelgift.app.ui.magic.MagicViewModel
import com.pixelgift.app.ui.screens.magic.BirthdaySection
import com.pixelgift.app.ui.screens.magic.MessageSection
import com.pixelgift.app.ui.screens.magic.DrawSection
import com.pixelgift.app.ui.screens.magic.MusicSection
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
    val tabTitles = listOf("Birthday", "Message", "Draw (10×10)", "Music")

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
                0 -> BirthdaySection(
                    isPlayingShow = isPlayingShow,
                    name = editNameText,
                    greeting = editGreetingText,
                    feedback = birthdayFeedback,
                    onNameChanged = { editNameText = it },
                    onGreetingChanged = { editGreetingText = it },
                    onPlayShow = { viewModel.onPlayBirthday() },
                    onStopShow = { viewModel.onStopShow() },
                    onSaveContent = { viewModel.onSaveBirthdayContent(editNameText, editGreetingText) }
                )

                1 -> MessageSection(
                    text = messageText,
                    speed = messageSpeed,
                    isAsciiValid = isMessageAsciiValid,
                    feedback = messageFeedback,
                    onTextChanged = { viewModel.onMessageTextChanged(it) },
                    onSpeedChanged = { viewModel.onMessageSpeedChanged(it) },
                    onSendMessage = { viewModel.onSendMessage() }
                )

                2 -> DrawSection(
                    canvasPixels = canvasPixels,
                    selectedColor = selectedDrawColor,
                    savedDrawings = savedDrawings,
                    feedback = drawFeedback,
                    onPixelClicked = { viewModel.onPixelClicked(it) },
                    onColorSelected = { viewModel.onSelectDrawColor(it) },
                    onUndo = { viewModel.onUndo() },
                    onClear = { viewModel.onClearCanvas() },
                    onCommit = { viewModel.onCommitDrawing() },
                    onPromptSave = {
                        newDrawingName = ""
                        showSaveDrawingDialog = true
                    },
                    onLoadDrawing = { viewModel.onLoadDrawing(it) }
                )

                3 -> MusicSection(
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

    // Dialog: Save Drawing Favorite
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
                TextButton(
                    onClick = {
                        viewModel.onSaveDrawing(newDrawingName)
                        showSaveDrawingDialog = false
                    },
                    enabled = newDrawingName.isNotBlank()
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
