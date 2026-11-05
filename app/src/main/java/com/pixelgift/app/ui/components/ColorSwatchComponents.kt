package com.pixelgift.app.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.pixelgift.app.data.model.NamedPalette
import com.pixelgift.app.protocol.Rgb

/**
 * Reusable circular color swatch chip with accessibility, selection state, and custom sizing.
 */
@Composable
fun ColorChip(
    color: Rgb,
    isSelected: Boolean = false,
    size: Dp = 36.dp,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val composeColor = Color(color.r, color.g, color.b)
    val borderColor = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outlineVariant
    val borderWidth = if (isSelected) 3.dp else 1.dp

    Box(
        modifier = modifier
            .size(size)
            .clip(CircleShape)
            .background(composeColor)
            .border(borderWidth, borderColor, CircleShape)
            .clickable(onClick = onClick)
            .semantics {
                contentDescription = "Color #%02X%02X%02X%s".format(color.r, color.g, color.b, if (isSelected) ", selected" else "")
            }
    )
}

/**
 * Reusable palette chip showing palette name and mini preview dots.
 */
@Composable
fun PaletteChip(
    palette: NamedPalette,
    isSelected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    FilterChip(
        selected = isSelected,
        onClick = onClick,
        label = { Text(palette.name) },
        leadingIcon = {
            Row(
                horizontalArrangement = Arrangement.spacedBy(2.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                palette.colors.take(4).forEach { rgb ->
                    Box(
                        modifier = Modifier
                            .size(8.dp)
                            .clip(CircleShape)
                            .background(Color(rgb.r, rgb.g, rgb.b))
                    )
                }
            }
        },
        colors = FilterChipDefaults.filterChipColors(
            selectedContainerColor = MaterialTheme.colorScheme.primaryContainer,
            selectedLabelColor = MaterialTheme.colorScheme.onPrimaryContainer
        ),
        modifier = modifier
    )
}

/**
 * Horizontally scrollable row of color chips.
 */
@Composable
fun PaletteColorRow(
    colors: List<Rgb>,
    selectedColor: Rgb? = null,
    onColorSelected: (Rgb) -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier.horizontalScroll(rememberScrollState()),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        colors.forEach { rgb ->
            ColorChip(
                color = rgb,
                isSelected = selectedColor == rgb,
                onClick = { onColorSelected(rgb) }
            )
        }
    }
}
