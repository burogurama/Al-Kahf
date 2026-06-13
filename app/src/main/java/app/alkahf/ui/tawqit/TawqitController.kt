package app.alkahf.ui.tawqit

import androidx.media3.common.MediaItem
import androidx.media3.common.Player
import androidx.media3.exoplayer.ExoPlayer
import app.alkahf.data.PageAyah
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
) {
    private val _state = MutableStateFlow(TawqitUiState())
    val state: StateFlow<TawqitUiState> = _state.asStateFlow()

    // Total duration of playlist items already passed, for cumulative position.
    private var completedMs = 0L
    private var lastIndex = 0
    private var ticker: Job? = null

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
        player.setMediaItems(uris.map { MediaItem.fromUri(it) })
        player.addListener(object : Player.Listener {
            override fun onMediaItemTransition(mediaItem: MediaItem?, reason: Int) {
                val index = player.currentMediaItemIndex
                if (index > lastIndex) {
                    // Accumulate the duration of the item we just left.
                    completedMs += player.duration.coerceAtLeast(0)
                    lastIndex = index
                }
            }
        })
        player.setPlaybackSpeed(speed)
        player.prepare()
        startTicker()
    }

    private fun startTicker() {
        ticker?.cancel()
        ticker = scope.launch {
            while (true) {
                val total = totalDurationEstimate()
                _state.update {
                    it.copy(
                        positionMs = positionMs(),
                        durationMs = total,
                        isPlaying = player.isPlaying,
                    )
                }
                delay(60)
            }
        }
    }

    private fun positionMs(): Long = completedMs + player.currentPosition.coerceAtLeast(0)

    private fun totalDurationEstimate(): Long {
        // Best-effort: current cumulative base + the current item's duration.
        val current = player.duration.coerceAtLeast(0)
        return completedMs + current
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
        _state.update {
            it.copy(
                endTimesMs = it.endTimesMs.dropLast(1),
                currentIndex = (it.currentIndex - 1).coerceAtLeast(0),
            )
        }
    }

    fun nudgeOffset(deltaMs: Long) {
        _state.update { it.copy(globalOffsetMs = it.globalOffsetMs + deltaMs) }
    }

    fun release() {
        ticker?.cancel()
        player.release()
    }
}
