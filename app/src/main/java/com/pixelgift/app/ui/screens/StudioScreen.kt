package com.pixelgift.app.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.pixelgift.app.R
import com.pixelgift.app.protocol.Rgb
import com.pixelgift.app.ui.color.ColorMath
import com.pixelgift.app.ui.color.PaintColorWheel
import com.pixelgift.app.ui.theme.PixelGiftSpacing

@Composable
fun StudioScreen(
    selectedColor: Rgb,
    committedColor: Rgb,
    recentColors: List<Rgb>,
    hexInput: String,
    rInput: String,
    gInput: String,
    bInput: String,
    onColorDragged: (Rgb) -> Unit,
    onHexChanged: (String) -> Unit,
    onRgbChanged: (r: String, g: String, b: String) -> Unit,
    onRecentColorSelected: (Rgb) -> Unit,
    onCancel: () -> Unit,
    onOk: () -> Unit,
    modifier: Modifier = Modifier
) {
    val scrollState = rememberScrollState()
    val haptic = LocalHapticFeedback.current

    val currentComposeColor = ColorMath.rgbToComposeColor(selectedColor)
    val committedComposeColor = ColorMath.rgbToComposeColor(committedColor)

    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(scrollState)
            .padding(horizontal = PixelGiftSpacing.lg, vertical = PixelGiftSpacing.md),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(PixelGiftSpacing.lg)
    ) {
        // 1. Live Color Preview Swatch Header
        Card(
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
            shape = MaterialTheme.shapes.large,
            modifier = Modifier.fillMaxWidth()
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(PixelGiftSpacing.lg)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(PixelGiftSpacing.md)
                ) {
                    // Swatch split showing committed vs active live color
                    Box(
                        modifier = Modifier
                            .size(52.dp)
                            .clip(RoundedCornerShape(12.dp))
                            .border(1.5.dp, MaterialTheme.colorScheme.outline, RoundedCornerShape(12.dp))
                    ) {
                        Row(modifier = Modifier.fillMaxSize()) {
                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .fillMaxSize()
                                    .background(committedComposeColor)
                            )
                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .fillMaxSize()
                                    .background(currentComposeColor)
                            )
                        }
                    }

                    Column {
                        Text(
                            text = stringResource(R.string.studio_color_preview_label),
                            style = MaterialTheme.typography.titleMedium,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            text = ColorMath.rgbToHex(selectedColor),
                            style = MaterialTheme.typography.bodyMedium.copy(fontFamily = FontFamily.Monospace),
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                }

                // Action Buttons: Cancel and OK
                Row(horizontalArrangement = Arrangement.spacedBy(PixelGiftSpacing.sm)) {
                    OutlinedButton(
                        onClick = onCancel,
                        modifier = Modifier
                            .defaultMinSize(minWidth = 48.dp, minHeight = 48.dp)
                            .semantics {
                                contentDescription = "Cancel and revert color"
                            }
                    ) {
                        Text(stringResource(R.string.action_cancel))
                    }

                    Button(
                        onClick = {
                            haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                            onOk()
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary),
                        modifier = Modifier
                            .defaultMinSize(minWidth = 48.dp, minHeight = 48.dp)
                            .semantics {
                                contentDescription = "OK: commit color with haptic confirmation"
                            }
                    ) {
                        Text(stringResource(R.string.action_ok))
                    }
                }
            }
        }

        // 2. Custom Canvas Paint Color Wheel
        Card(
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            shape = MaterialTheme.shapes.extraLarge,
            modifier = Modifier.fillMaxWidth()
        ) {
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(PixelGiftSpacing.sm)
            ) {
                PaintColorWheel(
                    selectedRgb = selectedColor,
                    onColorChanged = onColorDragged
                )
            }
        }

        // 3. HEX & RGB Exact Inputs Row
        Card(
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
            shape = MaterialTheme.shapes.large,
            modifier = Modifier.fillMaxWidth()
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(PixelGiftSpacing.md),
                horizontalArrangement = Arrangement.spacedBy(PixelGiftSpacing.sm),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // HEX input field
                OutlinedTextField(
                    value = hexInput,
                    onValueChange = onHexChanged,
                    label = { Text(stringResource(R.string.studio_hex_label)) },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(
                        capitalization = KeyboardCapitalization.Characters,
                        keyboardType = KeyboardType.Ascii
                    ),
                    modifier = Modifier
                        .weight(1.3f)
                        .defaultMinSize(minHeight = 48.dp)
                )

                // R channel
                OutlinedTextField(
                    value = rInput,
                    onValueChange = { onRgbChanged(it, gInput, bInput) },
                    label = { Text(stringResource(R.string.studio_r_label)) },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    modifier = Modifier
                        .weight(0.9f)
                        .defaultMinSize(minHeight = 48.dp)
                )

                // G channel
                OutlinedTextField(
                    value = gInput,
                    onValueChange = { onRgbChanged(rInput, it, bInput) },
                    label = { Text(stringResource(R.string.studio_g_label)) },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    modifier = Modifier
                        .weight(0.9f)
                        .defaultMinSize(minHeight = 48.dp)
                )

                // B channel
                OutlinedTextField(
                    value = bInput,
                    onValueChange = { onRgbChanged(rInput, gInput, it) },
                    label = { Text(stringResource(R.string.studio_b_label)) },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    modifier = Modifier
                        .weight(0.9f)
                        .defaultMinSize(minHeight = 48.dp)
                )
            }
        }

        // 4. Recent Colors Palette Row
        Card(
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
            shape = MaterialTheme.shapes.large,
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(PixelGiftSpacing.md),
                verticalArrangement = Arrangement.spacedBy(PixelGiftSpacing.sm)
            ) {
                Text(
                    text = stringResource(R.string.studio_recent_colors_title),
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onSurface
                )

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(PixelGiftSpacing.sm),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    recentColors.forEach { color ->
                        val hex = ColorMath.rgbToHex(color)
                        val composeColor = ColorMath.rgbToComposeColor(color)
                        val isSelected = color == selectedColor

                        Box(
                            contentAlignment = Alignment.Center,
                            modifier = Modifier
                                .size(48.dp)
                                .clip(CircleShape)
                                .background(composeColor)
                                .border(
                                    width = if (isSelected) 3.dp else 1.dp,
                                    color = if (isSelected) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.outline,
                                    shape = CircleShape
                                )
                                .clickable(
                                    role = Role.Button,
                                    onClick = { onRecentColorSelected(color) }
                                )
                                .semantics {
                                    contentDescription = "Select color $hex"
                                }
                        ) {
                            if (isSelected) {
                                Box(
                                    modifier = Modifier
                                        .size(8.dp)
                                        .clip(CircleShape)
                                        .background(if (color.r + color.g + color.b > 380) Color.Black else Color.White)
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}
