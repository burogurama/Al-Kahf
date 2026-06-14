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
            reciters = repository.reciterStatuses(),
            presets = repository.presets(),
        )
    }

    suspend fun activateReciter(key: String) {
        repository.setActiveReciter(key)
        load()
    }

    suspend fun createReciter(name: String, riwayah: Riwayah) {
        repository.createCustomReciter(name, riwayah)
        load()
    }
}
