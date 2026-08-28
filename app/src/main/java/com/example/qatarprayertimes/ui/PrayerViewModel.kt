package com.example.qatarprayertimes.ui

import android.app.Application
import android.net.Uri
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.qatarprayertimes.azan.AzanAudioStore
import com.example.qatarprayertimes.azan.AzanScheduler
import com.example.qatarprayertimes.azan.CustomAzan
import com.example.qatarprayertimes.azan.StockAzan
import com.example.qatarprayertimes.data.AzanSoundState
import com.example.qatarprayertimes.data.AppLocale
import com.example.qatarprayertimes.data.AreaId
import com.example.qatarprayertimes.data.PrayerId
import com.example.qatarprayertimes.data.PrayerTimesPayload
import com.example.qatarprayertimes.data.PrayerTimesRepository
import com.example.qatarprayertimes.data.PreferencesStore
import com.example.qatarprayertimes.data.TimeFormat
import com.example.qatarprayertimes.data.UserPreferences
import com.example.qatarprayertimes.data.ViewMode
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class PrayerUiState(
    val loading: Boolean = true,
    val error: Boolean = false,
    val payload: PrayerTimesPayload? = null,
    val prefs: UserPreferences = UserPreferences(),
    val soundStates: Map<PrayerId, AzanSoundState> = emptyMap(),
    val stockSounds: List<StockAzan> = emptyList(),
    val customSounds: List<CustomAzan> = emptyList(),
)

class PrayerViewModel(application: Application) : AndroidViewModel(application) {
    private val repository = PrayerTimesRepository(application)
    private val preferencesStore = PreferencesStore(application)
    private val soundStore = AzanAudioStore(application)

    private val _state = MutableStateFlow(
        PrayerUiState(
            prefs = preferencesStore.read(),
            soundStates = soundStore.snapshot(),
            stockSounds = soundStore.getStockSounds(),
            customSounds = soundStore.getCustomSounds(),
        ),
    )
    val state: StateFlow<PrayerUiState> = _state

    init {
        refresh(force = false)
    }

    fun refresh(force: Boolean = true) {
        viewModelScope.launch(Dispatchers.IO) {
            _state.update { it.copy(loading = true, error = false) }
            runCatching { repository.load(forceRefresh = force) }
                .onSuccess { payload ->
                    _state.update { it.copy(loading = false, error = false, payload = payload) }
                    AzanScheduler.reschedule(getApplication(), payload.prayers)
                }
                .onFailure {
                    _state.update { it.copy(loading = false, error = true, payload = null) }
                }
        }
    }

    fun updatePrefs(transform: (UserPreferences) -> UserPreferences) {
        _state.update { current ->
            val next = transform(current.prefs)
            preferencesStore.write(next)
            current.copy(prefs = next)
        }
    }

    fun setArea(area: AreaId) = updatePrefs { it.copy(area = area) }
    fun setTimeFormat(format: TimeFormat) = updatePrefs { it.copy(timeFormat = format) }
    fun setViewMode(mode: ViewMode) = updatePrefs { it.copy(viewMode = mode) }
    fun setLocale(locale: AppLocale) = updatePrefs { it.copy(locale = locale) }

    fun toggleMute(id: PrayerId) {
        val current = _state.value.soundStates[id] ?: return
        soundStore.setMuted(id, !current.muted)
        updateSoundStates()
    }

    fun toggleGlobalMute() {
        soundStore.toggleAllMute()
        updateSoundStates()
    }

    private fun updateSoundStates() {
        _state.update { it.copy(
            soundStates = soundStore.snapshot(),
            customSounds = soundStore.getCustomSounds(),
        ) }
        _state.value.payload?.prayers?.let { prayers ->
            AzanScheduler.reschedule(getApplication(), prayers)
        }
    }

    fun selectStockSound(stock: StockAzan) {
        soundStore.setGlobalStock(stock)
        updateSoundStates()
    }

    fun selectCustomSound(custom: CustomAzan) {
        soundStore.setGlobalCustom(custom)
        updateSoundStates()
    }

    fun deleteCustomSound(custom: CustomAzan) {
        soundStore.deleteCustomAzan(custom)
        updateSoundStates()
    }

    fun setGlobalCustomSound(uri: Uri) {
        if (soundStore.saveGlobalCustom(uri)) {
            updateSoundStates()
        }
    }
}
