package app.alkahf.ui.mushaf

import app.alkahf.data.QuranRepository
import app.alkahf.data.ReciterStatus
import app.alkahf.data.Riwayah
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/** A pending built-in-reciter range download awaiting the user's confirmation. */
data class RangeDownloadRequest(
    val reciterKey: String,
    val reciterName: String,
    val surah: Int,
    val from: Int,
    val to: Int,
    val count: Int,
)

/** The range-listening config panel and its pending download. */
data class MushafRangeState(
    val open: Boolean = false,
    val mode: MushafAudioMode = MushafAudioMode.LISTEN,
    val speed: Float = 1f,
    val times: Int = 1,
    val reciters: List<ReciterStatus> = emptyList(),
    val reciterKey: String = "",
    val downloadPrompt: RangeDownloadRequest? = null,
    val downloadProgress: Float? = null,
)

/**
 * Drives the Mushaf's range-listening flow: the reciter/config panel, choosing a
 * voice, and the built-in-reciter download confirmation. Plays the selected āyāt
 * through [MushafAudioController]; an imported reciter whose range isn't timed is
 * routed to import/time via [onImportReciter]. The selected āyāt are passed in
 * per call (they live in the screen's selection state).
 */
class MushafRangeController(
    private val repository: QuranRepository,
    private val audio: MushafAudioController,
    private val scope: CoroutineScope,
    private val onImportReciter: (ReciterStatus) -> Unit,
    initialReciterKey: String,
) {
    private val _state = MutableStateFlow(MushafRangeState(reciterKey = initialReciterKey))
    val state: StateFlow<MushafRangeState> = _state.asStateFlow()
    private var downloadJob: Job? = null

    fun setMode(mode: MushafAudioMode) = _state.update { it.copy(mode = mode) }
    fun setSpeed(speed: Float) = _state.update { it.copy(speed = speed) }
    fun setTimes(times: Int) = _state.update { it.copy(times = times) }
    fun setReciter(key: String) = _state.update { it.copy(reciterKey = key) }
    fun setOpen(open: Boolean) = _state.update { it.copy(open = open) }
    fun toggleOpen() = _state.update { it.copy(open = !it.open) }

    /** Reloads the reciter list for [riwayah], keeping a valid selection. */
    suspend fun loadReciters(riwayah: Riwayah) {
        val list = repository.reciterStatuses(riwayah)
        _state.update { s ->
            val key = list.firstOrNull { it.key == s.reciterKey }?.key
                ?: list.firstOrNull()?.key ?: s.reciterKey
            s.copy(reciters = list, reciterKey = key)
        }
    }

    /** Plays [ids] with [mode] using the current speed/repeat config. */
    fun play(mode: MushafAudioMode, ids: List<Int>) {
        if (ids.isEmpty()) return
        val s = _state.value
        audio.setMode(mode)
        audio.setSpeed(s.speed)
        audio.playAyahIds(ids, repeat = s.times)
        _state.update { it.copy(mode = mode) }
    }

    /**
     * Listen pressed in the config panel. Applies the chosen reciter, then:
     *  - imported reciter whose sūrah isn't imported+timed → route to import/time
     *  - built-in reciter with audio not yet cached → confirm + download, play after
     *  - otherwise → play immediately.
     */
    fun listen(ids: List<Int>, displayRiwayah: Riwayah) {
        if (ids.isEmpty()) return
        val s = _state.value
        val chosen = s.reciters.firstOrNull { it.key == s.reciterKey }
        if (chosen == null) {
            setOpen(false)
            play(s.mode, ids)
            return
        }
        if (chosen.isImported) {
            // Imported reciter: play from the Tawqīt timings if the range is timed
            // (partial timings are fine for their covered āyāt); otherwise route the
            // user to import + time the sūrah.
            val surah = ids.first() / 1000
            val nums = ids.filter { it / 1000 == surah }.map { it % 1000 }
            scope.launch {
                val playback = repository.importedRangePlayback(chosen.key, surah, nums.min(), nums.max())
                setOpen(false)
                if (playback != null) {
                    audio.setMode(s.mode)
                    audio.setSpeed(s.speed)
                    audio.playImported(playback.fileUri, playback.ayahIds, playback.segments, repeat = s.times)
                } else {
                    onImportReciter(chosen)
                }
            }
            return
        }
        // Built-in reciter.
        repository.setActiveReciter(chosen.key)
        audio.setReciter(chosen.key, chosen.displayName)
        val surah = ids.first() / 1000
        val nums = ids.filter { it / 1000 == surah }.map { it % 1000 }
        val from = nums.min()
        val to = nums.max()
        scope.launch {
            val ready = repository.rangeAudioAvailable(chosen.key, surah, from, to, displayRiwayah)
            if (ready) {
                setOpen(false)
                play(s.mode, ids)
            } else {
                _state.update {
                    it.copy(
                        downloadPrompt = RangeDownloadRequest(
                            reciterKey = chosen.key,
                            reciterName = chosen.displayName,
                            surah = surah,
                            from = from,
                            to = to,
                            count = ids.size,
                        ),
                    )
                }
            }
        }
    }

    /** Confirms the pending download, then plays [ids] once it finishes. */
    fun startDownload(request: RangeDownloadRequest, displayRiwayah: Riwayah, ids: List<Int>) {
        _state.update { it.copy(downloadProgress = 0f) }
        downloadJob = scope.launch {
            try {
                repository.downloadRange(
                    request.reciterKey, request.surah, request.from, request.to, displayRiwayah,
                ) { p ->
                    _state.update { it.copy(downloadProgress = p) }
                }
                _state.update { it.copy(downloadProgress = null, downloadPrompt = null, open = false) }
                downloadJob = null
                play(_state.value.mode, ids)
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                _state.update { it.copy(downloadProgress = null, downloadPrompt = null) }
                downloadJob = null
            }
        }
    }

    fun cancelDownload() {
        downloadJob?.cancel()
        downloadJob = null
        _state.update { it.copy(downloadProgress = null, downloadPrompt = null) }
    }
}
