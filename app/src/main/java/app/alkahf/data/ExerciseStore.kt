package app.alkahf.data

import app.alkahf.data.exercises.AyatItem
import app.alkahf.data.exercises.ExerciseQuestion
import app.alkahf.data.exercises.ExerciseScope
import app.alkahf.data.exercises.ExerciseType
import app.alkahf.data.exercises.SurahChoice
import app.alkahf.data.exercises.ArabicText
import java.time.LocalDate
import kotlin.random.Random

/** A completed Exercises session, summarised for the Today card. */
data class ExerciseResult(
    val correct: Int,
    val total: Int,
    val toRevisit: Int,
    val epochDay: Long,
)

/**
 * How much the user has ready to test, for the Today card's readiness line
 * ("3 sūrahs · 56 āyāt ready to test"): the count of memorized āyāt and how many
 * sūrahs they span.
 */
data class ExerciseReadiness(val ayahCount: Int, val surahCount: Int)

/**
 * Builds and grades Exercises sessions (memorization self-testing). Stateless
 * between calls: a session is just the list of [ExerciseQuestion]s returned by
 * [buildSession], each carrying its own answer key, so grading is local and
 * needs no DB round-trip. Pulls eligible āyāt from [MemorizationStore], text
 * from [QuranTextStore] (active riwāyah), and logs a completed session as a
 * practice event via [ReviewStore] so it counts toward the streak/weekly time.
 */
