package com.pixelgift.app.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
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
import androidx.compose.material.icons.filled.Alarm
import androidx.compose.material.icons.filled.Bedtime
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.HourglassBottom
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material.icons.filled.Sync
import androidx.compose.material.icons.filled.WarningAmber
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Slider
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.pixelgift.app.data.model.ScheduleItem
import com.pixelgift.app.protocol.ProtocolConstants
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

        // --- 5. Sleep Timer Section ---
        SleepTimerCard(
            activeMinutes = activeSleepMinutes,
            onSetSleepTimer = { viewModel.onSetSleepTimer(it) },
            onCancelSleepTimer = { viewModel.onCancelSleepTimer() }
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

// -------------------------------------------------------------
// 1. Clock Sync Banner (SPEC 6.5)
// -------------------------------------------------------------
@Composable
private fun ClockSyncBanner(
    isSynced: Boolean,
    lastSyncTimestamp: Long?,
    onSyncClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    if (!isSynced) {
        Card(
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.errorContainer
            ),
            shape = RoundedCornerShape(16.dp),
            modifier = modifier
                .fillMaxWidth()
                .semantics { contentDescription = "Device Clock Not Synced Warning" }
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(PixelGiftSpacing.md),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(PixelGiftSpacing.md)
            ) {
                Icon(
                    imageVector = Icons.Default.WarningAmber,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.error,
                    modifier = Modifier.size(32.dp)
                )

                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "Device Clock Not Synced",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onErrorContainer
                    )
                    Text(
                        text = "ESP32 lost time on power cycle. Clock & scheduled alarms pause until synced.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onErrorContainer.copy(alpha = 0.85f)
                    )
                }

                Button(
                    onClick = onSyncClick,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.error
                    ),
                    modifier = Modifier.defaultMinSize(minHeight = 48.dp)
                ) {
                    Icon(Icons.Default.Sync, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Sync")
                }
            }
        }
    } else {
        Card(
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surfaceVariant
            ),
            shape = RoundedCornerShape(12.dp),
            modifier = modifier.fillMaxWidth()
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = PixelGiftSpacing.md, vertical = PixelGiftSpacing.sm),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(PixelGiftSpacing.sm)
                ) {
                    Icon(
                        imageVector = Icons.Default.CheckCircle,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(20.dp)
                    )
                    Text(
                        text = "Clock in sync with phone",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                OutlinedButton(
                    onClick = onSyncClick,
                    modifier = Modifier.defaultMinSize(minHeight = 44.dp)
                ) {
                    Icon(Icons.Default.Sync, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Re-sync", style = MaterialTheme.typography.labelMedium)
                }
            }
        }
    }
}

// -------------------------------------------------------------
// 2. Clock Mode & Style Card (SPEC 6.6)
// -------------------------------------------------------------
@Composable
private fun ClockModeCard(
    isClockMode: Boolean,
    clockStyle: Int,
    onToggleClockMode: (Boolean) -> Unit,
    onSelectStyle: (Int) -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
        shape = RoundedCornerShape(20.dp),
        modifier = modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(PixelGiftSpacing.lg),
            verticalArrangement = Arrangement.spacedBy(PixelGiftSpacing.md)
        ) {
            // Header with Switch
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(PixelGiftSpacing.sm)
                ) {
                    Icon(
                        imageVector = Icons.Default.Schedule,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(24.dp)
                    )
                    Column {
                        Text(
                            text = "Matrix Clock Mode",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = if (isClockMode) "Active on 9×9 matrix" else "Inactive",
                            style = MaterialTheme.typography.bodySmall,
                            color = if (isClockMode) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                Switch(
                    checked = isClockMode,
                    onCheckedChange = onToggleClockMode,
                    colors = SwitchDefaults.colors(
                        checkedThumbColor = MaterialTheme.colorScheme.primary,
                        checkedTrackColor = MaterialTheme.colorScheme.primaryContainer
                    ),
                    modifier = Modifier.semantics {
                        contentDescription = "Toggle Clock Mode"
                    }
                )
            }

            Text(
                text = "Display Style",
                style = MaterialTheme.typography.labelLarge,
                fontWeight = FontWeight.Bold
            )

            // Two Clock Styles per SPEC 6.6
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(PixelGiftSpacing.md)
            ) {
                ClockStyleOption(
                    title = "Stacked 3×4",
                    subtitle = "Hours top, mins bottom, center blinking dot",
                    selected = clockStyle == 0,
                    onClick = { onSelectStyle(0) },
                    modifier = Modifier.weight(1f)
                )
                ClockStyleOption(
                    title = "Scrolling 5×7",
                    subtitle = "Horizontal marquee banner (HH:MM)",
                    selected = clockStyle == 1,
                    onClick = { onSelectStyle(1) },
                    modifier = Modifier.weight(1f)
                )
            }
        }
    }
}

