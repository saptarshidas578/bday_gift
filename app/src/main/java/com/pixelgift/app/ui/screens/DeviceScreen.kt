package com.pixelgift.app.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Devices
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.pixelgift.app.R
import com.pixelgift.app.protocol.DeviceNotification
import com.pixelgift.app.protocol.ProtocolConstants
import com.pixelgift.app.transport.ConnectionState
import com.pixelgift.app.ui.theme.PixelGiftSpacing

@Composable
fun DeviceScreen(
    connectionState: ConnectionState,
    statusSnapshot: DeviceNotification.StatusSnapshot,
    onConnectToggle: () -> Unit,
    onSendColor: (Int, Int, Int) -> Unit,
    onToggleRandomDisconnect: (Boolean) -> Unit,
    modifier: Modifier = Modifier
) {
    val scrollState = rememberScrollState()
    var randomDisconnectEnabled by remember { mutableStateOf(false) }

    val isConnected = connectionState is ConnectionState.Connected
    val isConnecting = connectionState is ConnectionState.Connecting

    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(scrollState)
            .padding(PixelGiftSpacing.lg),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(PixelGiftSpacing.lg)
    ) {
        // Overview Card
        Card(
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surfaceVariant
            ),
            shape = MaterialTheme.shapes.large,
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(PixelGiftSpacing.lg),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(PixelGiftSpacing.sm)
            ) {
                Box(
                    modifier = Modifier
                        .size(56.dp)
                        .clip(CircleShape)
                        .background(MaterialTheme.colorScheme.secondaryContainer),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Outlined.Devices,
                        contentDescription = stringResource(R.string.cd_nav_device),
                        tint = MaterialTheme.colorScheme.secondary,
                        modifier = Modifier.size(28.dp)
                    )
                }

                Text(
                    text = stringResource(R.string.device_title),
                    style = MaterialTheme.typography.titleLarge,
                    color = MaterialTheme.colorScheme.onSurface
                )

                Text(
                    text = stringResource(R.string.device_subtitle),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center
                )
            }
        }

        // Live Simulator Interactive Testing Card
        Card(
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surfaceContainer
            ),
            shape = MaterialTheme.shapes.large,
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(PixelGiftSpacing.lg),
                verticalArrangement = Arrangement.spacedBy(PixelGiftSpacing.md)
            ) {
                Text(
                    text = stringResource(R.string.test_device_status_header),
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.primary
                )

                // Connection button
                Button(
                    onClick = onConnectToggle,
                    enabled = !isConnecting,
                    modifier = Modifier.fillMaxWidth(),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = if (isConnected) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary
                    )
                ) {
                    Text(
                        text = when {
                            isConnecting -> stringResource(R.string.status_connecting)
                            isConnected -> stringResource(R.string.test_device_disconnect)
                            else -> stringResource(R.string.test_device_connect)
                        }
                    )
                }

                // Random disconnect simulation switch
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = stringResource(R.string.test_random_disconnect_label),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Switch(
                        checked = randomDisconnectEnabled,
                        onCheckedChange = { checked ->
                            randomDisconnectEnabled = checked
                            onToggleRandomDisconnect(checked)
                        }
                    )
                }

                // Live status telemetry
                if (isConnected) {
                    val modeName = when (statusSnapshot.mode) {
                        ProtocolConstants.MODE_SOLID -> "SOLID"
                        ProtocolConstants.MODE_EFFECT -> "EFFECT"
                        ProtocolConstants.MODE_CLOCK -> "CLOCK"
                        ProtocolConstants.MODE_TIMER -> "TIMER"
                        ProtocolConstants.MODE_BIRTHDAY -> "BIRTHDAY"
                        ProtocolConstants.MODE_MESSAGE -> "MESSAGE"
                        ProtocolConstants.MODE_DRAW -> "DRAW"
                        ProtocolConstants.MODE_MUSIC -> "MUSIC"
                        ProtocolConstants.MODE_OFF -> "OFF"
                        else -> "UNKNOWN (${statusSnapshot.mode})"
                    }

                    val liveColor = Color(
                        statusSnapshot.r,
                        statusSnapshot.g,
                        statusSnapshot.b
                    )

                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(PixelGiftSpacing.md)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(36.dp)
                                .clip(RoundedCornerShape(8.dp))
                                .background(liveColor)
                        )
                        Column {
                            Text(
                                text = stringResource(
                                    R.string.test_color_label,
                                    statusSnapshot.r,
                                    statusSnapshot.g,
                                    statusSnapshot.b
                                ),
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Text(
                                text = stringResource(R.string.test_mode_label, modeName),
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }

                    Text(
                        text = stringResource(R.string.test_brightness_label, statusSnapshot.brightness),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    Text(
                        text = stringResource(
                            R.string.test_flags_label,
                            statusSnapshot.flags,
                            statusSnapshot.isTimeSynced.toString(),
                            statusSnapshot.isShowRunning.toString()
                        ),
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.outline
                    )

                    // Test command trigger buttons
                    Text(
                        text = "Send Test Protocol Commands:",
                        style = MaterialTheme.typography.labelLarge,
                        color = MaterialTheme.colorScheme.onSurface
                    )

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(PixelGiftSpacing.sm)
                    ) {
                        OutlinedButton(
                            onClick = { onSendColor(255, 122, 0) },
                            modifier = Modifier.weight(1f)
                        ) {
                            Text("Amber", style = MaterialTheme.typography.labelMedium)
                        }

                        OutlinedButton(
                            onClick = { onSendColor(0, 229, 255) },
                            modifier = Modifier.weight(1f)
                        ) {
                            Text("Cyan", style = MaterialTheme.typography.labelMedium)
                        }

                        OutlinedButton(
                            onClick = { onSendColor(158, 119, 237) },
                            modifier = Modifier.weight(1f)
                        ) {
                            Text("Violet", style = MaterialTheme.typography.labelMedium)
                        }
                    }
                }
            }
        }
    }
}
