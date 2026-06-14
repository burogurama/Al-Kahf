package app.alkahf.data

import android.content.Context
import app.alkahf.data.review.ReviewGrade
import app.alkahf.data.review.ReviewScheduler
import app.alkahf.data.audio.AudioStore
import app.alkahf.data.audio.Reciter
import app.alkahf.data.audio.recitersFor
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
    val riwayah: Riwayah = Riwayah.HAFS,
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

/**
 * Resolved playback for a range from an imported (user-timed) reciter: the
 * single audio file plus the start..end millisecond segment of each āyah.
 */
data class ImportedPlayback(
    val fileUri: String,
    val ayahIds: List<Int>,
    val segments: List<LongRange>,
)

/** An imported sūrah's single file plus each timed āyah's segment, by āyah number. */
data class ImportedSurahAudio(val fileUri: String, val segments: Map<Int, LongRange>)

enum class ReviewPacing(val growthFactor: Double) {
    GENTLE(3.0), STANDARD(2.5), AGGRESSIVE(2.0),
}

/** All user-configurable settings, read together for the Settings screen. */
data class SettingsData(
    val themeMode: String,
    val appLanguage: String,
    val arabicTextSizePt: Int,
    val dailyBudgetMin: Int,
    val newPerDay: Int,
    val reviewPacing: ReviewPacing,
    val autoLowerOnStumble: Boolean,
    val keepScreenOn: Boolean,
    val backgroundAudio: Boolean,
    val remindersEnabled: Boolean,
    /** Daily reminder times as minutes after midnight, ascending. */
    val reminderTimes: List<Int>,
)

/** A surah + ayah range (e.g. the current sabaq). */
data class AyahRange(val surah: Int, val from: Int, val to: Int) {
    val ayahIds: Set<Int> get() = (from..to).map { surah * 1000 + it }.toSet()
}

/** The sabaq summarised for a reminder notification. */
data class SabaqReference(val surahNameLatin: String, val from: Int, val to: Int)

