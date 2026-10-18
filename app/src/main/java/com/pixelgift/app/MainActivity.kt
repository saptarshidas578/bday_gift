package com.pixelgift.app

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import com.pixelgift.app.ui.screens.PixelGiftApp
import com.pixelgift.app.ui.theme.PixelGiftTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        // Initialize Android 12+ Splash Screen API
        installSplashScreen()
        super.onCreate(savedInstanceState)

        // Enable edge-to-edge drawing
        enableEdgeToEdge()

        setContent {
            PixelGiftTheme {
                PixelGiftApp()
            }
        }
    }
}
