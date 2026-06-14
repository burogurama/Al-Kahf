package app.alkahf.ui.mushaf

import app.alkahf.data.MemorizationState
import app.alkahf.data.MushafPage
import app.alkahf.data.PageAyah
import app.alkahf.data.QuranRepository
import app.alkahf.data.Riwayah
import app.alkahf.data.SettingsData
import app.alkahf.data.SurahOption
import app.alkahf.data.audio.Reciter

/**
 * Data access for the Mushaf screen: page loading, memorization-state and
 * reveal bookkeeping, sabaq actions, and the screen's persisted preferences.
 * The screen keeps its page sessions, selection, and pager as Compose state and
 * its playback in the audio/range controllers; this is its one path to the
 * repository for data.
 */
class MushafController(private val repository: QuranRepository) {
    /** App settings, read once when the screen opens. */
    val settings: SettingsData = repository.settings()
    val systemRiwayah: Riwayah get() = repository.riwayah
    val activeReciter: Reciter get() = repository.activeReciter
    val activeReciterPath: String get() = repository.activeReciterPath

    var lastMushafPage: Int?
        get() = repository.lastMushafPage
        set(value) {
            repository.lastMushafPage = value
        }

    var lastMushafHideMode: Boolean
        get() = repository.lastMushafHideMode
        set(value) {
            repository.lastMushafHideMode = value
        }

    suspend fun page(number: Int, riwayah: Riwayah): MushafPage = repository.page(number, riwayah)

    suspend fun revealStates(ayahIds: List<Int>): Map<Int, Int> = repository.revealStates(ayahIds)

    suspend fun ayahStates(ayahIds: List<Int>): Map<Int, MemorizationState> =
        repository.ayahStatesForPage(ayahIds)

    suspend fun saveRevealState(ayahId: Int, count: Int) = repository.saveRevealState(ayahId, count)

    suspend fun clearRevealStates(ayahIds: List<Int>) = repository.clearRevealStates(ayahIds)

    suspend fun setAyahState(ayahId: Int, state: MemorizationState) =
        repository.setAyahState(ayahId, state)

    suspend fun maybeAdvanceSabaq() = repository.maybeAdvanceSabaq()

    suspend fun setSabaqToRange(surah: Int, from: Int, to: Int) =
        repository.setSabaqToRange(surah, from, to)

    suspend fun startLearningSurah(surah: Int) = repository.startLearningSurah(surah)

    suspend fun surahOptions(): List<SurahOption> = repository.surahOptions()

    suspend fun ayahsForRange(surah: Int, from: Int, to: Int): List<PageAyah> =
        repository.ayahsForRange(surah, from, to)

    suspend fun surahAyahCount(surah: Int): Int = repository.surahAyahCount(surah)

    suspend fun pageOfAyah(surah: Int, ayah: Int): Int = repository.pageOfAyah(surah, ayah)

    suspend fun firstPageOfSurah(surah: Int): Int = repository.firstPageOfSurah(surah)
}
