package com.pixelgift.app.ui.color

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.pixelgift.app.protocol.Rgb
import kotlin.math.PI
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.hypot
import kotlin.math.sin
import kotlin.math.sqrt

private enum class DragTarget {
    NONE,
    HUE_RING,
    SV_BOX
}

/**
 * Custom Paint-Style Color Wheel:
 * - Outer 360-degree Hue sweep gradient ring.
 * - Inner Saturation / Value gradient area.
 * - Magnifier loupe floating above finger while dragging.
 * - Drag anywhere to choose smoothly.
 */
@Composable
fun PaintColorWheel(
    selectedRgb: Rgb,
    onColorChanged: (Rgb) -> Unit,
    onDragStateChanged: (isDragging: Boolean) -> Unit = {},
    modifier: Modifier = Modifier,
    ringThicknessDp: Dp = 28.dp,
    loupeRadiusDp: Dp = 26.dp,
    loupeOffsetDp: Dp = 56.dp
) {
    val density = LocalDensity.current
    val ringThicknessPx = with(density) { ringThicknessDp.toPx() }
    val loupeRadiusPx = with(density) { loupeRadiusDp.toPx() }
    val loupeOffsetPx = with(density) { loupeOffsetDp.toPx() }

    // Convert initial RGB to HSV
    val initialHsv = remember(selectedRgb) {
        ColorMath.rgbToHsv(selectedRgb.r, selectedRgb.g, selectedRgb.b)
    }

    var hue by remember { mutableFloatStateOf(initialHsv[0]) }
    var saturation by remember { mutableFloatStateOf(initialHsv[1]) }
    var value by remember { mutableFloatStateOf(initialHsv[2]) }

    // Sync with external RGB changes (e.g. from HEX or RGB fields or recent color selection)
    val currentRgb = ColorMath.hsvToRgb(hue, saturation, value)
    if (currentRgb != selectedRgb) {
        val newHsv = ColorMath.rgbToHsv(selectedRgb.r, selectedRgb.g, selectedRgb.b)
        hue = newHsv[0]
        saturation = newHsv[1]
        value = newHsv[2]
    }

    var isDragging by remember { mutableStateOf(false) }
    var dragPosition by remember { mutableStateOf(Offset.Zero) }
    var activeTarget by remember { mutableStateOf(DragTarget.NONE) }

    // Precomputed sweep colors for hue ring
    val hueSweepColors = remember {
        listOf(
            Color.Red,
            Color.Yellow,
            Color.Green,
            Color.Cyan,
            Color.Blue,
            Color.Magenta,
            Color.Red
        )
    }

    Box(
        modifier = modifier
            .fillMaxWidth()
            .aspectRatio(1f)
            .padding(12.dp)
            .pointerInput(Unit) {
                val inputSize = this.size
                detectTapGestures(
                    onPress = { offset ->
                        val center = Offset(inputSize.width / 2f, inputSize.height / 2f)
                        val outerRadius = inputSize.width / 2f
                        val innerRadius = outerRadius - ringThicknessPx

                        val dist = hypot(offset.x - center.x, offset.y - center.y)
                        val boxHalf = (innerRadius - 8.dp.toPx()) / sqrt(2f)
                        val svRect = Rect(
                            center.x - boxHalf,
                            center.y - boxHalf,
                            center.x + boxHalf,
                            center.y + boxHalf
                        )

                        if (dist >= innerRadius - 4.dp.toPx()) {
                            // Hit Hue Ring
                            val angleRad = atan2(offset.y - center.y, offset.x - center.x)
                            val deg = (Math.toDegrees(angleRad.toDouble()).toFloat() + 360f) % 360f
                            hue = deg
                            val updatedRgb = ColorMath.hsvToRgb(hue, saturation, value)
                            onColorChanged(updatedRgb)
                        } else if (svRect.contains(offset)) {
                            // Hit SV Box
                            saturation = ((offset.x - svRect.left) / svRect.width).coerceIn(0f, 1f)
                            value = (1f - (offset.y - svRect.top) / svRect.height).coerceIn(0f, 1f)
                            val updatedRgb = ColorMath.hsvToRgb(hue, saturation, value)
                            onColorChanged(updatedRgb)
                        }
                    }
                )
            }
            .pointerInput(Unit) {
                val inputSize = this.size
                detectDragGestures(
                    onDragStart = { offset ->
                        isDragging = true
                        dragPosition = offset
                        onDragStateChanged(true)

                        val center = Offset(inputSize.width / 2f, inputSize.height / 2f)
                        val outerRadius = inputSize.width / 2f
                        val innerRadius = outerRadius - ringThicknessPx

                        val dist = hypot(offset.x - center.x, offset.y - center.y)
                        activeTarget = if (dist >= innerRadius - 8.dp.toPx()) {
                            DragTarget.HUE_RING
                        } else {
                            DragTarget.SV_BOX
                        }

                        // Immediately process start touch
                        val boxHalf = (innerRadius - 8.dp.toPx()) / sqrt(2f)
                        val svRect = Rect(
                            center.x - boxHalf,
                            center.y - boxHalf,
                            center.x + boxHalf,
                            center.y + boxHalf
                        )

                        if (activeTarget == DragTarget.HUE_RING) {
                            val angleRad = atan2(offset.y - center.y, offset.x - center.x)
                            val deg = (Math.toDegrees(angleRad.toDouble()).toFloat() + 360f) % 360f
                            hue = deg
                        } else {
                            saturation = ((offset.x - svRect.left) / svRect.width).coerceIn(0f, 1f)
                            value = (1f - (offset.y - svRect.top) / svRect.height).coerceIn(0f, 1f)
                        }
                        onColorChanged(ColorMath.hsvToRgb(hue, saturation, value))
                    },
                    onDrag = { change, _ ->
                        change.consume()
                        dragPosition = change.position

                        val center = Offset(inputSize.width / 2f, inputSize.height / 2f)
                        val outerRadius = inputSize.width / 2f
                        val innerRadius = outerRadius - ringThicknessPx
                        val boxHalf = (innerRadius - 8.dp.toPx()) / sqrt(2f)
                        val svRect = Rect(
                            center.x - boxHalf,
                            center.y - boxHalf,
                            center.x + boxHalf,
                            center.y + boxHalf
                        )

                        if (activeTarget == DragTarget.HUE_RING) {
                            val angleRad = atan2(change.position.y - center.y, change.position.x - center.x)
                            val deg = (Math.toDegrees(angleRad.toDouble()).toFloat() + 360f) % 360f
                            hue = deg
                        } else {
                            saturation = ((change.position.x - svRect.left) / svRect.width).coerceIn(0f, 1f)
                            value = (1f - (change.position.y - svRect.top) / svRect.height).coerceIn(0f, 1f)
                        }

                        val updatedRgb = ColorMath.hsvToRgb(hue, saturation, value)
                        onColorChanged(updatedRgb)
                    },
                    onDragEnd = {
                        isDragging = false
                        activeTarget = DragTarget.NONE
                        onDragStateChanged(false)
                    },
                    onDragCancel = {
                        isDragging = false
                        activeTarget = DragTarget.NONE
                        onDragStateChanged(false)
                    }
                )
            }
    ) {
        Canvas(modifier = Modifier.matchParentSize()) {
            val center = Offset(size.width / 2f, size.height / 2f)
            val outerRadius = size.width / 2f - 2.dp.toPx()
            val ringRadius = outerRadius - ringThicknessPx / 2f
            val innerRadius = outerRadius - ringThicknessPx

            // 1. Draw Hue Ring with Sweep Gradient
            val sweepBrush = Brush.sweepGradient(
                colors = hueSweepColors,
                center = center
            )
            drawCircle(
                brush = sweepBrush,
                radius = ringRadius,
                center = center,
                style = Stroke(width = ringThicknessPx, cap = StrokeCap.Round)
            )

            // Hue Ring Thumb Indicator
            val hueAngleRad = Math.toRadians(hue.toDouble())
            val hueThumbCenter = Offset(
                x = center.x + ringRadius * cos(hueAngleRad).toFloat(),
                y = center.y + ringRadius * sin(hueAngleRad).toFloat()
            )
            drawCircle(
                color = Color.Black.copy(alpha = 0.5f),
                radius = ringThicknessPx / 2f + 2.dp.toPx(),
                center = hueThumbCenter
            )
            drawCircle(
                color = Color.White,
                radius = ringThicknessPx / 2f,
                center = hueThumbCenter
            )
            drawCircle(
                color = ColorMath.hsvToComposeColor(hue, 1f, 1f),
                radius = ringThicknessPx / 2f - 3.dp.toPx(),
                center = hueThumbCenter
            )

            // 2. Draw Inner Saturation / Value Box
            val boxHalf = (innerRadius - 10.dp.toPx()) / sqrt(2f)
            val svRect = Rect(
                center.x - boxHalf,
                center.y - boxHalf,
                center.x + boxHalf,
                center.y + boxHalf
            )

            // Base pure Hue color
            val pureHueColor = ColorMath.hsvToComposeColor(hue, 1f, 1f)

            // Horizontal gradient (White to Pure Hue)
            val saturationBrush = Brush.horizontalGradient(
                colors = listOf(Color.White, pureHueColor),
                startX = svRect.left,
                endX = svRect.right
            )
            drawRect(
                brush = saturationBrush,
                topLeft = Offset(svRect.left, svRect.top),
                size = Size(svRect.width, svRect.height)
            )

            // Vertical gradient (Transparent to Black) with multiply blend mode
            val valueBrush = Brush.verticalGradient(
                colors = listOf(Color.Transparent, Color.Black),
                startY = svRect.top,
                endY = svRect.bottom
            )
            drawRect(
                brush = valueBrush,
                topLeft = Offset(svRect.left, svRect.top),
                size = Size(svRect.width, svRect.height),
                blendMode = BlendMode.SrcOver
            )

            // Border around SV box
            drawRect(
                color = Color.White.copy(alpha = 0.25f),
                topLeft = Offset(svRect.left, svRect.top),
                size = Size(svRect.width, svRect.height),
                style = Stroke(width = 1.5.dp.toPx())
            )

            // 3. Draw SV Selector Thumb Indicator
            val thumbX = svRect.left + saturation * svRect.width
            val thumbY = svRect.top + (1f - value) * svRect.height
            val svThumbCenter = Offset(thumbX, thumbY)

            drawCircle(
                color = Color.Black.copy(alpha = 0.6f),
                radius = 10.dp.toPx(),
                center = svThumbCenter
            )
            drawCircle(
                color = Color.White,
                radius = 8.5.dp.toPx(),
                center = svThumbCenter
            )
            drawCircle(
                color = ColorMath.hsvToComposeColor(hue, saturation, value),
                radius = 6.dp.toPx(),
                center = svThumbCenter
            )

            // 4. Magnifier Loupe Floating Above Finger While Dragging
            if (isDragging) {
                val loupeCenter = Offset(
                    x = dragPosition.x.coerceIn(loupeRadiusPx + 4.dp.toPx(), size.width - loupeRadiusPx - 4.dp.toPx()),
                    y = (dragPosition.y - loupeOffsetPx).coerceAtLeast(loupeRadiusPx + 4.dp.toPx())
                )

                // Loupe drop shadow / outer halo
                drawCircle(
                    color = Color.Black.copy(alpha = 0.45f),
                    radius = loupeRadiusPx + 5.dp.toPx(),
                    center = loupeCenter
                )

                // Loupe white border ring
                drawCircle(
                    color = Color.White,
                    radius = loupeRadiusPx + 3.dp.toPx(),
                    center = loupeCenter
                )

                // Loupe inner color preview
                val liveSelectedColor = ColorMath.hsvToComposeColor(hue, saturation, value)
                drawCircle(
                    color = liveSelectedColor,
                    radius = loupeRadiusPx,
                    center = loupeCenter
                )

                // Little indicator pin line connecting loupe to touch point
                drawLine(
                    color = Color.White.copy(alpha = 0.8f),
                    start = Offset(loupeCenter.x, loupeCenter.y + loupeRadiusPx + 2.dp.toPx()),
                    end = Offset(dragPosition.x, dragPosition.y - 12.dp.toPx()),
                    strokeWidth = 2.dp.toPx()
                )
            }
        }
    }
}
