package app.alkahf.data

import android.content.Context
import app.alkahf.data.quran.QuranDatabase
import app.alkahf.data.review.ReviewGrade
import app.alkahf.data.review.ReviewScheduler
import app.alkahf.data.user.AyahStateEntity
import app.alkahf.data.user.PracticeEventEntity
import app.alkahf.data.user.RevealStateEntity
import app.alkahf.data.user.ReviewPortionEntity
import app.alkahf.data.user.StumbleEntity
import app.alkahf.data.user.UserDatabase
import java.time.LocalDate

/** One ayah as rendered on a mushaf page. */
data class PageAyah(
    val id: Int,
    val surah: Int,
    val number: Int,
    val words: List<String>,
    val marker: String,
)

/**
 * A run of consecutive ayat from one surah on a page, with the surah header
 * band and basmala when the surah begins on this page.
 */
data class PageGroup(
    val surahNumber: Int,
    val surahNameArabic: String,
    val surahNameLatin: String,
    val surahMeta: String,
    val showSurahHeader: Boolean,
    val basmala: String?,
    val ayahs: List<PageAyah>,
)

data class MushafPage(
    val number: Int,
    val juz: Int,
    val hizb: Int,
    val groups: List<PageGroup>,
) {
    val ayahs: List<PageAyah> get() = groups.flatMap { it.ayahs }
    val primarySurahLatin: String get() = groups.first().surahNameLatin
    val pageNumberArabic: String get() = number.toArabicIndic()
}

data class WordStumble(val ayahId: Int, val wordIndex: Int)

enum class MemorizationState(val value: Int) {
    NOT_STARTED(0), LEARNING(1), MEMORIZED(2), STRONG(3);

    companion object {
        fun of(value: Int): MemorizationState =
            entries.firstOrNull { it.value == value } ?: NOT_STARTED
    }
}

enum class JuzStatus { COMPLETE, IN_PROGRESS, LEARNING }

data class JuzProgress(
    val juz: Int,
    val fillFraction: Float,
    val percent: Int,
    val status: JuzStatus,
)

/** Everything the Progress screen shows, aggregated from local data. */
data class ProgressSnapshot(
    val memorizedAyahCount: Int,
    val percentOfQuran: Float,
    val pageStates: List<MemorizationState>,
    val memorizedPageCount: Int,
    val juzProgress: List<JuzProgress>,
    val streakDays: Int,
    val weekAyahCount: Int,
    val totalPracticeMs: Long,
)

/** Saved drill configuration for the loop player. */
data class LoopPreset(
    val surah: Int,
    val surahNameLatin: String,
    val ayahFrom: Int,
    val ayahTo: Int,
    val reciterPath: String,
    val reciterName: String,
    val perAyah: Int,
    val perChain: Int,
    val gapMultiplier: Float,
    val speed: Float,
)

/** A surah picker entry. */
data class SurahOption(val number: Int, val nameLatin: String, val ayahCount: Int)

/** A portion due for murājaʿah, with its text loaded for the self-test. */
data class ReviewPortion(
    val id: Long,
    val surah: Int,
    val surahNameLatin: String,
    val ayahFrom: Int,
    val ayahTo: Int,
    val intervalDays: Int,
    val ayahs: List<PageAyah>,
)

fun Int.toArabicIndic(): String =
    toString().map { digit -> '٠' + (digit - '0') }.joinToString("")

class QuranRepository(context: Context) {
    private val quranDao = QuranDatabase.open(context).quranDao()
    private val userDao = UserDatabase.open(context).userDao()
    private val prefs = context.getSharedPreferences("alkahf_prefs", Context.MODE_PRIVATE)
    private var cachedBasmala: String? = null

    /** Last page open in the Mushaf, so reading resumes where the user left off. */
    var lastMushafPage: Int?
        get() = prefs.getInt(KEY_LAST_MUSHAF_PAGE, 0).takeIf { it in 1..PAGE_COUNT }
        set(value) {
            prefs.edit().putInt(KEY_LAST_MUSHAF_PAGE, value ?: 0).apply()
        }

