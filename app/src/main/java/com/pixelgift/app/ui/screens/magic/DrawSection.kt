package com.pixelgift.app.ui.screens.magic

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Undo
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Save
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.pixelgift.app.data.model.PixelDrawing
import com.pixelgift.app.protocol.ProtocolConstants
import com.pixelgift.app.protocol.Rgb
import com.pixelgift.app.ui.color.ColorMath
import com.pixelgift.app.ui.theme.PixelGiftSpacing

@Composable
fun DrawSection(
    canvasPixels: List<Rgb>,
    selectedColor: Rgb,
    savedDrawings: List<PixelDrawing>,
    feedback: String?,
    onPixelClicked: (Int) -> Unit,
    onColorSelected: (Rgb) -> Unit,
    onUndo: () -> Unit,
    onClear: () -> Unit,
    onCommit: () -> Unit,
    onPromptSave: () -> Unit,
    onLoadDrawing: (PixelDrawing) -> Unit,
    onPlayMemoriesReel: () -> Unit = {},
    onEvolveLife: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    val paletteOptions = listOf(
        Rgb(255, 45, 85),   // Pink
        Rgb(255, 122, 0),  // Amber
        Rgb(255, 214, 0),  // Yellow
        Rgb(0, 230, 118),  // Green
        Rgb(0, 229, 255),  // Cyan
        Rgb(158, 119, 237),// Violet
        Rgb(255, 255, 255),// White
        Rgb.BLACK          // Eraser
    )

    Card(
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
        shape = MaterialTheme.shapes.large,
        modifier = modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(PixelGiftSpacing.md),
            verticalArrangement = Arrangement.spacedBy(PixelGiftSpacing.md),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text("10×10 Pixel Draw Canvas (300-Byte Frame)", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)

            // 10x10 Interactive Canvas Grid
            Box(
                modifier = Modifier
                    .size(240.dp)
                    .clip(RoundedCornerShape(16.dp))
                    .background(Color(0xFF0F1117))
                    .border(2.dp, MaterialTheme.colorScheme.outlineVariant, RoundedCornerShape(16.dp))
                    .padding(8.dp)
            ) {
                Canvas(
                    modifier = Modifier
                        .fillMaxSize()
                        .aspectRatio(1f)
                        .pointerInput(Unit) {
                            detectTapGestures { offset ->
                                val cellW = size.width / ProtocolConstants.MATRIX_WIDTH.toFloat()
                                val cellH = size.height / ProtocolConstants.MATRIX_HEIGHT.toFloat()
                                val col = (offset.x / cellW).toInt().coerceIn(0, ProtocolConstants.MATRIX_WIDTH - 1)
                                val row = (offset.y / cellH).toInt().coerceIn(0, ProtocolConstants.MATRIX_HEIGHT - 1)
                                onPixelClicked(row * ProtocolConstants.MATRIX_WIDTH + col)
                            }
                        }
                        .pointerInput(Unit) {
                            detectDragGestures { change, _ ->
                                change.consume()
                                val cellW = size.width / ProtocolConstants.MATRIX_WIDTH.toFloat()
                                val cellH = size.height / ProtocolConstants.MATRIX_HEIGHT.toFloat()
                                val col = (change.position.x / cellW).toInt().coerceIn(0, ProtocolConstants.MATRIX_WIDTH - 1)
                                val row = (change.position.y / cellH).toInt().coerceIn(0, ProtocolConstants.MATRIX_HEIGHT - 1)
                                onPixelClicked(row * ProtocolConstants.MATRIX_WIDTH + col)
                            }
                        }
                ) {
                    val cellW = size.width / ProtocolConstants.MATRIX_WIDTH.toFloat()
                    val cellH = size.height / ProtocolConstants.MATRIX_HEIGHT.toFloat()
                    val radius = minOf(cellW, cellH) * 0.42f

                    for (r in 0 until ProtocolConstants.MATRIX_HEIGHT) {
                        for (c in 0 until ProtocolConstants.MATRIX_WIDTH) {
                            val idx = r * ProtocolConstants.MATRIX_WIDTH + c
                            val pixelColor = ColorMath.rgbToComposeColor(canvasPixels[idx])
                            val center = Offset((c + 0.5f) * cellW, (r + 0.5f) * cellH)

                            drawCircle(
                                color = if (canvasPixels[idx] == Rgb.BLACK) Color(0xFF1E222D) else pixelColor,
                                radius = radius,
                                center = center
                            )
                            drawCircle(
                                color = Color(0x33FFFFFF),
                                radius = radius,
                                center = center,
                                style = Stroke(width = 1f)
                            )
                        }
                    }
                }
            }

            // Draw Palette Row
            Row(
                modifier = Modifier.horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(PixelGiftSpacing.sm)
            ) {
                paletteOptions.forEach { rgb ->
                    val isSelected = selectedColor == rgb
                    Box(
                        modifier = Modifier
                            .size(36.dp)
                            .clip(CircleShape)
                            .background(ColorMath.rgbToComposeColor(rgb))
                            .border(
                                width = if (isSelected) 3.dp else 1.dp,
                                color = if (isSelected) MaterialTheme.colorScheme.primary else Color(0x44FFFFFF),
                                shape = CircleShape
                            )
                            .clickable { onColorSelected(rgb) },
                        contentAlignment = Alignment.Center
                    ) {
                        if (rgb == Rgb.BLACK) {
                            Icon(Icons.Default.Clear, contentDescription = "Eraser", tint = Color.Gray, modifier = Modifier.size(16.dp))
                        }
                    }
                }
            }

            // Actions Row: Undo, Clear, Save, Commit
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(PixelGiftSpacing.xs)
            ) {
                OutlinedButton(onClick = onUndo, modifier = Modifier.weight(1f).defaultMinSize(minHeight = 44.dp)) {
                    Icon(Icons.AutoMirrored.Filled.Undo, contentDescription = "Undo", modifier = Modifier.size(16.dp))
                }
                OutlinedButton(onClick = onClear, modifier = Modifier.weight(1f).defaultMinSize(minHeight = 44.dp)) {
                    Icon(Icons.Default.Clear, contentDescription = "Clear", modifier = Modifier.size(16.dp))
                }
                OutlinedButton(onClick = onPromptSave, modifier = Modifier.weight(1f).defaultMinSize(minHeight = 44.dp)) {
                    Icon(Icons.Default.Save, contentDescription = "Save Favorite", modifier = Modifier.size(16.dp))
                }
                Button(
                    onClick = onCommit,
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary),
                    modifier = Modifier.weight(1.5f).defaultMinSize(minHeight = 44.dp)
                ) {
                    Text("Display", style = MaterialTheme.typography.labelMedium)
                }
            }

            // Feature #12: Interactive Life Sandbox
            Card(
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth().padding(PixelGiftSpacing.md),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text("Interactive Life Sandbox", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
                        Text("Evolves Conway's Game of Life on the matrix starting from your current drawn pixels.", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                    Spacer(modifier = Modifier.width(PixelGiftSpacing.sm))
                    Button(
                        onClick = onEvolveLife,
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF00E676)),
                        modifier = Modifier.defaultMinSize(minHeight = 44.dp)
                    ) {
                        Text("🧬 Evolve", fontWeight = FontWeight.Bold, color = Color.Black)
                    }
                }
            }

            feedback?.let {
                Text(it, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Bold)
            }

            // Feature #5: Memories Reel Banner
            Card(
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth().padding(PixelGiftSpacing.md),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text("Animated Memories Reel", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
                        Text("Slideshow: Heart → Star → Smile → Gift Box", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                    Spacer(modifier = Modifier.width(PixelGiftSpacing.sm))
                    Button(
                        onClick = onPlayMemoriesReel,
                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary),
                        modifier = Modifier.defaultMinSize(minHeight = 44.dp)
                    ) {
                        Icon(Icons.Default.PlayArrow, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Play Reel")
                    }
                }
            }

            // Saved Favorites Row
            if (savedDrawings.isNotEmpty()) {
                Column(modifier = Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(PixelGiftSpacing.xs)) {
                    Text("Favorite & Preset Drawings", style = MaterialTheme.typography.labelMedium)
                    Row(
                        modifier = Modifier.horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(PixelGiftSpacing.sm)
                    ) {
                        savedDrawings.forEach { drawing ->
                            FilterChip(
                                selected = false,
                                onClick = { onLoadDrawing(drawing) },
                                label = { Text(drawing.name) }
                            )
                        }
                    }
                }
            }
        }
    }
}

