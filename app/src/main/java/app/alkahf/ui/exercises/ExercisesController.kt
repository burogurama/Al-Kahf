package app.alkahf.ui.exercises

import app.alkahf.data.ExerciseSession
import app.alkahf.data.QuranRepository
import app.alkahf.data.exercises.ExerciseQuestion
import app.alkahf.data.exercises.ExerciseScope
import app.alkahf.data.exercises.ExerciseType
import app.alkahf.data.exercises.SurahChoice
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

/** The grading status of a single answered question. */
enum class AnswerStatus { UNANSWERED, CORRECT, NOT_QUITE }

/**
 * One question's working state inside a session: the user's in-progress answer
 * and, once checked, whether it was correct. The answer shape depends on the
 * question type (a picked sūrah id, a written Arabic string, or an ordering of
 * āyah ids); only the relevant field is used per type.
 */
data class QuestionState(
    val status: AnswerStatus = AnswerStatus.UNANSWERED,
    /** Guess the Sūrah — the picked sūrah number, or null until one is chosen. */
    val pickedSurah: Int? = null,
    /** Finish the Āyah — the live written continuation. */
    val written: String = "",
    /** Order the Āyāt — the current arrangement as āyah ids; null until seeded. */
    val order: List<Int>? = null,
)

/** The whole Exercises session, published as one [StateFlow]. */
data class ExercisesUiState(
    val questions: List<ExerciseQuestion> = emptyList(),
    val answers: List<QuestionState> = emptyList(),
    val currentIndex: Int = 0,
    val surahChoices: List<SurahChoice> = emptyList(),
    val durationMs: Long = 0L,
    /** Null until the session has been built. */
    val ready: Boolean = false,
) {
    val total: Int get() = questions.size
    val current: ExerciseQuestion? get() = questions.getOrNull(currentIndex)
    val currentState: QuestionState get() = answers.getOrNull(currentIndex) ?: QuestionState()
    val correctCount: Int get() = answers.count { it.status == AnswerStatus.CORRECT }
    val answeredCount: Int get() = answers.count { it.status != AnswerStatus.UNANSWERED }
    val toRevisitCount: Int get() = answers.count { it.status == AnswerStatus.NOT_QUITE }

    /** Indices of questions answered not-quite, in order — the "to revisit" list. */
    val toRevisitIndices: List<Int>
        get() = answers.indices.filter { answers[it].status == AnswerStatus.NOT_QUITE }

    /** True when every question has been checked. */
    val allAnswered: Boolean get() = total > 0 && answeredCount == total
}

/**
 * Drives an Exercises session: building it from a chosen scope/types/length,
 * holding the per-question answer + status, free next/previous navigation, the
 * running tallies, and persisting the final result.
 *
 * Mirrors the per-screen controller pattern ([app.alkahf.ui.library.LibraryController],
 * [app.alkahf.ui.review.ReviewController]): one [StateFlow] reloaded after each
 * mutation so the runner and result screens read from a single source.
 */
class ExercisesController(private val repository: QuranRepository) {
    private val _state = MutableStateFlow(ExercisesUiState())
    val state: StateFlow<ExercisesUiState> = _state.asStateFlow()

    /** Persisted sessions for the setup screen's "Recent" list. */
    private val _savedSessions = MutableStateFlow<List<ExerciseSession>>(emptyList())
    val savedSessions: StateFlow<List<ExerciseSession>> = _savedSessions.asStateFlow()

    private var sessionStartMs: Long = 0L
    private var resultRecorded = false

