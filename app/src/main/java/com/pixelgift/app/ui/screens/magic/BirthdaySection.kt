package com.pixelgift.app.ui.screens.magic

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Cake
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.foundation.layout.Row
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.Send
import androidx.compose.material3.IconButton
import androidx.compose.material.icons.filled.Save
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.pixelgift.app.ui.theme.PixelGiftSpacing

@Composable
fun BirthdaySection(
    isPlayingShow: Boolean,
    name: String,
    greeting: String,
    feedback: String?,
    onNameChanged: (String) -> Unit,
    onGreetingChanged: (String) -> Unit,
    onPlayShow: () -> Unit,
    onStopShow: () -> Unit,
    onSaveContent: () -> Unit,
    onSendHeartbeat: () -> Unit = {},
    onSendSecretNote: (String) -> Unit = {},
    modifier: Modifier = Modifier
) {
    val countdownNotes = listOf(
        "Day 1 (Jan 18)" to "A little spark of joy for the most wonderful person.",
        "Day 2 (Jan 19)" to "Counting down the days until we celebrate your 19th!",
        "Day 3 (Jan 20)" to "Every moment with you shines brighter than 100 LEDs.",
        "Day 4 (Jan 21)" to "Thank you for being the sweetest warmth in my life.",
        "Day 5 (Jan 22)" to "Almost there! Your smile lights up every room.",
        "Day 6 (Jan 23)" to "Tomorrow is your big day - get ready for magic!",
        "Day 7 (Jan 24)" to "Happy 19th Birthday Madhumitha! You mean the world to me."
    )
    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(PixelGiftSpacing.lg)
    ) {
        Card(
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
            shape = MaterialTheme.shapes.large,
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(PixelGiftSpacing.lg),
                verticalArrangement = Arrangement.spacedBy(PixelGiftSpacing.md),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Box(
                    modifier = Modifier
                        .size(68.dp)
                        .clip(CircleShape)
                        .background(
                            Brush.radialGradient(
                                listOf(Color(0xFFFF2D55), Color(0xFFFF7A00))
                            )
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Cake,
                        contentDescription = null,
                        tint = Color.White,
                        modifier = Modifier.size(36.dp)
                    )
                }

                Text(
                    text = "Birthday Show Mode",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = "Plays intro sparkle, name reveal, heart & balloon animations, fireworks, and rainbow celebration (Lights-only, speaker deferred).",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                // Play / Stop Button
                if (isPlayingShow) {
                    Button(
                        onClick = onStopShow,
                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error),
                        modifier = Modifier
                            .fillMaxWidth()
                            .defaultMinSize(minHeight = 48.dp)
                    ) {
                        Icon(Icons.Default.Stop, contentDescription = null)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Stop Birthday Show")
                    }
                } else {
                    Button(
                        onClick = onPlayShow,
                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary),
                        modifier = Modifier
                            .fillMaxWidth()
                            .defaultMinSize(minHeight = 48.dp)
                    ) {
                        Icon(Icons.Default.PlayArrow, contentDescription = null)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Start Birthday Show Now")
                    }
                }

                feedback?.let {
                    Text(it, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Bold)
                }
            }
        }

        // Personalized Content Editor
        Card(
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
            shape = MaterialTheme.shapes.large,
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(PixelGiftSpacing.lg),
                verticalArrangement = Arrangement.spacedBy(PixelGiftSpacing.sm)
            ) {
                Text("Personalized Birthday Text (SET_TEXT Chunks)", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)

                OutlinedTextField(
                    value = name,
                    onValueChange = onNameChanged,
                    label = { Text("Birthday Person's Name (Target 1)") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                OutlinedTextField(
                    value = greeting,
                    onValueChange = onGreetingChanged,
                    label = { Text("Custom Greeting (Target 2)") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                OutlinedButton(
                    onClick = onSaveContent,
                    modifier = Modifier
                        .fillMaxWidth()
                        .defaultMinSize(minHeight = 48.dp)
                    ) {
                    Icon(Icons.Default.Save, contentDescription = null)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Save Name & Greeting to Device")
                }
            }
        }

        // Feature #16: "Thinking of You" Heartbeat Tap
        Card(
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
            shape = MaterialTheme.shapes.large,
            modifier = Modifier.fillMaxWidth()
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(PixelGiftSpacing.lg),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text("Thinking of You Tap", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                    Text("Send an immediate glowing heartbeat pulse across her matrix right now.", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                Spacer(modifier = Modifier.width(PixelGiftSpacing.md))
                Button(
                    onClick = onSendHeartbeat,
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFFF2D55)),
                    modifier = Modifier.defaultMinSize(minHeight = 48.dp)
                ) {
                    Icon(Icons.Default.Favorite, contentDescription = null, tint = Color.White)
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Tap")
                }
            }
        }

        // Feature #4: 7-Day Countdown Daily Secret Gift Notes
        Card(
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
            shape = MaterialTheme.shapes.large,
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(PixelGiftSpacing.lg),
                verticalArrangement = Arrangement.spacedBy(PixelGiftSpacing.md)
            ) {
                Text("Daily Secret Countdown Notes (Jan 18 - Jan 24)", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                Text("Tap any secret message to display and scroll it across the 10x10 matrix.", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)

                countdownNotes.forEach { (day, note) ->
                    Card(
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                        shape = MaterialTheme.shapes.medium,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(PixelGiftSpacing.md),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(day, style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                                Text(note, style = MaterialTheme.typography.bodySmall)
                            }
                            IconButton(onClick = { onSendSecretNote(note) }) {
                                Icon(Icons.Default.Send, contentDescription = "Send Note", tint = MaterialTheme.colorScheme.primary)
                            }
                        }
                    }
                }
            }
        }
    }
}