    suspend fun firstPageOfSurah(surah: Int): Int = quranDao.firstPageOfSurah(surah)

    suspend fun page(number: Int): MushafPage {
        val ayahs = quranDao.ayahsOnPage(number)
        val basmala = cachedBasmala ?: quranDao.basmala().also { cachedBasmala = it }
        val groups = ayahs
            .groupBy { it.surah }
            .map { (surahNumber, surahAyahs) ->
                val surah = quranDao.surah(surahNumber)
                val beginsHere = surahAyahs.first().number == 1
                PageGroup(
                    surahNumber = surahNumber,
                    surahNameArabic = "سُورَةُ ${surah.nameArabic}",
                    surahNameLatin = surah.nameLatin,
                    surahMeta = surahMeta(surah.revelationType, surah.ayahCount),
                    showSurahHeader = beginsHere,
                    basmala = basmala.takeIf { beginsHere && surahAyahs.first().hasBasmala },
                    ayahs = surahAyahs.map { ayah ->
                        PageAyah(
                            id = ayah.id,
                            surah = ayah.surah,
                            number = ayah.number,
                            words = ayah.text.split(' '),
                            marker = "۝${ayah.number.toArabicIndic()}",
                        )
                    },
                )
            }
        val first = ayahs.first()
        return MushafPage(
            number = number,
            juz = first.juz,
            hizb = (first.hizbQuarter - 1) / 4 + 1,
            groups = groups,
        )
    }

    /** Last saved loop preset, or null before the user ever saves one. */
    var loopPreset: LoopPreset?
        get() {
            if (!prefs.contains("preset_surah")) return null
            return LoopPreset(
                surah = prefs.getInt("preset_surah", 18),
                surahNameLatin = prefs.getString("preset_surah_name", "Al-Kahf") ?: "Al-Kahf",
                ayahFrom = prefs.getInt("preset_from", 1),
                ayahTo = prefs.getInt("preset_to", 5),
                reciterPath = prefs.getString("preset_reciter_path", "Husary_128kbps") ?: "Husary_128kbps",
                reciterName = prefs.getString("preset_reciter_name", "Ḥuṣarī") ?: "Ḥuṣarī",
                perAyah = prefs.getInt("preset_per_ayah", 3),
                perChain = prefs.getInt("preset_per_chain", 5),
                gapMultiplier = prefs.getFloat("preset_gap", 1.5f),
                speed = prefs.getFloat("preset_speed", 1.0f),
            )
        }
        set(value) {
            if (value == null) return
            prefs.edit()
                .putInt("preset_surah", value.surah)
                .putString("preset_surah_name", value.surahNameLatin)
                .putInt("preset_from", value.ayahFrom)
                .putInt("preset_to", value.ayahTo)
                .putString("preset_reciter_path", value.reciterPath)
                .putString("preset_reciter_name", value.reciterName)
                .putInt("preset_per_ayah", value.perAyah)
                .putInt("preset_per_chain", value.perChain)
                .putFloat("preset_gap", value.gapMultiplier)
                .putFloat("preset_speed", value.speed)
                .apply()
        }

    suspend fun surahOptions(): List<SurahOption> =
        quranDao.allSurahs().map { SurahOption(it.number, it.nameLatin, it.ayahCount) }

    suspend fun ayahsForRange(surah: Int, from: Int, to: Int): List<PageAyah> =
        quranDao.ayahRange(surah, from, to).map { ayah ->
            PageAyah(
                id = ayah.id,
                surah = ayah.surah,
                number = ayah.number,
                words = ayah.text.split(' '),
                marker = "۝${ayah.number.toArabicIndic()}",
            )
        }

    suspend fun surahNameLatin(surah: Int): String = quranDao.surah(surah).nameLatin

    suspend fun stumblesForPage(page: MushafPage): List<WordStumble> =
        userDao.stumblesForAyahs(page.ayahs.map { it.id })
            .map { WordStumble(it.ayahId, it.wordIndex) }

