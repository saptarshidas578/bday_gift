package com.pixelgift.app.ui.screens.magic

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Slider
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.pixelgift.app.ui.theme.PixelGiftSpacing

@Composable
fun MessageSection(
    text: String,
    speed: Int,
    isAsciiValid: Boolean,
    feedback: String?,
    onTextChanged: (String) -> Unit,
    onSpeedChanged: (Int) -> Unit,
    onSendMessage: () -> Unit,
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
            verticalArrangement = Arrangement.spacedBy(PixelGiftSpacing.md)
        ) {
            Text("Custom Scrolling Message", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
            Text("Sends text in 14-byte chunks (target 0) and triggers SHOW_MESSAGE.", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)

            OutlinedTextField(
                value = text,
                onValueChange = onTextChanged,
                label = { Text("Message Text") },
                isError = !isAsciiValid,
                supportingText = {
                    if (!isAsciiValid) {
                        Text("Only ASCII characters (0x00..0x7F) are supported", color = MaterialTheme.colorScheme.error)
                    } else {
                        Text("${text.length} chars • ${if (text.isEmpty()) 1 else ((text.length + 13) / 14)} chunk(s)")
                    }
                },
                modifier = Modifier.fillMaxWidth()
            )

            Column {
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    Text("Scroll Speed", style = MaterialTheme.typography.labelMedium)
                    Text("$speed%", style = MaterialTheme.typography.bodySmall.copy(fontFamily = FontFamily.Monospace))
                }
                Slider(
                    value = speed.toFloat(),
                    onValueChange = { onSpeedChanged(it.toInt()) },
                    valueRange = 1f..100f
                )
            }

            Button(
                onClick = onSendMessage,
                enabled = isAsciiValid && text.isNotBlank(),
                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary),
                modifier = Modifier
                    .fillMaxWidth()
                    .defaultMinSize(minHeight = 48.dp)
            ) {
                Icon(Icons.AutoMirrored.Filled.Send, contentDescription = null)
                Spacer(modifier = Modifier.width(8.dp))
                Text("Send & Scroll Message")
            }

            feedback?.let {
                Text(it, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Bold)
            }
        }
    }
}

