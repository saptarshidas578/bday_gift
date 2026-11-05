package com.pixelgift.app.ui.screens

import android.Manifest
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.provider.Settings
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
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
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.Bluetooth
import androidx.compose.material.icons.filled.Cake
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.ColorLens
import androidx.compose.material.icons.filled.GraphicEq
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.WarningAmber
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.pixelgift.app.data.model.PermissionStatus
import com.pixelgift.app.ui.device.DeviceViewModel
import com.pixelgift.app.ui.theme.PixelGiftSpacing
import kotlinx.coroutines.launch

data class OnboardingPageData(
    val title: String,
    val subtitle: String,
    val description: String,
    val icon: ImageVector,
    val gradientColors: List<Color>
)

val ONBOARDING_PAGES = listOf(
    OnboardingPageData(
        title = "100 Vibrant RGB LEDs",
        subtitle = "10×10 Handmade Pixel Matrix",
        description = "Experience 500+ hypnotic visual effect variations, custom paint-style color picking, and dynamic brightness control.",
        icon = Icons.Default.ColorLens,
        gradientColors = listOf(Color(0xFF6C5CE7), Color(0xFFFF7675))
    ),
    OnboardingPageData(
        title = "Magic & Celebration",
        subtitle = "Birthday Melodies & Equalizer",
        description = "Start the personalized birthday light & song show, draw freehand pixels directly on the matrix, or turn your room into a music visualizer.",
        icon = Icons.Default.Cake,
        gradientColors = listOf(Color(0xFFFF2D55), Color(0xFFFF9500))
    ),
    OnboardingPageData(
        title = "Smart Clock & Offline Timer",
        subtitle = "Persistent Local ESP32 Engine",
        description = "Keep accurate digital time with stacked 3×4 or scrolling styles, schedule sunrise alarms, and set kitchen timers that run offline.",
        icon = Icons.Default.Schedule,
        gradientColors = listOf(Color(0xFF00CEC9), Color(0xFF0984E3))
    )
)

