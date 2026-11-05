package com.pixelgift.app.ui.effects

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.pixelgift.app.data.model.EffectItem
import com.pixelgift.app.data.model.NamedPalette
import com.pixelgift.app.data.repository.AppDataRepository
import com.pixelgift.app.data.repository.EffectCatalogRepository
import com.pixelgift.app.data.repository.InMemoryAppDataRepository
import com.pixelgift.app.protocol.Command
import com.pixelgift.app.protocol.ProtocolConstants
import com.pixelgift.app.protocol.Rgb
import com.pixelgift.app.transport.DeviceTransport
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlin.random.Random

class EffectsViewModel(
    val transport: DeviceTransport,
    val catalogRepository: EffectCatalogRepository,
    val appDataRepository: AppDataRepository = InMemoryAppDataRepository(),
    private val externalScope: CoroutineScope? = null
) : ViewModel() {

    private val scope: CoroutineScope
        get() = externalScope ?: viewModelScope

    private val allEffects = catalogRepository.getEffects()

    val categories: List<String> = listOf("All", "Favorites") + allEffects.map { it.category }.distinct()

    private val _selectedCategory = MutableStateFlow("All")
    val selectedCategory: StateFlow<String> = _selectedCategory.asStateFlow()

    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery.asStateFlow()

    private val _favoriteEffectIds = MutableStateFlow<Set<Int>>(setOf(1, 6, 10, 16, 20, 43))
    val favoriteEffectIds: StateFlow<Set<Int>> = _favoriteEffectIds.asStateFlow()

    // Filtered effects list
    private val _filteredEffects = MutableStateFlow(allEffects)
    val filteredEffects: StateFlow<List<EffectItem>> = _filteredEffects.asStateFlow()

    private fun recomputeFilteredEffects() {
        val category = _selectedCategory.value
        val query = _searchQuery.value
        val favorites = _favoriteEffectIds.value
        _filteredEffects.value = allEffects.filter { effect ->
            val matchesCategory = when (category) {
                "All" -> true
                "Favorites" -> favorites.contains(effect.id)
                else -> effect.category.equals(category, ignoreCase = true)
            }
            val matchesQuery = query.isBlank() ||
                    effect.name.contains(query, ignoreCase = true) ||
                    effect.category.contains(query, ignoreCase = true)

            matchesCategory && matchesQuery
        }
    }

    // Detail Sheet State
    private val _selectedEffect = MutableStateFlow<EffectItem?>(null)
    val selectedEffect: StateFlow<EffectItem?> = _selectedEffect.asStateFlow()

    private val _speed = MutableStateFlow(50)
    val speed: StateFlow<Int> = _speed.asStateFlow()

    private val _dir = MutableStateFlow(0)
    val dir: StateFlow<Int> = _dir.asStateFlow()

    private val _mirror = MutableStateFlow(false)
    val mirror: StateFlow<Boolean> = _mirror.asStateFlow()

    private val _colorMode = MutableStateFlow(0)
    val colorMode: StateFlow<Int> = _colorMode.asStateFlow()

    private val _c1 = MutableStateFlow(Rgb(255, 122, 0))
    val c1: StateFlow<Rgb> = _c1.asStateFlow()

    private val _c2 = MutableStateFlow(Rgb(0, 229, 255))
    val c2: StateFlow<Rgb> = _c2.asStateFlow()

    private val _selectedPalette = MutableStateFlow<NamedPalette?>(
        appDataRepository.appData.value.palettes.firstOrNull()
    )
    val selectedPalette: StateFlow<NamedPalette?> = _selectedPalette.asStateFlow()

    // Throttling for slider changes
    private var lastEffectSendTimeMs: Long = 0L
    private var pendingEffectSendJob: Job? = null
    var sendCountForTesting: Int = 0
        private set

    fun onCategorySelected(category: String) {
        _selectedCategory.value = category
        recomputeFilteredEffects()
    }

    fun onSearchQueryChanged(query: String) {
        _searchQuery.value = query
        recomputeFilteredEffects()
    }

    fun onToggleFavorite(effectId: Int) {
        val current = _favoriteEffectIds.value
        _favoriteEffectIds.value = if (current.contains(effectId)) {
            current - effectId
        } else {
            current + effectId
        }
        recomputeFilteredEffects()
    }

    fun isFavorite(effectId: Int): Boolean {
        return _favoriteEffectIds.value.contains(effectId)
    }

    fun onSelectEffect(effect: EffectItem) {
        _selectedEffect.value = effect
        _speed.value = effect.defaults.speed
        _dir.value = effect.defaults.dir
        _mirror.value = effect.defaults.mirror != 0
        _colorMode.value = effect.defaults.colorMode
        if (effect.defaults.colorMode == 2) {
            _selectedPalette.value?.let { p ->
                scope.launch {
                    transport.send(Command.SetPalette(p.colors.take(8)))
                }
            }
        }
        applyEffect()
    }

    fun onDismissDetailSheet() {
        _selectedEffect.value = null
    }

    fun onSpeedChanged(speed: Int) {
        _speed.value = speed.coerceIn(0, 100)
        throttleDispatchEffect()
    }

    fun onDirChanged(dir: Int) {
        _dir.value = dir.coerceIn(0, 5)
        throttleDispatchEffect()
    }

    fun onMirrorChanged(mirror: Boolean) {
        _mirror.value = mirror
        throttleDispatchEffect()
    }

    fun onColorModeChanged(mode: Int) {
        _colorMode.value = mode.coerceIn(0, 3)
        if (_colorMode.value == 2) {
            _selectedPalette.value?.let { p ->
                scope.launch {
                    transport.send(Command.SetPalette(p.colors.take(8)))
                }
            }
        }
        throttleDispatchEffect()
    }

    fun onC1Changed(rgb: Rgb) {
        _c1.value = rgb
        throttleDispatchEffect()
    }

    fun onC2Changed(rgb: Rgb) {
        _c2.value = rgb
        throttleDispatchEffect()
    }

    fun onPaletteSelected(palette: NamedPalette) {
        _selectedPalette.value = palette
        scope.launch {
            transport.send(Command.SetPalette(palette.colors.take(8)))
        }
        throttleDispatchEffect()
    }

    /**
     * "Surprise Me" selects a random effect from the catalog and applies it.
     */
    fun onSurpriseMe(): EffectItem? {
        if (allEffects.isEmpty()) return null
        val randomEffect = allEffects[Random.nextInt(allEffects.size)]
        onSelectEffect(randomEffect)
        return randomEffect
    }

    private fun throttleDispatchEffect() {
        val now = System.currentTimeMillis()
        val elapsed = now - lastEffectSendTimeMs

        if (elapsed >= 100L) {
            lastEffectSendTimeMs = now
            pendingEffectSendJob?.cancel()
            pendingEffectSendJob = null
            applyEffect()
        } else {
            pendingEffectSendJob?.cancel()
            pendingEffectSendJob = scope.launch {
                delay(100L - elapsed)
                lastEffectSendTimeMs = System.currentTimeMillis()
                applyEffect()
            }
        }
    }

    fun applyEffect() {
        val effect = _selectedEffect.value ?: return
        sendCountForTesting++
        scope.launch {
            transport.send(
                Command.SetEffect(
                    base = effect.id,
                    speed = _speed.value,
                    dir = _dir.value,
                    mirror = if (_mirror.value) 1 else 0,
                    colorMode = _colorMode.value,
                    c1 = _c1.value,
                    c2 = _c2.value
                )
            )
        }
    }

    /**
     * Updates ViewModel state from incoming device StatusSnapshot without overwriting device state.
     * Per SPEC 7.1.
     */
    fun updateFromSnapshot(snapshot: com.pixelgift.app.protocol.DeviceNotification.StatusSnapshot) {
        if (snapshot.mode == ProtocolConstants.MODE_EFFECT) {
            val matching = allEffects.find { it.id == snapshot.effectBase }
            if (matching != null) {
                _selectedEffect.value = matching
            }
            if (snapshot.speed > 0) {
                _speed.value = snapshot.speed
            }
        }
    }
}
