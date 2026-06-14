package app.alkahf.ui.loop

import android.content.Context
import androidx.media3.exoplayer.ExoPlayer
import app.alkahf.audio.AyahPlayer
import app.alkahf.audio.LoopMode
import app.alkahf.audio.LoopSequencer
import app.alkahf.audio.LoopStep
import app.alkahf.audio.PlayResult
import app.alkahf.data.LoopPreset
import app.alkahf.data.Riwayah
import app.alkahf.data.PageAyah
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
    val surah: Int = 18,
    val surahLatin: String = "",
    val surahAyahCount: Int = 110,
    val reciterPath: String = AudioStore.DEFAULT_RECITER,
    val reciterName: String = "Ḥuṣarī",
    val riwayah: Riwayah = Riwayah.HAFS,
    val ayahs: Map<Int, PageAyah> = emptyMap(),
    val errorMessage: String? = null,
) {
    fun toPreset(): LoopPreset = LoopPreset(
        surah = surah,
        surahNameLatin = surahLatin,
        ayahFrom = rangeStart,
        ayahTo = rangeEnd,
        reciterPath = reciterPath,
        reciterName = reciterName,
        perAyah = perAyah,
        perChain = perChain,
        gapMultiplier = gapMultiplier,
        speed = speed,
        riwayah = riwayah,
    )
}

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
    private val context: Context,
    initialPreset: LoopPreset? = null,
) {
    private val _state = MutableStateFlow(
        initialPreset?.let { applyPresetTo(LoopUiState(), it) } ?: LoopUiState(),
    )
    val state: StateFlow<LoopUiState> = _state.asStateFlow()

    private var steps: List<LoopStep> = emptyList()
    private var stepIndex = 0
    private var jumpTarget: Int? = null
    private var sessionJob: Job? = null
    private var loadedSurah = -1
    private var loadedRiwayah: Riwayah? = null
    // Resolved when the current reciter is an imported one ("custom:<id>"): the
    // single audio file plus each āyah's segment. Null for built-in reciters.
    private var importedAudio: app.alkahf.data.ImportedSurahAudio? = null

    fun applyPreset(preset: LoopPreset) {
        _state.update { applyPresetTo(it, preset) }
        player.setPlaybackSpeed(preset.speed)
        start()
    }

    private fun applyPresetTo(base: LoopUiState, preset: LoopPreset): LoopUiState = base.copy(
        surah = preset.surah,
        surahLatin = preset.surahNameLatin,
        rangeStart = preset.ayahFrom,
        rangeEnd = preset.ayahTo,
        reciterPath = preset.reciterPath,
        reciterName = preset.reciterName,
        riwayah = preset.riwayah,
        perAyah = preset.perAyah,
        perChain = preset.perChain,
        gapMultiplier = preset.gapMultiplier,
        speed = preset.speed,
        isPaused = false,
        errorMessage = null,
    )

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
        val maxEnd = minOf(current.rangeStart + MAX_SPAN - 1, current.surahAyahCount)
        val newEnd = (current.rangeEnd + delta).coerceIn(current.rangeStart, maxEnd)
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
        // Reload when the sūrah OR the riwāyah changes, so the displayed text
        // always matches the audio for the current reading.
        if (loadedSurah != _state.value.surah || loadedRiwayah != _state.value.riwayah) {
            val surah = _state.value.surah
            // Load the drill's own riwāyah text (independent of the app setting).
            val ayahs = repository.ayahsForRange(surah, 1, 300, _state.value.riwayah)
                .associateBy { it.number }
            val latin = repository.surahNameLatin(surah)
            _state.update {
                it.copy(
                    ayahs = ayahs,
                    surahLatin = latin,
                    surahAyahCount = ayahs.size,
                    rangeEnd = it.rangeEnd.coerceAtMost(ayahs.size),
                )
            }
            loadedSurah = surah
            loadedRiwayah = _state.value.riwayah
        }
        // An imported reciter plays from its single timed file; resolve it once.
        val reciterPath = _state.value.reciterPath
        importedAudio = if (reciterPath.startsWith("custom:")) {
            repository.importedSurahAudio(reciterPath, _state.value.surah)
        } else {
            null
        }
        val config = _state.value
        steps = LoopSequencer.steps(
            config.mode, config.rangeStart, config.rangeEnd, config.perAyah, config.perChain,
        )
        stepIndex = 0
        while (true) {
            val step = steps.getOrNull(stepIndex) ?: break
            applyStep(step)
            val finishedNaturally = playAyah(step) ?: return
            val jumped = consumeJump()
            if (jumped != null) {
                stepIndex = jumped
                continue
            }
            if (finishedNaturally) {
                repository.logPractice(
                    type = "loop",
                    ayahCount = 1,
                    durationMs = _state.value.durationMs,
                )
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

    /**
     * Plays one āyah of [step]. Returns whether it finished naturally (false when
     * a jump interrupted it), or null when the audio couldn't be obtained.
     */
    private suspend fun playAyah(step: LoopStep): Boolean? {
        if (_state.value.reciterPath.startsWith("custom:")) {
            val segment = importedAudio?.segments?.get(step.ayah)
            if (importedAudio == null || segment == null) {
                // The imported reciter hasn't timed this āyah — nothing to play.
                _state.update {
                    it.copy(
                        phase = LoopPhase.ERROR,
                        errorMessage = context.getString(R.string.loop_audio_download_failed),
                    )
                }
                return null
            }
            return playImportedSegment(importedAudio!!.fileUri, segment)
        }
        // One drill āyah maps to one (Hafs) or several (Warsh merges) everyayah
        // files; play them back to back as a single step.
        val hafsAyahs = repository.audioAyahs(_state.value.surah, step.ayah, _state.value.riwayah)
        var finished = true
        for (hafsAyah in hafsAyahs) {
            val file = downloadAyah(hafsAyah) ?: return null
            finished = playFile(file)
            if (!finished) break
        }
        return finished
    }

    /** Plays a single āyah's [segment] of an imported file. False if a jump cut it. */
    private suspend fun playImportedSegment(fileUri: String, segment: LongRange): Boolean {
        val total = (segment.last - segment.first).coerceAtLeast(0)
        _state.update { it.copy(phase = LoopPhase.PLAYING, durationMs = total) }
        val result = AyahPlayer.playSegment(
            player, fileUri, segment, _state.value.speed, { _state.value.isPaused },
        ) { position, durationMs ->
            _state.update {
                it.copy(positionMs = position, durationMs = durationMs, highlightIndex = highlight(position, durationMs))
            }
        }
        if (result == PlayResult.STOPPED) return false
        player.pause()
        return true
    }

    private suspend fun downloadAyah(ayah: Int): File? {
        _state.update { it.copy(phase = LoopPhase.PREPARING) }
        return try {
            audioStore.ayahFile(_state.value.surah, ayah, _state.value.reciterPath)
        } catch (e: IOException) {
            _state.update {
                it.copy(
                    phase = LoopPhase.ERROR,
                    errorMessage = context.getString(R.string.loop_audio_download_failed),
                )
            }
            null
        }
    }

    /** Plays the file to its end. Returns false if interrupted by a jump. */
    private suspend fun playFile(file: File): Boolean {
        _state.update { it.copy(phase = LoopPhase.PLAYING) }
        val result = AyahPlayer.playFile(
            player, file, _state.value.speed, { _state.value.isPaused },
        ) { position, durationMs ->
            _state.update {
                it.copy(positionMs = position, durationMs = durationMs, highlightIndex = highlight(position, durationMs))
            }
        }
        return result == PlayResult.ENDED
    }

    /** Even split of the current āyah's words across [durationMs]; -1 when unknown. */
    private fun highlight(positionMs: Long, durationMs: Long): Int {
        val words = _state.value.ayahs[_state.value.currentAyah]?.words.orEmpty()
        if (durationMs <= 0 || words.isEmpty()) return -1
        return ((positionMs * words.size) / durationMs).toInt().coerceIn(0, words.size - 1)
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
        const val MAX_SPAN = 20
        private const val GAP_TICK_MS = 100L
        private val PER_AYAH_OPTIONS = listOf(2, 3, 5, 7)
        private val PER_CHAIN_OPTIONS = listOf(3, 5, 7)
        private val GAP_OPTIONS = listOf(1.0f, 1.5f, 2.0f)
        private val SPEED_OPTIONS = listOf(0.75f, 1.0f, 1.25f, 1.5f)
    }
}
