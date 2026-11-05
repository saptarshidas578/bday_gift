package com.pixelgift.app.ui.screens.studio

import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import com.pixelgift.app.R
import com.pixelgift.app.protocol.Rgb
import com.pixelgift.app.ui.components.ColorChip
import com.pixelgift.app.ui.theme.PixelGiftSpacing

@Composable
fun FavoritesRecentSection(
    favoriteColors: List<Rgb>,
    recentColors: List<Rgb>,
    selectedColor: Rgb,
    onFavoriteSelected: (Rgb) -> Unit,
    onRecentColorSelected: (Rgb) -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(PixelGiftSpacing.lg)
    ) {
        // Favorites Row
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
                            ColorChip(
                                color = color,
                                isSelected = color == selectedColor,
                                onClick = { onFavoriteSelected(color) }
                            )
                        }
                    }
                }
            }
        }

        // Recent Colors Row
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
                        ColorChip(
                            color = color,
                            isSelected = color == selectedColor,
                            onClick = { onRecentColorSelected(color) }
                        )
                    }
                }
            }
        }
    }
}

