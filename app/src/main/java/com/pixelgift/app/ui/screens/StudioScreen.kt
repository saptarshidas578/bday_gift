package com.pixelgift.app.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.horizontalScroll
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
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowDownward
import androidx.compose.material.icons.filled.ArrowUpward
import androidx.compose.material.icons.filled.BrightnessHigh
import androidx.compose.material.icons.filled.BrightnessLow
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material.icons.filled.Save
import androidx.compose.material.icons.filled.SwapVert
import androidx.compose.material.icons.filled.Upload
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.AssistChip
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Slider
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.pixelgift.app.R
import com.pixelgift.app.data.model.NamedPalette
import com.pixelgift.app.protocol.Rgb
import com.pixelgift.app.ui.color.ColorMath
import com.pixelgift.app.ui.color.PaintColorWheel
import com.pixelgift.app.ui.matrix.MiniMatrixPreview
import com.pixelgift.app.ui.theme.PixelGiftSpacing
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlin.math.roundToInt

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun StudioScreen(
    selectedColor: Rgb,
    committedColor: Rgb,
    brightness: Int,
    favoriteColors: List<Rgb>,
    recentColors: List<Rgb>,
    palettes: List<NamedPalette>,
    activeScene: Int?,
    sceneFeedback: String?,
    hexInput: String,
    rInput: String,
    gInput: String,
    bInput: String,
    onColorDragged: (Rgb) -> Unit,
    onBrightnessChanged: (Int) -> Unit,
    onHexChanged: (String) -> Unit,
    onRgbChanged: (r: String, g: String, b: String) -> Unit,
    onRecentColorSelected: (Rgb) -> Unit,
    onFavoriteSelected: (Rgb) -> Unit,
    onToggleFavorite: () -> Unit,
    isFavorite: Boolean,
    onLoadScene: (Int) -> Unit,
    onSaveScene: (Int) -> Unit,
    onClearSceneFeedback: () -> Unit,
    onPaletteSelected: (NamedPalette) -> Unit,
    onCreatePalette: (name: String, colors: List<Rgb>) -> Unit,
    onRenamePalette: (id: String, newName: String) -> Unit,
    onDeletePalette: (id: String) -> Unit,
    onReorderPalettes: (List<NamedPalette>) -> Unit,
    onAddColorToPalette: (id: String) -> Unit,
    onExportJson: suspend () -> String,
    onImportJson: suspend (String) -> Boolean,
    onCancel: () -> Unit,
    onOk: () -> Unit,
    modifier: Modifier = Modifier
) {
    val scrollState = rememberScrollState()
    val haptic = LocalHapticFeedback.current
    val clipboardManager = LocalClipboardManager.current
    val coroutineScope = rememberCoroutineScope()

    // Dialog state
    var showCreatePaletteDialog by remember { mutableStateOf(false) }
    var newPaletteName by remember { mutableStateOf("") }
    var renamePaletteTarget by remember { mutableStateOf<NamedPalette?>(null) }
    var renamePaletteText by remember { mutableStateOf("") }
    var showJsonSyncDialog by remember { mutableStateOf(false) }
    var jsonSyncText by remember { mutableStateOf("") }
    var jsonSyncStatus by remember { mutableStateOf<String?>(null) }

    // Auto-dismiss scene feedback after 3s
    LaunchedEffect(sceneFeedback) {
        if (sceneFeedback != null) {
            delay(3000)
            onClearSceneFeedback()
        }
    }

    val currentComposeColor = ColorMath.rgbToComposeColor(selectedColor)
    val committedComposeColor = ColorMath.rgbToComposeColor(committedColor)

    val cancelCd = stringResource(R.string.cd_action_cancel)
    val okCd = stringResource(R.string.cd_action_ok)
    val hexCd = stringResource(R.string.cd_hex_input)
    val rCd = stringResource(R.string.cd_r_input)
    val gCd = stringResource(R.string.cd_g_input)
    val bCd = stringResource(R.string.cd_b_input)

    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(scrollState)
            .padding(horizontal = PixelGiftSpacing.lg, vertical = PixelGiftSpacing.md),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(PixelGiftSpacing.lg)
    ) {
        // 1. Live 9x9 Mini Matrix & Control Header
        Card(
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
            shape = MaterialTheme.shapes.large,
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(PixelGiftSpacing.md),
                verticalArrangement = Arrangement.spacedBy(PixelGiftSpacing.md)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    // Mini Matrix Preview
                    MiniMatrixPreview(
                        color = selectedColor,
                        brightness = brightness
                    )

                    // Color Info + Favorite Toggle + Actions
                    Column(
                        modifier = Modifier
                            .weight(1f)
                            .padding(start = PixelGiftSpacing.md),
                        verticalArrangement = Arrangement.spacedBy(PixelGiftSpacing.sm)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column {
                                Text(
                                    text = ColorMath.rgbToHex(selectedColor),
                                    style = MaterialTheme.typography.titleLarge.copy(fontFamily = FontFamily.Monospace),
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.primary
                                )
                                Text(
                                    text = "RGB(${selectedColor.r}, ${selectedColor.g}, ${selectedColor.b})",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }

                            // Favorite Toggle Heart
                            IconButton(
                                onClick = {
                                    haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                    onToggleFavorite()
                                },
                                modifier = Modifier
                                    .defaultMinSize(minWidth = 48.dp, minHeight = 48.dp)
                                    .semantics { contentDescription = "Toggle favorite color" }
                            ) {
                                Icon(
                                    imageVector = if (isFavorite) Icons.Default.Favorite else Icons.Default.FavoriteBorder,
                                    contentDescription = null,
                                    tint = if (isFavorite) Color(0xFFFF2D55) else MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }

                        // Split Swatch (Committed vs Current)
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(28.dp)
                                .clip(RoundedCornerShape(8.dp))
                                .border(1.dp, MaterialTheme.colorScheme.outline, RoundedCornerShape(8.dp))
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

                        // Cancel and OK Buttons
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(PixelGiftSpacing.sm)
                        ) {
                            OutlinedButton(
                                onClick = onCancel,
                                modifier = Modifier
                                    .weight(1f)
                                    .defaultMinSize(minHeight = 48.dp)
                                    .semantics { contentDescription = cancelCd }
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
                                    .weight(1f)
                                    .defaultMinSize(minHeight = 48.dp)
                                    .semantics { contentDescription = okCd }
                            ) {
                                Text(stringResource(R.string.action_ok))
                            }
                        }
                    }
                }
            }
        }

        // 2. Brightness Slider Card
        Card(
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
            shape = MaterialTheme.shapes.large,
            modifier = Modifier.fillMaxWidth()
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

        // 3. Hardware Scenes Row (8 slots: 0..7)
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
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = stringResource(R.string.studio_scenes_title),
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.SemiBold
                    )
                    Text(
                        text = "Tap: Load • Long-press: Save",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                AnimatedVisibility(
                    visible = sceneFeedback != null,
                    enter = fadeIn(),
                    exit = fadeOut()
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(8.dp))
                            .background(MaterialTheme.colorScheme.primaryContainer)
                            .padding(horizontal = PixelGiftSpacing.md, vertical = 6.dp)
                    ) {
                        Text(
                            text = sceneFeedback ?: "",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onPrimaryContainer,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(PixelGiftSpacing.sm)
                ) {
                    for (slot in 0..7) {
                        val isSelected = activeScene == slot
                        Box(
                            modifier = Modifier
                                .defaultMinSize(minWidth = 64.dp, minHeight = 48.dp)
                                .clip(RoundedCornerShape(12.dp))
                                .background(
                                    if (isSelected) MaterialTheme.colorScheme.primary
                                    else MaterialTheme.colorScheme.surface
                                )
                                .border(
                                    1.dp,
                                    if (isSelected) MaterialTheme.colorScheme.primary
                                    else MaterialTheme.colorScheme.outlineVariant,
                                    RoundedCornerShape(12.dp)
                                )
                                .combinedClickable(
                                    onClick = {
                                        haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                        onLoadScene(slot)
                                    },
                                    onLongClick = {
                                        haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                        onSaveScene(slot)
                                    }
                                )
                                .padding(horizontal = 12.dp, vertical = 8.dp)
                                .semantics {
                                    contentDescription = "Scene slot ${slot + 1}. Tap to load, long press to save."
                                },
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = "S${slot + 1}",
                                style = MaterialTheme.typography.labelMedium,
                                fontWeight = FontWeight.Bold,
                                color = if (isSelected) MaterialTheme.colorScheme.onPrimary
                                else MaterialTheme.colorScheme.onSurface
                            )
                        }
                    }
                }
            }
        }

        // 4. Custom Canvas Paint Color Wheel
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

        // 5. HEX & RGB Exact Inputs Row
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
                        .semantics { contentDescription = hexCd }
                )

                OutlinedTextField(
                    value = rInput,
                    onValueChange = { onRgbChanged(it, gInput, bInput) },
                    label = { Text(stringResource(R.string.studio_r_label)) },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    modifier = Modifier
                        .weight(1f)
                        .semantics { contentDescription = rCd }
                )

                OutlinedTextField(
                    value = gInput,
                    onValueChange = { onRgbChanged(rInput, it, bInput) },
                    label = { Text(stringResource(R.string.studio_g_label)) },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    modifier = Modifier
                        .weight(1f)
                        .semantics { contentDescription = gCd }
                )

                OutlinedTextField(
                    value = bInput,
                    onValueChange = { onRgbChanged(rInput, gInput, it) },
                    label = { Text(stringResource(R.string.studio_b_label)) },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    modifier = Modifier
                        .weight(1f)
                        .semantics { contentDescription = bCd }
                )
            }
        }

        // 6. Favorites Row
        if (favoriteColors.isNotEmpty()) {
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
                        text = stringResource(R.string.studio_favorites_title),
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.SemiBold
                    )
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(PixelGiftSpacing.md)
                    ) {
                        for (color in favoriteColors) {
                            ColorCircleChip(
                                rgb = color,
                                isSelected = color == selectedColor,
                                onClick = { onFavoriteSelected(color) }
                            )
                        }
                    }
                }
            }
        }

        // 7. Recent Colors Row
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
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.SemiBold
                )
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(PixelGiftSpacing.md)
                ) {
                    for (color in recentColors) {
                        ColorCircleChip(
                            rgb = color,
                            isSelected = color == selectedColor,
                            onClick = { onRecentColorSelected(color) }
                        )
                    }
                }
            }
        }

        // 8. Named Palettes Manager Section
        Card(
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
            shape = MaterialTheme.shapes.large,
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(PixelGiftSpacing.md),
                verticalArrangement = Arrangement.spacedBy(PixelGiftSpacing.md)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = stringResource(R.string.studio_palettes_title),
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold
                    )

                    Row(horizontalArrangement = Arrangement.spacedBy(PixelGiftSpacing.xs)) {
                        TextButton(
                            onClick = {
                                jsonSyncStatus = null
                                coroutineScope.launch {
                                    jsonSyncText = onExportJson()
                                    showJsonSyncDialog = true
                                }
                            }
                        ) {
                            Icon(Icons.Default.Upload, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(stringResource(R.string.studio_btn_export_import), style = MaterialTheme.typography.labelMedium)
                        }

                        Button(
                            onClick = {
                                newPaletteName = ""
                                showCreatePaletteDialog = true
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
                        ) {
                            Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(stringResource(R.string.studio_btn_new_palette), style = MaterialTheme.typography.labelMedium)
                        }
                    }
                }

                // Palettes list
                Column(verticalArrangement = Arrangement.spacedBy(PixelGiftSpacing.sm)) {
                    palettes.forEachIndexed { index, palette ->
                        PaletteItemCard(
                            palette = palette,
                            isFirst = index == 0,
                            isLast = index == palettes.size - 1,
                            onSelect = { onPaletteSelected(palette) },
                            onAddCurrentColor = { onAddColorToPalette(palette.id) },
                            onRename = {
                                renamePaletteTarget = palette
                                renamePaletteText = palette.name
                            },
                            onDelete = { onDeletePalette(palette.id) },
                            onMoveUp = {
                                if (index > 0) {
                                    val list = palettes.toMutableList()
                                    val temp = list[index]
                                    list[index] = list[index - 1]
                                    list[index - 1] = temp
                                    onReorderPalettes(list)
                                }
                            },
                            onMoveDown = {
                                if (index < palettes.size - 1) {
                                    val list = palettes.toMutableList()
                                    val temp = list[index]
                                    list[index] = list[index + 1]
                                    list[index + 1] = temp
                                    onReorderPalettes(list)
                                }
                            }
                        )
                    }
                }
            }
        }
    }

    // Dialog: Create Palette
    if (showCreatePaletteDialog) {
        AlertDialog(
            onDismissRequest = { showCreatePaletteDialog = false },
            title = { Text(stringResource(R.string.studio_dialog_create_palette)) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(PixelGiftSpacing.md)) {
                    Text(
                        text = "Creates a palette with the currently chosen color (${ColorMath.rgbToHex(selectedColor)}). You can add more colors anytime.",
                        style = MaterialTheme.typography.bodyMedium
                    )
                    OutlinedTextField(
                        value = newPaletteName,
                        onValueChange = { newPaletteName = it },
                        label = { Text(stringResource(R.string.studio_dialog_palette_name)) },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        onCreatePalette(newPaletteName, listOf(selectedColor))
                        showCreatePaletteDialog = false
                    }
                ) {
                    Text(stringResource(R.string.studio_btn_save))
                }
            },
            dismissButton = {
                TextButton(onClick = { showCreatePaletteDialog = false }) {
                    Text(stringResource(R.string.action_cancel))
                }
            }
        )
    }

    // Dialog: Rename Palette
    renamePaletteTarget?.let { target ->
        AlertDialog(
            onDismissRequest = { renamePaletteTarget = null },
            title = { Text(stringResource(R.string.studio_dialog_rename_palette)) },
            text = {
                OutlinedTextField(
                    value = renamePaletteText,
                    onValueChange = { renamePaletteText = it },
                    label = { Text(stringResource(R.string.studio_dialog_palette_name)) },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        onRenamePalette(target.id, renamePaletteText)
                        renamePaletteTarget = null
                    }
                ) {
                    Text(stringResource(R.string.studio_btn_save))
                }
            },
            dismissButton = {
                TextButton(onClick = { renamePaletteTarget = null }) {
                    Text(stringResource(R.string.action_cancel))
                }
            }
        )
    }

    // Dialog: Export / Import JSON
    if (showJsonSyncDialog) {
        AlertDialog(
            onDismissRequest = { showJsonSyncDialog = false },
            title = { Text(stringResource(R.string.studio_dialog_export_title)) },
            text = {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(300.dp)
                        .verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(PixelGiftSpacing.sm)
                ) {
                    Text(
                        text = "Copy this JSON to back up palettes and settings, or paste JSON here and tap Import.",
                        style = MaterialTheme.typography.bodySmall
                    )

                    jsonSyncStatus?.let { status ->
                        Text(
                            text = status,
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.primary,
                            fontWeight = FontWeight.Bold
                        )
                    }

                    OutlinedTextField(
                        value = jsonSyncText,
                        onValueChange = { jsonSyncText = it },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(200.dp),
                        textStyle = MaterialTheme.typography.bodySmall.copy(fontFamily = FontFamily.Monospace)
                    )
                }
            },
            confirmButton = {
                Row(horizontalArrangement = Arrangement.spacedBy(PixelGiftSpacing.xs)) {
                    OutlinedButton(
                        onClick = {
                            clipboardManager.setText(AnnotatedString(jsonSyncText))
                            jsonSyncStatus = "JSON copied to clipboard!"
                        }
                    ) {
                        Text(stringResource(R.string.studio_btn_export))
                    }

                    Button(
                        onClick = {
                            coroutineScope.launch {
                                val success = onImportJson(jsonSyncText)
                                jsonSyncStatus = if (success) "Imported successfully!" else "Import failed: invalid JSON format"
                                if (success) {
                                    delay(800)
                                    showJsonSyncDialog = false
                                }
                            }
                        }
                    ) {
                        Text(stringResource(R.string.studio_btn_import))
                    }
                }
            },
            dismissButton = {
                TextButton(onClick = { showJsonSyncDialog = false }) {
                    Text(stringResource(R.string.action_cancel))
                }
            }
        )
    }
}