    suspend fun addStumble(stumble: WordStumble) {
        userDao.addStumble(
            StumbleEntity(
                ayahId = stumble.ayahId,
                wordIndex = stumble.wordIndex,
                createdAt = System.currentTimeMillis(),
            ),
        )
    }

    suspend fun removeStumble(stumble: WordStumble) {
        userDao.deleteStumble(stumble.ayahId, stumble.wordIndex)
    }

    suspend fun revealStates(ayahIds: List<Int>): Map<Int, Int> =
        userDao.revealStatesForAyahs(ayahIds).associate { it.ayahId to it.revealedCount }

    suspend fun saveRevealState(ayahId: Int, revealedCount: Int) {
        userDao.upsertRevealState(RevealStateEntity(ayahId, revealedCount))
    }

    suspend fun clearRevealStates(ayahIds: List<Int>) {
        userDao.clearRevealStates(ayahIds)
    }

    suspend fun dueReviewPortions(): List<ReviewPortion> {
        ensureSeeded()
        return userDao.duePortions(LocalDate.now().toEpochDay()).map { entity ->
            ReviewPortion(
                id = entity.id,
                surah = entity.surah,
                surahNameLatin = quranDao.surah(entity.surah).nameLatin,
                ayahFrom = entity.ayahFrom,
                ayahTo = entity.ayahTo,
                intervalDays = entity.intervalDays,
                ayahs = ayahsForRange(entity.surah, entity.ayahFrom, entity.ayahTo),
            )
        }
    }

    suspend fun commitReviewGrade(portion: ReviewPortion, grade: ReviewGrade) {
        val nextInterval = ReviewScheduler.nextIntervalDays(portion.intervalDays, grade)
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
        userDao.upsertAyahStates(portion.ayahs.map { AyahStateEntity(it.id, state.value) })
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

    suspend fun progressSnapshot(): ProgressSnapshot {
        ensureSeeded()
        val states = userDao.allAyahStates().associate { it.ayahId to it.state }
        val locations = quranDao.ayahLocations()

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

    private suspend fun currentStreak(): Int {
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

    private suspend fun ensureSeeded() {
        if (userDao.portionCount() == 0) {
            seedDefaultPortions()
        }
    }

    /**
     * Until the Progress feature lets the user mark portions memorized, seed
     * the scheduler with the short surahs from the product definition.
     */
    private suspend fun seedDefaultPortions() {
        val today = LocalDate.now().toEpochDay()
        val portions = listOf(
            ReviewPortionEntity(surah = 1, ayahFrom = 1, ayahTo = 7, intervalDays = 6, dueEpochDay = today),
            ReviewPortionEntity(surah = 114, ayahFrom = 1, ayahTo = 6, intervalDays = 6, dueEpochDay = today),
            ReviewPortionEntity(surah = 113, ayahFrom = 1, ayahTo = 5, intervalDays = 6, dueEpochDay = today),
            ReviewPortionEntity(surah = 112, ayahFrom = 1, ayahTo = 4, intervalDays = 6, dueEpochDay = today),
            ReviewPortionEntity(surah = 18, ayahFrom = 1, ayahTo = 5, intervalDays = 6, dueEpochDay = today),
        )
        userDao.insertPortions(portions)
        userDao.upsertAyahStates(
            portions.flatMap { portion ->
                (portion.ayahFrom..portion.ayahTo).map { ayah ->
                    AyahStateEntity(portion.surah * 1000 + ayah, MemorizationState.MEMORIZED.value)
                }
            },
        )
    }

    private fun surahMeta(revelationType: String, ayahCount: Int): String {
        val place = if (revelationType == "Meccan") "MAKKĪ" else "MADANĪ"
        return "$place · $ayahCount ĀYĀT"
    }

    companion object {
        const val PAGE_COUNT = 604
        const val TOTAL_AYAH_COUNT = 6236
        private const val KEY_LAST_MUSHAF_PAGE = "last_mushaf_page"
    }
}
