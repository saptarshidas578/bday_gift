package com.pixelgift.app.ui.effects

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp

/**
 * Animated gradient preview chip reflecting the effect category per SPEC 8.
 */
@Composable
fun AnimatedGradientChip(
    category: String,
    modifier: Modifier = Modifier
) {
    val transition = rememberInfiniteTransition(label = "gradientAnim")
    val progress by transition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 2800, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "gradientProgress"
    )

    val colors = when (category.lowercase()) {
        "rainbow/gradient" -> listOf(
            Color(0xFFFF0055),
            Color(0xFFFF7A00),
            Color(0xFFFFD600),
            Color(0xFF00E676),
            Color(0xFF00E5FF),
            Color(0xFF7928CA),
            Color(0xFFFF0055)
        )
        "nature/fire" -> listOf(
            Color(0xFFFF2200),
            Color(0xFFFF7A00),
            Color(0xFFFFD600),
            Color(0xFFCC1100)
        )
        "motion" -> listOf(
            Color(0xFF0055FF),
            Color(0xFF00E5FF),
            Color(0xFF7928CA),
            Color(0xFF0055FF)
        )
        "particles" -> listOf(
            Color(0xFF9E77ED),
            Color(0xFFFF66C4),
            Color(0xFFFFDE59),
            Color(0xFF9E77ED)
        )
        "geometric" -> listOf(
            Color(0xFF00F2FE),
            Color(0xFF4FACFE),
            Color(0xFF6B11FF),
            Color(0xFF00F2FE)
        )
        "retro" -> listOf(
            Color(0xFF003B00),
            Color(0xFF00FF66),
            Color(0xFF00AA44),
            Color(0xFF003B00)
        )
        "celebration" -> listOf(
            Color(0xFFFF2D55),
            Color(0xFFFFD600),
            Color(0xFF00E5FF),
            Color(0xFFFF2D55)
        )
        else -> listOf( // Static/glow
            Color(0xFFFF7A00),
            Color(0xFFFFB74D),
            Color(0xFFFF5722),
            Color(0xFFFF7A00)
        )
    }

    val brush = Brush.linearGradient(
        colors = colors,
        start = Offset(x = progress * 150f, y = 0f),
        end = Offset(x = (1f - progress) * 200f + 200f, y = 200f)
    )

    Box(
        modifier = modifier
            .clip(RoundedCornerShape(12.dp))
            .background(brush)
            .border(1.dp, Color(0x33FFFFFF), RoundedCornerShape(12.dp))
    )
}
