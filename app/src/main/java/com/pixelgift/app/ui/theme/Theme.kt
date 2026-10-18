package com.pixelgift.app.ui.theme

import android.app.Activity
import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ColorScheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.Typography
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.view.WindowCompat

// --- Brand Palette (Jewel & Ambient Glow, Dark-First) ---
val BrandPrimary = Color(0xFFFF7A00)       // Vivid Amber Glow
val BrandOnPrimary = Color(0xFF000000)
val BrandPrimaryContainer = Color(0xFF4D2400)
val BrandOnPrimaryContainer = Color(0xFFFFDCC2)

val BrandSecondary = Color(0xFF00E5FF)     // Electric Cyan Neon
val BrandOnSecondary = Color(0xFF000000)
val BrandSecondaryContainer = Color(0xFF004D57)
val BrandOnSecondaryContainer = Color(0xFFB8F5FF)

val BrandTertiary = Color(0xFF9E77ED)      // Radiant Violet
val BrandOnTertiary = Color(0xFF000000)
val BrandTertiaryContainer = Color(0xFF3B1E78)
val BrandOnTertiaryContainer = Color(0xFFE9D7FE)

val DarkSurface = Color(0xFF0F1117)        // Deep Obsidian
val DarkSurfaceVariant = Color(0xFF1E2230) // Elevated Obsidian
val DarkSurfaceContainer = Color(0xFF161922)
val DarkBackground = Color(0xFF090A0E)
val DarkOnBackground = Color(0xFFF2F4F7)
val DarkOnSurface = Color(0xFFE4E7EC)
val DarkOnSurfaceVariant = Color(0xFF98A2B3)
val DarkOutline = Color(0xFF344054)
val DarkOutlineVariant = Color(0xFF1D2939)

val LightBackground = Color(0xFFF9FAFB)
val LightSurface = Color(0xFFFFFFFF)
val LightSurfaceVariant = Color(0xFFF2F4F7)
val LightOnBackground = Color(0xFF101828)
val LightOnSurface = Color(0xFF1D2939)
val LightOnSurfaceVariant = Color(0xFF475467)
val LightOutline = Color(0xFFD0D5DD)

// Custom Status Colors
val StatusConnected = Color(0xFF12B76A)    // Emerald Green
val StatusReconnecting = Color(0xFFF79009) // Warning Orange
val StatusOffline = Color(0xFFF04438)      // Crimson Red

private val DarkColorScheme = darkColorScheme(
    primary = BrandPrimary,
    onPrimary = BrandOnPrimary,
    primaryContainer = BrandPrimaryContainer,
    onPrimaryContainer = BrandOnPrimaryContainer,
    secondary = BrandSecondary,
    onSecondary = BrandOnSecondary,
    secondaryContainer = BrandSecondaryContainer,
    onSecondaryContainer = BrandOnSecondaryContainer,
    tertiary = BrandTertiary,
    onTertiary = BrandOnTertiary,
    tertiaryContainer = BrandTertiaryContainer,
    onTertiaryContainer = BrandOnTertiaryContainer,
    background = DarkBackground,
    onBackground = DarkOnBackground,
    surface = DarkSurface,
    onSurface = DarkOnSurface,
    surfaceVariant = DarkSurfaceVariant,
    onSurfaceVariant = DarkOnSurfaceVariant,
    outline = DarkOutline,
    outlineVariant = DarkOutlineVariant
)

private val LightColorScheme = lightColorScheme(
    primary = BrandPrimary,
    onPrimary = Color.White,
    primaryContainer = Color(0xFFFFE8D7),
    onPrimaryContainer = Color(0xFF381A00),
    secondary = Color(0xFF00838F),
    onSecondary = Color.White,
    secondaryContainer = Color(0xFFB2EBF2),
    onSecondaryContainer = Color(0xFF00363A),
    tertiary = Color(0xFF6941C6),
    onTertiary = Color.White,
    tertiaryContainer = Color(0xFFE9D7FE),
    onTertiaryContainer = Color(0xFF24005A),
    background = LightBackground,
    onBackground = LightOnBackground,
    surface = LightSurface,
    onSurface = LightOnSurface,
    surfaceVariant = LightSurfaceVariant,
    onSurfaceVariant = LightOnSurfaceVariant,
    outline = LightOutline
)

