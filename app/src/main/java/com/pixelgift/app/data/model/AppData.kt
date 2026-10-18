package com.pixelgift.app.data.model

import com.pixelgift.app.protocol.Rgb

/**
 * A named color palette containing 1 to 8 RGB colors (hardware cap).
 */
data class NamedPalette(
    val id: String,
    val name: String,
    val colors: List<Rgb>
) {
    init {
        require(colors.isNotEmpty()) { "Palette must contain at least 1 color" }
        require(colors.size <= 8) { "Palette can contain at most 8 colors (hardware limit)" }
    }
}

/**
 * App-side persistent state stored in DataStore per SPEC 9.5 and 11.
 */
data class AppData(
    val version: Int = 1,
    val brightness: Int = 128,
    val favoriteColors: List<Rgb> = DEFAULT_FAVORITES,
    val recentColors: List<Rgb> = DEFAULT_RECENTS,
    val palettes: List<NamedPalette> = DEFAULT_PALETTES
) {
    companion object {
        val DEFAULT_FAVORITES = listOf(
            Rgb(255, 122, 0),   // Amber
            Rgb(0, 229, 255),   // Cyan
            Rgb(255, 45, 85),   // Magenta
            Rgb(0, 230, 118),   // Spring Green
            Rgb(255, 214, 0)    // Golden Yellow
        )

        val DEFAULT_RECENTS = listOf(
            Rgb(255, 122, 0),
            Rgb(0, 229, 255),
            Rgb(158, 119, 237),
            Rgb(255, 45, 85),
            Rgb(0, 230, 118),
            Rgb(255, 214, 0),
            Rgb(41, 121, 255),
            Rgb(255, 255, 255)
        )

        val DEFAULT_PALETTES = listOf(
            NamedPalette(
                id = "palette_sunset",
                name = "Sunset Glow",
                colors = listOf(
                    Rgb(255, 69, 0),
                    Rgb(255, 140, 0),
                    Rgb(255, 215, 0),
                    Rgb(255, 20, 147)
                )
            ),
            NamedPalette(
                id = "palette_cyberpunk",
                name = "Cyberpunk Neon",
                colors = listOf(
                    Rgb(0, 255, 255),
                    Rgb(255, 0, 127),
                    Rgb(121, 40, 202),
                    Rgb(0, 255, 102)
                )
            ),
            NamedPalette(
                id = "palette_ocean",
                name = "Deep Ocean",
                colors = listOf(
                    Rgb(0, 31, 63),
                    Rgb(0, 116, 217),
                    Rgb(127, 219, 255),
                    Rgb(57, 204, 204)
                )
            ),
            NamedPalette(
                id = "palette_forest",
                name = "Forest Moss",
                colors = listOf(
                    Rgb(46, 204, 64),
                    Rgb(1, 255, 112),
                    Rgb(46, 125, 50),
                    Rgb(139, 195, 74)
                )
            )
        )
    }
}