class ExerciseStore(
    private val quranText: QuranTextStore,
    private val memorization: MemorizationStore,
    private val review: ReviewStore,
    private val settings: UserPreferences,
) {
    /**
     * Generate up to [length] questions over [scope], each a random eligible
     * āyah paired with a random requested type. If a type can't be satisfied for
     * a pick (e.g. FINISH on a one-word āyah, ORDER without a long-enough run),
     * another requested type is tried. Returns fewer (or empty) when the pool is
     * too small. [seed] is exposed for deterministic tests; the default is random.
     */
    suspend fun buildSession(
        scope: ExerciseScope,
        types: Set<ExerciseType>,
        length: Int,
        seed: Long = Random.nextLong(),
    ): List<ExerciseQuestion> {
        if (types.isEmpty() || length <= 0) return emptyList()
        val eligible = eligibleAyahIds(scope)
        if (eligible.isEmpty()) return emptyList()

        // Load every eligible āyah once, grouped by sūrah, so ORDER can find
        // consecutive runs and FINISH/GUESS can read text without per-pick DB hits.
        val ayahsBySurah = loadAyahs(eligible)
        val byId = ayahsBySurah.values.flatten().associateBy { it.id }

        val rng = Random(seed)
        val pickPool = eligible.toMutableList()
        val typeList = types.toList()
        val questions = ArrayList<ExerciseQuestion>(length)

        while (questions.size < length && pickPool.isNotEmpty()) {
            val ayahId = pickPool.removeAt(rng.nextInt(pickPool.size))
            val ayah = byId[ayahId] ?: continue
            // Try the requested types in a random order until one fits this pick.
            val question = typeList.shuffled(rng)
                .firstNotNullOfOrNull { buildQuestion(it, ayah, ayahsBySurah, rng) }
            if (question != null) questions.add(question)
        }
        return questions
    }

    /** All 114 sūrahs for the Guess-the-Sūrah type-ahead. */
    suspend fun surahChoices(): List<SurahChoice> =
        quranText.surahOptions().map { SurahChoice(it.number, it.nameLatin, it.nameArabic) }

    // --- Validation helpers (called by the UI on Check) ---

    /** True when [input] reproduces the āyah's continuation on the rasm level. */
    fun checkFinish(input: String, q: ExerciseQuestion.FinishAyah): Boolean =
        ArabicText.matchesAyah(input, q.continuationText)

    /** True when the picked sūrah is the one the āyah belongs to. */
    fun checkGuess(pickedSurah: Int, q: ExerciseQuestion.GuessSurah): Boolean =
        pickedSurah == q.correctSurah

    /** True when [userOrder] (āyah ids) equals the correct sequence of ids. */
    fun checkOrder(userOrder: List<Int>, q: ExerciseQuestion.OrderAyat): Boolean =
        userOrder == q.correctOrder.map { it.id }

    /**
     * Per-position correctness for the Order result: `result[i]` is true when the
     * user placed the right āyah in position i. Length follows [userOrder]; a
     * short/long list is compared position-wise against the correct sequence.
     */
    fun orderPositionsCorrect(userOrder: List<Int>, q: ExerciseQuestion.OrderAyat): List<Boolean> {
        val correct = q.correctOrder.map { it.id }
        return userOrder.mapIndexed { i, id -> i < correct.size && correct[i] == id }
    }

    /**
     * Records a finished session: persists the summary for the Today card and
     * logs one practice event (type "exercise", ayah_count = questions answered)
     * so it counts toward the streak and weekly time. [durationMs] is the time
     * the user spent.
     */
    suspend fun recordResult(correct: Int, total: Int, toRevisit: Int, durationMs: Long) {
        settings.recordExerciseResult(
            correct = correct,
            total = total,
            toRevisit = toRevisit,
            epochDay = LocalDate.now().toEpochDay(),
        )
        review.logPractice(type = "exercise", ayahCount = total, durationMs = durationMs)
    }

    /** The last completed session for the Today card, or null when none. */
    fun lastResult(): ExerciseResult? = settings.lastExerciseResult

    /** How many memorized āyāt are ready to test and how many sūrahs they span. */
    suspend fun readiness(): ExerciseReadiness {
        val ids = memorizedAyahIds()
        val surahs = ids.map { it / 1000 }.distinct().size
        return ExerciseReadiness(ayahCount = ids.size, surahCount = surahs)
    }

    // --- Internals ---

    private suspend fun eligibleAyahIds(scope: ExerciseScope): List<Int> = when (scope) {
        ExerciseScope.AllMemorized -> memorizedAyahIds()
        is ExerciseScope.Surahs -> scope.ids.distinct().flatMap { surah ->
            val count = quranText.surahAyahCount(surah)
            (1..count).map { surah * 1000 + it }
        }
    }

    private suspend fun memorizedAyahIds(): List<Int> =
        memorization.allStates()
            .filter { (_, state) -> state >= MemorizationState.MEMORIZED.value }
            .keys
            .toList()

    /** Loads the text of [ids], grouped by sūrah and sorted by āyah number. */
    private suspend fun loadAyahs(ids: List<Int>): Map<Int, List<PageAyah>> {
        val result = HashMap<Int, List<PageAyah>>()
        for ((surah, surahIds) in ids.groupBy { it / 1000 }) {
            val numbers = surahIds.map { it % 1000 }
            val ayahs = quranText.ayahsForRange(surah, numbers.min(), numbers.max())
            // ayahsForRange returns a contiguous span; keep only the eligible ones.
            val wanted = surahIds.toSet()
            result[surah] = ayahs.filter { it.id in wanted }.sortedBy { it.number }
        }
        return result
    }

    private fun buildQuestion(
        type: ExerciseType,
        ayah: PageAyah,
        ayahsBySurah: Map<Int, List<PageAyah>>,
        rng: Random,
    ): ExerciseQuestion? = when (type) {
        ExerciseType.GUESS_SURAH -> ExerciseQuestion.GuessSurah(
            ayahId = ayah.id,
            ayahText = ayah.words.joinToString(" "),
            correctSurah = ayah.surah,
        )
        ExerciseType.FINISH_AYAH -> buildFinish(ayah)
        ExerciseType.ORDER_AYAT -> buildOrder(ayah, ayahsBySurah[ayah.surah].orEmpty(), rng)
    }

    /**
     * Split an āyah into a prompt (its first half by words) and a continuation
     * (the rest). Needs ≥ 2 words so both sides are non-empty; the split leaves
     * at least one word on each side. Null for too-short āyāt.
     */
    private fun buildFinish(ayah: PageAyah): ExerciseQuestion.FinishAyah? {
        val words = ayah.words
        if (words.size < 2) return null
        val cut = (words.size / 2).coerceIn(1, words.size - 1)
        return ExerciseQuestion.FinishAyah(
            ayahId = ayah.id,
            surah = ayah.surah,
            number = ayah.number,
            promptText = words.subList(0, cut).joinToString(" "),
            continuationText = words.subList(cut, words.size).joinToString(" "),
        )
    }

    /**
     * Build an Order question from a run of 3–5 consecutive āyāt within the
     * sūrah's eligible set that contains [ayah]. Null when no such run exists.
     * The shuffled order is guaranteed to differ from the correct one (unless the
     * run is degenerate).
     */
    private fun buildOrder(
        ayah: PageAyah,
        surahAyahs: List<PageAyah>,
        rng: Random,
    ): ExerciseQuestion.OrderAyat? {
        val run = consecutiveRunAround(ayah, surahAyahs, max = 5) ?: return null
        val correct = run.map { AyatItem(it.id, it.number, it.words.joinToString(" ")) }
        var shuffled = correct.shuffled(rng)
        // Avoid handing back the already-correct order.
        var attempts = 0
        while (shuffled == correct && attempts < 8) {
            shuffled = correct.shuffled(rng)
            attempts++
        }
        return ExerciseQuestion.OrderAyat(
            surah = ayah.surah,
            correctOrder = correct,
            shuffledOrder = shuffled,
        )
    }

    /**
     * The longest run of consecutive āyāt (by number) within [surahAyahs] that
     * includes [ayah], capped at [max] and centred on [ayah]. Null when fewer
     * than 3 consecutive āyāt are available around it.
     */
    private fun consecutiveRunAround(
        ayah: PageAyah,
        surahAyahs: List<PageAyah>,
        max: Int,
    ): List<PageAyah>? {
        if (surahAyahs.size < 3) return null
        val byNumber = surahAyahs.associateBy { it.number }
        // Extend left and right from the anchor while numbers stay contiguous.
        var lo = ayah.number
        var hi = ayah.number
        while (hi - lo + 1 < max && byNumber.containsKey(lo - 1)) lo--
        while (hi - lo + 1 < max && byNumber.containsKey(hi + 1)) hi++
        val run = (lo..hi).mapNotNull { byNumber[it] }
        return if (run.size >= 3) run else null
    }
}
