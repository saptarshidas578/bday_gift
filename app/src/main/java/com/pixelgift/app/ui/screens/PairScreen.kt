package com.pixelgift.app.ui.screens

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.provider.Settings
import androidx.compose.animation.AnimatedVisibility
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
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.BluetoothSearching
import androidx.compose.material.icons.filled.Bluetooth
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.ErrorOutline
import androidx.compose.material.icons.filled.Key
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.SignalCellularAlt
import androidx.compose.material.icons.filled.SignalCellularAlt1Bar
import androidx.compose.material.icons.filled.SignalCellularAlt2Bar
import androidx.compose.material.icons.filled.WarningAmber
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.pixelgift.app.data.model.DiscoveredDevice
import com.pixelgift.app.data.model.PairingUiState
import com.pixelgift.app.data.model.PermissionStatus
import com.pixelgift.app.ui.device.DeviceViewModel
import com.pixelgift.app.ui.theme.PixelGiftSpacing

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PairScreen(
    viewModel: DeviceViewModel,
    onNavigateBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val pairingState by viewModel.pairingState.collectAsState()
    val discoveredDevices by viewModel.discoveredDevices.collectAsState()
    val selectedDeviceForPasskey by viewModel.selectedDeviceForPasskey.collectAsState()
    val permissionStatus by viewModel.permissionStatus.collectAsState()

    // Start scan on screen entry if idle
    LaunchedEffect(Unit) {
        if (pairingState is PairingUiState.Idle) {
            viewModel.startScan()
        }
    }

    var passkeyInput by remember { mutableStateOf("123456") }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Pair New Device") },
                navigationIcon = {
                    IconButton(
                        onClick = onNavigateBack,
                        modifier = Modifier.defaultMinSize(minHeight = 48.dp, minWidth = 48.dp)
                    ) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    if (pairingState !is PairingUiState.Success && pairingState !is PairingUiState.Connecting) {
                        IconButton(
                            onClick = { viewModel.startScan() },
                            modifier = Modifier.defaultMinSize(minHeight = 48.dp, minWidth = 48.dp)
                        ) {
                            Icon(Icons.Default.Refresh, contentDescription = "Refresh Scan")
                        }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background
                )
            )
        },
        modifier = modifier.fillMaxSize()
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .verticalScroll(rememberScrollState())
                .padding(PixelGiftSpacing.lg),
            verticalArrangement = Arrangement.spacedBy(PixelGiftSpacing.lg),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Permission Denied Alert Banner
            if (permissionStatus == PermissionStatus.DENIED || permissionStatus == PermissionStatus.PERMANENTLY_DENIED) {
                Card(
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.errorContainer),
                    shape = RoundedCornerShape(16.dp),
                    modifier = Modifier.fillMaxWidth().semantics { contentDescription = "Permission Denied Warning" }
                ) {
                    Column(
                        modifier = Modifier.fillMaxWidth().padding(PixelGiftSpacing.md),
                        verticalArrangement = Arrangement.spacedBy(PixelGiftSpacing.sm)
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(PixelGiftSpacing.sm)) {
                            Icon(Icons.Default.WarningAmber, contentDescription = null, tint = MaterialTheme.colorScheme.error)
                            Text("Bluetooth Permission Denied", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onErrorContainer)
                        }
                        Text("Android requires Bluetooth scanning permission to discover nearby PixelGift matrices.", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onErrorContainer)
                        Button(
                            onClick = {
                                val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
                                    data = Uri.fromParts("package", context.packageName, null)
                                }
                                context.startActivity(intent)
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error),
                            modifier = Modifier.defaultMinSize(minHeight = 48.dp)
                        ) {
                            Icon(Icons.Default.Settings, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Open App Settings")
                        }
                    }
                }
            }

            // Main State Switcher
            when (val state = pairingState) {
                is PairingUiState.Idle -> {
                    Column(
                        modifier = Modifier.fillMaxWidth().padding(top = 40.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(PixelGiftSpacing.md)
                    ) {
                        Icon(Icons.AutoMirrored.Filled.BluetoothSearching, contentDescription = null, modifier = Modifier.size(64.dp), tint = MaterialTheme.colorScheme.outline)
                        Text("Ready to Search for PixelGift", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                        Text("Make sure your ESP32 matrix is powered on and advertising.", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant, textAlign = TextAlign.Center)
                        Button(onClick = { viewModel.startScan() }, modifier = Modifier.defaultMinSize(minHeight = 48.dp)) {
                            Text("Start Scanning")
                        }
                    }
                }

                is PairingUiState.Scanning -> {
                    Column(
                        modifier = Modifier.fillMaxWidth().padding(top = 40.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(PixelGiftSpacing.md)
                    ) {
                        CircularProgressIndicator(modifier = Modifier.size(56.dp), color = MaterialTheme.colorScheme.primary)
                        Text("Scanning for Nearby PixelGift Devices…", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                        Text("Looking for devices named 'PixelGift-XXXX'", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }

                is PairingUiState.Discovered -> {
                    Column(
                        modifier = Modifier.fillMaxWidth(),
                        verticalArrangement = Arrangement.spacedBy(PixelGiftSpacing.md)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text("Discovered Devices (${state.devices.size})", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                            OutlinedButton(onClick = { viewModel.startScan() }, modifier = Modifier.defaultMinSize(minHeight = 44.dp)) {
                                Text("Rescan")
                            }
                        }

                        state.devices.forEach { device ->
                            DiscoveredDeviceCard(
                                device = device,
                                onConnectClick = { viewModel.onSelectDeviceForPasskey(device) }
                            )
                        }
                    }
                }

                is PairingUiState.Connecting -> {
                    Column(
                        modifier = Modifier.fillMaxWidth().padding(top = 60.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(PixelGiftSpacing.lg)
                    ) {
                        CircularProgressIndicator(modifier = Modifier.size(64.dp))
                        Text("Bonding with ${state.device.name}…", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                        Text("Negotiating AES LE Secure Connection and saving bond keys.", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant, textAlign = TextAlign.Center)
                    }
                }

                is PairingUiState.Success -> {
                    Card(
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                        shape = RoundedCornerShape(24.dp),
                        modifier = Modifier.fillMaxWidth().padding(top = 20.dp).semantics { contentDescription = "Pairing Successful Card" }
                    ) {
                        Column(
                            modifier = Modifier.fillMaxWidth().padding(PixelGiftSpacing.xl),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(PixelGiftSpacing.md)
                        ) {
                            Box(
                                modifier = Modifier.size(72.dp).clip(CircleShape).background(MaterialTheme.colorScheme.primaryContainer),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(Icons.Default.CheckCircle, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(44.dp))
                            }

                            Text("Connected & Bonded!", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
                            Text("${state.device.name} (${state.device.address})", style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.primary, fontFamily = FontFamily.Monospace)
                            Text("Your phone is now securely paired. PixelGift will automatically reconnect on future launches.", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant, textAlign = TextAlign.Center)

                            Spacer(modifier = Modifier.height(PixelGiftSpacing.sm))

                            Button(
                                onClick = onNavigateBack,
                                modifier = Modifier.fillMaxWidth().defaultMinSize(minHeight = 50.dp)
                            ) {
                                Text("Enter PixelGift Studio", style = MaterialTheme.typography.titleMedium)
                            }
                        }
                    }
                }

                is PairingUiState.Failed -> {
                    Card(
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.errorContainer),
                        shape = RoundedCornerShape(20.dp),
                        modifier = Modifier.fillMaxWidth().padding(top = 20.dp).semantics { contentDescription = "Pairing Failed Card" }
                    ) {
                        Column(
                            modifier = Modifier.fillMaxWidth().padding(PixelGiftSpacing.xl),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(PixelGiftSpacing.md)
                        ) {
                            Icon(Icons.Default.ErrorOutline, contentDescription = null, tint = MaterialTheme.colorScheme.error, modifier = Modifier.size(56.dp))
                            Text("Pairing Failed", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onErrorContainer)
                            Text(state.reason, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onErrorContainer, textAlign = TextAlign.Center)

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(PixelGiftSpacing.md)
                            ) {
                                OutlinedButton(
                                    onClick = { viewModel.resetPairingState() },
                                    modifier = Modifier.weight(1f).defaultMinSize(minHeight = 48.dp)
                                ) {
                                    Text("Back to Scan")
                                }
                                Button(
                                    onClick = {
                                        if (state.device != null) {
                                            viewModel.onSelectDeviceForPasskey(state.device)
                                        } else {
                                            viewModel.startScan()
                                        }
                                    },
                                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error),
                                    modifier = Modifier.weight(1f).defaultMinSize(minHeight = 48.dp)
                                ) {
                                    Text("Retry")
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    // 6-digit Passkey Entry Guidance Dialog (SPEC 7.1)
    selectedDeviceForPasskey?.let { device ->
        AlertDialog(
            onDismissRequest = { viewModel.onDismissPasskeyDialog() },
            icon = {
                Icon(Icons.Default.Key, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(32.dp))
            },
            title = {
                Text("Enter Bluetooth Passkey")
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(PixelGiftSpacing.md)) {
                    Text(
                        text = "Enter the 6-digit PIN written on your gift card to securely pair with ${device.name}.",
                        style = MaterialTheme.typography.bodyMedium
                    )

                    OutlinedTextField(
                        value = passkeyInput,
                        onValueChange = { if (it.length <= 6 && it.all { c -> c.isDigit() }) passkeyInput = it },
                        label = { Text("6-Digit Passkey") },
                        placeholder = { Text("123456") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.NumberPassword),
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )

                    Text(
                        text = "Note: Default testing PIN is 123456.",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.outline
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = { viewModel.confirmPairing(device, passkeyInput) },
                    enabled = passkeyInput.length == 6,
                    modifier = Modifier.defaultMinSize(minHeight = 48.dp)
                ) {
                    Text("Pair & Connect")
                }
            },
            dismissButton = {
                TextButton(onClick = { viewModel.onDismissPasskeyDialog() }) {
                    Text("Cancel")
                }
            }
        )
    }
}

@Composable
private fun DiscoveredDeviceCard(
    device: DiscoveredDevice,
    onConnectClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
        shape = RoundedCornerShape(16.dp),
        modifier = modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(PixelGiftSpacing.md),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(PixelGiftSpacing.md)
            ) {
                Box(
                    modifier = Modifier
                        .size(44.dp)
                        .clip(CircleShape)
                        .background(MaterialTheme.colorScheme.primaryContainer),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(Icons.Default.Bluetooth, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                }

                Column {
                    Text(text = device.name, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                    Text(text = device.address, style = MaterialTheme.typography.bodySmall, fontFamily = FontFamily.Monospace, color = MaterialTheme.colorScheme.onSurfaceVariant)

                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        val signalIcon = when (device.signalStrengthBars) {
                            4 -> Icons.Default.SignalCellularAlt
                            3 -> Icons.Default.SignalCellularAlt2Bar
                            else -> Icons.Default.SignalCellularAlt1Bar
                        }
                        Icon(signalIcon, contentDescription = null, modifier = Modifier.size(14.dp), tint = MaterialTheme.colorScheme.primary)
                        Text(text = "${device.rssi} dBm", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.outline)
                    }
                }
            }

            Button(
                onClick = onConnectClick,
                modifier = Modifier.defaultMinSize(minHeight = 48.dp)
            ) {
                Text("Connect")
            }
        }
    }
}
