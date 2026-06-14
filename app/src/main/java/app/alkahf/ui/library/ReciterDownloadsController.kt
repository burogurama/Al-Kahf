package app.alkahf.ui.library

import app.alkahf.data.QuranRepository
import app.alkahf.data.ReciterSurahItem
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/** Per-sūrah download/import state for one reciter, plus in-flight progress. */
data class ReciterDownloadsUiState(
    val surahs: List<ReciterSurahItem> = emptyList(),
    val progress: Map<Int, Float> = emptyMap(),
    val bulkRunning: Boolean = false,
)

/**
 * Manages a single reciter's offline audio: listing each sūrah's download/import
 * state, downloading (one or all), importing a file, and deleting. Download
 * progress per sūrah is reported through the state.
 */
class ReciterDownloadsController(
    private val repository: QuranRepository,
    private val reciterKey: String,
    private val scope: CoroutineScope,
) {
    private val _state = MutableStateFlow(ReciterDownloadsUiState())
    val state: StateFlow<ReciterDownloadsUiState> = _state.asStateFlow()

    private var bulkJob: Job? = null

    suspend fun load() {
        _state.update { it.copy(surahs = repository.reciterSurahItems(reciterKey)) }
    }

    private fun setProgress(surah: Int, value: Float?) {
        _state.update { s ->
            val next = s.progress.toMutableMap()
            if (value == null) next.remove(surah) else next[surah] = value
            s.copy(progress = next)
        }
    }

    fun downloadOne(surah: Int) {
        scope.launch {
            setProgress(surah, 0f)
            repository.downloadSurah(reciterKey, surah) { setProgress(surah, it) }
            setProgress(surah, null)
            load()
        }
    }

    fun downloadAll() {
        _state.update { it.copy(bulkRunning = true) }
        bulkJob = scope.launch {
            for (item in _state.value.surahs) {
                if (item.hasAudio) continue
                setProgress(item.surah, 0f)
                repository.downloadSurah(reciterKey, item.surah) { setProgress(item.surah, it) }
                setProgress(item.surah, null)
                load()
            }
            bulkJob = null
            _state.update { it.copy(bulkRunning = false) }
        }
    }

    fun cancelBulk() {
        bulkJob?.cancel()
        bulkJob = null
        _state.update { it.copy(bulkRunning = false) }
    }

    fun importSurah(surah: Int, uri: String) {
        scope.launch {
            repository.importSurah(reciterKey, surah, uri)
            load()
        }
    }

    fun delete(surah: Int, isImported: Boolean) {
        scope.launch {
            if (isImported) {
                repository.removeImportedSurah(reciterKey, surah)
            } else {
                repository.deleteSurahAudio(reciterKey, surah)
            }
            load()
        }
    }
}
