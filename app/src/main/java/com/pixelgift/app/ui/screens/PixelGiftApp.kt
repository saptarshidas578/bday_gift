package com.pixelgift.app.ui.screens

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.pixelgift.app.ui.MainViewModel
import com.pixelgift.app.ui.components.PixelGiftBottomBar
import com.pixelgift.app.ui.components.PixelGiftTopBar
import com.pixelgift.app.ui.navigation.Screen
import com.pixelgift.app.ui.studio.StudioViewModel

@Composable
fun PixelGiftApp(
    viewModel: MainViewModel = remember { MainViewModel() }
) {
    val navController = rememberNavController()
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = navBackStackEntry?.destination?.route ?: Screen.Studio.route

    val connectionStatus by viewModel.uiConnectionStatus.collectAsState()
    val connectionState by viewModel.connectionState.collectAsState()
    val statusSnapshot by viewModel.statusSnapshot.collectAsState()

    val studioViewModel = remember { StudioViewModel(viewModel.transport) }
    val studioSelectedColor by studioViewModel.selectedColor.collectAsState()
    val studioCommittedColor by studioViewModel.committedColor.collectAsState()
    val studioRecentColors by studioViewModel.recentColors.collectAsState()
    val studioHexInput by studioViewModel.hexInput.collectAsState()
    val studioRInput by studioViewModel.rInput.collectAsState()
    val studioGInput by studioViewModel.gInput.collectAsState()
    val studioBInput by studioViewModel.bInput.collectAsState()

    val currentScreen = Screen.bottomNavItems.find { it.route == currentRoute } ?: Screen.Studio
    val currentTitle = stringResource(currentScreen.titleRes)

    Scaffold(
        topBar = {
            PixelGiftTopBar(
                title = currentTitle,
                connectionStatus = connectionStatus,
                onRetryConnection = {
                    viewModel.onConnectionChipClicked()
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
            composable(Screen.Studio.route) {
                StudioScreen(
                    selectedColor = studioSelectedColor,
                    committedColor = studioCommittedColor,
                    recentColors = studioRecentColors,
                    hexInput = studioHexInput,
                    rInput = studioRInput,
                    gInput = studioGInput,
                    bInput = studioBInput,
                    onColorDragged = { studioViewModel.onColorDragged(it) },
                    onHexChanged = { studioViewModel.onHexChanged(it) },
                    onRgbChanged = { r, g, b -> studioViewModel.onRgbChannelChanged(r, g, b) },
                    onRecentColorSelected = { studioViewModel.onRecentColorSelected(it) },
                    onCancel = { studioViewModel.onCancel() },
                    onOk = { studioViewModel.onCommitOk() }
                )
            }
            composable(Screen.Effects.route) { EffectsScreen() }
            composable(Screen.Magic.route) { MagicScreen() }
            composable(Screen.Time.route) { TimeScreen() }
            composable(Screen.Device.route) {
                DeviceScreen(
                    connectionState = connectionState,
                    statusSnapshot = statusSnapshot,
                    onConnectToggle = { viewModel.onConnectionChipClicked() },
                    onSendColor = { r, g, b -> viewModel.sendTestColor(r, g, b) },
                    onToggleRandomDisconnect = { enabled -> viewModel.toggleRandomDisconnects(enabled) }
                )
            }
        }
    }
}
