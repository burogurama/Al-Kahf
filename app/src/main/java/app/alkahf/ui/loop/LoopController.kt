package app.alkahf.ui.loop

import android.net.Uri
import androidx.media3.common.MediaItem
import androidx.media3.common.Player
import androidx.media3.exoplayer.ExoPlayer
import app.alkahf.audio.LoopMode
import app.alkahf.audio.LoopSequencer
import app.alkahf.audio.LoopStep
import app.alkahf.data.PageAyah
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

enum class LoopPhase { PREPARING, PLAYING, GAP, COMPLETE, ERROR }

data class LoopUiState(
    val mode: LoopMode = LoopMode.CHAIN,
    val rangeStart: Int = 1,
    val rangeEnd: Int = 5,
    val perAyah: Int = 3,
    val perChain: Int = 5,
    val gapMultiplier: Float = 1.5f,
    val speed: Float = 1.0f,
    val isPaused: Boolean = false,
    val phase: LoopPhase = LoopPhase.PREPARING,
    val currentAyah: Int = 1,
    val spanStart: Int = 1,
    val spanEnd: Int = 1,
    val pass: Int = 1,
    val passCount: Int = 1,
    val nextToAdd: Int? = null,
    val positionMs: Long = 0,
    val durationMs: Long = 0,
    val highlightIndex: Int = -1,
    val surahLatin: String = "",
    val ayahs: Map<Int, PageAyah> = emptyMap(),
    val errorMessage: String? = null,
)

/**
 * Drives a drill session: expands the configuration via [LoopSequencer] and
 * walks the steps — play the ayah's audio, then hold a silent recite-back gap
 * scaled to the ayah's duration. Runs on the main dispatcher (ExoPlayer
 * requirement); downloads happen inside [AudioStore] on IO.
 */
