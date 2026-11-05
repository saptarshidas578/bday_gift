package com.pixelgift.app.ui.screens.studio

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowDownward
import androidx.compose.material.icons.filled.ArrowUpward
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Upload
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.pixelgift.app.R
import com.pixelgift.app.data.model.NamedPalette
import com.pixelgift.app.protocol.Rgb
import com.pixelgift.app.ui.color.ColorMath
import com.pixelgift.app.ui.theme.PixelGiftSpacing
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

@Composable
fun PalettesSection(
    palettes: List<NamedPalette>,
    selectedColor: Rgb,
    onPaletteSelected: (NamedPalette) -> Unit,
    onCreatePalette: (name: String, colors: List<Rgb>) -> Unit,
    onRenamePalette: (id: String, newName: String) -> Unit,
    onDeletePalette: (id: String) -> Unit,
    onReorderPalettes: (List<NamedPalette>) -> Unit,
    onAddColorToPalette: (id: String) -> Unit,
    onExportJson: suspend () -> String,
    onImportJson: suspend (String) -> Boolean,
    modifier: Modifier = Modifier
) {
    val clipboardManager = LocalClipboardManager.current
    val coroutineScope = rememberCoroutineScope()

    var showCreatePaletteDialog by remember { mutableStateOf(false) }
    var newPaletteName by remember { mutableStateOf("") }
    var renamePaletteTarget by remember { mutableStateOf<NamedPalette?>(null) }
    var renamePaletteText by remember { mutableStateOf("") }
    var showJsonSyncDialog by remember { mutableStateOf(false) }
    var jsonSyncText by remember { mutableStateOf("") }
    var jsonSyncStatus by remember { mutableStateOf<String?>(null) }

    Card(
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
        shape = MaterialTheme.shapes.large,
        modifier = modifier.fillMaxWidth()
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

