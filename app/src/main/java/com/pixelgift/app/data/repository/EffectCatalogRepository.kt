package com.pixelgift.app.data.repository

import android.content.Context
import com.pixelgift.app.data.model.EffectItem
import com.pixelgift.app.data.model.EffectParser

interface EffectCatalogRepository {
    fun getEffects(): List<EffectItem>
}

class AssetEffectCatalogRepository(
    private val context: Context
) : EffectCatalogRepository {

    private val cachedEffects: List<EffectItem> by lazy {
        try {
            val json = context.assets.open("effects.json").bufferedReader().use { it.readText() }
            EffectParser.fromJson(json)
        } catch (_: Exception) {
            emptyList()
        }
    }

    override fun getEffects(): List<EffectItem> = cachedEffects
}

class InMemoryEffectCatalogRepository(
    private val effects: List<EffectItem>
) : EffectCatalogRepository {
    override fun getEffects(): List<EffectItem> = effects
}
