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
    val drillPresetTitle: String = "",
    val drillPresetDetail: String = "",
    val weekSummary: String = "",
    val weekDays: List<WeekDay> = emptyList(),
) {
    val memorizedInSabaq: Int
        get() = sabaqAyahStates.count {
            it == AyahMemorizationState.MEMORIZED || it == AyahMemorizationState.STRONG
        }
}
