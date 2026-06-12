package app.alkahf.data.review

import kotlin.math.max
import kotlin.math.roundToInt

enum class ReviewGrade { FORGOT, HESITANT, PERFECT }

/**
 * SM-2-style interval scheduling for murājaʿah portions.
 *
 * The grade buttons in the review UI must show the intervals this object
 * actually assigns — they are the honest consequence of each grade.
 */
object ReviewScheduler {

    /** Stumbles recorded during the self-test cap the grade: a stumbled "Perfect" is Hesitant. */
    fun effectiveGrade(grade: ReviewGrade, stumbleCount: Int): ReviewGrade =
        if (grade == ReviewGrade.PERFECT && stumbleCount > 0) ReviewGrade.HESITANT else grade

    fun nextIntervalDays(currentIntervalDays: Int, grade: ReviewGrade): Int = when (grade) {
        ReviewGrade.FORGOT -> 1
        ReviewGrade.HESITANT -> 3
        ReviewGrade.PERFECT -> max(4, (currentIntervalDays * GROWTH_FACTOR).roundToInt())
    }

    fun intervalLabel(days: Int): String = when {
        days <= 1 -> "again tomorrow"
        days < 7 -> "in $days days"
        days < 11 -> "in 1 week"
        else -> "in ${(days + 3) / 7} weeks"
    }

    fun gradeLabel(grade: ReviewGrade): String = when (grade) {
        ReviewGrade.FORGOT -> "Forgot"
        ReviewGrade.HESITANT -> "Hesitant"
        ReviewGrade.PERFECT -> "Perfect"
    }

    private const val GROWTH_FACTOR = 2.5
}
