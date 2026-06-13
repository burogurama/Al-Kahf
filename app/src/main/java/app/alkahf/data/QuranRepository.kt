package app.alkahf.data

import android.content.Context
import app.alkahf.data.quran.QuranDatabase
import app.alkahf.data.review.ReviewGrade
import app.alkahf.data.review.ReviewScheduler
import app.alkahf.data.audio.AudioStore
import app.alkahf.data.audio.RECITERS
import app.alkahf.data.audio.Reciter
import app.alkahf.data.user.AyahStateEntity
import app.alkahf.data.user.LoopPresetEntity
import app.alkahf.data.user.PracticeEventEntity
import app.alkahf.data.user.RevealStateEntity
import app.alkahf.data.user.TimingTrackEntity
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
    val id: Long = 0,
    val name: String = "Drill",
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
    val isDefault: Boolean = false,
)

/** A downloadable/active reciter voice and its on-device download summary. */
data class ReciterStatus(
    val path: String,
    val displayName: String,
    val arabicInitial: String,
    val isActive: Boolean,
    val downloadedSurahs: Int,
    val bytes: Long,
)

/** A surah's downloaded audio for the active reciter. */
data class DownloadedSurah(
    val surah: Int,
    val nameLatin: String,
    val downloadedAyahs: Int,
    val totalAyahs: Int,
    val bytes: Long,
)

/** Storage occupied by offline audio vs. total device storage. */
data class StorageInfo(val usedBytes: Long, val totalBytes: Long)

enum class TawqitSourceType { IMPORT, RECITER }