// --- Spacing Tokens (4/8 dp grid per SPEC 9.4) ---
object PixelGiftSpacing {
    val none: Dp = 0.dp
    val xxs: Dp = 2.dp
    val xs: Dp = 4.dp
    val sm: Dp = 8.dp
    val md: Dp = 12.dp
    val lg: Dp = 16.dp
    val xl: Dp = 24.dp
    val xxl: Dp = 32.dp
    val xxxl: Dp = 48.dp
}

// --- Elevation Tokens ---
object PixelGiftElevation {
    val level0: Dp = 0.dp
    val level1: Dp = 1.dp
    val level2: Dp = 3.dp
    val level3: Dp = 6.dp
    val level4: Dp = 8.dp
    val level5: Dp = 12.dp
}

// --- Shape Tokens ---
val PixelGiftShapes = Shapes(
    extraSmall = RoundedCornerShape(4.dp),
    small = RoundedCornerShape(8.dp),
    medium = RoundedCornerShape(12.dp),
    large = RoundedCornerShape(16.dp),
    extraLarge = RoundedCornerShape(24.dp)
)

// --- Typography Tokens ---
val PixelGiftTypography = Typography(
    displayLarge = TextStyle(
        fontFamily = FontFamily.Default,
        fontWeight = FontWeight.Bold,
        fontSize = 32.sp,
        lineHeight = 40.sp,
        letterSpacing = (-0.5).sp
    ),
    headlineMedium = TextStyle(
        fontFamily = FontFamily.Default,
        fontWeight = FontWeight.SemiBold,
        fontSize = 22.sp,
        lineHeight = 28.sp
    ),
    titleLarge = TextStyle(
        fontFamily = FontFamily.Default,
        fontWeight = FontWeight.SemiBold,
        fontSize = 18.sp,
        lineHeight = 24.sp
    ),
    titleMedium = TextStyle(
        fontFamily = FontFamily.Default,
        fontWeight = FontWeight.Medium,
        fontSize = 16.sp,
        lineHeight = 22.sp
    ),
    bodyLarge = TextStyle(
        fontFamily = FontFamily.Default,
        fontWeight = FontWeight.Normal,
        fontSize = 16.sp,
        lineHeight = 24.sp
    ),
    bodyMedium = TextStyle(
        fontFamily = FontFamily.Default,
        fontWeight = FontWeight.Normal,
        fontSize = 14.sp,
        lineHeight = 20.sp
    ),
    labelLarge = TextStyle(
        fontFamily = FontFamily.Default,
        fontWeight = FontWeight.SemiBold,
        fontSize = 13.sp,
        lineHeight = 18.sp,
        letterSpacing = 0.1.sp
    ),
    labelMedium = TextStyle(
        fontFamily = FontFamily.Default,
        fontWeight = FontWeight.Medium,
        fontSize = 12.sp,
        lineHeight = 16.sp
    )
)

/**
 * PixelGiftTheme enforces a dark-first aesthetic by default,
 * with edge-to-edge transparent system bar handling.
 */
@Composable
fun PixelGiftTheme(
    darkTheme: Boolean = true, // Dark-first by specification
    content: @Composable () -> Unit
) {
    val colorScheme = if (darkTheme) DarkColorScheme else LightColorScheme

    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as? Activity)?.window ?: return@SideEffect
            val controller = WindowCompat.getInsetsController(window, view)
            controller.isAppearanceLightStatusBars = !darkTheme
            controller.isAppearanceLightNavigationBars = !darkTheme
        }
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = PixelGiftTypography,
        shapes = PixelGiftShapes,
        content = content
    )
}
