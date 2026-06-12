package app.alkahf.data

import android.content.Context
import app.alkahf.data.quran.QuranDatabase
import app.alkahf.data.review.ReviewGrade
import app.alkahf.data.review.ReviewScheduler
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

    suspend fun dueReviewPortions(): List<ReviewPortion> {
        if (userDao.portionCount() == 0) {
            seedDefaultPortions()
        }
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
    }

    /**
     * Until the Progress feature lets the user mark portions memorized, seed
     * the scheduler with the short surahs from the product definition.
     */
    private suspend fun seedDefaultPortions() {
        val today = LocalDate.now().toEpochDay()
        userDao.insertPortions(
            listOf(
                ReviewPortionEntity(surah = 1, ayahFrom = 1, ayahTo = 7, intervalDays = 6, dueEpochDay = today),
                ReviewPortionEntity(surah = 114, ayahFrom = 1, ayahTo = 6, intervalDays = 6, dueEpochDay = today),
                ReviewPortionEntity(surah = 113, ayahFrom = 1, ayahTo = 5, intervalDays = 6, dueEpochDay = today),
                ReviewPortionEntity(surah = 112, ayahFrom = 1, ayahTo = 4, intervalDays = 6, dueEpochDay = today),
                ReviewPortionEntity(surah = 18, ayahFrom = 1, ayahTo = 5, intervalDays = 6, dueEpochDay = today),
            ),
        )
    }

    private fun surahMeta(revelationType: String, ayahCount: Int): String {
        val place = if (revelationType == "Meccan") "MAKKĪ" else "MADANĪ"
        return "$place · $ayahCount ĀYĀT"
    }

    companion object {
        const val PAGE_COUNT = 604
        private const val KEY_LAST_MUSHAF_PAGE = "last_mushaf_page"
    }
}
