package com.pixelgift.app.ui.screens.studio

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.BrightnessHigh
import androidx.compose.material.icons.filled.BrightnessLow
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import com.pixelgift.app.R
import com.pixelgift.app.ui.theme.PixelGiftSpacing
import kotlin.math.roundToInt

@Composable
fun BrightnessSection(
    brightness: Int,
    onBrightnessChanged: (Int) -> Unit,
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
                .padding(horizontal = PixelGiftSpacing.lg, vertical = PixelGiftSpacing.md)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = stringResource(R.string.studio_brightness_title),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold
                )
                Text(
                    text = "${((brightness / 255f) * 100).roundToInt()}% ($brightness)",
                    style = MaterialTheme.typography.bodyMedium.copy(fontFamily = FontFamily.Monospace),
                    color = MaterialTheme.colorScheme.primary
                )
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(PixelGiftSpacing.sm)
            ) {
                Icon(
                    imageVector = Icons.Default.BrightnessLow,
                    contentDescription = "Low Brightness",
                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                )

                Slider(
                    value = brightness.toFloat(),
                    onValueChange = { onBrightnessChanged(it.roundToInt()) },
                    valueRange = 0f..255f,
                    modifier = Modifier
                        .weight(1f)
                        .semantics {
                            contentDescription = "Matrix brightness slider: ${((brightness / 255f) * 100).roundToInt()} percent"
                        }
                )

                Icon(
                    imageVector = Icons.Default.BrightnessHigh,
                    contentDescription = "High Brightness",
                    tint = MaterialTheme.colorScheme.primary
                )
            }
        }
    }
}

