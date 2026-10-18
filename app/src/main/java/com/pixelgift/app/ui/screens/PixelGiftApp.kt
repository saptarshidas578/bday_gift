package com.pixelgift.app.ui.screens

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.pixelgift.app.data.repository.AssetEffectCatalogRepository
import com.pixelgift.app.data.repository.DataStoreAppDataRepository
import com.pixelgift.app.ui.MainViewModel
import com.pixelgift.app.ui.components.PixelGiftBottomBar
import com.pixelgift.app.ui.components.PixelGiftTopBar
import com.pixelgift.app.ui.effects.EffectsViewModel
import com.pixelgift.app.ui.magic.MagicViewModel
import com.pixelgift.app.ui.navigation.Screen
import com.pixelgift.app.ui.studio.StudioViewModel
import com.pixelgift.app.ui.time.TimeViewModel

@Composable
fun PixelGiftApp(
    viewModel: MainViewModel = remember { MainViewModel() }
) {
    val context = LocalContext.current.applicationContext
    val navController = rememberNavController()
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = navBackStackEntry?.destination?.route ?: Screen.Studio.route

    val connectionStatus by viewModel.uiConnectionStatus.collectAsState()
    val connectionState by viewModel.connectionState.collectAsState()
    val statusSnapshot by viewModel.statusSnapshot.collectAsState()

    val appDataRepository = remember { DataStoreAppDataRepository(context) }
    val studioViewModel = remember { StudioViewModel(viewModel.transport, appDataRepository) }

    val effectCatalogRepository = remember { AssetEffectCatalogRepository(context) }
    val effectsViewModel = remember {
        EffectsViewModel(
            transport = viewModel.transport,
            catalogRepository = effectCatalogRepository,
            appDataRepository = appDataRepository
        )
    }
    val magicViewModel = remember { MagicViewModel(viewModel.transport) }
    val timeViewModel = remember { TimeViewModel(viewModel.transport) }

    LaunchedEffect(statusSnapshot) {
        timeViewModel.updateFromSnapshot(statusSnapshot)
    }

    LaunchedEffect(connectionState) {
        if (connectionState is com.pixelgift.app.transport.ConnectionState.Connected) {
            timeViewModel.autoSyncOnConnect()
        }
    }

    val studioSelectedColor by studioViewModel.selectedColor.collectAsState()
    val studioCommittedColor by studioViewModel.committedColor.collectAsState()
    val studioBrightness by studioViewModel.brightness.collectAsState()
    val studioFavoriteColors by studioViewModel.favoriteColors.collectAsState()
    val studioRecentColors by studioViewModel.recentColors.collectAsState()
    val studioPalettes by studioViewModel.palettes.collectAsState()
    val studioActiveScene by studioViewModel.activeScene.collectAsState()
    val studioSceneFeedback by studioViewModel.sceneFeedback.collectAsState()
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
                    brightness = studioBrightness,
                    favoriteColors = studioFavoriteColors,
                    recentColors = studioRecentColors,
                    palettes = studioPalettes,
                    activeScene = studioActiveScene,
                    sceneFeedback = studioSceneFeedback,
                    hexInput = studioHexInput,
                    rInput = studioRInput,
                    gInput = studioGInput,
                    bInput = studioBInput,
                    onColorDragged = { studioViewModel.onColorDragged(it) },
                    onBrightnessChanged = { studioViewModel.onBrightnessChanged(it) },
                    onHexChanged = { studioViewModel.onHexChanged(it) },
                    onRgbChanged = { r, g, b -> studioViewModel.onRgbChannelChanged(r, g, b) },
                    onRecentColorSelected = { studioViewModel.onRecentColorSelected(it) },
                    onFavoriteSelected = { studioViewModel.onFavoriteSelected(it) },
                    onToggleFavorite = {
                        if (studioViewModel.isFavorite()) {
                            studioViewModel.onRemoveFavorite(studioSelectedColor)
                        } else {
                            studioViewModel.onAddFavorite(studioSelectedColor)
                        }
                    },
                    isFavorite = studioViewModel.isFavorite(studioSelectedColor),
                    onLoadScene = { studioViewModel.onLoadScene(it) },
                    onSaveScene = { studioViewModel.onSaveScene(it) },
                    onClearSceneFeedback = { studioViewModel.clearSceneFeedback() },
                    onPaletteSelected = { studioViewModel.onPaletteSelected(it) },
                    onCreatePalette = { name, colors -> studioViewModel.onCreatePalette(name, colors) },
                    onRenamePalette = { id, newName -> studioViewModel.onRenamePalette(id, newName) },
                    onDeletePalette = { studioViewModel.onDeletePalette(it) },
                    onReorderPalettes = { studioViewModel.onReorderPalettes(it) },
                    onAddColorToPalette = { studioViewModel.onAddColorToPalette(it) },
                    onExportJson = { studioViewModel.exportJson() },
                    onImportJson = { studioViewModel.importJson(it) },
                    onCancel = { studioViewModel.onCancel() },
                    onOk = { studioViewModel.onCommitOk() }
                )
            }
            composable(Screen.Effects.route) {
                EffectsScreen(
                    viewModel = effectsViewModel,
                    availablePalettes = studioPalettes
                )
            }
            composable(Screen.Magic.route) { MagicScreen(viewModel = magicViewModel) }
            composable(Screen.Time.route) { TimeScreen(viewModel = timeViewModel) }
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