/** A Tawqīt timing track aligning an audio source to a portion of the mushaf. */
data class TawqitTrack(
    val id: Long = 0,
    val sourceType: TawqitSourceType,
    val sourceRef: String,
    val sourceLabel: String,
    val surah: Int,
    val surahNameLatin: String,
    val ayahFrom: Int,
    val ayahTo: Int,
    val endTimesMs: List<Long>,
    val globalOffsetMs: Long,
    val complete: Boolean,
) {
    val ayahCount: Int get() = ayahTo - ayahFrom + 1
}

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
    private val audioStore = AudioStore(context)
    private val filesDir = context.filesDir
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

    // --- Active reciter (the voice used by Review and Mushaf listening) ---

    val activeReciterPath: String
        get() {
            val stored = prefs.getString(KEY_ACTIVE_RECITER, null)
            // Fall back to a valid reciter if the stored one was removed.
            return RECITERS.firstOrNull { it.path == stored }?.path ?: RECITERS.first().path
        }

    val activeReciter: Reciter
        get() = RECITERS.firstOrNull { it.path == activeReciterPath } ?: RECITERS.first()

    fun setActiveReciter(path: String) {
        prefs.edit().putString(KEY_ACTIVE_RECITER, path).apply()
    }

    // --- Loop presets (Room-backed; one is the default) ---

    suspend fun presets(): List<LoopPreset> {
        ensurePresetSeed()
        return userDao.allPresets().map { it.toLoopPreset() }
    }

    suspend fun defaultPreset(): LoopPreset {
        ensurePresetSeed()
        return (userDao.defaultPreset() ?: userDao.allPresets().first()).toLoopPreset()
    }

    /** Inserts a new preset, or updates it in place when it already has an id. */
    suspend fun savePreset(preset: LoopPreset, makeDefault: Boolean): Long {
        val entity = LoopPresetEntity(
            id = preset.id,
            name = preset.name,
            surah = preset.surah,
            surahName = preset.surahNameLatin,
            ayahFrom = preset.ayahFrom,
            ayahTo = preset.ayahTo,
            reciterPath = preset.reciterPath,
            reciterName = preset.reciterName,
            perAyah = preset.perAyah,
            perChain = preset.perChain,
            gapMultiplier = preset.gapMultiplier,
            speed = preset.speed,
            isDefault = preset.isDefault,
        )
        val id = if (preset.id == 0L) {
            userDao.insertPreset(entity)
        } else {
            userDao.updatePreset(entity)
            preset.id
        }
        if (makeDefault) setDefaultPreset(id)
        return id
    }

    suspend fun presetById(id: Long): LoopPreset? =
        userDao.allPresets().firstOrNull { it.id == id }?.toLoopPreset()

    suspend fun setDefaultPreset(id: Long) {
        userDao.clearDefaultPresets()
        userDao.markDefaultPreset(id)
    }

    suspend fun deletePreset(id: Long) {
        userDao.deletePreset(id)
        if (userDao.defaultPreset() == null) {
            userDao.allPresets().firstOrNull()?.let { userDao.markDefaultPreset(it.id) }
        }
    }

    private suspend fun ensurePresetSeed() {
        if (userDao.presetCount() > 0) return
        val reciter = activeReciter
        userDao.insertPreset(
            LoopPresetEntity(
                name = "Al-Kahf opening",
                surah = 18,
                surahName = "Al-Kahf",
                ayahFrom = 1,
                ayahTo = 5,
                reciterPath = reciter.path,
                reciterName = reciter.displayName,
                perAyah = 3,
                perChain = 5,
                gapMultiplier = 1.5f,
                speed = 1.0f,
                isDefault = true,
            ),
        )
    }

    // --- Audio downloads / storage ---

    suspend fun reciterStatuses(): List<ReciterStatus> {
        val active = activeReciterPath
        return RECITERS.map { reciter ->
            ReciterStatus(
                path = reciter.path,
                displayName = reciter.displayName,
                arabicInitial = reciterInitial(reciter.path),
                isActive = reciter.path == active,
                downloadedSurahs = audioStore.downloadedSurahs(reciter.path).size,
                bytes = audioStore.reciterBytes(reciter.path),
            )
        }
    }

    suspend fun downloadedSurahs(reciterPath: String): List<DownloadedSurah> {
        val surahs = quranDao.allSurahs().associateBy { it.number }
        return audioStore.downloadedSurahs(reciterPath).mapNotNull { surahNumber ->
            val surah = surahs[surahNumber] ?: return@mapNotNull null
            DownloadedSurah(
                surah = surahNumber,
                nameLatin = surah.nameLatin,
                downloadedAyahs = audioStore.downloadedAyahCount(reciterPath, surahNumber),
                totalAyahs = surah.ayahCount,
                bytes = audioStore.surahBytes(reciterPath, surahNumber),
            )
        }
    }

    /** Download state of every surah for a reciter, for the download manager. */
    suspend fun surahDownloadStates(reciterPath: String): List<DownloadedSurah> =
        quranDao.allSurahs().map { surah ->
            DownloadedSurah(
                surah = surah.number,
                nameLatin = surah.nameLatin,
                downloadedAyahs = audioStore.downloadedAyahCount(reciterPath, surah.number),
                totalAyahs = surah.ayahCount,
                bytes = audioStore.surahBytes(reciterPath, surah.number),
            )
        }

    suspend fun downloadSurah(reciterPath: String, surah: Int, onProgress: (Float) -> Unit) {
        val ayahCount = quranDao.surah(surah).ayahCount
        audioStore.downloadSurah(reciterPath, surah, ayahCount, onProgress)
    }

    suspend fun deleteSurahAudio(reciterPath: String, surah: Int) =
        audioStore.deleteSurah(reciterPath, surah)

    fun storageInfo(): StorageInfo {
        val stat = android.os.StatFs(filesDir.absolutePath)
        return StorageInfo(
            usedBytes = audioStore.totalDownloadedBytes(),
            totalBytes = stat.blockCountLong * stat.blockSizeLong,
        )
    }

    // --- Tawqīt timing tracks ---

    suspend fun tawqitTracks(): List<TawqitTrack> =
        userDao.allTimingTracks().map { it.toTawqitTrack() }

    suspend fun tawqitTrack(id: Long): TawqitTrack? =
        userDao.timingTrack(id)?.toTawqitTrack()

    suspend fun saveTawqitTrack(track: TawqitTrack): Long {
        val entity = TimingTrackEntity(
            id = track.id,
            sourceType = if (track.sourceType == TawqitSourceType.IMPORT) "import" else "reciter",
            sourceRef = track.sourceRef,
            sourceLabel = track.sourceLabel,
            surah = track.surah,
            surahName = track.surahNameLatin,
            ayahFrom = track.ayahFrom,
            ayahTo = track.ayahTo,
            endTimesCsv = track.endTimesMs.joinToString(","),
            globalOffsetMs = track.globalOffsetMs,
            complete = track.complete,
            updatedAt = System.currentTimeMillis(),
        )
        return if (track.id == 0L) {
            userDao.insertTimingTrack(entity)
        } else {
            userDao.updateTimingTrack(entity)
            track.id
        }
    }

    suspend fun deleteTawqitTrack(id: Long) = userDao.deleteTimingTrack(id)

    /** Resolves the per-ayah audio files for a reciter source (downloads as needed). */
    suspend fun reciterAyahUris(reciterPath: String, surah: Int, from: Int, to: Int): List<String> =
        (from..to).map { ayah ->
            android.net.Uri.fromFile(audioStore.ayahFile(surah, ayah, reciterPath)).toString()
        }

    private fun TimingTrackEntity.toTawqitTrack() = TawqitTrack(
        id = id,
        sourceType = if (sourceType == "import") TawqitSourceType.IMPORT else TawqitSourceType.RECITER,
        sourceRef = sourceRef,
        sourceLabel = sourceLabel,
        surah = surah,
        surahNameLatin = surahName,
        ayahFrom = ayahFrom,
        ayahTo = ayahTo,
        endTimesMs = endTimesCsv.split(",").filter { it.isNotBlank() }.map { it.toLong() },
        globalOffsetMs = globalOffsetMs,
        complete = complete,
    )

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

    private fun LoopPresetEntity.toLoopPreset() = LoopPreset(
        id = id,
        name = name,
        surah = surah,
        surahNameLatin = surahName,
        ayahFrom = ayahFrom,
        ayahTo = ayahTo,
        reciterPath = reciterPath,
        reciterName = reciterName,
        perAyah = perAyah,
        perChain = perChain,
        gapMultiplier = gapMultiplier,
        speed = speed,
        isDefault = isDefault,
    )

    private fun reciterInitial(reciterPath: String): String = when {
        reciterPath.startsWith("Husary") -> "ح"
        reciterPath.startsWith("Alafasy") -> "ع"
        reciterPath.startsWith("Abdul_Basit") -> "ع"
        reciterPath.startsWith("Minshawy") -> "م"
        else -> "ق"
    }

    companion object {
        const val PAGE_COUNT = 604
        const val TOTAL_AYAH_COUNT = 6236
        private const val KEY_LAST_MUSHAF_PAGE = "last_mushaf_page"
        private const val KEY_ACTIVE_RECITER = "active_reciter"
    }
}