class LoopController(
    private val repository: QuranRepository,
    private val audioStore: AudioStore,
    private val player: ExoPlayer,
    private val scope: CoroutineScope,
    private val surah: Int = 18,
) {
    private val _state = MutableStateFlow(LoopUiState())
    val state: StateFlow<LoopUiState> = _state.asStateFlow()

    private var steps: List<LoopStep> = emptyList()
    private var stepIndex = 0
    private var jumpTarget: Int? = null
    private var sessionJob: Job? = null

    fun start() {
        sessionJob?.cancel()
        jumpTarget = null
        player.stop()
        sessionJob = scope.launch { runSession() }
    }

    fun release() {
        sessionJob?.cancel()
        player.release()
    }

    fun togglePlayPause() {
        when (_state.value.phase) {
            LoopPhase.COMPLETE, LoopPhase.ERROR -> {
                _state.update { it.copy(isPaused = false, errorMessage = null) }
                start()
            }
            else -> {
                _state.update { it.copy(isPaused = !it.isPaused) }
                player.playWhenReady = !_state.value.isPaused
            }
        }
    }

    fun nextAyah() = jumpTo(stepIndex + 1)

    fun previousAyah() = jumpTo(stepIndex - 1)

    private fun jumpTo(target: Int) {
        if (steps.isEmpty()) return
        jumpTarget = target.coerceIn(0, steps.lastIndex)
        player.stop()
    }

    fun setMode(mode: LoopMode) {
        if (mode == _state.value.mode) return
        _state.update { it.copy(mode = mode, isPaused = false) }
        start()
    }

    fun adjustRangeEnd(delta: Int) {
        val current = _state.value
        val newEnd = (current.rangeEnd + delta).coerceIn(current.rangeStart, MAX_RANGE_END)
        if (newEnd == current.rangeEnd) return
        _state.update { it.copy(rangeEnd = newEnd, isPaused = false) }
        start()
    }

    fun cyclePerAyah() {
        _state.update { it.copy(perAyah = cycle(PER_AYAH_OPTIONS, it.perAyah)) }
        start()
    }

    fun cyclePerChain() {
        _state.update { it.copy(perChain = cycle(PER_CHAIN_OPTIONS, it.perChain)) }
        start()
    }

    fun cycleGap() {
        _state.update { it.copy(gapMultiplier = cycle(GAP_OPTIONS, it.gapMultiplier)) }
    }

    fun cycleSpeed() {
        val newSpeed = cycle(SPEED_OPTIONS, _state.value.speed)
        _state.update { it.copy(speed = newSpeed) }
        player.setPlaybackSpeed(newSpeed)
    }

    private fun <T> cycle(options: List<T>, current: T): T {
        val index = options.indexOf(current)
        return options[(index + 1) % options.size]
    }

    private suspend fun runSession() {
        if (_state.value.ayahs.isEmpty()) {
            val ayahs = repository.ayahsForRange(surah, 1, MAX_RANGE_END).associateBy { it.number }
            val latin = repository.surahNameLatin(surah)
            _state.update { it.copy(ayahs = ayahs, surahLatin = latin) }
        }
        val config = _state.value
        steps = LoopSequencer.steps(
            config.mode, config.rangeStart, config.rangeEnd, config.perAyah, config.perChain,
        )
        stepIndex = 0
        while (true) {
            val step = steps.getOrNull(stepIndex) ?: break
            applyStep(step)
            val file = downloadStep(step) ?: return
            val finishedNaturally = playFile(file)
            val jumped = consumeJump()
            if (jumped != null) {
                stepIndex = jumped
                continue
            }
            if (finishedNaturally) {
                reciteBackGap()
                val jumpedInGap = consumeJump()
                if (jumpedInGap != null) {
                    stepIndex = jumpedInGap
                    continue
                }
            }
            stepIndex++
        }
        _state.update { it.copy(phase = LoopPhase.COMPLETE, isPaused = false, highlightIndex = -1) }
    }

    private fun applyStep(step: LoopStep) {
        _state.update {
            it.copy(
                currentAyah = step.ayah,
                spanStart = step.spanStart,
                spanEnd = step.spanEnd,
                pass = step.pass,
                passCount = step.passCount,
                nextToAdd = step.nextToAdd,
                positionMs = 0,
                durationMs = 0,
                highlightIndex = -1,
            )
        }
    }

    private suspend fun downloadStep(step: LoopStep): File? {
        _state.update { it.copy(phase = LoopPhase.PREPARING) }
        return try {
            audioStore.ayahFile(surah, step.ayah)
        } catch (e: IOException) {
            _state.update {
                it.copy(
                    phase = LoopPhase.ERROR,
                    errorMessage = "Audio download failed — check your connection",
                )
            }
            null
        }
    }

    /** Plays the file to its end. Returns false if interrupted by a jump. */
    private suspend fun playFile(file: File): Boolean {
        player.setMediaItem(MediaItem.fromUri(Uri.fromFile(file)))
        player.prepare()
        player.setPlaybackSpeed(_state.value.speed)
        player.playWhenReady = !_state.value.isPaused
        _state.update { it.copy(phase = LoopPhase.PLAYING) }
        while (true) {
            when (player.playbackState) {
                Player.STATE_ENDED -> return true
                Player.STATE_IDLE -> return false
                else -> {}
            }
            val duration = player.duration.takeIf { it > 0 } ?: 0
            val position = player.currentPosition
            val words = _state.value.ayahs[_state.value.currentAyah]?.words.orEmpty()
            // Even-split word highlight; will be replaced by per-reciter QUL
            // word timestamps.
            val highlight = if (duration > 0 && words.isNotEmpty()) {
                ((position * words.size) / duration).toInt().coerceIn(0, words.size - 1)
            } else {
                -1
            }
            _state.update {
                it.copy(positionMs = position, durationMs = duration, highlightIndex = highlight)
            }
            delay(120)
        }
    }

    /** Silent pause after each play, sized for the user to recite back. */
    private suspend fun reciteBackGap() {
        val playedDuration = _state.value.durationMs
        val total = (playedDuration * _state.value.gapMultiplier / _state.value.speed).toLong()
        if (total <= 0) return
        _state.update {
            it.copy(phase = LoopPhase.GAP, positionMs = 0, durationMs = total, highlightIndex = -1)
        }
        var elapsed = 0L
        while (elapsed < total) {
            if (jumpTarget != null) return
            if (!_state.value.isPaused) {
                elapsed += GAP_TICK_MS
                _state.update { it.copy(positionMs = elapsed.coerceAtMost(total)) }
            }
            delay(GAP_TICK_MS)
        }
    }

    private fun consumeJump(): Int? {
        val target = jumpTarget ?: return null
        jumpTarget = null
        return target
    }

    companion object {
        const val MAX_RANGE_END = 6
        private const val GAP_TICK_MS = 100L
        private val PER_AYAH_OPTIONS = listOf(2, 3, 5, 7)
        private val PER_CHAIN_OPTIONS = listOf(3, 5, 7)
        private val GAP_OPTIONS = listOf(1.0f, 1.5f, 2.0f)
        private val SPEED_OPTIONS = listOf(0.75f, 1.0f, 1.25f, 1.5f)
    }
}
