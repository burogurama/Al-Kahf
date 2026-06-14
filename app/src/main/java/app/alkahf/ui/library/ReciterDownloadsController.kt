package app.alkahf.ui.library

import app.alkahf.data.QuranRepository
import app.alkahf.data.ReciterSurahItem
import app.alkahf.data.audio.AudioDownloadWorker
import androidx.work.ExistingWorkPolicy
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.OutOfQuotaPolicy
import androidx.work.WorkInfo
import androidx.work.WorkManager
import androidx.work.workDataOf
import kotlinx.coroutines.CoroutineScope
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
 * Manages a single reciter's offline audio. Downloads run in a WorkManager
 * foreground job ([AudioDownloadWorker]) so they continue when the screen is
 * left or the app is backgrounded; this controller enqueues that work and
 * mirrors its progress into the UI state. Imports and deletes (quick, local) run
 * inline.
 */
class ReciterDownloadsController(
    private val repository: QuranRepository,
    private val reciterKey: String,
    private val reciterName: String,
    private val workManager: WorkManager,
    private val scope: CoroutineScope,
) {
    private val _state = MutableStateFlow(ReciterDownloadsUiState())
    val state: StateFlow<ReciterDownloadsUiState> = _state.asStateFlow()

    private var lastDone = -1

    init {
        // Reflect any download already running for this reciter (e.g. started then
        // navigated away and back), and keep mirroring its progress.
        scope.launch {
            workManager.getWorkInfosForUniqueWorkFlow(AudioDownloadWorker.uniqueName(reciterKey))
                .collect { infos -> onWorkInfos(infos) }
        }
    }

    suspend fun load() {
        _state.update { it.copy(surahs = repository.reciterSurahItems(reciterKey)) }
    }

    private suspend fun onWorkInfos(infos: List<WorkInfo>) {
        val active = infos.any { !it.state.isFinished }
        val running = infos.firstOrNull { it.state == WorkInfo.State.RUNNING }?.progress
        val total = running?.getInt(AudioDownloadWorker.KEY_PROGRESS_TOTAL, 0) ?: 0
        val surah = running?.getInt(AudioDownloadWorker.KEY_PROGRESS_SURAH, -1) ?: -1
        val fraction = running?.getFloat(AudioDownloadWorker.KEY_PROGRESS_FRACTION, 0f) ?: 0f
        val done = running?.getInt(AudioDownloadWorker.KEY_PROGRESS_DONE, 0) ?: 0
        val progress = if (total > 0 && surah > 0) mapOf(surah to fraction) else emptyMap()
        _state.update { it.copy(progress = progress, bulkRunning = active) }
        // Refresh the sūrah list (hasAudio flags) when a sūrah finishes or the
        // whole job ends — not on every per-āyah progress tick.
        if (done != lastDone || (!active && infos.isNotEmpty())) {
            lastDone = done
            load()
        }
    }

    fun downloadOne(surah: Int) = enqueue(intArrayOf(surah))

    fun downloadAll() {
        val missing = _state.value.surahs.filter { !it.hasAudio }.map { it.surah }
        if (missing.isNotEmpty()) enqueue(missing.toIntArray())
    }

    fun cancelBulk() {
        workManager.cancelUniqueWork(AudioDownloadWorker.uniqueName(reciterKey))
    }

    private fun enqueue(surahs: IntArray) {
        val request = OneTimeWorkRequestBuilder<AudioDownloadWorker>()
            .setInputData(
                workDataOf(
                    AudioDownloadWorker.KEY_RECITER_KEY to reciterKey,
                    AudioDownloadWorker.KEY_RECITER_NAME to reciterName,
                    AudioDownloadWorker.KEY_SURAHS to surahs,
                ),
            )
            .setExpedited(OutOfQuotaPolicy.RUN_AS_NON_EXPEDITED_WORK_REQUEST)
            .build()
        // APPEND so tapping a single sūrah while "download all" runs queues after it
        // instead of cancelling it.
        workManager.enqueueUniqueWork(
            AudioDownloadWorker.uniqueName(reciterKey),
            ExistingWorkPolicy.APPEND_OR_REPLACE,
            request,
        )
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
