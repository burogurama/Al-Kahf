package app.alkahf.ui.library

import app.alkahf.data.LoopPreset
import app.alkahf.data.QuranRepository
import app.alkahf.data.ReciterStatus
import app.alkahf.data.Riwayah
import app.alkahf.data.StorageInfo
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/** Storage usage, reciters, and drill presets shown on the Library screen. */
data class LibraryUiState(
    val storage: StorageInfo? = null,
    val reciters: List<ReciterStatus> = emptyList(),
    val presets: List<LoopPreset> = emptyList(),
)

/** Loads the Library screen's data and handles reciter activation/creation. */
class LibraryController(private val repository: QuranRepository) {
    private val _state = MutableStateFlow(LibraryUiState())
    val state: StateFlow<LibraryUiState> = _state.asStateFlow()

    /** The active riwāyah, the default for a newly created imported reciter. */
    val defaultRiwayah: Riwayah get() = repository.riwayah

    suspend fun load() {
        _state.value = LibraryUiState(
            storage = repository.storageInfo(),
            reciters = repository.allReciterStatuses(),
            presets = repository.presets(),
        )
    }

    /**
     * Makes [reciter] the active voice. If it belongs to a different riwāyah than
     * the system one, switches the reading too and returns true so the caller can
     * recreate the activity to apply it; otherwise activates in place.
     */
    suspend fun activateReciter(reciter: ReciterStatus): Boolean {
        if (reciter.riwayah != repository.riwayah) {
            repository.riwayah = reciter.riwayah
            repository.setActiveReciter(reciter.key)
            return true
        }
        repository.setActiveReciter(reciter.key)
        load()
        return false
    }

    /** Creates an imported reciter; returns false (without creating) if the name is taken. */
    suspend fun createReciter(name: String, riwayah: Riwayah): Boolean {
        val created = repository.createCustomReciter(name, riwayah) != null
        if (created) load()
        return created
    }
}
