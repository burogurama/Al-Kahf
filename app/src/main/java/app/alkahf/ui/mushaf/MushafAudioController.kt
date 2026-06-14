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

    /**
     * Switches the reciter used for subsequent playback. Stops any current
     * playback so the next āyah is fetched from the new reciter's files.
     */
    fun setReciter(path: String, name: String) {
        if (path == _state.value.reciterPath) {
            _state.update { it.copy(reciterName = name) }
            return
        }
        stop()
        _state.update { it.copy(reciterPath = path, reciterName = name) }
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

    /**
     * Plays an imported reciter's single audio file by [segments] (one start..end
     * millisecond range per āyah, parallel to [ayahIds]), looping [repeat] times
     * (0 = forever).
     */
    fun playImported(
        fileUri: String,
        ayahIds: List<Int>,
        segments: List<LongRange>,
        repeat: Int = 1,
    ) {
        job?.cancel()
        player.stop()
        _state.update { it.copy(isPaused = false, errorMessage = null) }
        job = coroutineScope.launch { runImported(fileUri, ayahIds, segments, repeat) }
    }

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

    private suspend fun runImported(
        fileUri: String,
        ayahIds: List<Int>,
        segments: List<LongRange>,
        repeat: Int,
    ) {
        player.setMediaItem(MediaItem.fromUri(Uri.parse(fileUri)))
        player.prepare()
        player.setPlaybackSpeed(playbackSpeed)
        // Recite-back wants a pause after every āyah; plain listening should flow
        // through the whole range without the seek-induced gap between āyāt.
        if (_state.value.mode == MushafAudioMode.RECITE_BACK) {
            runImportedReciteBack(ayahIds, segments, repeat)
        } else {
            runImportedContinuous(ayahIds, segments, repeat)
        }
        player.pause()
        _state.update { it.copy(phase = MushafAudioPhase.IDLE, currentAyahId = null) }
    }

    /**
     * Plays the whole range as one continuous stretch (seek once to the start,
     * stop at the range end), mapping the play head to the current segment so the
     * highlight still tracks each āyah.
     */
    private suspend fun runImportedContinuous(
        ayahIds: List<Int>,
        segments: List<LongRange>,
        repeat: Int,
    ) {
        val rangeStart = segments.first().first
        val rangeEnd = segments.last().last
        var pass = 0
        while (repeat <= 0 || pass < repeat) {
            pass++
            _state.update {
                it.copy(currentAyahId = ayahIds.firstOrNull(), phase = MushafAudioPhase.PREPARING)
            }
            player.seekTo(rangeStart)
            player.playWhenReady = !_state.value.isPaused
            _state.update { it.copy(phase = MushafAudioPhase.PLAYING) }
            var currentIndex = -1
            while (true) {
                when (player.playbackState) {
                    Player.STATE_IDLE -> return // stopped
                    Player.STATE_ENDED -> break
                    else -> {
                        val pos = player.currentPosition
                        if (pos >= rangeEnd) break
                        val idx = segments.indexOfFirst { pos < it.last }
                            .let { if (it < 0) segments.lastIndex else it }
                        if (idx != currentIndex) {
                            currentIndex = idx
                            _state.update { it.copy(currentAyahId = ayahIds.getOrNull(idx)) }
                        }
                    }
                }
                delay(40)
            }
            lastDurationMs = (rangeEnd - rangeStart).coerceAtLeast(0)
            repository.logPractice(
                type = "listen",
                ayahCount = segments.size,
                durationMs = lastDurationMs,
            )
        }
    }

    /** Plays each āyah in turn, pausing for a recite-back gap after every one. */
    private suspend fun runImportedReciteBack(
        ayahIds: List<Int>,
        segments: List<LongRange>,
        repeat: Int,
    ) {
        var pass = 0
        while (repeat <= 0 || pass < repeat) {
            pass++
            for (i in segments.indices) {
                val seg = segments[i]
                _state.update {
                    it.copy(currentAyahId = ayahIds.getOrNull(i), phase = MushafAudioPhase.PREPARING)
                }
                player.seekTo(seg.first)
                player.playWhenReady = !_state.value.isPaused
                _state.update { it.copy(phase = MushafAudioPhase.PLAYING) }
                while (true) {
                    when (player.playbackState) {
                        Player.STATE_IDLE -> return // stopped
                        Player.STATE_ENDED -> break
                        else -> if (player.currentPosition >= seg.last) break
                    }
                    delay(40)
                }
                player.pause()
                lastDurationMs = (seg.last - seg.first).coerceAtLeast(0)
                repository.logPractice(type = "listen", ayahCount = 1, durationMs = lastDurationMs)
                reciteBackGap()
            }
        }
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
