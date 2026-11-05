package com.pixelgift.app.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.pixelgift.app.data.repository.AppDataRepository
import com.pixelgift.app.data.repository.EffectCatalogRepository
import com.pixelgift.app.transport.DeviceTransport
import com.pixelgift.app.ui.device.DeviceViewModel
import com.pixelgift.app.ui.effects.EffectsViewModel
import com.pixelgift.app.ui.magic.MagicViewModel
import com.pixelgift.app.ui.studio.StudioViewModel
import com.pixelgift.app.ui.time.TimeViewModel

@Suppress("UNCHECKED_CAST")
class MainViewModelFactory(
    private val transport: DeviceTransport
) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        return MainViewModel(transport) as T
    }
}

@Suppress("UNCHECKED_CAST")
class StudioViewModelFactory(
    private val transport: DeviceTransport,
    private val repository: AppDataRepository
) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        return StudioViewModel(transport, repository) as T
    }
}

@Suppress("UNCHECKED_CAST")
class EffectsViewModelFactory(
    private val transport: DeviceTransport,
    private val catalogRepository: EffectCatalogRepository,
    private val appDataRepository: AppDataRepository
) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        return EffectsViewModel(transport, catalogRepository, appDataRepository) as T
    }
}

@Suppress("UNCHECKED_CAST")
class MagicViewModelFactory(
    private val transport: DeviceTransport
) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        return MagicViewModel(transport) as T
    }
}

@Suppress("UNCHECKED_CAST")
class TimeViewModelFactory(
    private val transport: DeviceTransport
) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        return TimeViewModel(transport) as T
    }
}

@Suppress("UNCHECKED_CAST")
class DeviceViewModelFactory(
    private val transport: DeviceTransport
) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        return DeviceViewModel(transport) as T
    }
}