    // The active session's persistence identity + config, so edits can be saved
    // and the session re-encoded as the user works through it.
    private var sessionId: Long? = null
    private var sessionScope: ExerciseScope = ExerciseScope.AllMemorized
    private var sessionTypes: Set<ExerciseType> = emptySet()
    private var sessionLength: Int = 0
    private val persistScope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)

    /** Loads the sūrah type-ahead choices once, for the setup screen and runner. */
    suspend fun loadChoices() {
        if (_state.value.surahChoices.isNotEmpty()) return
        _state.value = _state.value.copy(surahChoices = repository.exerciseSurahChoices())
    }

    /** Builds a fresh session, persists it, and enters it at the first question. */
    suspend fun generate(scope: ExerciseScope, types: Set<ExerciseType>, length: Int) {
        val questions = repository.buildExerciseSession(scope, types, length)
        val choices = _state.value.surahChoices.ifEmpty { repository.exerciseSurahChoices() }
        sessionStartMs = System.currentTimeMillis()
        resultRecorded = false
        sessionScope = scope
        sessionTypes = types
        sessionLength = length
        val answers = questions.map { q ->
            QuestionState(order = (q as? ExerciseQuestion.OrderAyat)?.shuffledOrder?.map { it.id })
        }
        _state.value = ExercisesUiState(
            questions = questions,
            answers = answers,
            currentIndex = 0,
            surahChoices = choices,
            ready = true,
        )
        sessionId = if (questions.isEmpty()) {
            null
        } else {
            val payload = ExerciseSessionCodec.encode(scope, types, length, questions, answers, 0, 0L)
            repository.saveExerciseSession(payload, types, questions.size)
        }
    }

    /** Loads the persisted sessions (pruning finished ones older than a day). */
    suspend fun loadSavedSessions() {
        repository.pruneExerciseSessions()
        _savedSessions.value = repository.exerciseSessions()
    }

    /**
     * Opens a saved session for resume (in-progress) or review (finished). Returns
     * true when the session is already finished. Resume lands on the first
     * unanswered question.
     */
    suspend fun open(id: Long): Boolean {
        val session = repository.exerciseSession(id) ?: return false
        val decoded = ExerciseSessionCodec.decode(session.payload)
        val choices = _state.value.surahChoices.ifEmpty { repository.exerciseSurahChoices() }
        sessionId = id
        sessionScope = decoded.scope
        sessionTypes = decoded.types
        sessionLength = decoded.length
        resultRecorded = session.isFinished
        sessionStartMs = System.currentTimeMillis() - decoded.durationMs
        val firstUnanswered = decoded.answers.indexOfFirst { it.status == AnswerStatus.UNANSWERED }
        val index = when {
            session.isFinished -> 0
            firstUnanswered >= 0 -> firstUnanswered
            else -> decoded.currentIndex
        }.coerceIn(0, (decoded.questions.size - 1).coerceAtLeast(0))
        _state.value = ExercisesUiState(
            questions = decoded.questions,
            answers = decoded.answers,
            currentIndex = index,
            surahChoices = choices,
            durationMs = decoded.durationMs,
            ready = true,
        )
        return session.isFinished
    }

    /** Deletes a persisted session and refreshes the list. */
    suspend fun deleteSession(id: Long) {
        repository.deleteExerciseSession(id)
        if (sessionId == id) sessionId = null
        loadSavedSessions()
    }

    fun goTo(index: Int) {
        val s = _state.value
        if (index in s.questions.indices) _state.value = s.copy(currentIndex = index)
    }

    fun next() = goTo(_state.value.currentIndex + 1)

    fun previous() = goTo(_state.value.currentIndex - 1)

    // --- Per-question editing (only valid before the answer is checked) ---

    fun pickSurah(surah: Int) = updateCurrent { it.copy(pickedSurah = surah) }

    fun setWritten(text: String) = updateCurrent { it.copy(written = text) }

    fun appendWritten(piece: String) = updateCurrent { it.copy(written = it.written + piece) }

    fun backspaceWritten() = updateCurrent {
        if (it.written.isEmpty()) it else it.copy(written = it.written.dropLast(1))
    }

    /** Reorders the current Order-the-Āyāt answer by swapping two positions. */
    fun swapOrder(a: Int, b: Int) = updateCurrent { qs ->
        val order = qs.order?.toMutableList() ?: return@updateCurrent qs
        if (a in order.indices && b in order.indices) {
            val tmp = order[a]; order[a] = order[b]; order[b] = tmp
        }
        qs.copy(order = order)
    }

    /** Moves the item at [index] up (−1) or down (+1) by one position. */
    fun moveOrder(index: Int, delta: Int) {
        val target = index + delta
        val order = _state.value.currentState.order ?: return
        if (index in order.indices && target in order.indices) swapOrder(index, target)
    }

    /** Grades the current answer and locks it. Idempotent if already checked. */
    fun checkCurrent() {
        val s = _state.value
        val q = s.current ?: return
        val qs = s.currentState
        if (qs.status != AnswerStatus.UNANSWERED) return
        val correct = when (q) {
            is ExerciseQuestion.GuessSurah ->
                qs.pickedSurah != null && repository.checkExerciseGuess(qs.pickedSurah, q)
            is ExerciseQuestion.FinishAyah ->
                repository.checkExerciseFinish(qs.written, q)
            is ExerciseQuestion.OrderAyat ->
                qs.order != null && repository.checkExerciseOrder(qs.order, q)
        }
        updateCurrent {
            it.copy(status = if (correct) AnswerStatus.CORRECT else AnswerStatus.NOT_QUITE)
        }
        persistProgress()
    }

    /** Saves the current answers to the persisted session (fire-and-forget). */
    private fun persistProgress() {
        val id = sessionId ?: return
        val s = _state.value
        val duration = (System.currentTimeMillis() - sessionStartMs).coerceAtLeast(0L)
        val payload = ExerciseSessionCodec.encode(
            sessionScope, sessionTypes, sessionLength, s.questions, s.answers, s.currentIndex, duration,
        )
        val correct = s.correctCount
        val answered = s.answeredCount
        persistScope.launch { repository.updateExerciseSession(id, payload, correct, answered) }
    }

    /** Per-position correctness of the current Order answer (for the marked rows). */
    fun orderPositions(q: ExerciseQuestion.OrderAyat): List<Boolean> {
        val order = _state.value.currentState.order ?: return emptyList()
        return repository.exerciseOrderPositions(order, q)
    }

    /**
     * Persists the finished session (once). Called when the result screen opens.
     */
    suspend fun finish() {
        if (resultRecorded) return
        resultRecorded = true
        val s = _state.value
        val duration = (System.currentTimeMillis() - sessionStartMs).coerceAtLeast(0L)
        _state.value = s.copy(durationMs = duration)
        repository.recordExerciseResult(
            correct = s.correctCount,
            total = s.total,
            toRevisit = s.toRevisitCount,
            durationMs = duration,
        )
        sessionId?.let { id ->
            val payload = ExerciseSessionCodec.encode(
                sessionScope, sessionTypes, sessionLength, s.questions, s.answers, s.currentIndex, duration,
            )
            repository.finishExerciseSession(id, payload, s.correctCount, s.answeredCount)
        }
    }

    private inline fun updateCurrent(transform: (QuestionState) -> QuestionState) {
        val s = _state.value
        val i = s.currentIndex
        if (i !in s.answers.indices) return
        val updated = s.answers.toMutableList()
        updated[i] = transform(updated[i])
        _state.value = s.copy(answers = updated)
    }
}
