package com.pixelgift.app.ui.screens

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.pixelgift.app.data.repository.AssetEffectCatalogRepository
import com.pixelgift.app.data.repository.DataStoreAppDataRepository
import com.pixelgift.app.ui.DeviceViewModelFactory
import com.pixelgift.app.ui.EffectsViewModelFactory
import com.pixelgift.app.ui.MagicViewModelFactory
import com.pixelgift.app.ui.MainViewModel
import com.pixelgift.app.ui.StudioViewModelFactory
import com.pixelgift.app.ui.TimeViewModelFactory
import com.pixelgift.app.ui.components.PixelGiftBottomBar
import com.pixelgift.app.ui.components.PixelGiftTopBar
import com.pixelgift.app.ui.device.DeviceViewModel
import com.pixelgift.app.ui.effects.EffectsViewModel
import com.pixelgift.app.ui.magic.MagicViewModel
import com.pixelgift.app.ui.navigation.Screen
import com.pixelgift.app.ui.studio.StudioViewModel
import com.pixelgift.app.ui.time.TimeViewModel

@Composable
fun PixelGiftApp(
    viewModel: MainViewModel? = null
) {
    val context = LocalContext.current.applicationContext
    val transport = remember {
        try {
            val manager = context.getSystemService(android.bluetooth.BluetoothManager::class.java)
            val adapter = manager?.adapter
            if (adapter != null) {
                com.pixelgift.app.transport.BleTransport(context)
            } else {
                com.pixelgift.app.transport.FakeDevice()
            }
        } catch (e: Exception) {
            com.pixelgift.app.transport.FakeDevice()
        }
    }
    val activeViewModel = viewModel ?: remember { MainViewModel(transport) }

    val navController = rememberNavController()
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = navBackStackEntry?.destination?.route ?: Screen.Studio.route

    val connectionStatus by activeViewModel.uiConnectionStatus.collectAsState()
    val connectionState by activeViewModel.connectionState.collectAsState()
    val statusSnapshot by activeViewModel.statusSnapshot.collectAsState()

    val appDataRepository = remember { DataStoreAppDataRepository(context) }
    val studioViewModel: StudioViewModel = viewModel(
        factory = StudioViewModelFactory(activeViewModel.transport, appDataRepository)
    )

    val effectCatalogRepository = remember { AssetEffectCatalogRepository(context) }
    val effectsViewModel: EffectsViewModel = viewModel(
        factory = EffectsViewModelFactory(
            transport = activeViewModel.transport,
            catalogRepository = effectCatalogRepository,
            appDataRepository = appDataRepository
        )
    )
    val magicViewModel: MagicViewModel = viewModel(
        factory = MagicViewModelFactory(activeViewModel.transport)
    )
    val timeViewModel: TimeViewModel = viewModel(
        factory = TimeViewModelFactory(activeViewModel.transport)
    )
    val deviceViewModel: DeviceViewModel = viewModel(
        factory = DeviceViewModelFactory(activeViewModel.transport)
    )

    LaunchedEffect(statusSnapshot) {
        timeViewModel.updateFromSnapshot(statusSnapshot)
        studioViewModel.updateFromSnapshot(statusSnapshot)
        effectsViewModel.updateFromSnapshot(statusSnapshot)
    }

    LaunchedEffect(connectionState) {
        if (connectionState is com.pixelgift.app.transport.ConnectionState.Connected) {
            timeViewModel.autoSyncOnConnect()
        }
    }

    val studioPalettes by studioViewModel.palettes.collectAsState()

    val currentScreen = Screen.bottomNavItems.find { it.route == currentRoute } ?: Screen.Studio
    val currentTitle = stringResource(currentScreen.titleRes)

    Scaffold(
        topBar = {
            PixelGiftTopBar(
                title = currentTitle,
                connectionStatus = connectionStatus,
                onRetryConnection = {
                    activeViewModel.onConnectionChipClicked()
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
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding),
            contentAlignment = Alignment.TopCenter
        ) {
            NavHost(
                navController = navController,
                startDestination = Screen.Studio.route,
                modifier = Modifier
                    .fillMaxSize()
                    .widthIn(max = 840.dp)
            ) {
                composable(Screen.Studio.route) {
                    StudioRoute(viewModel = studioViewModel)
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
                        viewModel = deviceViewModel,
                        onNavigateToPair = { navController.navigate(Screen.Pair.route) },
                        onNavigateToOnboarding = { navController.navigate(Screen.Onboarding.route) }
                    )
                }
                composable(Screen.Onboarding.route) {
                    OnboardingScreen(
                        viewModel = deviceViewModel,
                        onFinishOnboarding = { navController.navigate(Screen.Pair.route) }
                    )
                }
                composable(Screen.Pair.route) {
                    PairScreen(
                        viewModel = deviceViewModel,
                        onNavigateBack = { navController.popBackStack() }
                    )
                }
            }
        }
    }
}
