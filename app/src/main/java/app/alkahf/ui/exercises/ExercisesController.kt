package app.alkahf.ui.exercises

import app.alkahf.data.QuranRepository
import app.alkahf.data.exercises.ExerciseQuestion
import app.alkahf.data.exercises.ExerciseScope
import app.alkahf.data.exercises.ExerciseType
import app.alkahf.data.exercises.SurahChoice
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

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

    private var sessionStartMs: Long = 0L
    private var resultRecorded = false

    /** Loads the sūrah type-ahead choices once, for the setup screen and runner. */
    suspend fun loadChoices() {
        if (_state.value.surahChoices.isNotEmpty()) return
        _state.value = _state.value.copy(surahChoices = repository.exerciseSurahChoices())
    }

    /** Builds a fresh session and enters it at the first question. */
    suspend fun generate(scope: ExerciseScope, types: Set<ExerciseType>, length: Int) {
        val questions = repository.buildExerciseSession(scope, types, length)
        val choices = _state.value.surahChoices.ifEmpty { repository.exerciseSurahChoices() }
        sessionStartMs = System.currentTimeMillis()
        resultRecorded = false
        _state.value = ExercisesUiState(
            questions = questions,
            answers = questions.map { q ->
                QuestionState(
                    order = (q as? ExerciseQuestion.OrderAyat)?.shuffledOrder?.map { it.id },
                )
            },
            currentIndex = 0,
            surahChoices = choices,
            ready = true,
        )
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
