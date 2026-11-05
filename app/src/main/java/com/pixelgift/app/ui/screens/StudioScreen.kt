package com.pixelgift.app.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import com.pixelgift.app.data.model.NamedPalette
import com.pixelgift.app.protocol.Rgb
import com.pixelgift.app.ui.screens.studio.BrightnessSection
import com.pixelgift.app.ui.screens.studio.ColorWheelSection
import com.pixelgift.app.ui.screens.studio.FavoritesRecentSection
import com.pixelgift.app.ui.screens.studio.PalettesSection
import com.pixelgift.app.ui.screens.studio.PreviewHeaderSection
import com.pixelgift.app.ui.screens.studio.RgbHexInputSection
import com.pixelgift.app.ui.screens.studio.ScenesSection
import com.pixelgift.app.ui.studio.StudioViewModel
import com.pixelgift.app.ui.theme.PixelGiftSpacing

@Composable
fun StudioRoute(
    viewModel: StudioViewModel,
    modifier: Modifier = Modifier
) {
    val selectedColor by viewModel.selectedColor.collectAsState()
    val committedColor by viewModel.committedColor.collectAsState()
    val brightness by viewModel.brightness.collectAsState()
    val favoriteColors by viewModel.favoriteColors.collectAsState()
    val recentColors by viewModel.recentColors.collectAsState()
    val palettes by viewModel.palettes.collectAsState()
    val activeScene by viewModel.activeScene.collectAsState()
    val sceneFeedback by viewModel.sceneFeedback.collectAsState()
    val hexInput by viewModel.hexInput.collectAsState()
    val rInput by viewModel.rInput.collectAsState()
    val gInput by viewModel.gInput.collectAsState()
    val bInput by viewModel.bInput.collectAsState()
    val isFavorite = viewModel.isFavorite(selectedColor)

    StudioScreen(
        selectedColor = selectedColor,
        committedColor = committedColor,
        brightness = brightness,
        favoriteColors = favoriteColors,
        recentColors = recentColors,
        palettes = palettes,
        activeScene = activeScene,
        sceneFeedback = sceneFeedback,
        hexInput = hexInput,
        rInput = rInput,
        gInput = gInput,
        bInput = bInput,
        onColorDragged = { viewModel.onColorDragged(it) },
        onBrightnessChanged = { viewModel.onBrightnessChanged(it) },
        onHexChanged = { viewModel.onHexChanged(it) },
        onRgbChanged = { r, g, b -> viewModel.onRgbChannelChanged(r, g, b) },
        onRecentColorSelected = { viewModel.onRecentColorSelected(it) },
        onFavoriteSelected = { viewModel.onFavoriteSelected(it) },
        onToggleFavorite = {
            if (viewModel.isFavorite(selectedColor)) {
                viewModel.onRemoveFavorite(selectedColor)
            } else {
                viewModel.onAddFavorite(selectedColor)
            }
        },
        isFavorite = isFavorite,
        onLoadScene = { viewModel.onLoadScene(it) },
        onSaveScene = { viewModel.onSaveScene(it) },
        onClearSceneFeedback = { viewModel.clearSceneFeedback() },
        onPaletteSelected = { viewModel.onPaletteSelected(it) },
        onCreatePalette = { name, colors -> viewModel.onCreatePalette(name, colors) },
        onRenamePalette = { id, name -> viewModel.onRenamePalette(id, name) },
        onDeletePalette = { viewModel.onDeletePalette(it) },
        onReorderPalettes = { viewModel.onReorderPalettes(it) },
        onAddColorToPalette = { viewModel.onAddColorToPalette(it) },
        onExportJson = { viewModel.exportJson() },
        onImportJson = { viewModel.importJson(it) },
        onCancel = { viewModel.onCancel() },
        onOk = { viewModel.onCommitOk() },
        modifier = modifier
    )
}

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

    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(scrollState)
            .padding(horizontal = PixelGiftSpacing.lg, vertical = PixelGiftSpacing.md),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(PixelGiftSpacing.lg)
    ) {
        // 1. Live Mini Matrix & Preview Header
        PreviewHeaderSection(
            selectedColor = selectedColor,
            committedColor = committedColor,
            brightness = brightness,
            isFavorite = isFavorite,
            onToggleFavorite = onToggleFavorite,
            onCancel = onCancel,
            onOk = onOk
        )

        // 2. Brightness Slider Card
        BrightnessSection(
            brightness = brightness,
            onBrightnessChanged = onBrightnessChanged
        )

        // 3. Hardware Scenes Row
        ScenesSection(
            activeScene = activeScene,
            sceneFeedback = sceneFeedback,
            onLoadScene = onLoadScene,
            onSaveScene = onSaveScene,
            onClearSceneFeedback = onClearSceneFeedback
        )

        // 4. Custom Canvas Paint Color Wheel
        ColorWheelSection(
            selectedColor = selectedColor,
            onColorChanged = onColorDragged
        )

        // 5. HEX & RGB Exact Inputs Row
        RgbHexInputSection(
            hexInput = hexInput,
            rInput = rInput,
            gInput = gInput,
            bInput = bInput,
            onHexChanged = onHexChanged,
            onRgbChanged = onRgbChanged
        )

        // 6. Favorites & Recent Colors
        FavoritesRecentSection(
            favoriteColors = favoriteColors,
            recentColors = recentColors,
            selectedColor = selectedColor,
            onFavoriteSelected = onFavoriteSelected,
            onRecentColorSelected = onRecentColorSelected
        )

        // 7. Named Palettes Manager Section
        PalettesSection(
            palettes = palettes,
            selectedColor = selectedColor,
            onPaletteSelected = onPaletteSelected,
            onCreatePalette = onCreatePalette,
            onRenamePalette = onRenamePalette,
            onDeletePalette = onDeletePalette,
            onReorderPalettes = onReorderPalettes,
            onAddColorToPalette = onAddColorToPalette,
            onExportJson = onExportJson,
            onImportJson = onImportJson
        )
    }
}
