package com.pixelgift.app.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.pixelgift.app.data.model.EffectItem
import com.pixelgift.app.data.model.NamedPalette
import com.pixelgift.app.protocol.Rgb
import com.pixelgift.app.ui.effects.AnimatedGradientChip
import com.pixelgift.app.ui.effects.EffectDetailSheet
import com.pixelgift.app.ui.effects.EffectsViewModel
import com.pixelgift.app.ui.theme.PixelGiftSpacing

@Composable
fun EffectsScreen(
    viewModel: EffectsViewModel,
    availablePalettes: List<NamedPalette>,
    modifier: Modifier = Modifier
) {
    val haptic = LocalHapticFeedback.current

    val categories = viewModel.categories
    val selectedCategory by viewModel.selectedCategory.collectAsState()
    val searchQuery by viewModel.searchQuery.collectAsState()
    val filteredEffects by viewModel.filteredEffects.collectAsState()

    val selectedEffect by viewModel.selectedEffect.collectAsState()
    val speed by viewModel.speed.collectAsState()
    val dir by viewModel.dir.collectAsState()
    val mirror by viewModel.mirror.collectAsState()
    val colorMode by viewModel.colorMode.collectAsState()
    val c1 by viewModel.c1.collectAsState()
    val c2 by viewModel.c2.collectAsState()
    val activePalette by viewModel.selectedPalette.collectAsState()

    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(horizontal = PixelGiftSpacing.md, vertical = PixelGiftSpacing.sm)
    ) {
        // Search & Surprise Me Header
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(PixelGiftSpacing.sm)
        ) {
            OutlinedTextField(
                value = searchQuery,
                onValueChange = { viewModel.onSearchQueryChanged(it) },
                placeholder = { Text("Search 60+ effects…") },
                leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
                trailingIcon = {
                    if (searchQuery.isNotEmpty()) {
                        IconButton(onClick = { viewModel.onSearchQueryChanged("") }) {
                            Icon(Icons.Default.Clear, contentDescription = "Clear search")
                        }
                    }
                },
                singleLine = true,
                modifier = Modifier
                    .weight(1f)
                    .semantics { contentDescription = "Search effects catalog" }
            )

            Button(
                onClick = {
                    haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                    viewModel.onSurpriseMe()
                },
                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary),
                modifier = Modifier
                    .defaultMinSize(minHeight = 48.dp)
                    .semantics { contentDescription = "Surprise Me: play a random effect" }
            ) {
                Icon(Icons.Default.AutoAwesome, contentDescription = null, modifier = Modifier.size(18.dp))
                Spacer(modifier = Modifier.width(4.dp))
                Text("Surprise", style = MaterialTheme.typography.labelMedium)
            }
        }

        Spacer(modifier = Modifier.height(PixelGiftSpacing.sm))

        // Category Filter Chips Row
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(PixelGiftSpacing.sm)
        ) {
            categories.forEach { category ->
                FilterChip(
                    selected = selectedCategory == category,
                    onClick = { viewModel.onCategorySelected(category) },
                    label = { Text(category) },
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = MaterialTheme.colorScheme.primary,
                        selectedLabelColor = MaterialTheme.colorScheme.onPrimary
                    ),
                    modifier = Modifier.semantics { contentDescription = "Filter category $category" }
                )
            }
        }

        Spacer(modifier = Modifier.height(PixelGiftSpacing.sm))

        // Effects List
        if (filteredEffects.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(32.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "No effects found matching \"$searchQuery\"",
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(vertical = PixelGiftSpacing.sm),
                verticalArrangement = Arrangement.spacedBy(PixelGiftSpacing.sm)
            ) {
                items(filteredEffects, key = { it.id }) { effect ->
                    val isFav = viewModel.isFavorite(effect.id)
                    EffectItemCard(
                        effect = effect,
                        isFavorite = isFav,
                        onToggleFavorite = {
                            haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                            viewModel.onToggleFavorite(effect.id)
                        },
                        onClick = {
                            viewModel.onSelectEffect(effect)
                        }
                    )
                }
            }
        }
    }

    // Detail Sheet
    selectedEffect?.let { effect ->
        EffectDetailSheet(
            effect = effect,
            speed = speed,
            dir = dir,
            mirror = mirror,
            colorMode = colorMode,
            c1 = c1,
            c2 = c2,
            selectedPalette = activePalette,
            availablePalettes = availablePalettes,
            onSpeedChanged = { viewModel.onSpeedChanged(it) },
            onDirChanged = { viewModel.onDirChanged(it) },
            onMirrorChanged = { viewModel.onMirrorChanged(it) },
            onColorModeChanged = { viewModel.onColorModeChanged(it) },
            onC1Changed = { viewModel.onC1Changed(it) },
            onC2Changed = { viewModel.onC2Changed(it) },
            onPaletteSelected = { viewModel.onPaletteSelected(it) },
            onApply = { viewModel.applyEffect() },
            onDismiss = { viewModel.onDismissDetailSheet() }
        )
    }
}

@Composable
private fun EffectItemCard(
    effect: EffectItem,
    isFavorite: Boolean,
    onToggleFavorite: () -> Unit,
    onClick: () -> Unit
) {
    Card(
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
        shape = MaterialTheme.shapes.medium,
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .semantics { contentDescription = "Effect ${effect.name}, ${effect.category}" }
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(PixelGiftSpacing.md),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(PixelGiftSpacing.md),
                modifier = Modifier.weight(1f)
            ) {
                AnimatedGradientChip(
                    category = effect.category,
                    modifier = Modifier.size(54.dp)
                )

                Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                    Text(
                        text = effect.name,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = effect.category,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    // Feature tags
                    Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                        if (effect.supports.dir) FeatureTag("Dir")
                        if (effect.supports.mirror) FeatureTag("Mirror")
                        if (effect.supports.colors) FeatureTag("RGB")
                        if (effect.supports.palette) FeatureTag("Palette")
                    }
                }
            }

            IconButton(
                onClick = onToggleFavorite,
                modifier = Modifier
                    .defaultMinSize(minWidth = 48.dp, minHeight = 48.dp)
                    .semantics { contentDescription = "Toggle favorite for ${effect.name}" }
            ) {
                Icon(
                    imageVector = if (isFavorite) Icons.Default.Favorite else Icons.Default.FavoriteBorder,
                    contentDescription = null,
                    tint = if (isFavorite) Color(0xFFFF2D55) else MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

@Composable
private fun FeatureTag(text: String) {
    Surface(
        color = MaterialTheme.colorScheme.surface,
        shape = RoundedCornerShape(4.dp)
    ) {
        Text(
            text = text,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp)
        )
    }
}