@Composable
private fun ClockStyleOption(
    title: String,
    subtitle: String,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val borderColor by animateColorAsState(
        if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outlineVariant,
        label = "clockStyleBorder"
    )
    val bgColor by animateColorAsState(
        if (selected) MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.35f) else MaterialTheme.colorScheme.surface,
        label = "clockStyleBg"
    )

    Card(
        colors = CardDefaults.cardColors(containerColor = bgColor),
        shape = RoundedCornerShape(12.dp),
        modifier = modifier
            .border(width = if (selected) 2.dp else 1.dp, color = borderColor, shape = RoundedCornerShape(12.dp))
            .clickable(onClick = onClick)
            .defaultMinSize(minHeight = 84.dp)
            .semantics { contentDescription = "Clock Style $title, ${if (selected) "Selected" else "Not selected"}" }
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(PixelGiftSpacing.md),
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold,
                    color = if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface
                )
                if (selected) {
                    Icon(
                        imageVector = Icons.Default.Check,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(16.dp)
                    )
                }
            }
            Text(
                text = subtitle,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

// -------------------------------------------------------------
// 3. Timer Card with Countdown & Presets (SPEC 7.4, 9.2)
// -------------------------------------------------------------
@Composable
private fun TimerCard(
    isRunning: Boolean,
    totalSeconds: Long,
    remainingSeconds: Long,
    customMinutes: Int,
    onCustomMinutesChanged: (Int) -> Unit,
    onStartTimer: (Long) -> Unit,
    onCancelTimer: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
        shape = RoundedCornerShape(20.dp),
        modifier = modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(PixelGiftSpacing.lg),
            verticalArrangement = Arrangement.spacedBy(PixelGiftSpacing.md)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(PixelGiftSpacing.sm)
            ) {
                Icon(
                    imageVector = Icons.Default.HourglassBottom,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(24.dp)
                )
                Text(
                    text = "Offline Timer",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
            }

            // Live Countdown Display
            val mins = remainingSeconds / 60
            val secs = remainingSeconds % 60
            val formattedTime = "%02d:%02d".format(mins, secs)
            val progress = if (totalSeconds > 0) {
                (remainingSeconds.toFloat() / totalSeconds.toFloat()).coerceIn(0f, 1f)
            } else 0f

            Card(
                colors = CardDefaults.cardColors(
                    containerColor = if (isRunning) MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f) else MaterialTheme.colorScheme.surface
                ),
                shape = RoundedCornerShape(16.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(PixelGiftSpacing.lg),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(PixelGiftSpacing.sm)
                ) {
                    Text(
                        text = formattedTime,
                        style = MaterialTheme.typography.displayMedium,
                        fontWeight = FontWeight.Bold,
                        fontFamily = FontFamily.Monospace,
                        color = if (isRunning) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface
                    )

                    LinearProgressIndicator(
                        progress = { progress },
                        modifier = Modifier
                            .fillMaxWidth(0.85f)
                            .height(8.dp)
                            .clip(RoundedCornerShape(4.dp)),
                        color = MaterialTheme.colorScheme.primary,
                        trackColor = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)
                    )

                    Text(
                        text = if (isRunning) "Timer running on device • 3 beeps when done" else "Timer ready",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    if (isRunning) {
                        Button(
                            onClick = onCancelTimer,
                            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error),
                            modifier = Modifier
                                .fillMaxWidth(0.7f)
                                .defaultMinSize(minHeight = 48.dp)
                        ) {
                            Icon(Icons.Default.Stop, contentDescription = null, modifier = Modifier.size(20.dp))
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Cancel Timer")
                        }
                    }
                }
            }

            // Quick Presets: 1m, 5m, 10m, 15m, 30m
            Text(
                text = "Quick Presets",
                style = MaterialTheme.typography.labelLarge,
                fontWeight = FontWeight.Bold
            )
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(PixelGiftSpacing.xs)
            ) {
                listOf(1, 5, 10, 15, 30).forEach { presetMin ->
                    OutlinedButton(
                        onClick = { onStartTimer(presetMin * 60L) },
                        modifier = Modifier
                            .weight(1f)
                            .defaultMinSize(minHeight = 48.dp)
                    ) {
                        Text("${presetMin}m", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.bodySmall)
                    }
                }
            }

            // Custom Slider
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(PixelGiftSpacing.xs)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Custom Duration",
                        style = MaterialTheme.typography.labelMedium
                    )
                    Text(
                        text = "$customMinutes min",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )
                }

                Slider(
                    value = customMinutes.toFloat(),
                    onValueChange = { onCustomMinutesChanged(it.toInt()) },
                    valueRange = 1f..120f,
                    steps = 119,
                    modifier = Modifier.fillMaxWidth()
                )

                Button(
                    onClick = { onStartTimer(customMinutes * 60L) },
                    modifier = Modifier
                        .fillMaxWidth()
                        .defaultMinSize(minHeight = 48.dp)
                ) {
                    Icon(Icons.Default.PlayArrow, contentDescription = null)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Start $customMinutes min Timer")
                }
            }
        }
    }
}

