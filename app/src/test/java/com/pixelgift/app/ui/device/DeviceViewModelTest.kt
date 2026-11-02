package com.pixelgift.app.ui.device

import com.pixelgift.app.data.model.DiscoveredDevice
import com.pixelgift.app.data.model.PairingUiState
import com.pixelgift.app.data.model.PermissionStatus
import com.pixelgift.app.protocol.Command
import com.pixelgift.app.protocol.ProtocolConstants
import com.pixelgift.app.transport.FakeDevice
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class DeviceViewModelTest {

    private val testScope = CoroutineScope(Dispatchers.Default)

    @Test
    fun permissionDenied_stateHandling() {
        val fakeDevice = FakeDevice(scope = testScope)
        val viewModel = DeviceViewModel(fakeDevice, externalScope = testScope)

        assertEquals(PermissionStatus.NOT_REQUESTED, viewModel.permissionStatus.value)

        // Denied with rationale
        viewModel.onPermissionResult(isGranted = false, shouldShowRationale = true)
        assertEquals(PermissionStatus.DENIED, viewModel.permissionStatus.value)

        // Permanently denied (user checked "Don't ask again" -> no rationale)
        viewModel.onPermissionResult(isGranted = false, shouldShowRationale = false)
        assertEquals(PermissionStatus.PERMANENTLY_DENIED, viewModel.permissionStatus.value)

        // Granted
        viewModel.onPermissionResult(isGranted = true, shouldShowRationale = false)
        assertEquals(PermissionStatus.GRANTED, viewModel.permissionStatus.value)
    }

    @Test
    fun pairingFailed_invalidPasskey_transitionsToFailedState() = runBlocking {
        val fakeDevice = FakeDevice(scope = testScope)
        fakeDevice.connect("FA:KE:DE:VI:CE:00")
        val viewModel = DeviceViewModel(fakeDevice, externalScope = testScope)

        val targetDevice = DiscoveredDevice(name = "PixelGift-Test", address = "11:22:33:44:55:66", rssi = -55)

        // Invalid passkey (less than 6 digits)
        viewModel.confirmPairing(targetDevice, "123")
        assertTrue(viewModel.pairingState.value is PairingUiState.Failed)
        val failedState = viewModel.pairingState.value as PairingUiState.Failed
        assertEquals(targetDevice, failedState.device)
        assertTrue(failedState.reason.contains("6 digits"))

        // Invalid non-digit passkey
        viewModel.confirmPairing(targetDevice, "abcdef")
        assertTrue(viewModel.pairingState.value is PairingUiState.Failed)
    }

    @Test
    fun pairingSuccess_validPasskey_transitionsToSuccess() = runBlocking {
        val fakeDevice = FakeDevice(scope = testScope, connectDelayMs = 0L)
        val viewModel = DeviceViewModel(fakeDevice, externalScope = testScope)

        val targetDevice = DiscoveredDevice(name = "PixelGift-Sim", address = "FA:KE:DE:VI:CE:00", rssi = -42)

        viewModel.confirmPairing(targetDevice, "123456")
        delay(100)

        assertTrue("Expected PairingUiState.Success, got ${viewModel.pairingState.value}", viewModel.pairingState.value is PairingUiState.Success)
        val successState = viewModel.pairingState.value as PairingUiState.Success
        assertEquals(targetDevice, successState.device)
    }

    @Test
    fun scanFlow_discoversDevices() = runBlocking {
        val fakeDevice = FakeDevice(scope = testScope)
        val viewModel = DeviceViewModel(fakeDevice, externalScope = testScope)

        assertEquals(PairingUiState.Idle, viewModel.pairingState.value)

        viewModel.startScan()
        assertEquals(PairingUiState.Scanning, viewModel.pairingState.value)

        delay(600) // Allow simulated scan to deliver results
        assertTrue(viewModel.pairingState.value is PairingUiState.Discovered)
        val discovered = viewModel.pairingState.value as PairingUiState.Discovered
        assertTrue(discovered.devices.isNotEmpty())
        assertEquals("PixelGift-Sim", discovered.devices[0].name)
    }

    @Test
    fun deviceTabActions_muteBondsOtaScenes() = runBlocking {
        val fakeDevice = FakeDevice(scope = testScope)
        fakeDevice.connect("FA:KE:DE:VI:CE:00")
        val viewModel = DeviceViewModel(fakeDevice, externalScope = testScope)

        // Mute toggle
        viewModel.onToggleMute(true)
        delay(50)
        assertTrue(fakeDevice.status.value.isMuted)

        viewModel.onToggleMute(false)
        delay(50)
        assertFalse(fakeDevice.status.value.isMuted)

        // Clear bonds
        val preBondsCount = viewModel.sendCountForTesting
        viewModel.onClearBonds()
        delay(50)
        assertEquals(preBondsCount + 1, viewModel.sendCountForTesting)

        // OTA begin
        val preOtaCount = viewModel.sendCountForTesting
        viewModel.onBeginOta()
        delay(50)
        assertEquals(preOtaCount + 1, viewModel.sendCountForTesting)

        // Save & Load Scene
        viewModel.onSaveScene(3)
        delay(50)
        viewModel.onLoadScene(3)
        delay(50)

        // Refresh Status
        val preRefreshCount = viewModel.sendCountForTesting
        viewModel.onRefreshStatus()
        delay(50)
        assertEquals(preRefreshCount + 1, viewModel.sendCountForTesting)
    }

    @Test
    fun autoReconnect_toggle() {
        val fakeDevice = FakeDevice(scope = testScope)
        val viewModel = DeviceViewModel(fakeDevice, externalScope = testScope)

        assertTrue(viewModel.autoReconnect.value)
        viewModel.onToggleAutoReconnect(false)
        assertFalse(viewModel.autoReconnect.value)
        viewModel.onToggleAutoReconnect(true)
        assertTrue(viewModel.autoReconnect.value)
    }

    @Test
    fun confirmPairing_stopsScanBeforeConnecting() = runBlocking {
        val fakeDevice = FakeDevice(scope = testScope, connectDelayMs = 0L)
        val viewModel = DeviceViewModel(fakeDevice, externalScope = testScope)

        viewModel.startScan()
        assertEquals(PairingUiState.Scanning, viewModel.pairingState.value)

        val targetDevice = DiscoveredDevice(name = "PixelGift-Sim", address = "FA:KE:DE:VI:CE:00", rssi = -42)
        viewModel.confirmPairing(targetDevice, "123456")

        // Scanning job must be cancelled immediately when connecting starts
        assertNull("Scan job must be null after confirmPairing", viewModel.getScanJobForTesting())
    }

    @Test
    fun confirmPairing_alreadyBonded_bypassesPasskey() = runBlocking {
        val fakeDevice = FakeDevice(scope = testScope, connectDelayMs = 0L)
        val viewModel = DeviceViewModel(fakeDevice, externalScope = testScope)

        // Device is already bonded: empty passkey should NOT fail
        val bondedDevice = DiscoveredDevice(name = "PixelGift-Bonded", address = "FA:KE:DE:VI:CE:00", rssi = -50, isBonded = true)
        viewModel.confirmPairing(bondedDevice, "")
        delay(100)

        assertTrue(
            "Already bonded device should succeed without passkey, got: ${viewModel.pairingState.value}",
            viewModel.pairingState.value is PairingUiState.Success
        )
    }

    @Test
    fun deviceTab_factoryReset_sendsMagicBytes() = runBlocking {
        val fakeDevice = FakeDevice(scope = testScope)
        fakeDevice.connect("FA:KE:DE:VI:CE:00")
        val viewModel = DeviceViewModel(fakeDevice, externalScope = testScope)

        val preCount = viewModel.sendCountForTesting
        viewModel.onFactoryReset()
        delay(50)
        assertEquals(preCount + 1, viewModel.sendCountForTesting)
        assertNotNull(viewModel.feedback.value)
        assertTrue(viewModel.feedback.value!!.contains("Factory reset"))
    }
}