/** The current sabaq (portion being learned) for the Home hero card. */
data class SabaqCard(
    val surah: Int,
    val surahNameLatin: String,
    val ayahFrom: Int,
    val ayahTo: Int,
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
    val sabaq: SabaqCard?,
    val drill: LoopPreset?,
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
data class SurahOption(
    val number: Int,
    val nameLatin: String,
    val nameArabic: String,
    val ayahCount: Int,
)

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
    private val settings = UserPreferences(context)
    // The read-only Qur'an text (both riwāyāt) lives in its own store; the Mushaf
    // can briefly show the other reading by passing an explicit riwāyah.
    private val quranText = QuranTextStore(context) { settings.riwayah }
    private val userDao = UserDatabase.open(context).userDao()
    private val audioStore = AudioStore(context)
    private val filesDir = context.filesDir

    /** Last page open in the Mushaf, so reading resumes where the user left off. */
    var lastMushafPage: Int?
        get() = settings.lastMushafPage
        set(value) { settings.lastMushafPage = value }

    /** Whether the Mushaf was last in hide/self-test mode, so it reopens the same way. */
    var lastMushafHideMode: Boolean
        get() = settings.lastMushafHideMode
        set(value) { settings.lastMushafHideMode = value }

    /** The current sabaq range, or null when no sabaq is active. */
    val sabaqRange: AyahRange? get() = settings.sabaqRange

    private val sectionLength: Int get() = settings.sectionLength

    /** Marks a whole surah as learning and starts its sabaq at the first section. */
    suspend fun startLearningSurah(surah: Int) {
        val len = quranText.surahAyahCount(surah)
        val ids = (1..len).map { surah * 1000 + it }
        val states = ayahStatesForPage(ids)
        val toLearning = ids
            .filter { (states[it] ?: MemorizationState.NOT_STARTED).value < MemorizationState.MEMORIZED.value }
            .map { AyahStateEntity(it, MemorizationState.LEARNING.value) }
        if (toLearning.isNotEmpty()) userDao.upsertAyahStates(toLearning)
        // Begin at the first not-yet-memorized āyah so already-memorized leading
        // āyāt aren't re-served as new material; sections then align from there.
        val start = (1..len).firstOrNull {
            (states[surah * 1000 + it] ?: MemorizationState.NOT_STARTED).value < MemorizationState.MEMORIZED.value
        }
        if (start == null) {
            setSabaq(0, 0, 0)
            return
        }
        setSabaq(surah, start, minOf(start + sectionLength - 1, len))
        maybeAdvanceSabaq()
    }

    /** Manually sets a range as the sabaq (ensuring it holds a learning ayah). */
    suspend fun setSabaqToRange(surah: Int, from: Int, to: Int) {
        val ids = (from..to).map { surah * 1000 + it }
        val states = ayahStatesForPage(ids)
        val toLearning = ids
            .filter { (states[it] ?: MemorizationState.NOT_STARTED) == MemorizationState.NOT_STARTED }
            .map { AyahStateEntity(it, MemorizationState.LEARNING.value) }
        if (toLearning.isNotEmpty()) userDao.upsertAyahStates(toLearning)
        setSabaq(surah, from, to)
    }

    /**
     * Marks every āyah in the current sabaq range as memorized in one step, then
     * advances. The "I've memorized this section" shortcut — independent of how
     * many pages the range spans or what is selected in the Mushaf.
     */
    suspend fun markSabaqMemorized() {
        val range = sabaqRange ?: return
        val ids = range.ayahIds.toList()
        val states = ayahStatesForPage(ids)
        // Only upgrade āyāt not yet memorized; never demote a "strong" āyah.
        val toMark = ids
            .filter { (states[it] ?: MemorizationState.NOT_STARTED).value < MemorizationState.MEMORIZED.value }
            .map { AyahStateEntity(it, MemorizationState.MEMORIZED.value) }
        if (toMark.isNotEmpty()) userDao.upsertAyahStates(toMark)
        maybeAdvanceSabaq()
    }

    /**
     * Registers a memorized range for spaced-repetition review (idempotent). The
     * first review is scheduled for the same day so the just-memorized portion
     * joins today's murājaʿah, then SM-2 spaces it out from there.
     */
    private suspend fun enrollReviewPortion(surah: Int, from: Int, to: Int) {
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

    /**
     * Advances the sabaq past any fully-memorized section: the sabaq steps
     * forward by the section length until it lands on a section that still has
     * an unmemorized ayah, or clears when it runs off the end of the surah.
     */
    suspend fun maybeAdvanceSabaq() {
        var range = sabaqRange ?: return
        val len = quranText.surahAyahCount(range.surah)
        val l = sectionLength
        while (true) {
            val ids = (range.from..range.to).map { range.surah * 1000 + it }
            val states = ayahStatesForPage(ids)
            val allDone = ids.all {
                (states[it] ?: MemorizationState.NOT_STARTED).value >= MemorizationState.MEMORIZED.value
            }
            if (!allDone) break
            // A fully-memorized section graduates into spaced-repetition review.
            enrollReviewPortion(range.surah, range.from, range.to)
            val nextFrom = range.to + 1
            if (nextFrom > len) {
                setSabaq(0, 0, 0)
                return
            }
            range = AyahRange(range.surah, nextFrom, minOf(nextFrom + l - 1, len))
        }
        // Ensure the landing section holds a learning ayah, then persist it.
        val ids = (range.from..range.to).map { range.surah * 1000 + it }
        val states = ayahStatesForPage(ids)
        val toLearning = ids
            .filter { (states[it] ?: MemorizationState.NOT_STARTED) == MemorizationState.NOT_STARTED }
            .map { AyahStateEntity(it, MemorizationState.LEARNING.value) }
        if (toLearning.isNotEmpty()) userDao.upsertAyahStates(toLearning)
        setSabaq(range.surah, range.from, range.to)
    }

    suspend fun pageOfAyah(surah: Int, ayah: Int): Int = quranText.pageOfAyah(surah, ayah)

    suspend fun surahAyahCount(surah: Int): Int = quranText.surahAyahCount(surah)

    /** The current sabaq as a display reference, or null when there's no sabaq. */
    suspend fun sabaqReference(): SabaqReference? {
        val range = sabaqRange ?: return null
        return SabaqReference(quranText.surahNameLatin(range.surah), range.from, range.to)
    }

    fun setSabaq(surah: Int, from: Int, to: Int) = settings.setSabaq(surah, from, to)

    suspend fun homeData(): HomeData {
        maybeAdvanceSabaq()
        syncSabaqDrill()
        val today = LocalDate.now()
        return HomeData(
            streakDays = currentStreak(),
            sabaq = sabaqCard(),
            drill = sabaqDrill(),
            review = reviewSummary(),
            week = weekSummary(today),
        )
    }

    private suspend fun sabaqCard(): SabaqCard? {
        val range = sabaqRange ?: return null
        val nameLatin = quranText.surahNameLatin(range.surah)
        val ayahs = ayahsForRange(range.surah, range.from, range.to)
        if (ayahs.isEmpty()) return null
        val states = userDao.allAyahStates().associate { it.ayahId to it.state }
        val first = ayahs.first()
        return SabaqCard(
            surah = range.surah,
            surahNameLatin = nameLatin,
            ayahFrom = range.from,
            ayahTo = range.to,
            firstAyahText = first.words.joinToString(" "),
            firstAyahMarker = first.marker,
            states = ayahs.map { MemorizationState.of(states[it.id] ?: 0) },
        )
    }

    private suspend fun reviewSummary(): ReviewSummary {
        val due = userDao.duePortions(LocalDate.now().toEpochDay())
        val names = due.map { quranText.surahNameLatin(it.surah) }
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

    suspend fun firstPageOfSurah(surah: Int): Int = quranText.firstPageOfSurah(surah)

    /** A mushaf page, optionally in a riwāyah other than the active one (the
     * Mushaf's temporary toggle). Defaults to the active riwāyah. */
    suspend fun page(number: Int, riwayah: Riwayah = this.riwayah): MushafPage =
        quranText.page(number, riwayah)

    // --- Riwāyah (Hafs / Warsh) ---

    /** "hafs" (default) | "warsh". Drives the Qur'an DB, font, and reciter list. */
    var riwayah: Riwayah
        get() = settings.riwayah
        set(value) { settings.riwayah = value }

    /** Built-in reciters available for the active riwāyah. */
    val riwayahReciters: List<Reciter> get() = recitersFor(riwayah)

    // --- Active reciter (the voice used by Review and Mushaf listening) ---

    val activeReciterPath: String
        get() {
            val pool = riwayahReciters
            val stored = settings.activeReciterRaw
            // Keep the stored voice only if it belongs to the active riwāyah.
            return pool.firstOrNull { it.path == stored }?.path ?: pool.first().path
        }

    val activeReciter: Reciter
        get() = riwayahReciters.firstOrNull { it.path == activeReciterPath } ?: riwayahReciters.first()

    fun setActiveReciter(path: String) = settings.setActiveReciter(path)

    /** "light" | "dark" | "system" (default). */
    var themeMode: String
        get() = settings.themeMode
        set(value) { settings.themeMode = value }

    /** In-app UI language: "system" (default) | "en" | "ar". */
    var appLanguage: String
        get() = settings.appLanguage
        set(value) { settings.appLanguage = value }

    val arabicTextSizePt: Int get() = settings.arabicTextSizePt
    val dailyBudgetMin: Int get() = settings.dailyBudgetMin
    val reviewPacing: ReviewPacing get() = settings.reviewPacing
    val autoLowerOnStumble: Boolean get() = settings.autoLowerOnStumble

    /** Whether daily hifz reminders are armed. */
    val remindersEnabled: Boolean get() = settings.remindersEnabled

    /** Reminder times as minutes after midnight, ascending and de-duplicated. */
    val reminderTimes: List<Int> get() = settings.reminderTimes

    fun settings(): SettingsData = settings.settings()

    fun updateSettings(s: SettingsData) = settings.updateSettings(s)

    // --- Loop presets (Room-backed; the sabaq's drill is flagged isDefault) ---

    /** All drills, every riwāyah — each card shows its own riwāyah label. */
    suspend fun presets(): List<LoopPreset> =
        userDao.allPresets().map { it.toLoopPreset() }

    /** The single auto-managed sabaq drill, or null when there's no sabaq. */
    suspend fun sabaqDrill(): LoopPreset? = userDao.defaultPreset()?.toLoopPreset()

    /** A non-null base preset for the loop player (sabaq drill, else a template). */
    suspend fun defaultPreset(): LoopPreset =
        sabaqDrill()
            ?: userDao.allPresets().firstOrNull()?.toLoopPreset()
            ?: activeReciter.let { r ->
                LoopPreset(
                    surah = 18,
                    surahNameLatin = "Al-Kahf",
                    ayahFrom = 1,
                    ayahTo = 5,
                    reciterPath = r.path,
                    reciterName = r.displayName,
                    perAyah = 3,
                    perChain = 5,
                    gapMultiplier = 1.5f,
                    speed = 1.0f,
                )
            }

    /**
     * Keeps a single auto-generated drill in step with the sabaq: created when a
     * sabaq exists, its range refreshed (keeping the user's config) when the
     * sabaq advances, and removed when there's no sabaq. Flagged isDefault to
     * tell it apart from the user's own presets.
     */
    suspend fun syncSabaqDrill() {
        val range = sabaqRange
        val defaults = userDao.allPresets().filter { it.isDefault }
        if (range == null) {
            defaults.forEach { userDao.deletePreset(it.id) }
            return
        }
        // Keep exactly one sabaq drill, dropping any duplicates.
        val existing = defaults.firstOrNull()
        defaults.drop(1).forEach { userDao.deletePreset(it.id) }
        val surahName = quranText.surahNameLatin(range.surah)
        if (existing == null) {
            val reciter = activeReciter
            userDao.insertPreset(
                LoopPresetEntity(
                    name = surahName,
                    surah = range.surah,
                    surahName = surahName,
                    ayahFrom = range.from,
                    ayahTo = range.to,
                    reciterPath = reciter.path,
                    reciterName = reciter.displayName,
                    perAyah = 3,
                    perChain = 5,
                    gapMultiplier = 1.5f,
                    speed = 1.0f,
                    isDefault = true,
                    riwayah = riwayah.key,
                ),
            )
            return
        }
        // The sabaq drill follows the system riwāyah: convert its reading and (if
        // needed) reciter when the system reading toggled, and track the sabaq range.
        val reciterValid = existing.reciterPath.startsWith("custom:") ||
            recitersFor(riwayah).any { it.path == existing.reciterPath }
        val reciter = if (existing.riwayah != riwayah.key || !reciterValid) activeReciter else null
        userDao.updatePreset(
            existing.copy(
                name = surahName,
                surah = range.surah,
                surahName = surahName,
                ayahFrom = range.from,
                ayahTo = range.to,
                riwayah = riwayah.key,
                reciterPath = reciter?.path ?: existing.reciterPath,
                reciterName = reciter?.displayName ?: existing.reciterName,
            ),
        )
    }

    /** Inserts a new preset, or updates it in place when it already has an id. */
    suspend fun savePreset(preset: LoopPreset): Long {
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
            // The drill carries its own riwāyah, chosen in the editor.
            riwayah = preset.riwayah.key,
        )
        return if (preset.id == 0L) {
            userDao.insertPreset(entity)
        } else {
            userDao.updatePreset(entity)
            preset.id
        }
    }

    suspend fun presetById(id: Long): LoopPreset? =
        userDao.allPresets().firstOrNull { it.id == id }?.toLoopPreset()

    suspend fun deletePreset(id: Long) {
        userDao.deletePreset(id)
    }

    // --- Reciters (built-in + custom imported) ---

    suspend fun reciterStatuses(riwayah: Riwayah = this.riwayah): List<ReciterStatus> {
        val active = activeReciterPath
        val builtins = recitersFor(riwayah).map { reciter ->
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
        val customs = userDao.customRecitersForRiwayah(riwayah.key).map { reciter ->
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

    suspend fun createCustomReciter(name: String, riwayah: Riwayah = this.riwayah): String {
        val initial = name.trim().firstOrNull { !it.isWhitespace() }?.toString() ?: "ق"
        val id = userDao.insertCustomReciter(
            CustomReciterEntity(
                name = name.trim(),
                initial = initial,
                createdAt = System.currentTimeMillis(),
                riwayah = riwayah.key,
            ),
        )
        return "custom:$id"
    }

    /** Re-tags an imported reciter with a riwāyah. */
    suspend fun setReciterRiwayah(reciterKey: String, riwayah: Riwayah) {
        val id = customReciterId(reciterKey) ?: return
        userDao.setReciterRiwayah(id, riwayah.key)
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
        val surahs = quranText.allSurahs()
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
        val surahRow = quranText.surah(surah)
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
        val ayahCount = quranText.surahAyahCount(surah)
        audioStore.downloadSurah(reciterPath, surah, ayahCount, onProgress)
    }

    /**
     * Downloads just the āyāt [from]..[to] of a built-in reciter's sūrah,
     * reporting progress 0f..1f. Used by the Mushaf range-listening flow so a
     * user only fetches what they're about to hear.
     */
    suspend fun downloadRange(
        reciterPath: String,
        surah: Int,
        from: Int,
        to: Int,
        riwayah: Riwayah = this.riwayah,
        onProgress: (Float) -> Unit,
    ) {
        val audio = audioHafsRange(surah, from, to, riwayah)
        audioStore.downloadRange(reciterPath, surah, audio.first, audio.last, onProgress)
    }

    /** True when every āyah of [from]..[to] is already cached for the reciter. */
    suspend fun rangeAudioAvailable(
        reciterPath: String,
        surah: Int,
        from: Int,
        to: Int,
        riwayah: Riwayah = this.riwayah,
    ): Boolean {
        val audio = audioHafsRange(surah, from, to, riwayah)
        return audioStore.rangeDownloaded(reciterPath, surah, audio.first, audio.last)
    }

    /**
     * The standard (Hafs-numbered) audio āyāt for an app āyah. Identity in Hafs;
     * in Warsh it maps each verse to the everyayah file(s) that cover it (the
     * everyayah library numbers all audio by the Hafs counting).
     */
    suspend fun audioAyahs(surah: Int, ayah: Int, riwayah: Riwayah = this.riwayah): List<Int> =
        quranText.audioAyahs(surah, ayah, riwayah)

    /**
     * Converts an āyah range from one riwāyah's numbering to the other's so the
     * same passage is referenced (Ḥafṣ ↔ Warsh differ where verses split/merge).
     */
    suspend fun convertAyahRange(
        surah: Int,
        from: Int,
        to: Int,
        fromRiwayah: Riwayah,
        toRiwayah: Riwayah,
    ): IntRange = quranText.convertAyahRange(surah, from, to, fromRiwayah, toRiwayah)

    /** The Hafs audio āyāh range spanning an app range [from]..[to]. */
    suspend fun audioHafsRange(
        surah: Int,
        from: Int,
        to: Int,
        riwayah: Riwayah = this.riwayah,
    ): IntRange = quranText.audioHafsRange(surah, from, to, riwayah)

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

    /**
     * Resolves playback for an imported reciter over [from]..[to] of a sūrah from
     * its Tawqīt timings, or null when the reciter isn't imported, the sūrah has
     * no imported file, or the requested range isn't (yet) timed — partial timings
     * are fine as long as they cover the requested range. Imports are timed from
     * āyah 1, so endTimesMs[n-1] is the cumulative end of āyah n within the file.
     */
    suspend fun importedRangePlayback(
        reciterKey: String,
        surah: Int,
        from: Int,
        to: Int,
    ): ImportedPlayback? {
        val customId = customReciterId(reciterKey) ?: return null
        val import = userDao.importedSurah(customId, surah) ?: return null
        val track = userDao.allTimingTracks()
            .firstOrNull { it.sourceRef == import.uri && it.surah == surah }
            ?.toTawqitTrack() ?: return null
        val ends = track.endTimesMs
        val firstTimed = track.ayahFrom
        val lastTimed = firstTimed + ends.size - 1
        if (ends.isEmpty() || from < firstTimed || to > lastTimed) return null
        val offset = track.globalOffsetMs
        val segments = (from..to).map { n ->
            val startMs = if (n == firstTimed) 0L else ends[n - firstTimed - 1]
            val endMs = ends[n - firstTimed]
            (startMs + offset).coerceAtLeast(0L)..(endMs + offset).coerceAtLeast(0L)
        }
        return ImportedPlayback(
            fileUri = import.uri,
            ayahIds = (from..to).map { surah * 1000 + it },
            segments = segments,
        )
    }

    /**
     * The imported file for [reciterKey]'s [surah] plus the start..end segment of
     * every timed āyah (keyed by āyah number). Null when the reciter hasn't
     * imported/timed that sūrah. Used to play imported audio in the drill.
     */
    suspend fun importedSurahAudio(reciterKey: String, surah: Int): ImportedSurahAudio? {
        val customId = customReciterId(reciterKey) ?: return null
        val import = userDao.importedSurah(customId, surah) ?: return null
        val track = userDao.allTimingTracks()
            .firstOrNull { it.sourceRef == import.uri && it.surah == surah }
            ?.toTawqitTrack() ?: return null
        val ends = track.endTimesMs
        if (ends.isEmpty()) return null
        val firstTimed = track.ayahFrom
        val offset = track.globalOffsetMs
        val segments = buildMap {
            for (i in ends.indices) {
                val startMs = if (i == 0) 0L else ends[i - 1]
                put(
                    firstTimed + i,
                    (startMs + offset).coerceAtLeast(0L)..(ends[i] + offset).coerceAtLeast(0L),
                )
            }
        }
        return ImportedSurahAudio(import.uri, segments)
    }

    /** A riwāyah's reciters (built-in + imported), for drill/Mushaf pickers. */
    suspend fun allReciters(riwayah: Riwayah = this.riwayah): List<Reciter> =
        recitersFor(riwayah) +
            userDao.customRecitersForRiwayah(riwayah.key).map { Reciter("custom:${it.id}", it.name) }

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

    suspend fun surahOptions(): List<SurahOption> = quranText.surahOptions()

    suspend fun ayahsForRange(
        surah: Int,
        from: Int,
        to: Int,
        riwayah: Riwayah = this.riwayah,
    ): List<PageAyah> = quranText.ayahsForRange(surah, from, to, riwayah)

    suspend fun surahNameLatin(surah: Int): String = quranText.surahNameLatin(surah)

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
        // Cap the queue to the daily time budget (≈1.6 min per portion).
        val budgetLimit = (dailyBudgetMin / 1.6f).toInt().coerceAtLeast(1)
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
                ayahs = ayahsForRange(
                    entity.surah,
                    (entity.ayahFrom - 2).coerceAtLeast(1),
                    (entity.ayahTo + 2).coerceAtMost(surahLen),
                ),
            )
        }
    }

    suspend fun commitReviewGrade(portion: ReviewPortion, grade: ReviewGrade) {
        val nextInterval = ReviewScheduler.nextIntervalDays(
            portion.intervalDays, grade, reviewPacing.growthFactor,
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
        val states = userDao.allAyahStates().associate { it.ayahId to it.state }
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
        riwayah = Riwayah.fromKey(riwayah),
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
    }
}