@Composable
private fun ColorCircleChip(
    rgb: Rgb,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    val composeColor = ColorMath.rgbToComposeColor(rgb)
    Box(
        modifier = Modifier
            .size(48.dp)
            .clip(CircleShape)
            .background(composeColor)
            .border(
                width = if (isSelected) 3.dp else 1.dp,
                color = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outlineVariant,
                shape = CircleShape
            )
            .clickable(onClick = onClick)
            .semantics { contentDescription = "Select color ${ColorMath.rgbToHex(rgb)}" },
        contentAlignment = Alignment.Center
    ) {
        if (isSelected) {
            Box(
                modifier = Modifier
                    .size(10.dp)
                    .clip(CircleShape)
                    .background(Color.White)
            )
        }
    }
}

@Composable
private fun PaletteItemCard(
    palette: NamedPalette,
    isFirst: Boolean,
    isLast: Boolean,
    onSelect: () -> Unit,
    onAddCurrentColor: () -> Unit,
    onRename: () -> Unit,
    onDelete: () -> Unit,
    onMoveUp: () -> Unit,
    onMoveDown: () -> Unit
) {
    Card(
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        shape = MaterialTheme.shapes.medium,
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onSelect)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(PixelGiftSpacing.md),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(PixelGiftSpacing.xs)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(PixelGiftSpacing.xs)
                ) {
                    Text(
                        text = palette.name,
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold
                    )
                    IconButton(onClick = onRename, modifier = Modifier.size(28.dp)) {
                        Icon(Icons.Default.Edit, contentDescription = "Rename palette", modifier = Modifier.size(16.dp))
                    }
                    IconButton(onClick = onDelete, modifier = Modifier.size(28.dp)) {
                        Icon(Icons.Default.Delete, contentDescription = "Delete palette", modifier = Modifier.size(16.dp))
                    }
                }

                // Swatches row
                Row(
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    palette.colors.forEach { c ->
                        Box(
                            modifier = Modifier
                                .size(24.dp)
                                .clip(RoundedCornerShape(6.dp))
                                .background(ColorMath.rgbToComposeColor(c))
                                .border(1.dp, Color(0x33FFFFFF), RoundedCornerShape(6.dp))
                        )
                    }

                    if (palette.colors.size < 8) {
                        IconButton(
                            onClick = onAddCurrentColor,
                            modifier = Modifier.size(24.dp)
                        ) {
                            Icon(Icons.Default.Add, contentDescription = "Add color to palette", modifier = Modifier.size(16.dp))
                        }
                    }
                }
            }

            // Reorder controls
            Column(
                verticalArrangement = Arrangement.spacedBy(2.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                IconButton(
                    onClick = onMoveUp,
                    enabled = !isFirst,
                    modifier = Modifier.size(28.dp)
                ) {
                    Icon(Icons.Default.ArrowUpward, contentDescription = "Move palette up", modifier = Modifier.size(18.dp))
                }
                IconButton(
                    onClick = onMoveDown,
                    enabled = !isLast,
                    modifier = Modifier.size(28.dp)
                ) {
                    Icon(Icons.Default.ArrowDownward, contentDescription = "Move palette down", modifier = Modifier.size(18.dp))
                }
            }
        }
    }
}