@Composable
fun OnboardingScreen(
    viewModel: DeviceViewModel,
    onFinishOnboarding: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    val pagerState = rememberPagerState(pageCount = { ONBOARDING_PAGES.size })
    val permissionStatus by viewModel.permissionStatus.collectAsState()

    var showPermissionRationaleDialog by remember { mutableStateOf(false) }

    // Multi-permission launcher
    val permissionsToRequest = remember {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            arrayOf(
                Manifest.permission.BLUETOOTH_SCAN,
                Manifest.permission.BLUETOOTH_CONNECT
            )
        } else {
            arrayOf(
                Manifest.permission.ACCESS_FINE_LOCATION,
                Manifest.permission.ACCESS_COARSE_LOCATION
            )
        }
    }

    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestMultiplePermissions()
    ) { results ->
        val allGranted = results.values.all { it }
        viewModel.onPermissionResult(
            isGranted = allGranted,
            shouldShowRationale = !allGranted
        )
        if (allGranted) {
            onFinishOnboarding()
        }
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .padding(PixelGiftSpacing.lg),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.SpaceBetween
    ) {
        // Top skip bar
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = PixelGiftSpacing.md),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "Welcome to PixelGift",
                style = MaterialTheme.typography.titleSmall,
                color = MaterialTheme.colorScheme.primary,
                fontWeight = FontWeight.Bold
            )

            TextButton(
                onClick = onFinishOnboarding,
                modifier = Modifier.defaultMinSize(minHeight = 48.dp)
            ) {
                Text("Skip")
            }
        }

        // Pager Pages
        HorizontalPager(
            state = pagerState,
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f)
        ) { pageIndex ->
            val page = ONBOARDING_PAGES[pageIndex]
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(PixelGiftSpacing.lg),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                Box(
                    modifier = Modifier
                        .size(120.dp)
                        .clip(CircleShape)
                        .background(Brush.linearGradient(page.gradientColors)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = page.icon,
                        contentDescription = null,
                        tint = Color.White,
                        modifier = Modifier.size(56.dp)
                    )
                }

                Spacer(modifier = Modifier.height(PixelGiftSpacing.xl))

                Text(
                    text = page.title,
                    style = MaterialTheme.typography.headlineMedium,
                    fontWeight = FontWeight.Bold,
                    textAlign = TextAlign.Center,
                    color = MaterialTheme.colorScheme.onBackground
                )

                Spacer(modifier = Modifier.height(PixelGiftSpacing.xs))

                Text(
                    text = page.subtitle,
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.primary,
                    fontWeight = FontWeight.SemiBold,
                    textAlign = TextAlign.Center
                )

                Spacer(modifier = Modifier.height(PixelGiftSpacing.md))

                Text(
                    text = page.description,
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.padding(horizontal = PixelGiftSpacing.md)
                )
            }
        }

        // Bottom Controls: Page Dots & Action Button
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = PixelGiftSpacing.lg),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(PixelGiftSpacing.lg)
        ) {
            // Dots Indicator
            Row(
                horizontalArrangement = Arrangement.spacedBy(PixelGiftSpacing.sm),
                verticalAlignment = Alignment.CenterVertically
            ) {
                repeat(ONBOARDING_PAGES.size) { index ->
                    val isSelected = pagerState.currentPage == index
                    Box(
                        modifier = Modifier
                            .height(8.dp)
                            .width(if (isSelected) 24.dp else 8.dp)
                            .clip(RoundedCornerShape(4.dp))
                            .background(
                                if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outlineVariant
                            )
                    )
                }
            }

            // Action Button
            val isLastPage = pagerState.currentPage == ONBOARDING_PAGES.size - 1

            if (!isLastPage) {
                Button(
                    onClick = {
                        coroutineScope.launch {
                            pagerState.animateScrollToPage(pagerState.currentPage + 1)
                        }
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .defaultMinSize(minHeight = 52.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
                ) {
                    Text("Next", style = MaterialTheme.typography.titleMedium)
                    Spacer(modifier = Modifier.width(8.dp))
                    Icon(Icons.AutoMirrored.Filled.ArrowForward, contentDescription = null)
                }
            } else {
                Button(
                    onClick = {
                        if (permissionStatus == PermissionStatus.GRANTED) {
                            onFinishOnboarding()
                        } else {
                            showPermissionRationaleDialog = true
                        }
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .defaultMinSize(minHeight = 52.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
                ) {
                    Icon(Icons.Default.Bluetooth, contentDescription = null)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Connect PixelGift", style = MaterialTheme.typography.titleMedium)
                }
            }
        }
    }

    // Permission Explanation Dialog
    if (showPermissionRationaleDialog) {
        AlertDialog(
            onDismissRequest = { showPermissionRationaleDialog = false },
            icon = {
                Icon(
                    imageVector = Icons.Default.Security,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(32.dp)
                )
            },
            title = {
                Text("Bluetooth Permission Required")
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(PixelGiftSpacing.sm)) {
                    Text(
                        "PixelGift connects over Bluetooth Low Energy (BLE) to control colors, effects, and text locally on your 10×10 matrix.",
                        style = MaterialTheme.typography.bodyMedium
                    )
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                        Text(
                            "Android requires 'Nearby devices' permission to scan and communicate with Bluetooth peripherals.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    } else {
                        Text(
                            "On Android 11 and older, Location permission is required by Android to discover nearby BLE beacons and devices.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        showPermissionRationaleDialog = false
                        permissionLauncher.launch(permissionsToRequest)
                    }
                ) {
                    Text("Grant Permission")
                }
            },
            dismissButton = {
                TextButton(onClick = { showPermissionRationaleDialog = false }) {
                    Text("Not Now")
                }
            }
        )
    }

    // Permission Denied Card/Dialog
    if (permissionStatus == PermissionStatus.PERMANENTLY_DENIED) {
        AlertDialog(
            onDismissRequest = {},
            icon = {
                Icon(Icons.Default.WarningAmber, contentDescription = null, tint = MaterialTheme.colorScheme.error)
            },
            title = { Text("Permission Denied") },
            text = {
                Text("Bluetooth permission was permanently denied. Please enable 'Nearby devices' in Android Settings to pair with your PixelGift.")
            },
            confirmButton = {
                Button(
                    onClick = {
                        val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
                            data = Uri.fromParts("package", context.packageName, null)
                        }
                        context.startActivity(intent)
                    }
                ) {
                    Text("Open Settings")
                }
            },
            dismissButton = {
                TextButton(onClick = onFinishOnboarding) {
                    Text("Skip")
                }
            }
        )
    }
}
