package app.alkahf.ui.tawqit

import androidx.media3.common.MediaItem
import androidx.media3.exoplayer.ExoPlayer
import app.alkahf.data.PageAyah
import app.alkahf.data.QuranRepository
import app.alkahf.data.TawqitSourceType
import app.alkahf.data.TawqitTrack
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class TawqitUiState(
    val ayahs: List<PageAyah> = emptyList(),
    val currentIndex: Int = 0,
    val endTimesMs: List<Long> = emptyList(),
    val positionMs: Long = 0,
    val durationMs: Long = 0,
    val isPlaying: Boolean = false,
    val speed: Float = 0.75f,
    val globalOffsetMs: Long = 0,
) {
    val isComplete: Boolean get() = endTimesMs.size >= ayahs.size && ayahs.isNotEmpty()
}

/**
 * Drives the Tawqīt tagging session. Plays an audio source (a single imported
 * file, or a playlist of per-ayah reciter files) and records the playback
 * position when the user marks each ayah end. Positions are media-timeline ms
 * (1× scale regardless of playback speed).
 */
class TawqitController(
    private val player: ExoPlayer,
    private val scope: CoroutineScope,
    private val repository: QuranRepository,
) {
    private val _state = MutableStateFlow(TawqitUiState())
    val state: StateFlow<TawqitUiState> = _state.asStateFlow()

    // Durations of playlist items as they become known, for cumulative position.
    private val itemDurations = HashMap<Int, Long>()
    private var ticker: Job? = null

    /** Resolves a [draft]'s āyāt and audio sources, then begins a tagging session. */
    suspend fun loadDraft(draft: TawqitTrack) {
        val ayahs = repository.ayahsForRange(draft.surah, draft.ayahFrom, draft.ayahTo)
        val uris = when (draft.sourceType) {
            TawqitSourceType.IMPORT -> List(ayahs.size) { draft.sourceRef }.take(1)
            TawqitSourceType.RECITER ->
                repository.reciterAyahUris(draft.sourceRef, draft.surah, draft.ayahFrom, draft.ayahTo)
        }
        load(
            ayahs = ayahs,
            uris = uris,
            existingEndTimes = draft.endTimesMs,
            offsetMs = draft.globalOffsetMs,
            speed = 0.75f,
        )
    }

    /** Persists the current marks back onto [draft]. */
    suspend fun save(draft: TawqitTrack) {
        val s = _state.value
        repository.saveTawqitTrack(
            draft.copy(
                endTimesMs = s.endTimesMs,
                globalOffsetMs = s.globalOffsetMs,
                complete = s.isComplete,
            ),
        )
    }

    fun load(
        ayahs: List<PageAyah>,
        uris: List<String>,
        existingEndTimes: List<Long>,
        offsetMs: Long,
        speed: Float,
    ) {
        _state.update {
            it.copy(
                ayahs = ayahs,
                endTimesMs = existingEndTimes,
                currentIndex = existingEndTimes.size.coerceAtMost(ayahs.lastIndex.coerceAtLeast(0)),
                globalOffsetMs = offsetMs,
                speed = speed,
            )
        }
        itemDurations.clear()
        player.setMediaItems(uris.map { MediaItem.fromUri(it) })
        player.setPlaybackSpeed(speed)
        player.prepare()
        // Resume where tagging left off instead of replaying from the start.
        val resumeAt = existingEndTimes.lastOrNull()
        if (resumeAt != null) {
            if (uris.size <= 1) {
                // Single imported file: marks are ms within that one file.
                player.seekTo(0, resumeAt)
            } else {
                // Per-ayah playlist: jump to the current ayah's file.
                player.seekTo(existingEndTimes.size.coerceAtMost(uris.lastIndex), 0)
            }
        }
        startTicker()
    }

    private fun startTicker() {
        ticker?.cancel()
        ticker = scope.launch {
            while (true) {
                recordCurrentDuration()
                _state.update {
                    it.copy(
                        positionMs = positionMs(),
                        durationMs = totalDurationEstimate(),
                        isPlaying = player.isPlaying,
                    )
                }
                delay(60)
            }
        }
    }

    private fun recordCurrentDuration() {
        val duration = player.duration
        if (duration > 0) {
            val index = player.currentMediaItemIndex
            itemDurations[index] = maxOf(itemDurations[index] ?: 0L, duration)
        }
    }

    private fun baseMs(itemIndex: Int): Long =
        (0 until itemIndex).sumOf { itemDurations[it] ?: 0L }

    private fun positionMs(): Long =
        baseMs(player.currentMediaItemIndex) + player.currentPosition.coerceAtLeast(0)

    private fun totalDurationEstimate(): Long {
        val known = (0 until player.mediaItemCount).sumOf { itemDurations[it] ?: 0L }
        return maxOf(known, positionMs())
    }

    /** Seeks the audio to a cumulative (playlist-wide) millisecond position. */
    private fun seekToCumulative(target: Long) {
        var index = 0
        var acc = 0L
        while (index < player.mediaItemCount - 1) {
            val duration = itemDurations[index] ?: Long.MAX_VALUE
            if (acc + duration <= target) {
                acc += duration
                index++
            } else {
                break
            }
        }
        player.seekTo(index, (target - acc).coerceAtLeast(0))
    }

    fun playPause() {
        if (player.isPlaying) player.pause() else player.play()
    }

    fun cycleSpeed() {
        val next = when (_state.value.speed) {
            0.5f -> 0.75f
            0.75f -> 1.0f
            else -> 0.5f
        }
        player.setPlaybackSpeed(next)
        _state.update { it.copy(speed = next) }
    }

    fun markAyahEnd() {
        val s = _state.value
        if (s.currentIndex >= s.ayahs.size) return
        val newTimes = s.endTimesMs + positionMs()
        _state.update {
            it.copy(endTimesMs = newTimes, currentIndex = (it.currentIndex + 1))
        }
    }

    fun undo() {
        val s = _state.value
        if (s.endTimesMs.isEmpty()) return
        val newTimes = s.endTimesMs.dropLast(1)
        _state.update {
            it.copy(
                endTimesMs = newTimes,
                currentIndex = (it.currentIndex - 1).coerceAtLeast(0),
            )
        }
        // Rewind the audio to the start of the ayah now being re-tagged
        // (the previous mark, or the very beginning).
        seekToCumulative(newTimes.lastOrNull() ?: 0L)
    }

    fun nudgeOffset(deltaMs: Long) {
        _state.update { it.copy(globalOffsetMs = it.globalOffsetMs + deltaMs) }
    }

    fun release() {
        ticker?.cancel()
        player.release()
    }
}
