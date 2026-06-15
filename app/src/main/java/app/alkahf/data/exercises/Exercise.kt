package app.alkahf.data.exercises

/**
 * Domain types for the Exercises feature (memorization self-testing). Pure data:
 * no Android, no Compose, no persistence — the store builds these and the UI
 * renders them. Each [ExerciseQuestion] carries everything needed to display the
 * prompt and to grade the answer locally (the answer key travels with it).
 */

/** What the session tests over. */
sealed interface ExerciseScope {
    /** Every āyah the user has memorized (state ≥ MEMORIZED). */
    data object AllMemorized : ExerciseScope

    /** Every āyah of the chosen sūrahs (by sūrah number). */
    data class Surahs(val ids: List<Int>) : ExerciseScope
}

/** The kinds of exercise the user can enable for a session. */
enum class ExerciseType { GUESS_SURAH, FINISH_AYAH, ORDER_AYAT }

/** One āyah in an "Order the Āyāt" sequence. */
data class AyatItem(
    val id: Int, // surah * 1000 + number
    val number: Int,
    val text: String,
)

/**
 * A sūrah entry for the "Guess the Sūrah" type-ahead. Surah *meanings* are not
 * in the bundled Qurʾān DB, so they are omitted; the UI shows transliteration +
 * Arabic name + number, which the dropdown disambiguates.
 */
data class SurahChoice(
    val number: Int,
    val nameLatin: String,
    val nameArabic: String,
)

/**
 * A generated question with its prompt references and answer key. The variants
 * mirror the three exercise types; grading is done by the store's check helpers
 * against the fields below.
 */
sealed interface ExerciseQuestion {
    val type: ExerciseType

    /**
     * Show an āyah, name its sūrah. Answer key is [correctSurah]; the UI picks a
     * sūrah from the type-ahead and compares ids.
     */
    data class GuessSurah(
        val ayahId: Int,
        val ayahText: String,
        val correctSurah: Int,
    ) : ExerciseQuestion {
        override val type get() = ExerciseType.GUESS_SURAH
    }

    /**
     * Show the opening of an āyah ([promptText], the first ~half by words), the
     * user writes the rest. [continuationText] is the remaining words and is the
     * grading reference (matched on rasm via [ArabicText.matchesAyah]).
     */
    data class FinishAyah(
        val ayahId: Int,
        val surah: Int,
        val number: Int,
        val promptText: String,
        val continuationText: String,
    ) : ExerciseQuestion {
        override val type get() = ExerciseType.FINISH_AYAH
    }

    /**
     * Show 3–5 consecutive āyāt shuffled; the user restores [correctOrder].
     * [shuffledOrder] is the same items in the presented (jumbled) order.
     */
    data class OrderAyat(
        val surah: Int,
        val correctOrder: List<AyatItem>,
        val shuffledOrder: List<AyatItem>,
    ) : ExerciseQuestion {
        override val type get() = ExerciseType.ORDER_AYAT
    }
}
