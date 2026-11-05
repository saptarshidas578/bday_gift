package com.pixelgift.app.ui.screens.studio

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import com.pixelgift.app.protocol.Rgb
import com.pixelgift.app.ui.color.PaintColorWheel
import com.pixelgift.app.ui.theme.PixelGiftSpacing

@Composable
fun ColorWheelSection(
    selectedColor: Rgb,
    onColorChanged: (Rgb) -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        shape = MaterialTheme.shapes.extraLarge,
        modifier = modifier.fillMaxWidth()
    ) {
        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier
                .fillMaxWidth()
                .padding(PixelGiftSpacing.sm)
        ) {
            PaintColorWheel(
                selectedRgb = selectedColor,
                onColorChanged = onColorChanged
            )
        }
    }
}

