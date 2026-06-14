package app.alkahf.data

import android.content.Context
import app.alkahf.data.review.ReviewGrade
import app.alkahf.data.audio.AudioStore
import app.alkahf.data.audio.Reciter
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
    val riwayah: Riwayah,
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
    private val memorization = MemorizationStore(userDao)
    private val review = ReviewStore(userDao, quranText, memorization, settings)
    private val tawqit = TawqitStore(userDao, quranText)
    private val reciters = ReciterLibrary(AudioStore(context), userDao, quranText, settings, context.filesDir)
    private val presets = LoopPresetStore(userDao, quranText, reciters, settings)

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
        val states = memorization.statesFor(ids)
        val toLearning = ids
            .filter { (states[it] ?: MemorizationState.NOT_STARTED).value < MemorizationState.MEMORIZED.value }
        memorization.markStates(toLearning, MemorizationState.LEARNING)
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
    }

    /** Manually sets a range as the sabaq (ensuring it holds a learning ayah). */
    suspend fun setSabaqToRange(surah: Int, from: Int, to: Int) {
        val ids = (from..to).map { surah * 1000 + it }
        val states = memorization.statesFor(ids)
        val toLearning = ids
            .filter { (states[it] ?: MemorizationState.NOT_STARTED) == MemorizationState.NOT_STARTED }
        memorization.markStates(toLearning, MemorizationState.LEARNING)
        setSabaq(surah, from, to)
    }

    /** True when every āyah in the current sabaq is memorized or strong. */
    suspend fun isSabaqComplete(): Boolean {
        val range = sabaqRange ?: return false
        val ids = range.ayahIds.toList()
        val states = memorization.statesFor(ids)
        return ids.isNotEmpty() && ids.all {
            (states[it] ?: MemorizationState.NOT_STARTED).value >= MemorizationState.MEMORIZED.value
        }
    }

    /**
     * Marks the current sabaq section done and advances to the next. Rejected
     * (returns false, no change) unless every āyah in it is already memorized or
     * strong — āyāt are marked elsewhere, this only confirms completion.
     */
    suspend fun markSabaqDone(): Boolean {
        if (!isSabaqComplete()) return false
        advanceSabaq()
        return true
    }

    /**
     * Advances the sabaq past any fully-memorized section: the sabaq steps
     * forward by the section length until it lands on a section that still has
     * an unmemorized ayah, or clears when it runs off the end of the surah. Only
     * called when the user explicitly marks the sabaq done.
     */
    private suspend fun advanceSabaq() {
        var range = sabaqRange ?: return
        val len = quranText.surahAyahCount(range.surah)
        val l = sectionLength
        while (true) {
            val ids = (range.from..range.to).map { range.surah * 1000 + it }
            val states = memorization.statesFor(ids)
            val allDone = ids.all {
                (states[it] ?: MemorizationState.NOT_STARTED).value >= MemorizationState.MEMORIZED.value
            }
            if (!allDone) break
            // A fully-memorized section graduates into spaced-repetition review.
            review.enrollPortion(range.surah, range.from, range.to)
            val nextFrom = range.to + 1
            if (nextFrom > len) {
                setSabaq(0, 0, 0)
                return
            }
            range = AyahRange(range.surah, nextFrom, minOf(nextFrom + l - 1, len))
        }
        // Ensure the landing section holds a learning ayah, then persist it.
        val ids = (range.from..range.to).map { range.surah * 1000 + it }
        val states = memorization.statesFor(ids)
        val toLearning = ids
            .filter { (states[it] ?: MemorizationState.NOT_STARTED) == MemorizationState.NOT_STARTED }
        memorization.markStates(toLearning, MemorizationState.LEARNING)
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
        syncSabaqDrill()
        val today = LocalDate.now()
        return HomeData(
            streakDays = review.currentStreak(),
            sabaq = sabaqCard(),
            drill = sabaqDrill(),
            review = review.reviewSummary(),
            week = review.weekSummary(today),
        )
    }

    private suspend fun sabaqCard(): SabaqCard? {
        val range = sabaqRange ?: return null
        val nameLatin = quranText.surahNameLatin(range.surah)
        val ayahs = ayahsForRange(range.surah, range.from, range.to)
        if (ayahs.isEmpty()) return null
        val states = memorization.allStates()
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
    val riwayahReciters: List<Reciter> get() = reciters.riwayahReciters

    // --- Active reciter (the voice used by Review and Mushaf listening) ---

    val activeReciterPath: String get() = reciters.activeReciterPath

    val activeReciter: Reciter get() = reciters.activeReciter

    fun setActiveReciter(path: String) = reciters.setActiveReciter(path)

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
    suspend fun presets(): List<LoopPreset> = presets.all()

    /** The single auto-managed sabaq drill, or null when there's no sabaq. */
    suspend fun sabaqDrill(): LoopPreset? = presets.sabaqDrill()

    /** A non-null base preset for the loop player (sabaq drill, else a template). */
    suspend fun defaultPreset(): LoopPreset = presets.defaultPreset()

    /**
     * Keeps a single auto-generated drill in step with the sabaq: created when a
     * sabaq exists, its range refreshed (keeping the user's config) when the
     * sabaq advances, and removed when there's no sabaq. Flagged isDefault to
     * tell it apart from the user's own presets.
     */
    suspend fun syncSabaqDrill() = presets.syncSabaqDrill()

    /** Inserts a new preset, or updates it in place when it already has an id. */
    suspend fun savePreset(preset: LoopPreset): Long = presets.save(preset)

    suspend fun presetById(id: Long): LoopPreset? = presets.byId(id)

    suspend fun deletePreset(id: Long) = presets.delete(id)

    // --- Reciters (built-in + custom imported) ---

    suspend fun reciterStatuses(riwayah: Riwayah = this.riwayah): List<ReciterStatus> =
        reciters.statuses(riwayah)

    /** Every reciter across both riwāyāt, each tagged with its riwāyah. */
    suspend fun allReciterStatuses(): List<ReciterStatus> = reciters.allStatuses()

    /** Creates an imported reciter, or returns null when the name is already taken. */
    suspend fun createCustomReciter(name: String, riwayah: Riwayah = this.riwayah): String? =
        reciters.createCustom(name, riwayah)

    /** Re-tags an imported reciter with a riwāyah. */
    suspend fun setReciterRiwayah(reciterKey: String, riwayah: Riwayah) =
        reciters.setRiwayah(reciterKey, riwayah)

    suspend fun deleteCustomReciter(key: String) = reciters.deleteCustom(key)

    suspend fun importSurah(reciterKey: String, surah: Int, uri: String) =
        reciters.importSurah(reciterKey, surah, uri)

    suspend fun removeImportedSurah(reciterKey: String, surah: Int) =
        reciters.removeImportedSurah(reciterKey, surah)

    /** Per-surah rows for a reciter's surah list (download or import state). */
    suspend fun reciterSurahItems(reciterKey: String): List<ReciterSurahItem> =
        reciters.surahItems(reciterKey)

    /** A Tawqīt draft (new or resumed) for an imported surah of a custom reciter. */
    suspend fun tawqitDraftForImport(reciterKey: String, surah: Int): TawqitTrack? =
        tawqit.draftForImport(reciterKey, surah)

    suspend fun downloadSurah(reciterPath: String, surah: Int, onProgress: (Float) -> Unit) =
        reciters.downloadSurah(reciterPath, surah, onProgress)

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
    ) = reciters.downloadRange(reciterPath, surah, from, to, riwayah, onProgress)

    /** True when every āyah of [from]..[to] is already cached for the reciter. */
    suspend fun rangeAudioAvailable(
        reciterPath: String,
        surah: Int,
        from: Int,
        to: Int,
        riwayah: Riwayah = this.riwayah,
    ): Boolean = reciters.rangeAvailable(reciterPath, surah, from, to, riwayah)

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
        reciters.deleteSurahAudio(reciterPath, surah)

    fun storageInfo(): StorageInfo = reciters.storageInfo()

    // --- Tawqīt timing tracks ---

    suspend fun tawqitTracks(): List<TawqitTrack> = tawqit.tracks()

    suspend fun tawqitTrack(id: Long): TawqitTrack? = tawqit.track(id)

    suspend fun saveTawqitTrack(track: TawqitTrack): Long = tawqit.saveTrack(track)

    suspend fun deleteTawqitTrack(id: Long) = tawqit.deleteTrack(id)

    /** Resolves the per-ayah audio files for a reciter source (downloads as needed). */
    suspend fun reciterAyahUris(reciterPath: String, surah: Int, from: Int, to: Int): List<String> =
        reciters.ayahUris(reciterPath, surah, from, to)

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
    ): ImportedPlayback? = tawqit.rangePlayback(reciterKey, surah, from, to)

    /**
     * The imported file for [reciterKey]'s [surah] plus the start..end segment of
     * every timed āyah (keyed by āyah number). Null when the reciter hasn't
     * imported/timed that sūrah. Used to play imported audio in the drill.
     */
    suspend fun importedSurahAudio(reciterKey: String, surah: Int): ImportedSurahAudio? =
        tawqit.surahAudio(reciterKey, surah)

    /** A riwāyah's reciters (built-in + imported), for drill/Mushaf pickers. */
    suspend fun allReciters(riwayah: Riwayah = this.riwayah): List<Reciter> =
        reciters.allReciters(riwayah)

    suspend fun surahOptions(): List<SurahOption> = quranText.surahOptions()

    suspend fun ayahsForRange(
        surah: Int,
        from: Int,
        to: Int,
        riwayah: Riwayah = this.riwayah,
    ): List<PageAyah> = quranText.ayahsForRange(surah, from, to, riwayah)

    suspend fun surahNameLatin(surah: Int): String = quranText.surahNameLatin(surah)

    suspend fun stumblesForPage(page: MushafPage): List<WordStumble> =
        memorization.stumblesFor(page.ayahs.map { it.id })

    suspend fun addStumble(stumble: WordStumble) = memorization.addStumble(stumble)

    suspend fun removeStumble(stumble: WordStumble) = memorization.removeStumble(stumble)

    suspend fun revealStates(ayahIds: List<Int>): Map<Int, Int> =
        memorization.revealStates(ayahIds)

    suspend fun ayahStatesForPage(ayahIds: List<Int>): Map<Int, MemorizationState> =
        memorization.statesFor(ayahIds)

    suspend fun setAyahState(ayahId: Int, state: MemorizationState) =
        memorization.setState(ayahId, state)

    /** Sets every āyah of [surah] to [state] in one write. */
    suspend fun setSurahState(surah: Int, state: MemorizationState) {
        val count = quranText.surahAyahCount(surah)
        memorization.markStates((1..count).map { surah * 1000 + it }, state)
    }

    /** The sūrah's overall memorization state, aggregated from its āyāt. */
    suspend fun surahState(surah: Int): MemorizationState {
        val count = quranText.surahAyahCount(surah)
        val ids = (1..count).map { surah * 1000 + it }
        val states = memorization.statesFor(ids)
        return surahAggregateState(ids.map { states[it] ?: MemorizationState.NOT_STARTED })
    }

    suspend fun saveRevealState(ayahId: Int, revealedCount: Int) =
        memorization.saveRevealState(ayahId, revealedCount)

    suspend fun clearRevealStates(ayahIds: List<Int>) =
        memorization.clearRevealStates(ayahIds)

    suspend fun dueReviewPortions(): List<ReviewPortion> = review.duePortions()

    suspend fun commitReviewGrade(portion: ReviewPortion, grade: ReviewGrade) =
        review.commitGrade(portion, grade)

    suspend fun logPractice(type: String, ayahCount: Int, durationMs: Long) =
        review.logPractice(type, ayahCount, durationMs)

    suspend fun progressSnapshot(): ProgressSnapshot = review.progressSnapshot()

    companion object {
        const val PAGE_COUNT = 604
        const val TOTAL_AYAH_COUNT = 6236
    }
}
