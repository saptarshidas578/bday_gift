package com.pixelgift.app.ui

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.pixelgift.app.data.model.DiscoveredDevice
import com.pixelgift.app.data.model.PairingUiState
import com.pixelgift.app.data.model.PermissionStatus
import com.pixelgift.app.transport.FakeDevice
import com.pixelgift.app.ui.device.DeviceViewModel
import com.pixelgift.app.ui.screens.PairScreen
import com.pixelgift.app.ui.theme.PixelGiftTheme
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class DeviceScreenUiTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    @Test
    fun permissionDenied_showsWarningBannerAndSettingsButton() {
        val fakeDevice = FakeDevice()
        val viewModel = DeviceViewModel(fakeDevice)
        viewModel.setPermissionStatusForTesting(PermissionStatus.DENIED)

        composeTestRule.setContent {
            PixelGiftTheme {
                PairScreen(
                    viewModel = viewModel,
                    onNavigateBack = {}
                )
            }
        }

        // Verify "Bluetooth Permission Denied" banner is displayed
        composeTestRule.onNodeWithText("Bluetooth Permission Denied").assertIsDisplayed()
        composeTestRule.onNodeWithText("Open App Settings").assertIsDisplayed()
    }

    @Test
    fun pairingFailed_showsErrorCardAndRetryButton() {
        val fakeDevice = FakeDevice()
        val viewModel = DeviceViewModel(fakeDevice)
        val testDevice = DiscoveredDevice(name = "PixelGift-Failed", address = "AA:BB:CC:DD:EE:FF", rssi = -70)
        viewModel.setPairingStateForTesting(
            PairingUiState.Failed(testDevice, "Wrong passkey or device out of range")
        )

        composeTestRule.setContent {
            PixelGiftTheme {
                PairScreen(
                    viewModel = viewModel,
                    onNavigateBack = {}
                )
            }
        }

        // Verify "Pairing Failed" card and error reason are rendered
        composeTestRule.onNodeWithText("Pairing Failed").assertIsDisplayed()
        composeTestRule.onNodeWithText("Wrong passkey or device out of range").assertIsDisplayed()
        composeTestRule.onNodeWithText("Retry").assertIsDisplayed()
    }
}
