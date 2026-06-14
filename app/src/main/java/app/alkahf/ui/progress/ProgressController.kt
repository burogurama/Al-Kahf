package app.alkahf.ui.progress

import app.alkahf.data.ProgressSnapshot
import app.alkahf.data.QuranRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/** Loads the whole-mushaf progress roll-up for the Progress screen. */
class ProgressController(private val repository: QuranRepository) {
    private val _snapshot = MutableStateFlow<ProgressSnapshot?>(null)
    val snapshot: StateFlow<ProgressSnapshot?> = _snapshot.asStateFlow()

    suspend fun load() {
        _snapshot.value = repository.progressSnapshot()
    }
}
