package app.alkahf.ui.home

enum class AyahMemorizationState { NOT_STARTED, LEARNING, MEMORIZED, STRONG }

enum class DayActivity { PRACTICED, MISSED, TODAY }

data class WeekDay(val letter: String, val activity: DayActivity)

/**
 * Read-only dashboard state for the Home/Today screen, derived from the local
 * store (Room) by [app.alkahf.data.QuranRepository.homeData]. The defaults
 * here are only the neutral pre-load placeholder, not real content.
 */
data class HomeUiState(
    val greeting: String = "Assalāmu ʿalaykum",
    val streakDays: Int = 0,
    val hasSabaq: Boolean = false,
    val sabaqReference: String = "",
    val sabaqAyahText: String = "",
    val sabaqAyahMarker: String = "",
    val sabaqAyahStates: List<AyahMemorizationState> = emptyList(),
    val reviewPortionCount: Int = 0,
    val reviewEstimatedMinutes: Int = 0,
    val reviewPortionNames: List<String> = emptyList(),
    val reviewOverflowCount: Int = 0,
    val hasDrill: Boolean = false,
    val drillPresetId: Long = 0,
    val drillPresetTitle: String = "",
    val drillPresetDetail: String = "",
    val weekSummary: String = "",
    val weekDays: List<WeekDay> = emptyList(),
    // Khatam entry card. When [hasKhatam] is false the card invites the user to
    // begin one; when true it's a compact resume summary that deep-links to the
    // tracker.
    val hasKhatam: Boolean = false,
    val khatamPercent: Int = 0,
    val khatamRingFraction: Float = 0f,
    val khatamTodayReference: String = "",
    val khatamTodayJuz: Int = 0,
    // Exercises (memorization self-testing) entry card. [exerciseReady] gates
    // whether enough is memorized to test; [exerciseHasLastResult] toggles the
    // returning-user last-result row (omitted on first run).
    val exerciseReadinessLine: String = "",
    val exerciseReady: Boolean = false,
    val exerciseHasLastResult: Boolean = false,
    val exerciseLastScore: String = "",
    val exerciseLastWhen: String = "",
    val exerciseLastToRevisit: Int = 0,
) {
    val memorizedInSabaq: Int
        get() = sabaqAyahStates.count {
            it == AyahMemorizationState.MEMORIZED || it == AyahMemorizationState.STRONG
        }

    /** True when every āyah in the sabaq is memorized or strong — done is allowed. */
    val sabaqComplete: Boolean
        get() = sabaqAyahStates.isNotEmpty() && memorizedInSabaq == sabaqAyahStates.size
}
