package app.alkahf.data

import android.content.Context
import app.alkahf.data.quran.QuranDatabase
import app.alkahf.data.review.ReviewGrade
import app.alkahf.data.review.ReviewScheduler
import app.alkahf.data.audio.AudioStore
import app.alkahf.data.audio.RECITERS
import app.alkahf.data.audio.Reciter
import app.alkahf.data.user.AyahStateEntity
import app.alkahf.data.user.CustomReciterEntity
import app.alkahf.data.user.ImportedSurahEntity
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

/** A reciter (built-in downloadable, or a user-created imported profile). */
data class ReciterStatus(
    val key: String, // built-in: reciter path; imported: "custom:<id>"
    val displayName: String,
    val arabicInitial: String,
    val isActive: Boolean,
    val isImported: Boolean,
    val itemCount: Int, // downloaded sūrahs (built-in) or imported sūrahs (custom)
    val bytes: Long,
)

/** One surah row in a reciter's surah list. */
data class ReciterSurahItem(
    val surah: Int,
    val nameLatin: String,
    val ayahCount: Int,
    val isImported: Boolean,
    val hasAudio: Boolean, // fully downloaded (built-in) or imported file present (custom)
    val downloadedAyahs: Int,
    val bytes: Long,
    val timed: Boolean, // a complete Tawqīt track exists (imported only)
    val importedUri: String?,
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

/** A surah + ayah range (e.g. the current sabaq). */
data class AyahRange(val surah: Int, val from: Int, val to: Int) {
    val ayahIds: Set<Int> get() = (from..to).map { surah * 1000 + it }.toSet()
}

/** The current sabaq (portion being learned) for the Home hero card. */
data class SabaqCard(
    val surah: Int,
    val reference: String,
    val firstAyahText: String,
    val firstAyahMarker: String,
    val states: List<MemorizationState>,
)

data class ReviewSummary(val count: Int, val minutes: Int, val names: List<String>)

data class WeekSummary(
    val dayLetters: List<String>,
    val practiced: List<Boolean>,
    val ayatThisWeek: Int,
    val daysPracticed: Int,
)

/** Everything the Home/Today dashboard shows, from real local data. */
data class HomeData(
    val streakDays: Int,
    val sabaq: SabaqCard,
    val review: ReviewSummary,
    val week: WeekSummary,
)

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

    /** The surah + ayah range of the current sabaq. */
    val sabaqRange: AyahRange
        get() = AyahRange(
            surah = prefs.getInt(KEY_SABAQ_SURAH, 18),
            from = prefs.getInt(KEY_SABAQ_FROM, 1),
            to = prefs.getInt(KEY_SABAQ_TO, 5),
        )

    suspend fun pageOfAyah(surah: Int, ayah: Int): Int = quranDao.pageOfAyahId(surah * 1000 + ayah)

    fun setSabaq(surah: Int, from: Int, to: Int) {
        prefs.edit()
            .putInt(KEY_SABAQ_SURAH, surah)
            .putInt(KEY_SABAQ_FROM, from)
            .putInt(KEY_SABAQ_TO, to)
            .apply()
    }

    suspend fun homeData(): HomeData {
        ensureSeeded()
        val today = LocalDate.now()
        return HomeData(
            streakDays = currentStreak(),
            sabaq = sabaqCard(),
            review = reviewSummary(),
            week = weekSummary(today),
        )
    }

    private suspend fun sabaqCard(): SabaqCard {
        val surah = prefs.getInt(KEY_SABAQ_SURAH, 18)
        val from = prefs.getInt(KEY_SABAQ_FROM, 1)
        val to = prefs.getInt(KEY_SABAQ_TO, 5)
        val nameLatin = quranDao.surah(surah).nameLatin
        val ayahs = ayahsForRange(surah, from, to)
        val states = userDao.allAyahStates().associate { it.ayahId to it.state }
        val first = ayahs.first()
        return SabaqCard(
            surah = surah,
            reference = "Sūrat $nameLatin · $from–$to",
            firstAyahText = first.words.joinToString(" "),
            firstAyahMarker = first.marker,
            states = ayahs.map { MemorizationState.of(states[it.id] ?: 0) },
        )
    }

    private suspend fun reviewSummary(): ReviewSummary {
        val due = userDao.duePortions(LocalDate.now().toEpochDay())
        val names = due.map { quranDao.surah(it.surah).nameLatin }
        return ReviewSummary(
            count = due.size,
            minutes = (due.size * 1.6f + 0.5f).toInt().coerceAtLeast(if (due.isEmpty()) 0 else 1),
            names = names,
        )
    }

    private suspend fun weekSummary(today: LocalDate): WeekSummary {
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

    /** "light" | "dark" | "system" (default). */
    var themeMode: String
        get() = prefs.getString(KEY_THEME_MODE, "system") ?: "system"
        set(value) {
            prefs.edit().putString(KEY_THEME_MODE, value).apply()
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

    // --- Reciters (built-in + custom imported) ---

    suspend fun reciterStatuses(): List<ReciterStatus> {
        val active = activeReciterPath
        val builtins = RECITERS.map { reciter ->
            ReciterStatus(
                key = reciter.path,
                displayName = reciter.displayName,
                arabicInitial = reciterInitial(reciter.path),
                isActive = reciter.path == active,
                isImported = false,
                itemCount = audioStore.downloadedSurahs(reciter.path).size,
                bytes = audioStore.reciterBytes(reciter.path),
            )
        }
        val customs = userDao.customReciters().map { reciter ->
            ReciterStatus(
                key = "custom:${reciter.id}",
                displayName = reciter.name,
                arabicInitial = reciter.initial,
                isActive = false,
                isImported = true,
                itemCount = userDao.importedSurahs(reciter.id).size,
                bytes = 0L,
            )
        }
        return builtins + customs
    }

    suspend fun createCustomReciter(name: String): String {
        val initial = name.trim().firstOrNull { !it.isWhitespace() }?.toString() ?: "ق"
        val id = userDao.insertCustomReciter(
            CustomReciterEntity(name = name.trim(), initial = initial, createdAt = System.currentTimeMillis()),
        )
        return "custom:$id"
    }

    suspend fun deleteCustomReciter(key: String) {
        val id = customReciterId(key) ?: return
        userDao.deleteImportsForReciter(id)
        userDao.deleteCustomReciter(id)
    }

    suspend fun importSurah(reciterKey: String, surah: Int, uri: String) {
        val id = customReciterId(reciterKey) ?: return
        userDao.deleteImportedSurah(id, surah)
        userDao.insertImportedSurah(ImportedSurahEntity(reciterId = id, surah = surah, uri = uri))
    }

    suspend fun removeImportedSurah(reciterKey: String, surah: Int) {
        val id = customReciterId(reciterKey) ?: return
        userDao.deleteImportedSurah(id, surah)
    }

    private fun customReciterId(key: String): Long? =
        key.removePrefix("custom:").toLongOrNull().takeIf { key.startsWith("custom:") }

    /** Per-surah rows for a reciter's surah list (download or import state). */
    suspend fun reciterSurahItems(reciterKey: String): List<ReciterSurahItem> {
        val surahs = quranDao.allSurahs()
        val customId = customReciterId(reciterKey)
        if (customId == null) {
            return surahs.map { surah ->
                val downloaded = audioStore.downloadedAyahCount(reciterKey, surah.number)
                ReciterSurahItem(
                    surah = surah.number,
                    nameLatin = surah.nameLatin,
                    ayahCount = surah.ayahCount,
                    isImported = false,
                    hasAudio = downloaded >= surah.ayahCount,
                    downloadedAyahs = downloaded,
                    bytes = audioStore.surahBytes(reciterKey, surah.number),
                    timed = false,
                    importedUri = null,
                )
            }
        }
        val imports = userDao.importedSurahs(customId).associateBy { it.surah }
        val tracks = userDao.allTimingTracks()
        return surahs.map { surah ->
            val import = imports[surah.number]
            val timed = import != null && tracks.any {
                it.sourceRef == import.uri && it.surah == surah.number && it.complete
            }
            ReciterSurahItem(
                surah = surah.number,
                nameLatin = surah.nameLatin,
                ayahCount = surah.ayahCount,
                isImported = true,
                hasAudio = import != null,
                downloadedAyahs = if (import != null) surah.ayahCount else 0,
                bytes = 0L,
                timed = timed,
                importedUri = import?.uri,
            )
        }
    }

    /** A Tawqīt draft (new or resumed) for an imported surah of a custom reciter. */
    suspend fun tawqitDraftForImport(reciterKey: String, surah: Int): TawqitTrack? {
        val id = customReciterId(reciterKey) ?: return null
        val import = userDao.importedSurah(id, surah) ?: return null
        val surahRow = quranDao.surah(surah)
        val reciterName = userDao.customReciters().firstOrNull { it.id == id }?.name ?: "Imported"
        val existing = userDao.allTimingTracks()
            .firstOrNull { it.sourceRef == import.uri && it.surah == surah }
        return existing?.toTawqitTrack() ?: TawqitTrack(
            sourceType = TawqitSourceType.IMPORT,
            sourceRef = import.uri,
            sourceLabel = "$reciterName (imported)",
            surah = surah,
            surahNameLatin = surahRow.nameLatin,
            ayahFrom = 1,
            ayahTo = surahRow.ayahCount,
            endTimesMs = emptyList(),
            globalOffsetMs = 0,
            complete = false,
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

    suspend fun ayahStatesForPage(ayahIds: List<Int>): Map<Int, MemorizationState> =
        userDao.ayahStatesIn(ayahIds).associate { it.ayahId to MemorizationState.of(it.state) }

    suspend fun setAyahState(ayahId: Int, state: MemorizationState) {
        userDao.upsertAyahStates(listOf(AyahStateEntity(ayahId, state.value)))
    }

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
        // Five short surahs the user already holds, due for review today.
        val portions = listOf(
            ReviewPortionEntity(surah = 1, ayahFrom = 1, ayahTo = 7, intervalDays = 6, dueEpochDay = today),
            ReviewPortionEntity(surah = 114, ayahFrom = 1, ayahTo = 6, intervalDays = 6, dueEpochDay = today),
            ReviewPortionEntity(surah = 113, ayahFrom = 1, ayahTo = 5, intervalDays = 6, dueEpochDay = today),
            ReviewPortionEntity(surah = 112, ayahFrom = 1, ayahTo = 4, intervalDays = 6, dueEpochDay = today),
            ReviewPortionEntity(surah = 108, ayahFrom = 1, ayahTo = 3, intervalDays = 6, dueEpochDay = today),
        )
        userDao.insertPortions(portions)
        val memorized = portions.flatMap { portion ->
            (portion.ayahFrom..portion.ayahTo).map { ayah ->
                AyahStateEntity(portion.surah * 1000 + ayah, MemorizationState.MEMORIZED.value)
            }
        }
        // Sabaq: Al-Kahf 1–5, partway learned (ayat 1–2 held, 3 learning, 4–5 fresh).
        val sabaq = listOf(
            AyahStateEntity(18001, MemorizationState.MEMORIZED.value),
            AyahStateEntity(18002, MemorizationState.MEMORIZED.value),
            AyahStateEntity(18003, MemorizationState.LEARNING.value),
        )
        userDao.upsertAyahStates(memorized + sabaq)
        setSabaq(surah = 18, from = 1, to = 5)
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
        private const val KEY_THEME_MODE = "theme_mode"
        private const val KEY_SABAQ_SURAH = "sabaq_surah"
        private const val KEY_SABAQ_FROM = "sabaq_from"
        private const val KEY_SABAQ_TO = "sabaq_to"
    }
}
