package com.pixelgift.app.ui.screens.magic

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.GraphicEq
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.MicOff
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.pixelgift.app.protocol.ProtocolConstants
import com.pixelgift.app.ui.theme.PixelGiftSpacing

@Composable
fun MusicSection(
    isMusicActive: Boolean,
    liveLevels: ByteArray,
    onToggleMusic: () -> Unit,
    onSimulateMusic: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
        shape = MaterialTheme.shapes.large,
        modifier = modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(PixelGiftSpacing.lg),
            verticalArrangement = Arrangement.spacedBy(PixelGiftSpacing.md),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(PixelGiftSpacing.sm)) {
                    Icon(Icons.Default.GraphicEq, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                    Text("10-Band FFT Visualizer", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                }
                Text("<= 20 Hz", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }

            Text(
                text = "Captures microphone audio, performs FFT into 10 frequency bands, and sends MUSIC_LEVELS packets at 20 Hz via write-without-response.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            // Equalizer Bars (10 Columns)
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(180.dp)
                    .clip(RoundedCornerShape(16.dp))
                    .background(Color(0xFF0F1117))
                    .border(1.dp, MaterialTheme.colorScheme.outlineVariant, RoundedCornerShape(16.dp))
                    .padding(16.dp),
                contentAlignment = Alignment.BottomCenter
            ) {
                Canvas(modifier = Modifier.fillMaxSize()) {
                    val cols = ProtocolConstants.MATRIX_WIDTH
                    val colWidth = size.width / (cols * 1.5f)
                    val spacing = (size.width - (cols * colWidth)) / (cols - 1)

                    for (i in 0 until cols) {
                        val level = if (i < liveLevels.size) (liveLevels[i].toInt() and 0xFF) / 255f else 0.05f
                        val barHeight = maxOf(4f, level * size.height)
                        val left = i * (colWidth + spacing)
                        val top = size.height - barHeight

                        drawRoundRect(
                            brush = Brush.verticalGradient(
                                colors = listOf(Color(0xFFFF2D55), Color(0xFFFF7A00), Color(0xFF00E5FF)),
                                startY = 0f,
                                endY = size.height
                            ),
                            topLeft = Offset(left, top),
                            size = Size(colWidth, barHeight),
                            cornerRadius = CornerRadius(4f, 4f)
                        )
                    }
                }
            }

            // Controls
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(PixelGiftSpacing.sm)
            ) {
                Button(
                    onClick = onToggleMusic,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = if (isMusicActive) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary
                    ),
                    modifier = Modifier
                        .weight(1.5f)
                        .defaultMinSize(minHeight = 48.dp)
                ) {
                    Icon(
                        imageVector = if (isMusicActive) Icons.Default.MicOff else Icons.Default.Mic,
                        contentDescription = null
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(if (isMusicActive) "Stop Mic" else "Listen Mic")
                }

                OutlinedButton(
                    onClick = onSimulateMusic,
                    modifier = Modifier
                        .weight(1f)
                        .defaultMinSize(minHeight = 48.dp)
                ) {
                    Text(if (isMusicActive) "Stop Sim" else "Simulate")
                }
            }
        }
    }
}

