package com.pixelgift.app.data.model

/**
 * Flags indicating which runtime parameters this effect supports.
 */
data class EffectSupports(
    val dir: Boolean,
    val mirror: Boolean,
    val colors: Boolean,
    val palette: Boolean
)

/**
 * Default parameters for this effect.
 */
data class EffectDefaults(
    val speed: Int,
    val dir: Int,
    val mirror: Int,
    val colorMode: Int
)

/**
 * Catalog entry for a firmware base effect from protocol/effects.json.
 */
data class EffectItem(
    val id: Int,
    val name: String,
    val category: String,
    val supports: EffectSupports,
    val defaults: EffectDefaults
)

/**
 * Runtime configuration state for playing an effect.
 */
data class EffectConfig(
    val effect: EffectItem,
    val speed: Int = effect.defaults.speed,
    val dir: Int = effect.defaults.dir,
    val mirror: Int = effect.defaults.mirror,
    val colorMode: Int = effect.defaults.colorMode
)
