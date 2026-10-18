package com.pixelgift.app.data.repository

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.pixelgift.app.data.model.AppData
import com.pixelgift.app.data.model.AppDataJson
import com.pixelgift.app.data.model.NamedPalette
import com.pixelgift.app.protocol.Rgb
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch

private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "pixelgift_prefs")

interface AppDataRepository {
    val appData: StateFlow<AppData>

    suspend fun update(transform: (AppData) -> AppData)
    suspend fun setBrightness(brightness: Int)
    suspend fun addFavorite(rgb: Rgb)
    suspend fun removeFavorite(rgb: Rgb)
    suspend fun addRecent(rgb: Rgb)
    suspend fun savePalette(palette: NamedPalette)
    suspend fun deletePalette(paletteId: String)
    suspend fun reorderPalettes(palettes: List<NamedPalette>)
    suspend fun exportJson(): String
    suspend fun importJson(json: String): Boolean
}

class InMemoryAppDataRepository(
    initial: AppData = AppData()
) : AppDataRepository {

    private val _appData = MutableStateFlow(initial)
    override val appData: StateFlow<AppData> = _appData.asStateFlow()

    override suspend fun update(transform: (AppData) -> AppData) {
        _appData.value = transform(_appData.value)
    }

    override suspend fun setBrightness(brightness: Int) {
        update { it.copy(brightness = brightness.coerceIn(0, 255)) }
    }

    override suspend fun addFavorite(rgb: Rgb) {
        update { current ->
            if (current.favoriteColors.contains(rgb)) current
            else current.copy(favoriteColors = listOf(rgb) + current.favoriteColors)
        }
    }

    override suspend fun removeFavorite(rgb: Rgb) {
        update { current ->
            current.copy(favoriteColors = current.favoriteColors.filter { it != rgb })
        }
    }

    override suspend fun addRecent(rgb: Rgb) {
        update { current ->
            val list = mutableListOf(rgb)
            for (c in current.recentColors) {
                if (c != rgb && list.size < 12) list.add(c)
            }
            current.copy(recentColors = list)
        }
    }

    override suspend fun savePalette(palette: NamedPalette) {
        update { current ->
            val existingIdx = current.palettes.indexOfFirst { it.id == palette.id }
            val updated = current.palettes.toMutableList()
            if (existingIdx >= 0) {
                updated[existingIdx] = palette
            } else {
                updated.add(0, palette)
            }
            current.copy(palettes = updated)
        }
    }

    override suspend fun deletePalette(paletteId: String) {
        update { current ->
            current.copy(palettes = current.palettes.filter { it.id != paletteId })
        }
    }

    override suspend fun reorderPalettes(palettes: List<NamedPalette>) {
        update { it.copy(palettes = palettes) }
    }

    override suspend fun exportJson(): String {
        return AppDataJson.toJson(_appData.value)
    }

    override suspend fun importJson(json: String): Boolean {
        return try {
            val parsed = AppDataJson.fromJson(json)
            _appData.value = parsed
            true
        } catch (_: Exception) {
            false
        }
    }
}

class DataStoreAppDataRepository(
    private val context: Context,
    private val scope: CoroutineScope = CoroutineScope(Dispatchers.IO)
) : AppDataRepository {

    private val KEY_JSON = stringPreferencesKey("app_data_json")

    private val _appData = MutableStateFlow(AppData())
    override val appData: StateFlow<AppData> = _appData.asStateFlow()

    init {
        scope.launch {
            context.dataStore.data.collectLatest { prefs ->
                val jsonStr = prefs[KEY_JSON]
                if (!jsonStr.isNullOrBlank()) {
                    try {
                        _appData.value = AppDataJson.fromJson(jsonStr)
                    } catch (_: Exception) {
                        // Keep current if parse fails
                    }
                }
            }
        }
    }

    override suspend fun update(transform: (AppData) -> AppData) {
        val updated = transform(_appData.value)
        _appData.value = updated
        val json = AppDataJson.toJson(updated)
        context.dataStore.edit { prefs ->
            prefs[KEY_JSON] = json
        }
    }

    override suspend fun setBrightness(brightness: Int) {
        update { it.copy(brightness = brightness.coerceIn(0, 255)) }
    }

    override suspend fun addFavorite(rgb: Rgb) {
        update { current ->
            if (current.favoriteColors.contains(rgb)) current
            else current.copy(favoriteColors = listOf(rgb) + current.favoriteColors)
        }
    }

    override suspend fun removeFavorite(rgb: Rgb) {
        update { current ->
            current.copy(favoriteColors = current.favoriteColors.filter { it != rgb })
        }
    }

    override suspend fun addRecent(rgb: Rgb) {
        update { current ->
            val list = mutableListOf(rgb)
            for (c in current.recentColors) {
                if (c != rgb && list.size < 12) list.add(c)
            }
            current.copy(recentColors = list)
        }
    }

    override suspend fun savePalette(palette: NamedPalette) {
        update { current ->
            val existingIdx = current.palettes.indexOfFirst { it.id == palette.id }
            val updated = current.palettes.toMutableList()
            if (existingIdx >= 0) {
                updated[existingIdx] = palette
            } else {
                updated.add(0, palette)
            }
            current.copy(palettes = updated)
        }
    }

    override suspend fun deletePalette(paletteId: String) {
        update { current ->
            current.copy(palettes = current.palettes.filter { it.id != paletteId })
        }
    }

    override suspend fun reorderPalettes(palettes: List<NamedPalette>) {
        update { it.copy(palettes = palettes) }
    }

    override suspend fun exportJson(): String {
        return AppDataJson.toJson(_appData.value)
    }

    override suspend fun importJson(json: String): Boolean {
        return try {
            val parsed = AppDataJson.fromJson(json)
            update { parsed }
            true
        } catch (_: Exception) {
            false
        }
    }
}
