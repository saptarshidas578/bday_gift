package com.pixelgift.app.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import com.pixelgift.app.data.model.ScheduleItem
import com.pixelgift.app.ui.screens.time.ClockModeCard
import com.pixelgift.app.ui.screens.time.ClockSyncBanner
import com.pixelgift.app.ui.screens.time.EditScheduleDialog
import com.pixelgift.app.ui.screens.time.SchedulesCard
import com.pixelgift.app.ui.screens.time.SleepTimerCard
import com.pixelgift.app.ui.screens.time.TimerCard
import com.pixelgift.app.ui.theme.PixelGiftSpacing
import com.pixelgift.app.ui.time.TimeViewModel
import kotlinx.coroutines.delay

@Composable
fun TimeScreen(
    viewModel: TimeViewModel,
    modifier: Modifier = Modifier
) {
    val isTimeSynced by viewModel.isTimeSynced.collectAsState()
    val isClockMode by viewModel.isClockMode.collectAsState()
    val clockStyle by viewModel.clockStyle.collectAsState()
    val lastSyncTimestamp by viewModel.lastSyncTimestamp.collectAsState()

    val isTimerRunning by viewModel.isTimerRunning.collectAsState()
    val timerTotalSeconds by viewModel.timerTotalSeconds.collectAsState()
    val timerRemainingSeconds by viewModel.timerRemainingSeconds.collectAsState()
    val customTimerMinutes by viewModel.customTimerMinutes.collectAsState()

    val schedules by viewModel.schedules.collectAsState()
    val activeSleepMinutes by viewModel.activeSleepMinutes.collectAsState()
    val isAutoDimmerActive by viewModel.isAutoDimmerActive.collectAsState()
    val feedback by viewModel.feedback.collectAsState()

    // Clear feedback toast after 4s
    LaunchedEffect(feedback) {
        if (feedback != null) {
            delay(4000)
            viewModel.clearFeedback()
        }
    }

    var editingSchedule by remember { mutableStateOf<ScheduleItem?>(null) }

    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(PixelGiftSpacing.md),
        verticalArrangement = Arrangement.spacedBy(PixelGiftSpacing.lg)
    ) {
        // Feedback snackbar banner
        AnimatedVisibility(
            visible = feedback != null,
            enter = fadeIn(),
            exit = fadeOut()
        ) {
            feedback?.let { msg ->
                Card(
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.primaryContainer
                    ),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = msg,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onPrimaryContainer,
                        modifier = Modifier.padding(PixelGiftSpacing.md)
                    )
                }
            }
        }

        // --- 1. Clock Sync Banner ---
        ClockSyncBanner(
            isSynced = isTimeSynced,
            lastSyncTimestamp = lastSyncTimestamp,
            onSyncClick = { viewModel.onManualSyncTime() }
        )

        // --- 2. Clock Mode & Style Card ---
        ClockModeCard(
            isClockMode = isClockMode,
            clockStyle = clockStyle,
            onToggleClockMode = { viewModel.onToggleClockMode(it) },
            onSelectStyle = { viewModel.onSelectClockStyle(it) }
        )

        // --- 3. Timer Section ---
        TimerCard(
            isRunning = isTimerRunning,
            totalSeconds = timerTotalSeconds,
            remainingSeconds = timerRemainingSeconds,
            customMinutes = customTimerMinutes,
            onCustomMinutesChanged = { viewModel.onCustomMinutesChanged(it) },
            onStartTimer = { viewModel.onStartTimer(it) },
            onCancelTimer = { viewModel.onCancelTimer() }
        )

        // --- 4. Schedules Section ---
        SchedulesCard(
            schedules = schedules,
            onToggleSchedule = { viewModel.onToggleSchedule(it) },
            onEditSchedule = { editingSchedule = it }
        )

        // --- 5. Sleep Timer Section & Feature #13: Bedside Auto-Dimmer ---
        SleepTimerCard(
            activeMinutes = activeSleepMinutes,
            onSetSleepTimer = { viewModel.onSetSleepTimer(it) },
            onCancelSleepTimer = { viewModel.onCancelSleepTimer() },
            isAutoDimmerActive = isAutoDimmerActive,
            onToggleAutoDimmer = { viewModel.onToggleAutoDimmer() }
        )

        Spacer(modifier = Modifier.height(PixelGiftSpacing.xl))
    }

    // Schedule Edit Dialog
    editingSchedule?.let { schedule ->
        EditScheduleDialog(
            schedule = schedule,
            onDismiss = { editingSchedule = null },
            onSave = { updated ->
                viewModel.onSaveSchedule(updated)
                editingSchedule = null
            }
        )
    }
}
