package com.pixelgift.app.ui.screens

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.pixelgift.app.ui.components.DeviceConnectionStatus
import com.pixelgift.app.ui.components.PixelGiftBottomBar
import com.pixelgift.app.ui.components.PixelGiftTopBar
import com.pixelgift.app.ui.navigation.Screen

@Composable
fun PixelGiftApp() {
    val navController = rememberNavController()
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = navBackStackEntry?.destination?.route ?: Screen.Studio.route

    // Connection state placeholder (defaults to OFFLINE until BLE transport in track A02/C01)
    var connectionStatus by remember { mutableStateOf(DeviceConnectionStatus.OFFLINE) }

    val currentScreen = Screen.bottomNavItems.find { it.route == currentRoute } ?: Screen.Studio
    val currentTitle = stringResource(currentScreen.titleRes)

    Scaffold(
        topBar = {
            PixelGiftTopBar(
                title = currentTitle,
                connectionStatus = connectionStatus,
                onRetryConnection = {
                    // Placeholder retry toggle for verifying states
                    connectionStatus = when (connectionStatus) {
                        DeviceConnectionStatus.OFFLINE -> DeviceConnectionStatus.CONNECTING
                        DeviceConnectionStatus.CONNECTING -> DeviceConnectionStatus.CONNECTED
                        DeviceConnectionStatus.CONNECTED -> DeviceConnectionStatus.RECONNECTING
                        DeviceConnectionStatus.RECONNECTING -> DeviceConnectionStatus.OFFLINE
                    }
                }
            )
        },
        bottomBar = {
            PixelGiftBottomBar(
                currentRoute = currentRoute,
                onNavigate = { targetScreen ->
                    navController.navigate(targetScreen.route) {
                        popUpTo(navController.graph.findStartDestination().id) {
                            saveState = true
                        }
                        launchSingleTop = true
                        restoreState = true
                    }
                }
            )
        },
        modifier = Modifier.fillMaxSize()
    ) { innerPadding ->
        NavHost(
            navController = navController,
            startDestination = Screen.Studio.route,
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            composable(Screen.Studio.route) { StudioScreen() }
            composable(Screen.Effects.route) { EffectsScreen() }
            composable(Screen.Magic.route) { MagicScreen() }
            composable(Screen.Time.route) { TimeScreen() }
            composable(Screen.Device.route) { DeviceScreen() }
        }
    }
}
