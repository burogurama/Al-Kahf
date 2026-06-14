package app.alkahf.ui.mushaf

import android.content.Context
import android.net.Uri
import androidx.media3.common.MediaItem
import androidx.media3.common.Player
import androidx.media3.exoplayer.ExoPlayer
import app.alkahf.R
import app.alkahf.data.QuranRepository
import app.alkahf.data.audio.AudioStore
import java.io.File
import java.io.IOException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

enum class MushafAudioMode { LISTEN, RECITE_BACK, TAP }

enum class MushafAudioScope { PAGE, SURAH, FROM_AYAH }

enum class MushafAudioPhase { IDLE, PREPARING, PLAYING, GAP }

data class MushafAudioState(
    val mode: MushafAudioMode = MushafAudioMode.LISTEN,
    val scope: MushafAudioScope = MushafAudioScope.PAGE,
    val phase: MushafAudioPhase = MushafAudioPhase.IDLE,
    val isPaused: Boolean = false,
    val currentAyahId: Int? = null,
    val speed: Float = 1f,
    val reciterPath: String = AudioStore.DEFAULT_RECITER,
    val reciterName: String = "Ḥuṣarī",
    val errorMessage: String? = null,
)

/**
 * Sequential ayah playback for the Mushaf's listening modes: continuous
 * listening, listening with a recite-back gap after each ayah, and single
 * tapped-ayah playback. Runs on the main dispatcher (ExoPlayer requirement).
 */
class MushafAudioController(
    private val context: Context,
    private val repository: QuranRepository,
    private val audioStore: AudioStore,
    private val player: ExoPlayer,
    private val coroutineScope: CoroutineScope,
    reciterPath: String,
    reciterName: String,
) {
    private val _state = MutableStateFlow(
        MushafAudioState(reciterPath = reciterPath, reciterName = reciterName),
    )
    val state: StateFlow<MushafAudioState> = _state.asStateFlow()

    private var job: Job? = null
    private var lastDurationMs = 0L
    private var playbackSpeed = 1f

    fun setMode(mode: MushafAudioMode) {
        stop()
        _state.update { it.copy(mode = mode) }
    }

    fun setScope(scope: MushafAudioScope) {
        stop()
        _state.update { it.copy(scope = scope) }
    }

    /** Sets the playback rate applied to subsequent (and current) playback. */
    fun setSpeed(speed: Float) {
        playbackSpeed = speed
        player.setPlaybackSpeed(speed)
        _state.update { it.copy(speed = speed) }
    }

    /**
     * Plays the given ayat (ids encode surah * 1000 + number) in order,
     * looping the whole list [repeat] times. A [repeat] of 0 (or less) loops
     * forever until stopped.
     */
    fun playAyahIds(ayahIds: List<Int>, repeat: Int = 1) {
        job?.cancel()
        player.stop()
        _state.update { it.copy(isPaused = false, errorMessage = null) }
        job = coroutineScope.launch { run(ayahIds, repeat) }
    }

    fun playSingle(ayahId: Int) = playAyahIds(listOf(ayahId))

    fun togglePause() {
        _state.update { it.copy(isPaused = !it.isPaused) }
        player.playWhenReady = !_state.value.isPaused
    }

    fun stop() {
        job?.cancel()
        player.stop()
        _state.update {
            it.copy(phase = MushafAudioPhase.IDLE, currentAyahId = null, isPaused = false)
        }
    }

    fun release() {
        job?.cancel()
        player.release()
    }

    private suspend fun run(ayahIds: List<Int>, repeat: Int = 1) {
        var pass = 0
        // repeat <= 0 means loop forever (until stopped/cancelled).
        while (repeat <= 0 || pass < repeat) {
            pass++
            for (ayahId in ayahIds) {
                _state.update { it.copy(currentAyahId = ayahId, phase = MushafAudioPhase.PREPARING) }
                val file = try {
                    audioStore.ayahFile(ayahId / 1000, ayahId % 1000, _state.value.reciterPath)
                } catch (e: IOException) {
                    _state.update {
                        it.copy(
                            phase = MushafAudioPhase.IDLE,
                            currentAyahId = null,
                            errorMessage = context.getString(R.string.mushaf_audio_download_failed),
                        )
                    }
                    return
                }
                val finished = playFile(file)
                if (!finished) return
                repository.logPractice(type = "listen", ayahCount = 1, durationMs = lastDurationMs)
                if (_state.value.mode == MushafAudioMode.RECITE_BACK) {
                    reciteBackGap()
                }
            }
        }
        _state.update { it.copy(phase = MushafAudioPhase.IDLE, currentAyahId = null) }
    }

    /** Plays the file to its end. Returns false if playback was stopped. */
    private suspend fun playFile(file: File): Boolean {
        player.setMediaItem(MediaItem.fromUri(Uri.fromFile(file)))
        player.prepare()
        player.setPlaybackSpeed(playbackSpeed)
        player.playWhenReady = !_state.value.isPaused
        _state.update { it.copy(phase = MushafAudioPhase.PLAYING) }
        while (true) {
            when (player.playbackState) {
                Player.STATE_ENDED -> {
                    lastDurationMs = player.duration.coerceAtLeast(0)
                    return true
                }
                Player.STATE_IDLE -> return false
                else -> {}
            }
            delay(150)
        }
    }

    private suspend fun reciteBackGap() {
        if (lastDurationMs <= 0) return
        // Match the gap to how long the āyah actually took to play (speed-scaled).
        val gapMs = (lastDurationMs / playbackSpeed).toLong()
        _state.update { it.copy(phase = MushafAudioPhase.GAP) }
        var elapsed = 0L
        while (elapsed < gapMs) {
            if (!_state.value.isPaused) elapsed += 100
            delay(100)
        }
    }
}
