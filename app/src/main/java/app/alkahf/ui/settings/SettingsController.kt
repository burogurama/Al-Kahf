package app.alkahf.ui.settings

import app.alkahf.data.QuranRepository
import app.alkahf.data.Riwayah
import app.alkahf.data.SettingsData
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * Holds the Settings screen's persisted state — the [SettingsData] block and the
 * active riwāyah — and writes changes back to the repository. Android-side
 * effects (recreating the activity, rescheduling reminders, permission requests)
 * stay in the screen.
 */
class SettingsController(private val repository: QuranRepository) {
    private val _settings = MutableStateFlow(repository.settings())
    val settings: StateFlow<SettingsData> = _settings.asStateFlow()

    private val _riwayah = MutableStateFlow(repository.riwayah)
    val riwayah: StateFlow<Riwayah> = _riwayah.asStateFlow()

    /** Persists [data] and reflects it in state. */
    fun update(data: SettingsData) {
        _settings.value = data
        repository.updateSettings(data)
    }

    /**
     * Reflects [data] in state without persisting — for the language switch,
     * whose persistence and locale apply happen through the screen's
     * onLanguageChange path instead.
     */
    fun preview(data: SettingsData) {
        _settings.value = data
    }

    /** Persists the new riwāyah; the caller recreates the activity to apply it. */
    fun setRiwayah(value: Riwayah) {
        if (value == _riwayah.value) return
        repository.riwayah = value
        _riwayah.value = value
    }
}
