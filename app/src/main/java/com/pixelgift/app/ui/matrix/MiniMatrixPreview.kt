package com.pixelgift.app.ui.matrix

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import com.pixelgift.app.protocol.Rgb
import com.pixelgift.app.ui.color.ColorMath

/**
 * 9x9 LED Matrix preview (81 pixels) with hardware-faithful diode aesthetics
 * and real-time color glow per SPEC 9.2 & 9.3.
 */
@Composable
fun MiniMatrixPreview(
    color: Rgb,
    modifier: Modifier = Modifier,
    brightness: Int = 255
) {
    val composeColor = ColorMath.rgbToComposeColor(color)
    val factor = (brightness.coerceIn(0, 255) / 255f)
    val displayColor by animateColorAsState(
        targetValue = composeColor.copy(alpha = (0.25f + 0.75f * factor)),
        animationSpec = tween(durationMillis = 80),
        label = "matrixColor"
    )

    Column(
        modifier = modifier.semantics { contentDescription = "9 by 9 matrix live preview" },
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Box(
            modifier = Modifier
                .size(136.dp)
                .shadow(elevation = 8.dp, shape = RoundedCornerShape(16.dp))
                .clip(RoundedCornerShape(16.dp))
                .background(Color(0xFF0F1117))
                .border(
                    width = 1.dp,
                    brush = Brush.verticalGradient(
                        colors = listOf(
                            Color(0xFF333B4F),
                            Color(0xFF1E222D)
                        )
                    ),
                    shape = RoundedCornerShape(16.dp)
                )
                .padding(8.dp),
            contentAlignment = Alignment.Center
        ) {
            Canvas(
                modifier = Modifier
                    .fillMaxSize()
                    .aspectRatio(1f)
            ) {
                val cols = 9
                val rows = 9
                val cellW = size.width / cols
                val cellH = size.height / rows
                val radius = (minOf(cellW, cellH) * 0.36f)

                for (r in 0 until rows) {
                    for (c in 0 until cols) {
                        val cx = (c + 0.5f) * cellW
                        val cy = (r + 0.5f) * cellH
                        val center = Offset(cx, cy)

                        // Outer soft glow
                        drawCircle(
                            color = displayColor.copy(alpha = 0.28f * factor),
                            radius = radius * 1.55f,
                            center = center
                        )

                        // Main LED diode body
                        drawCircle(
                            color = displayColor,
                            radius = radius,
                            center = center
                        )

                        // Subtle inner core diode specular dot
                        drawCircle(
                            color = Color.White.copy(alpha = 0.45f * factor),
                            radius = radius * 0.32f,
                            center = center
                        )

                        // Subtle rim outline
                        drawCircle(
                            color = Color(0x33FFFFFF),
                            radius = radius,
                            center = center,
                            style = Stroke(width = 0.75f)
                        )
                    }
                }
            }
        }
        Spacer(modifier = Modifier.height(6.dp))
        Text(
            text = "9×9 Live Matrix",
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
        )
    }
}
