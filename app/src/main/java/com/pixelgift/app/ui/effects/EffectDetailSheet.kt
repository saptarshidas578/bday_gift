package com.pixelgift.app.ui.effects

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Slider
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.pixelgift.app.data.model.EffectItem
import com.pixelgift.app.data.model.NamedPalette
import com.pixelgift.app.protocol.Rgb
import com.pixelgift.app.ui.color.ColorMath
import com.pixelgift.app.ui.theme.PixelGiftSpacing

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EffectDetailSheet(
    effect: EffectItem,
    speed: Int,
    dir: Int,
    mirror: Boolean,
    colorMode: Int,
    c1: Rgb,
    c2: Rgb,
    selectedPalette: NamedPalette?,
    availablePalettes: List<NamedPalette>,
    onSpeedChanged: (Int) -> Unit,
    onDirChanged: (Int) -> Unit,
    onMirrorChanged: (Boolean) -> Unit,
    onColorModeChanged: (Int) -> Unit,
    onC1Changed: (Rgb) -> Unit,
    onC2Changed: (Rgb) -> Unit,
    onPaletteSelected: (NamedPalette) -> Unit,
    onApply: () -> Unit,
    onDismiss: () -> Unit
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    val directions = listOf(
        0 to "Left",
        1 to "Right",
        2 to "Up",
        3 to "Down",
        4 to "In",
        5 to "Out"
    )

    val presetColors = listOf(
        Rgb(255, 122, 0),
        Rgb(0, 229, 255),
        Rgb(255, 45, 85),
        Rgb(0, 230, 118),
        Rgb(158, 119, 237),
        Rgb(255, 214, 0),
        Rgb(255, 255, 255)
    )

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = MaterialTheme.colorScheme.surface
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = PixelGiftSpacing.lg)
                .padding(bottom = 32.dp),
            verticalArrangement = Arrangement.spacedBy(PixelGiftSpacing.md)
        ) {
            // Header
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(PixelGiftSpacing.md)
                ) {
                    AnimatedGradientChip(
                        category = effect.category,
                        modifier = Modifier.size(52.dp)
                    )
                    Column {
                        Text(
                            text = effect.name,
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "${effect.category} • ID #${effect.id}",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                IconButton(onClick = onDismiss) {
                    Icon(Icons.Default.Close, contentDescription = "Close sheet")
                }
            }

            // Speed Control (Always Supported)
            Card(
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                shape = MaterialTheme.shapes.medium
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(PixelGiftSpacing.md)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text("Speed", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold)
                        Text("$speed%", style = MaterialTheme.typography.bodyMedium.copy(fontFamily = FontFamily.Monospace))
                    }
                    Slider(
                        value = speed.toFloat(),
                        onValueChange = { onSpeedChanged(it.toInt()) },
                        valueRange = 0f..100f,
                        modifier = Modifier
                            .fillMaxWidth()
                            .semantics { contentDescription = "Effect speed slider $speed percent" }
                    )
                }
            }

            // Direction Control (ONLY if supported per SPEC)
            if (effect.supports.dir) {
                Card(
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                    shape = MaterialTheme.shapes.medium,
                    modifier = Modifier.testTag("direction_control")
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(PixelGiftSpacing.md),
                        verticalArrangement = Arrangement.spacedBy(PixelGiftSpacing.xs)
                    ) {
                        Text("Direction", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold)
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .horizontalScroll(rememberScrollState()),
                            horizontalArrangement = Arrangement.spacedBy(PixelGiftSpacing.sm)
                        ) {
                            directions.forEach { (dirValue, dirLabel) ->
                                FilterChip(
                                    selected = dir == dirValue,
                                    onClick = { onDirChanged(dirValue) },
                                    label = { Text(dirLabel) },
                                    colors = FilterChipDefaults.filterChipColors(
                                        selectedContainerColor = MaterialTheme.colorScheme.primary,
                                        selectedLabelColor = MaterialTheme.colorScheme.onPrimary
                                    )
                                )
                            }
                        }
                    }
                }
            }

            // Mirror Control (ONLY if supported per SPEC)
            if (effect.supports.mirror) {
                Card(
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                    shape = MaterialTheme.shapes.medium,
                    modifier = Modifier.testTag("mirror_control")
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(PixelGiftSpacing.md),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column {
                            Text("Mirror Animation", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold)
                            Text("Symmetrical bilateral rendering", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                        Switch(
                            checked = mirror,
                            onCheckedChange = onMirrorChanged,
                            modifier = Modifier.semantics { contentDescription = "Toggle mirror animation" }
                        )
                    }
                }
            }

            // Color Mode & Palette Controls (ONLY if supported per SPEC)
            if (effect.supports.colors || effect.supports.palette) {
                Card(
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                    shape = MaterialTheme.shapes.medium,
                    modifier = Modifier.testTag("color_mode_control")
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(PixelGiftSpacing.md),
                        verticalArrangement = Arrangement.spacedBy(PixelGiftSpacing.sm)
                    ) {
                        Text("Color Mode", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold)

                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .horizontalScroll(rememberScrollState()),
                            horizontalArrangement = Arrangement.spacedBy(PixelGiftSpacing.sm)
                        ) {
                            if (effect.supports.colors) {
                                FilterChip(
                                    selected = colorMode == 0,
                                    onClick = { onColorModeChanged(0) },
                                    label = { Text("Single") }
                                )
                                FilterChip(
                                    selected = colorMode == 1,
                                    onClick = { onColorModeChanged(1) },
                                    label = { Text("Dual") }
                                )
                            }
                            if (effect.supports.palette) {
                                FilterChip(
                                    selected = colorMode == 2,
                                    onClick = { onColorModeChanged(2) },
                                    label = { Text("Palette") }
                                )
                            }
                            FilterChip(
                                selected = colorMode == 3,
                                onClick = { onColorModeChanged(3) },
                                label = { Text("Rainbow") }
                            )
                        }

                        // Color 1 selection
                        if (colorMode in 0..1 && effect.supports.colors) {
                            Text("Primary Color (${ColorMath.rgbToHex(c1)})", style = MaterialTheme.typography.labelMedium)
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .horizontalScroll(rememberScrollState()),
                                horizontalArrangement = Arrangement.spacedBy(PixelGiftSpacing.sm)
                            ) {
                                presetColors.forEach { rgb ->
                                    ColorDot(
                                        rgb = rgb,
                                        isSelected = c1 == rgb,
                                        onClick = { onC1Changed(rgb) }
                                    )
                                }
                            }
                        }

                        // Color 2 selection
                        if (colorMode == 1 && effect.supports.colors) {
                            Spacer(modifier = Modifier.height(4.dp))
                            Text("Secondary Color (${ColorMath.rgbToHex(c2)})", style = MaterialTheme.typography.labelMedium)
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .horizontalScroll(rememberScrollState()),
                                horizontalArrangement = Arrangement.spacedBy(PixelGiftSpacing.sm)
                            ) {
                                presetColors.forEach { rgb ->
                                    ColorDot(
                                        rgb = rgb,
                                        isSelected = c2 == rgb,
                                        onClick = { onC2Changed(rgb) }
                                    )
                                }
                            }
                        }

                        // Palette selection
                        if (colorMode == 2 && effect.supports.palette) {
                            Spacer(modifier = Modifier.height(4.dp))
                            Text("Choose Palette", style = MaterialTheme.typography.labelMedium)
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .horizontalScroll(rememberScrollState()),
                                horizontalArrangement = Arrangement.spacedBy(PixelGiftSpacing.sm)
                            ) {
                                availablePalettes.forEach { p ->
                                    FilterChip(
                                        selected = selectedPalette?.id == p.id,
                                        onClick = { onPaletteSelected(p) },
                                        label = { Text(p.name) }
                                    )
                                }
                            }
                        }
                    }
                }
            }

            // Apply Button
            Button(
                onClick = {
                    onApply()
                    onDismiss()
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .defaultMinSize(minHeight = 48.dp),
                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
            ) {
                Icon(Icons.Default.PlayArrow, contentDescription = null)
                Spacer(modifier = Modifier.width(8.dp))
                Text("Play Effect Now", style = MaterialTheme.typography.titleMedium)
            }
        }
    }
}

@Composable
private fun ColorDot(
    rgb: Rgb,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    Box(
        modifier = Modifier
            .size(36.dp)
            .clip(CircleShape)
            .background(ColorMath.rgbToComposeColor(rgb))
            .border(
                width = if (isSelected) 3.dp else 1.dp,
                color = if (isSelected) MaterialTheme.colorScheme.primary else Color(0x33FFFFFF),
                shape = CircleShape
            )
            .clickable(onClick = onClick)
    )
}