// -------------------------------------------------------------
// 4. Schedules Card (SPEC 7.4)
// -------------------------------------------------------------
@Composable
private fun SchedulesCard(
    schedules: List<ScheduleItem>,
    onToggleSchedule: (Int) -> Unit,
    onEditSchedule: (ScheduleItem) -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
        shape = RoundedCornerShape(20.dp),
        modifier = modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(PixelGiftSpacing.lg),
            verticalArrangement = Arrangement.spacedBy(PixelGiftSpacing.md)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(PixelGiftSpacing.sm)
            ) {
                Icon(
                    imageVector = Icons.Default.Alarm,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(24.dp)
                )
                Text(
                    text = "Schedules & Alarms",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
            }

            Text(
                text = "Stored on device. Requires time sync to fire on schedule.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Column(verticalArrangement = Arrangement.spacedBy(PixelGiftSpacing.sm)) {
                schedules.forEach { item ->
                    ScheduleRow(
                        item = item,
                        onToggle = { onToggleSchedule(item.slot) },
                        onEdit = { onEditSchedule(item) }
                    )
                }
            }
        }
    }
}

@Composable
private fun ScheduleRow(
    item: ScheduleItem,
    onToggle: () -> Unit,
    onEdit: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        colors = CardDefaults.cardColors(
            containerColor = if (item.enabled) MaterialTheme.colorScheme.surface else MaterialTheme.colorScheme.surface.copy(alpha = 0.6f)
        ),
        shape = RoundedCornerShape(12.dp),
        modifier = modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(PixelGiftSpacing.md),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column(
                modifier = Modifier
                    .weight(1f)
                    .clickable(onClick = onEdit)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(PixelGiftSpacing.sm)
                ) {
                    Text(
                        text = item.formattedTime,
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        color = if (item.enabled) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        text = "Slot ${item.slot}",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.outline
                    )
                }

                Text(
                    text = "${item.daysSummary} • ${item.actionName}",
                    style = MaterialTheme.typography.bodySmall,
                    color = if (item.enabled) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            Row(verticalAlignment = Alignment.CenterVertically) {
                IconButton(
                    onClick = onEdit,
                    modifier = Modifier.defaultMinSize(minHeight = 48.dp, minWidth = 48.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Edit,
                        contentDescription = "Edit Schedule Slot ${item.slot}",
                        tint = MaterialTheme.colorScheme.outline,
                        modifier = Modifier.size(20.dp)
                    )
                }

                Switch(
                    checked = item.enabled,
                    onCheckedChange = { onToggle() },
                    colors = SwitchDefaults.colors(
                        checkedThumbColor = MaterialTheme.colorScheme.primary,
                        checkedTrackColor = MaterialTheme.colorScheme.primaryContainer
                    ),
                    modifier = Modifier.semantics {
                        contentDescription = "Toggle Schedule Slot ${item.slot}"
                    }
                )
            }
        }
    }
}

// -------------------------------------------------------------
// 5. Sleep Timer Card (SPEC 7.4)
// -------------------------------------------------------------
@Composable
private fun SleepTimerCard(
    activeMinutes: Int?,
    onSetSleepTimer: (Int) -> Unit,
    onCancelSleepTimer: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
        shape = RoundedCornerShape(20.dp),
        modifier = modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(PixelGiftSpacing.lg),
            verticalArrangement = Arrangement.spacedBy(PixelGiftSpacing.md)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(PixelGiftSpacing.sm)
            ) {
                Icon(
                    imageVector = Icons.Default.Bedtime,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(24.dp)
                )
                Text(
                    text = "Sleep Timer",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
            }

            Text(
                text = activeMinutes?.let { "Device turns OFF in $it minutes" } ?: "Automatically turn off matrix after bedtime",
                style = MaterialTheme.typography.bodySmall,
                color = if (activeMinutes != null) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(PixelGiftSpacing.xs)
            ) {
                listOf(15, 30, 45, 60, 90).forEach { mins ->
                    FilterChip(
                        selected = activeMinutes == mins,
                        onClick = { onSetSleepTimer(mins) },
                        label = { Text("${mins}m") },
                        modifier = Modifier
                            .weight(1f)
                            .defaultMinSize(minHeight = 48.dp)
                    )
                }
            }

            if (activeMinutes != null) {
                OutlinedButton(
                    onClick = onCancelSleepTimer,
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = MaterialTheme.colorScheme.error),
                    modifier = Modifier
                        .fillMaxWidth()
                        .defaultMinSize(minHeight = 48.dp)
                ) {
                    Icon(Icons.Default.Close, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Turn Off Sleep Timer")
                }
            }
        }
    }
}

