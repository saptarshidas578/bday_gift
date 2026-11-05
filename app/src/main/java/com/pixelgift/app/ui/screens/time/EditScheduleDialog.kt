package com.pixelgift.app.ui.screens.time

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.pixelgift.app.data.model.ScheduleItem
import com.pixelgift.app.protocol.ProtocolConstants
import com.pixelgift.app.ui.theme.PixelGiftSpacing

@Composable
fun EditScheduleDialog(
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

