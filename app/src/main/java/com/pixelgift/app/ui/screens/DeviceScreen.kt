package com.pixelgift.app.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
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
import androidx.compose.material.icons.filled.AutoMode
import androidx.compose.material.icons.filled.Bluetooth
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.CloudDownload
import androidx.compose.material.icons.filled.DeleteForever
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.automirrored.filled.VolumeMute
import androidx.compose.material.icons.automirrored.filled.VolumeUp
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Save
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material.icons.filled.Upload
import androidx.compose.material.icons.outlined.Devices
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.pixelgift.app.R
import com.pixelgift.app.protocol.DeviceNotification
import com.pixelgift.app.protocol.ProtocolConstants
import com.pixelgift.app.transport.ConnectionState
import com.pixelgift.app.ui.device.DeviceViewModel
import com.pixelgift.app.ui.theme.PixelGiftSpacing
import kotlinx.coroutines.delay

@Composable
fun DeviceScreen(
    viewModel: DeviceViewModel,
    onNavigateToPair: () -> Unit,
    onNavigateToOnboarding: () -> Unit,
    modifier: Modifier = Modifier
) {
    val connectionState by viewModel.connectionState.collectAsState()
    val statusSnapshot by viewModel.statusSnapshot.collectAsState()
    val autoReconnect by viewModel.autoReconnect.collectAsState()
    val feedback by viewModel.feedback.collectAsState()

    val isConnected = connectionState is ConnectionState.Connected
    val isConnecting = connectionState is ConnectionState.Connecting

    var showClearBondsDialog by remember { mutableStateOf(false) }
    var showOtaDialog by remember { mutableStateOf(false) }
    var showAboutDialog by remember { mutableStateOf(false) }

    // Clear feedback toast after 4s
    LaunchedEffect(feedback) {
        if (feedback != null) {
            delay(4000)
            viewModel.clearFeedback()
        }
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(PixelGiftSpacing.lg),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(PixelGiftSpacing.lg)
    ) {
        // Feedback toast banner
        AnimatedVisibility(
            visible = feedback != null,
            enter = fadeIn(),
            exit = fadeOut()
        ) {
            feedback?.let { msg ->
                Card(
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer),
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

        // --- 1. Connection Overview & Hardware Card ---
        Card(
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
            shape = RoundedCornerShape(20.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(PixelGiftSpacing.lg),
                verticalArrangement = Arrangement.spacedBy(PixelGiftSpacing.md)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(PixelGiftSpacing.md)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(48.dp)
                                .clip(CircleShape)
                                .background(if (isConnected) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.outlineVariant),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.Bluetooth,
                                contentDescription = null,
                                tint = if (isConnected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline,
                                modifier = Modifier.size(24.dp)
                            )
                        }

                        Column {
                            Text(
                                text = if (isConnected) (connectionState as ConnectionState.Connected).deviceName else "PixelGift Matrix",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = if (isConnected) (connectionState as ConnectionState.Connected).address else "Disconnected",
                                style = MaterialTheme.typography.bodySmall,
                                fontFamily = FontFamily.Monospace,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }

                    // Status Pill
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(12.dp))
                            .background(
                                when {
                                    isConnected -> MaterialTheme.colorScheme.primaryContainer
                                    isConnecting -> MaterialTheme.colorScheme.tertiaryContainer
                                    else -> MaterialTheme.colorScheme.errorContainer
                                }
                            )
                            .padding(horizontal = 10.dp, vertical = 6.dp)
                    ) {
                        Text(
                            text = when {
                                isConnected -> "Online"
                                isConnecting -> "Connecting…"
                                else -> "Offline"
                            },
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Bold,
                            color = when {
                                isConnected -> MaterialTheme.colorScheme.onPrimaryContainer
                                isConnecting -> MaterialTheme.colorScheme.onTertiaryContainer
                                else -> MaterialTheme.colorScheme.onErrorContainer
                            }
                        )
                    }
                }

                HorizontalDivider()

                // Hardware telemetry
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Column {
                        Text("Firmware", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.outline)
                        Text("v${statusSnapshot.fwMajor}.${statusSnapshot.fwMinor}", style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Bold)
                    }
                    Column {
                        Text("Power Safety Cap", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.outline)
                        Text(
                            if (statusSnapshot.isPowerCapped) "Active (1.5A)" else "Normal",
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.Bold,
                            color = if (statusSnapshot.isPowerCapped) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface
                        )
                    }
                    Column {
                        Text("Piezo Buzzer", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.outline)
                        Text(
                            "Not connected",
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.outline
                        )
                    }
                }

                // Connect / Disconnect Buttons
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(PixelGiftSpacing.md)
                ) {
                    Button(
                        onClick = { viewModel.onToggleConnect() },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = if (isConnected) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary
                        ),
                        modifier = Modifier
                            .weight(1f)
                            .defaultMinSize(minHeight = 48.dp)
                    ) {
                        Text(if (isConnected) "Disconnect" else "Connect Sim")
                    }

                    OutlinedButton(
                        onClick = onNavigateToPair,
                        modifier = Modifier
                            .weight(1f)
                            .defaultMinSize(minHeight = 48.dp)
                    ) {
                        Text("Pair Device…")
                    }
                }
            }
        }

        // --- 2. Auto-Reconnect & Audio Mute Toggles ---
        Card(
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
            shape = RoundedCornerShape(20.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(PixelGiftSpacing.lg),
                verticalArrangement = Arrangement.spacedBy(PixelGiftSpacing.md)
            ) {
                // Auto-reconnect
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(PixelGiftSpacing.sm)
                    ) {
                        Icon(Icons.Default.AutoMode, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                        Column {
                            Text("Auto-Reconnect", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
                            Text("Reconnect to bonded PixelGift on launch", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    }

                    Switch(
                        checked = autoReconnect,
                        onCheckedChange = { viewModel.onToggleAutoReconnect(it) },
                        modifier = Modifier.semantics { contentDescription = "Toggle Auto-Reconnect" }
                    )
                }

                HorizontalDivider()

                // Piezo Buzzer Mute (Disabled - Speaker not connected)
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
                            imageVector = Icons.AutoMirrored.Filled.VolumeMute,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.outline
                        )
                        Column {
                            Text("Mute Buzzer", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.outline)
                            Text("Speaker not connected (audio deferred to B07b)", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.outline)
                        }
                    }

                    Switch(
                        checked = false,
                        onCheckedChange = {},
                        enabled = false,
                        modifier = Modifier.semantics { contentDescription = "Speaker not connected" }
                    )
                }
            }
        }

        // --- 3. Scenes Manager (8 Slots on Device) ---
        Card(
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
            shape = RoundedCornerShape(20.dp),
            modifier = Modifier.fillMaxWidth()
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
                    Icon(Icons.Default.Save, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                    Text("Device Scenes Manager (8 Slots)", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                }

                Text(
                    "Scenes persist in ESP32 flash and survive reboots. Save your favorite color or effect snapshot into any slot.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                // 8 Scene slots grid (2 columns)
                for (row in 0 until 4) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(PixelGiftSpacing.sm)
                    ) {
                        val slot1 = row * 2
                        val slot2 = slot1 + 1
                        SceneSlotItem(
                            slot = slot1,
                            onSave = { viewModel.onSaveScene(slot1) },
                            onLoad = { viewModel.onLoadScene(slot1) },
                            modifier = Modifier.weight(1f)
                        )
                        SceneSlotItem(
                            slot = slot2,
                            onSave = { viewModel.onSaveScene(slot2) },
                            onLoad = { viewModel.onLoadScene(slot2) },
                            modifier = Modifier.weight(1f)
                        )
                    }
                }
            }
        }

        // --- 4. Danger & Maintenance Actions (Bonds, OTA, About) ---
        Card(
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
            shape = RoundedCornerShape(20.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(PixelGiftSpacing.lg),
                verticalArrangement = Arrangement.spacedBy(PixelGiftSpacing.md)
            ) {
                Text("Device Maintenance & System", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)

                // OTA Update
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(PixelGiftSpacing.sm)
                    ) {
                        Icon(Icons.Default.CloudDownload, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                        Column {
                            Text("Firmware OTA Update", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
                            Text("Current: v${statusSnapshot.fwMajor}.${statusSnapshot.fwMinor}", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    }

                    OutlinedButton(
                        onClick = { showOtaDialog = true },
                        modifier = Modifier.defaultMinSize(minHeight = 44.dp)
                    ) {
                        Text("Check")
                    }
                }

                HorizontalDivider()

                // Clear Bonds
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(PixelGiftSpacing.sm)
                    ) {
                        Icon(Icons.Default.DeleteForever, contentDescription = null, tint = MaterialTheme.colorScheme.error)
                        Column {
                            Text("Forget Pairings", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
                            Text("Clear all bonded phones on device", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    }

                    OutlinedButton(
                        onClick = { showClearBondsDialog = true },
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = MaterialTheme.colorScheme.error),
                        modifier = Modifier.defaultMinSize(minHeight = 44.dp)
                    ) {
                        Text("Clear")
                    }
                }

                HorizontalDivider()

                // About & Onboarding
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(PixelGiftSpacing.md)
                ) {
                    OutlinedButton(
                        onClick = onNavigateToOnboarding,
                        modifier = Modifier.weight(1f).defaultMinSize(minHeight = 48.dp)
                    ) {
                        Text("View Walkthrough")
                    }

                    Button(
                        onClick = { showAboutDialog = true },
                        modifier = Modifier.weight(1f).defaultMinSize(minHeight = 48.dp)
                    ) {
                        Icon(Icons.Default.Info, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("About")
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(PixelGiftSpacing.xl))
    }

    // Clear Bonds Confirmation Dialog
    if (showClearBondsDialog) {
        AlertDialog(
            onDismissRequest = { showClearBondsDialog = false },
            icon = { Icon(Icons.Default.DeleteForever, contentDescription = null, tint = MaterialTheme.colorScheme.error, modifier = Modifier.size(32.dp)) },
            title = { Text("Clear All Bonds?") },
            text = { Text("This will erase all Bluetooth encryption keys stored in the ESP32 NVS. All paired phones will need to re-pair using the 6-digit passkey.") },
            confirmButton = {
                Button(
                    onClick = {
                        viewModel.onClearBonds()
                        showClearBondsDialog = false
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                ) {
                    Text("Clear Bonds")
                }
            },
            dismissButton = {
                TextButton(onClick = { showClearBondsDialog = false }) { Text("Cancel") }
            }
        )
    }

    // OTA Confirmation Dialog
    if (showOtaDialog) {
        AlertDialog(
            onDismissRequest = { showOtaDialog = false },
            icon = { Icon(Icons.Default.CloudDownload, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(32.dp)) },
            title = { Text("Firmware Update Mode") },
            text = { Text("Prepare ESP32 for Over-The-Air firmware flashing. The device will switch to OTA receive mode until completion.") },
            confirmButton = {
                Button(
                    onClick = {
                        viewModel.onBeginOta()
                        showOtaDialog = false
                    }
                ) {
                    Text("Begin OTA")
                }
            },
            dismissButton = {
                TextButton(onClick = { showOtaDialog = false }) { Text("Cancel") }
            }
        )
    }

    // About Dialog
    if (showAboutDialog) {
        AlertDialog(
            onDismissRequest = { showAboutDialog = false },
            icon = {
                Icon(
                    Icons.Default.Info,
                    contentDescription = "About PixelGift",
                    tint = Color(0xFFD2042D),
                    modifier = Modifier.size(36.dp)
                )
            },
            title = {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        text = "PixelGift • 19th Birthday Edition",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        textAlign = TextAlign.Center
                    )
                    Text(
                        text = "Dedicated to Madhumitha • January 24",
                        style = MaterialTheme.typography.labelMedium,
                        color = Color(0xFFD2042D),
                        textAlign = TextAlign.Center
                    )
                }
            },
            text = {
                Column(
                    verticalArrangement = Arrangement.spacedBy(PixelGiftSpacing.sm),
                    modifier = Modifier.verticalScroll(rememberScrollState())
                ) {
                    Card(
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Column(modifier = Modifier.padding(PixelGiftSpacing.md)) {
                            Text(
                                text = "Personal Note",
                                style = MaterialTheme.typography.labelLarge,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.primary
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = stringResource(R.string.about_personal_note_body),
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }

                    Card(
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Column(modifier = Modifier.padding(PixelGiftSpacing.md)) {
                            Text(
                                text = "Specifications",
                                style = MaterialTheme.typography.labelLarge,
                                fontWeight = FontWeight.Bold
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = "• Hardware: ESP32 DevKit V1 • GPIO 5 (Data)\n• Display: 10×10 (100 LEDs) WS2812B GRB Serpentine\n• Palette: Cherry Red & Warm Gold Theme\n• BLE: LE Secure Connections (Nordic Android BLE)\n• Firmware: v1.0.0 Dual-Partition OTA Rollback",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = { showAboutDialog = false },
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFD2042D)),
                    modifier = Modifier.defaultMinSize(minHeight = 48.dp)
                ) {
                    Text("Close", color = Color.White)
                }
            }
        )
    }
}

@Composable
private fun SceneSlotItem(
    slot: Int,
    onSave: () -> Unit,
    onLoad: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        shape = RoundedCornerShape(12.dp),
        modifier = modifier.defaultMinSize(minHeight = 48.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = PixelGiftSpacing.sm, vertical = PixelGiftSpacing.xs),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(
                text = "Scene $slot",
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(start = 4.dp)
            )

            Row {
                IconButton(
                    onClick = onSave,
                    modifier = Modifier.size(36.dp)
                ) {
                    Icon(Icons.Default.Upload, contentDescription = "Save to Scene $slot", modifier = Modifier.size(18.dp), tint = MaterialTheme.colorScheme.primary)
                }

                IconButton(
                    onClick = onLoad,
                    modifier = Modifier.size(36.dp)
                ) {
                    Icon(Icons.Default.Download, contentDescription = "Load Scene $slot", modifier = Modifier.size(18.dp), tint = MaterialTheme.colorScheme.secondary)
                }
            }
        }
    }
}
