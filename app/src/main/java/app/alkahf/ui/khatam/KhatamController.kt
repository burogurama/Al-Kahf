package app.alkahf.ui.khatam

import app.alkahf.data.KhatamState
import app.alkahf.data.QuranRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * Drives the Khatam tracker, today's-portion, program, and log flow.
 *
 * Mirrors the per-screen controller pattern used elsewhere (e.g.
 * [app.alkahf.ui.library.LibraryController]): a single [StateFlow] of the
 * active [KhatamState], reloaded after every mutation (program / log / cancel)
 * so the tracker, map, ring, and stats all refresh from one source.
 *
 * [state] is null until the first [refresh]; thereafter null means "no active
 * khatam" and a non-null value is the fully-resolved tracker state.
 */
class KhatamController(private val repository: QuranRepository) {
    private val _state = MutableStateFlow<KhatamState?>(null)
    val state: StateFlow<KhatamState?> = _state.asStateFlow()

    /** True once the first load has completed (so the UI can tell empty from loading). */
    private val _loaded = MutableStateFlow(false)
    val loaded: StateFlow<Boolean> = _loaded.asStateFlow()

    suspend fun refresh() {
        _state.value = repository.activeKhatam()
        _loaded.value = true
    }

    /**
     * Creates a khatam at [pace] juzʼ/day with its daily reminder, and publishes
     * the resulting state.
     */
    suspend fun program(pace: Int, reminderEnabled: Boolean, reminderMinute: Int): KhatamState {
        val created = repository.programKhatam(pace, reminderEnabled, reminderMinute)
        _state.value = created
        _loaded.value = true
        return created
    }

    /** Updates the active khatam's reminder and refreshes state. */
    suspend fun setReminder(enabled: Boolean, minute: Int) {
        repository.setKhatamReminder(enabled, minute)
        refresh()
    }

    /** Logs today's portion complete (no-op at 30); publishes the advanced state. */
    suspend fun logTodayPortion() {
        repository.logTodayKhatamPortion()
        refresh()
    }

    /** Cancels the active khatam, returning the screen to the empty state. */
    suspend fun cancel() {
        repository.cancelKhatam()
        refresh()
    }
}
