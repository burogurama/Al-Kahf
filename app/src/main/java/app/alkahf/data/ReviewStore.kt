package app.alkahf.data

import app.alkahf.data.review.ReviewGrade
import app.alkahf.data.review.ReviewScheduler
import app.alkahf.data.user.PracticeEventEntity
import app.alkahf.data.user.ReviewPortionEntity
import app.alkahf.data.user.UserDao
import java.time.LocalDate

/**
 * Spaced-repetition review (the murājaʿah queue and SM-2 scheduling), practice
 * logging, and the whole-mushaf progress roll-up shown on the Progress and Home
 * screens. Pulls āyah states from [MemorizationStore] and sūrah data from
 * [QuranTextStore]; owns the practice/review tables.
 */
class ReviewStore(
    private val userDao: UserDao,
    private val quranText: QuranTextStore,
    private val memorization: MemorizationStore,
    private val settings: UserPreferences,
) {
    /**
     * Registers a memorized range for spaced-repetition review (idempotent). The
     * first review is scheduled for the same day so the just-memorized portion
     * joins today's murājaʿah, then SM-2 spaces it out from there.
     */
    suspend fun enrollPortion(surah: Int, from: Int, to: Int) {
        if (userDao.portionFor(surah, from, to) != null) return
        userDao.insertPortions(
            listOf(
                ReviewPortionEntity(
                    surah = surah,
                    ayahFrom = from,
                    ayahTo = to,
                    intervalDays = 1,
                    dueEpochDay = LocalDate.now().toEpochDay(),
                ),
            ),
        )
    }

    suspend fun duePortions(): List<ReviewPortion> {
        // Cap the queue to the daily time budget (≈1.6 min per portion).
        val budgetLimit = (settings.dailyBudgetMin / 1.6f).toInt().coerceAtLeast(1)
        return userDao.duePortions(LocalDate.now().toEpochDay()).take(budgetLimit).map { entity ->
            val surahRow = quranText.surah(entity.surah)
            val surahLen = surahRow.ayahCount
            // Load two āyāt of context on each side (shown, not concealed) so the
            // hafiz can place the portion they're reciting from memory.
            ReviewPortion(
                id = entity.id,
                surah = entity.surah,
                surahNameLatin = surahRow.nameLatin,
                ayahFrom = entity.ayahFrom,
                ayahTo = entity.ayahTo,
                intervalDays = entity.intervalDays,
                ayahs = quranText.ayahsForRange(
                    entity.surah,
                    (entity.ayahFrom - 2).coerceAtLeast(1),
                    (entity.ayahTo + 2).coerceAtMost(surahLen),
                ),
            )
        }
    }

    suspend fun commitGrade(portion: ReviewPortion, grade: ReviewGrade) {
        val nextInterval = ReviewScheduler.nextIntervalDays(
            portion.intervalDays, grade, settings.reviewPacing.growthFactor,
        )
        userDao.updateSchedule(
            id = portion.id,
            intervalDays = nextInterval,
            dueEpochDay = LocalDate.now().toEpochDay() + nextInterval,
        )
        // The grade is also the honest signal of the portion's ayah states.
        val state = when (grade) {
            ReviewGrade.FORGOT -> MemorizationState.LEARNING
            ReviewGrade.HESITANT -> MemorizationState.MEMORIZED
            ReviewGrade.PERFECT -> MemorizationState.STRONG
        }
        memorization.markStates(portion.ayahs.map { it.id }, state)
        logPractice(type = "review", ayahCount = portion.ayahs.size, durationMs = 0)
    }

    suspend fun logPractice(type: String, ayahCount: Int, durationMs: Long) {
        userDao.addPracticeEvent(
            PracticeEventEntity(
                type = type,
                ayahCount = ayahCount,
                durationMs = durationMs,
                epochDay = LocalDate.now().toEpochDay(),
                createdAt = System.currentTimeMillis(),
            ),
        )
    }

    /** A short summary of today's due murājaʿah for the Home card. */
    suspend fun reviewSummary(): ReviewSummary {
        val due = userDao.duePortions(LocalDate.now().toEpochDay())
        val names = due.map { quranText.surahNameLatin(it.surah) }
        return ReviewSummary(
            count = due.size,
            minutes = (due.size * 1.6f + 0.5f).toInt().coerceAtLeast(if (due.isEmpty()) 0 else 1),
            names = names,
        )
    }

    suspend fun weekSummary(today: LocalDate): WeekSummary {
        val practicedDays = userDao.practiceDays().toSet()
        val days = (6 downTo 0).map { today.minusDays(it.toLong()) }
        val letters = days.map { it.dayOfWeek.getDisplayName(java.time.format.TextStyle.NARROW, java.util.Locale.ENGLISH) }
        val practiced = days.map { it.toEpochDay() in practicedDays }
        return WeekSummary(
            dayLetters = letters,
            practiced = practiced,
            ayatThisWeek = userDao.ayahCountSince(today.toEpochDay() - 6),
            daysPracticed = practiced.count { it },
        )
    }

    suspend fun currentStreak(): Int {
        val days = userDao.practiceDays()
        if (days.isEmpty()) return 0
        val today = LocalDate.now().toEpochDay()
        var expected = if (days.first() == today) today else today - 1
        var streak = 0
        for (day in days) {
            if (day != expected) break
            streak++
            expected--
        }
        return streak
    }

    suspend fun progressSnapshot(): ProgressSnapshot {
        val states = memorization.allStates()
        val locations = quranText.ayahLocations()

        val pageWeakest = IntArray(PAGE_COUNT) { MemorizationState.STRONG.value }
        val pageTouched = BooleanArray(PAGE_COUNT)
        val juzTotals = IntArray(31)
        val juzMemorized = IntArray(31)
        val juzLearning = IntArray(31)
        for (location in locations) {
            val state = states[location.id] ?: 0
            val pageIndex = location.page - 1
            if (state > 0) pageTouched[pageIndex] = true
            if (state < pageWeakest[pageIndex]) pageWeakest[pageIndex] = state
            juzTotals[location.juz]++
            if (state >= MemorizationState.MEMORIZED.value) juzMemorized[location.juz]++
            if (state == MemorizationState.LEARNING.value) juzLearning[location.juz]++
        }

        // Rollup honesty rule: a page shows the WEAKEST state among its ayat;
        // any not-started or learning ayah on a touched page shows Learning.
        val pageStates = (0 until PAGE_COUNT).map { index ->
            when {
                !pageTouched[index] -> MemorizationState.NOT_STARTED
                pageWeakest[index] <= MemorizationState.LEARNING.value -> MemorizationState.LEARNING
                pageWeakest[index] == MemorizationState.MEMORIZED.value -> MemorizationState.MEMORIZED
                else -> MemorizationState.STRONG
            }
        }

        val juzProgress = (1..30)
            .filter { juzMemorized[it] + juzLearning[it] > 0 }
            .map { juz ->
                val memorizedFraction = juzMemorized[juz].toFloat() / juzTotals[juz]
                when {
                    juzMemorized[juz] == juzTotals[juz] ->
                        JuzProgress(juz, 1f, 100, JuzStatus.COMPLETE)
                    juzLearning[juz] > juzMemorized[juz] -> {
                        val touchedFraction =
                            (juzMemorized[juz] + juzLearning[juz]).toFloat() / juzTotals[juz]
                        JuzProgress(juz, touchedFraction, (touchedFraction * 100).toInt(), JuzStatus.LEARNING)
                    }
                    else ->
                        JuzProgress(juz, memorizedFraction, (memorizedFraction * 100).toInt(), JuzStatus.IN_PROGRESS)
                }
            }
            .sortedByDescending { it.fillFraction }

        val memorizedAyahCount = states.values.count { it >= MemorizationState.MEMORIZED.value }
        return ProgressSnapshot(
            memorizedAyahCount = memorizedAyahCount,
            percentOfQuran = memorizedAyahCount * 100f / TOTAL_AYAH_COUNT,
            pageStates = pageStates,
            memorizedPageCount = pageStates.count { it >= MemorizationState.MEMORIZED },
            juzProgress = juzProgress,
            streakDays = currentStreak(),
            weekAyahCount = userDao.ayahCountSince(LocalDate.now().toEpochDay() - 6),
            totalPracticeMs = userDao.totalPracticeMs(),
        )
    }

    companion object {
        private const val PAGE_COUNT = QuranRepository.PAGE_COUNT
        private const val TOTAL_AYAH_COUNT = QuranRepository.TOTAL_AYAH_COUNT
    }
}
