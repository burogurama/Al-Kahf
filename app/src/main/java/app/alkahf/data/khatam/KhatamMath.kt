package app.alkahf.data.khatam

import kotlin.math.ceil

/** How a khatam's progress compares to its planned pace. */
enum class PaceStatus { AHEAD, ON_TRACK, BEHIND }

/**
 * Pure khatam arithmetic — no Android, no I/O, no clock. Every value is a plain
 * function of the stored fields so it is trivial to reason about and reuse.
 *
 * Dates are epoch days (LocalDate.toEpochDay); a khatam covers [TOTAL_UNITS]
 * juzʼ at [pace] juzʼ per day.
 */
object KhatamMath {
    const val TOTAL_UNITS = 30

    /** The 1-based day number of the khatam, given today's epoch day. */
    fun currentDay(startDate: Long, today: Long): Int =
        ((today - startDate) + 1).coerceAtLeast(1L).toInt()

    /**
     * The planned finish date (epoch day): a stable target that depends only on
     * the start date and pace — the start plus the number of days the whole
     * [TOTAL_UNITS] takes at [pace]. It does not move with today or with
     * progress, so it stays constant for the life of the khatam (the pace-status
     * pill conveys ahead/behind).
     */
    fun derivedFinishDate(startDate: Long, pace: Int): Long {
        val p = pace.coerceAtLeast(1)
        val days = ceil(TOTAL_UNITS.toDouble() / p).toLong()
        return startDate + days
    }

    /**
     * Progress vs. plan, measured against [currentDay] (≥1, today) at [pace]
     * (≥1) juzʼ per day:
     *  - BEHIND when fewer units are read than prior days required
     *    (< (currentDay − 1) × pace);
     *  - AHEAD when more units are read than today requires
     *    (> currentDay × pace);
     *  - ON_TRACK otherwise.
     */
    fun paceStatus(unitsCompleted: Int, currentDay: Int, pace: Int): PaceStatus {
        val p = pace.coerceAtLeast(1)
        val day = currentDay.coerceAtLeast(1)
        val priorRequirement = (day - 1) * p
        val todayRequirement = day * p
        return when {
            unitsCompleted < priorRequirement -> PaceStatus.BEHIND
            unitsCompleted > todayRequirement -> PaceStatus.AHEAD
            else -> PaceStatus.ON_TRACK
        }
    }

    /** The juzʼ to read today: the next uncompleted unit, capped at 30. */
    fun todaysPortionJuz(unitsCompleted: Int): Int =
        (unitsCompleted + 1).coerceIn(1, TOTAL_UNITS)

    /** Completed fraction of the whole Qur'an, 0f..1f. */
    fun ringFraction(unitsCompleted: Int): Float =
        unitsCompleted.coerceIn(0, TOTAL_UNITS) / TOTAL_UNITS.toFloat()

    /** Completed percentage, 0..100. */
    fun percent(unitsCompleted: Int): Int =
        (ringFraction(unitsCompleted) * 100).toInt()
}
