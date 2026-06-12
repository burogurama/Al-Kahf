package app.alkahf.ui.home

enum class AyahMemorizationState { NOT_STARTED, LEARNING, MEMORIZED, STRONG }

enum class DayActivity { PRACTICED, MISSED, TODAY }

data class WeekDay(val letter: String, val activity: DayActivity)

/**
 * Read-only dashboard state for the Home/Today screen. Currently fake data
 * matching the design handoff; will be derived from the local store (Room)
 * once the data layer exists.
 */
data class HomeUiState(
    val greeting: String = "Assalāmu ʿalaykum",
    val streakDays: Int = 12,
    val sabaqReference: String = "Sūrat al-Kahf · 1–5",
    val sabaqAyahText: String =
        "ٱلْحَمْدُ لِلَّهِ ٱلَّذِىٓ أَنزَلَ عَلَىٰ عَبْدِهِ ٱلْكِتَٰبَ وَلَمْ يَجْعَل لَّهُۥ عِوَجَا",
    val sabaqAyahMarker: String = "۝١", // ۝١ end-of-ayah glyph + verse number
    val sabaqAyahStates: List<AyahMemorizationState> = listOf(
        AyahMemorizationState.MEMORIZED,
        AyahMemorizationState.MEMORIZED,
        AyahMemorizationState.LEARNING,
        AyahMemorizationState.NOT_STARTED,
        AyahMemorizationState.NOT_STARTED,
    ),
    val reviewPortionCount: Int = 5,
    val reviewEstimatedMinutes: Int = 8,
    val reviewPortionNames: List<String> = listOf("Al-Fātiḥah", "An-Nās", "Al-Falaq", "Al-Ikhlāṣ"),
    val reviewOverflowCount: Int = 1,
    val drillPresetTitle: String = "Husary · al-Kahf 1–5",
    val drillPresetDetail: String = "Cumulative chain · 3× each · 5× chain",
    val weekSummary: String = "6 of 7 days · 142 ayat",
    val weekDays: List<WeekDay> = listOf(
        WeekDay("M", DayActivity.PRACTICED),
        WeekDay("T", DayActivity.PRACTICED),
        WeekDay("W", DayActivity.PRACTICED),
        WeekDay("T", DayActivity.MISSED),
        WeekDay("F", DayActivity.PRACTICED),
        WeekDay("S", DayActivity.PRACTICED),
        WeekDay("S", DayActivity.TODAY),
    ),
) {
    val memorizedInSabaq: Int
        get() = sabaqAyahStates.count {
            it == AyahMemorizationState.MEMORIZED || it == AyahMemorizationState.STRONG
        }
}