// -------------------------------------------------------------
// Schedule Edit Dialog
// -------------------------------------------------------------
@Composable
private fun EditScheduleDialog(
    schedule: ScheduleItem,
    onDismiss: () -> Unit,
    onSave: (ScheduleItem) -> Unit
) {
    var hour by remember { mutableIntStateOf(schedule.hour) }
    var minute by remember { mutableIntStateOf(schedule.minute) }
    var dayMask by remember { mutableIntStateOf(schedule.dayMask) }
    var action by remember { mutableIntStateOf(schedule.action) }
    var arg by remember { mutableIntStateOf(schedule.arg) }
    var enabled by remember { mutableStateOf(schedule.enabled) }

    val dayLetters = listOf("M", "Tu", "W", "Th", "F", "Sa", "Su")

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Edit Schedule Slot ${schedule.slot}") },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(PixelGiftSpacing.md)
            ) {
                // Time selector
                Text("Time: %02d:%02d".format(hour, minute), style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                Row(horizontalArrangement = Arrangement.spacedBy(PixelGiftSpacing.md)) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text("Hour (0..23)", style = MaterialTheme.typography.labelSmall)
                        Slider(
                            value = hour.toFloat(),
                            onValueChange = { hour = it.toInt() },
                            valueRange = 0f..23f,
                            steps = 22
                        )
                    }
                    Column(modifier = Modifier.weight(1f)) {
                        Text("Minute (0..59)", style = MaterialTheme.typography.labelSmall)
                        Slider(
                            value = minute.toFloat(),
                            onValueChange = { minute = it.toInt() },
                            valueRange = 0f..59f,
                            steps = 58
                        )
                    }
                }

                // Days selector
                Text("Repeat Days", style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold)
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    dayLetters.forEachIndexed { index, letter ->
                        val bit = 1 shl index
                        val isSelected = (dayMask and bit) != 0
                        Box(
                            modifier = Modifier
                                .size(36.dp)
                                .clip(CircleShape)
                                .background(if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant)
                                .clickable {
                                    dayMask = if (isSelected) dayMask and bit.inv() else dayMask or bit
                                },
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = letter,
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.Bold,
                                color = if (isSelected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }

                // Action selector
                Text("Action", style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold)
                listOf(
                    ProtocolConstants.SCHEDULE_ACTION_ON to "Turn ON",
                    ProtocolConstants.SCHEDULE_ACTION_OFF to "Turn OFF",
                    ProtocolConstants.SCHEDULE_ACTION_LOAD_SCENE to "Load Scene $arg",
                    ProtocolConstants.SCHEDULE_ACTION_SUNRISE_ALARM to "Sunrise Alarm (${arg}m)"
                ).forEach { (act, label) ->
                    FilterChip(
                        selected = action == act,
                        onClick = { action = act },
                        label = { Text(label) },
                        modifier = Modifier.fillMaxWidth()
                    )
                }

                if (action == ProtocolConstants.SCHEDULE_ACTION_LOAD_SCENE) {
                    Text("Scene slot: $arg (0..7)", style = MaterialTheme.typography.labelSmall)
                    Slider(
                        value = arg.toFloat(),
                        onValueChange = { arg = it.toInt().coerceIn(0, 7) },
                        valueRange = 0f..7f,
                        steps = 6
                    )
                } else if (action == ProtocolConstants.SCHEDULE_ACTION_SUNRISE_ALARM) {
                    Text("Sunrise ramp-up: $arg min (5..45)", style = MaterialTheme.typography.labelSmall)
                    Slider(
                        value = arg.toFloat(),
                        onValueChange = { arg = it.toInt().coerceIn(5, 45) },
                        valueRange = 5f..45f,
                        steps = 7
                    )
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    onSave(
                        schedule.copy(
                            hour = hour,
                            minute = minute,
                            dayMask = dayMask,
                            action = action,
                            arg = arg,
                            enabled = enabled
                        )
                    )
                }
            ) {
                Text("Save")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}
