package app.alkahf.ui.theme

import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.graphics.Color

/**
 * The app's design tokens, swappable between a light and a dark palette.
 *
 * Call sites use `AlkahfColors.X`; each token is a getter that reads the
 * active [Palette] (a snapshot state), so switching themes recomposes /
 * redraws everything.
 */
data class Palette(
    val Paper: Color,
    val Surface: Color,
    val SurfaceHero: Color,
    val NavSurface: Color,
    val CardBorder: Color,
    val CardBorderHero: Color,
    val ChipBg: Color,
    val Ink: Color,
    val InkSecondary: Color,
    val InkSecondaryDark: Color,
    val InkMuted: Color,
    val InkFaint: Color,
    val InkFainter: Color,
    val Accent: Color,
    val AccentDeep: Color,
    val AccentTint: Color,
    val AccentTint2: Color,
    val AccentLight: Color,
    val OnAccent: Color,
    val Learning: Color,
    val NotStarted: Color,
    val GoldText: Color,
    val GoldBg: Color,
    val SecondaryButtonBorder: Color,
    val Chevron: Color,
    val PageSurface: Color,
    val Hairline: Color,
    val DockBorder: Color,
    val HeaderRule: Color,
    val InkChrome: Color,
    val InkFooter: Color,
    val ConcealedInk: Color,
    val ConcealedMedallion: Color,
    val StumbleAmber: Color,
    val StumbleBorder: Color,
    val StumbleBg: Color,
    val StumbleBgPressed: Color,
    val StumbleInk: Color,
    val SessionBg: Color,
    val LoopCardBorder: Color,
    val ControlBorder: Color,
    val ControlPressed: Color,
    val SegmentedTrack: Color,
    val SegmentedSelected: Color,
    val TransportGlyph: Color,
    val WordHighlightBg: Color,
    val WordHighlightInk: Color,
    val UpcomingWord: Color,
    val DashedNode: Color,
    val DashedLink: Color,
    val QueuedNumeral: Color,
    val ProgressTrack: Color,
    val ForgotInk: Color,
    val ForgotHint: Color,
    val ForgotBorder: Color,
    val ForgotBg: Color,
    val HesitantHint: Color,
    val PerfectHint: Color,
    val MapEmpty: Color,
    val WaveformBg: Color,
    val WaveformPlayed: Color,
    val WaveformUnplayed: Color,
    val TawqitCurrentInk: Color,
    val TaggedText: Color,
    val NowPlayingFill: Color,
    val AyahHighlightFill: Color,
    val PlayTile: Color,
    val PlayTileInk: Color,
    val KhatamGoldDeep: Color,
    val KhatamGoldBorder: Color,
    val KhatamProgressFill: Color,
    val KhatamToday: Color,
    val KhatamCardTint: Color,
    val KhatamInsetTint: Color,
    val KhatamCardBorder: Color,
    val KhatamBorderSoft: Color,
    val KhatamRingTrack: Color,
    val KhatamUpcoming: Color,
    val ModalScrim: Color,
    // Feedback — "Clay" (warm terracotta for "not quite"; never red)
    val ClayText: Color,
    val ClayTextSoft: Color,
    val ClayBg: Color,
    val ClayBorder: Color,
    val ClayChip: Color,
    // Exercise UI accents
    val ExerciseSelectedCard: Color,
    val ArabicAnswerInk: Color,
    val ExerciseSegmentedTrack: Color,
    // Quranic keyboard
    val KeyboardTray: Color,
    val KeyboardLetterKey: Color,
    val KeyboardSpecialKey: Color,
    val KeyboardHarakatBg: Color,
    val KeyboardHarakatKey: Color,
    val KeyboardHarakatBorder: Color,
    val KeyboardHarakatGlyph: Color,
    val KeyboardUthmaniBg: Color,
    val KeyboardUthmaniKey: Color,
    val KeyboardUthmaniBorder: Color,
    val KeyboardUthmaniGlyph: Color,
    val KeyboardWaqfRing: Color,
    val KeyboardWaqfBg: Color,
    val KeyboardPopoverBg: Color,
    val KeyboardPopoverKey: Color,
)

val LightPalette = Palette(
    Paper = Color(0xFFF5F0E6),
    Surface = Color(0xFFFCFAF4),
    SurfaceHero = Color(0xFFEBF1ED),
    NavSurface = Color(0xFFF8F3EA),
    CardBorder = Color(0xFFEAE3D4),
    CardBorderHero = Color(0xFFD6E4DC),
    ChipBg = Color(0xFFF4EFE4),
    Ink = Color(0xFF2A2620),
    InkSecondary = Color(0xFF6E665A),
    InkSecondaryDark = Color(0xFF4A453C),
    InkMuted = Color(0xFF8C8475),
    InkFaint = Color(0xFFA39A8B),
    InkFainter = Color(0xFFB6AD9C),
    Accent = Color(0xFF3E7D6E),
    AccentDeep = Color(0xFF2F6055),
    AccentTint = Color(0xFFDCEAE3),
    AccentTint2 = Color(0xFFE2EDE8),
    AccentLight = Color(0xFF6FA395),
    OnAccent = Color(0xFFFBFAF5),
    Learning = Color(0xFFD8B45A),
    NotStarted = Color(0xFFE4DBC8),
    GoldText = Color(0xFFB0863A),
    GoldBg = Color(0xFFF6EFDD),
    SecondaryButtonBorder = Color(0xFFC7D8CF),
    Chevron = Color(0xFFC2BAA9),
    PageSurface = Color(0xFFFBF7ED),
    Hairline = Color(0xFFECE5D6),
    DockBorder = Color(0xFFE7DFCF),
    HeaderRule = Color(0xFFDCD2BD),
    InkChrome = Color(0xFF5C5648),
    InkFooter = Color(0xFFB0A691),
    ConcealedInk = Color(0xFFB3AA98),
    ConcealedMedallion = Color(0xFFC8BFAD),
    StumbleAmber = Color(0xFFC28A45),
    StumbleBorder = Color(0xFFE3C99F),
    StumbleBg = Color(0xFFFAF2E4),
    StumbleBgPressed = Color(0xFFF3E7D2),
    StumbleInk = Color(0xFFA8702A),
    SessionBg = Color(0xFFF2ECDF),
    LoopCardBorder = Color(0xFFECE3D0),
    ControlBorder = Color(0xFFE2DAC9),
    ControlPressed = Color(0xFFF0E9D8),
    SegmentedTrack = Color(0xFFF0E8D6),
    SegmentedSelected = Color(0xFFFFFDF8),
    TransportGlyph = Color(0xFF3A352C),
    WordHighlightBg = Color(0xFFD3E7DE),
    WordHighlightInk = Color(0xFF235247),
    UpcomingWord = Color(0xFFBBB2A0),
    DashedNode = Color(0xFFCBC1AC),
    DashedLink = Color(0xFFD7CDB8),
    QueuedNumeral = Color(0xFFB7AD99),
    ProgressTrack = Color(0xFFE0D7C5),
    ForgotInk = Color(0xFFA05544),
    ForgotHint = Color(0xFFC09183),
    ForgotBorder = Color(0xFFDEC4BC),
    ForgotBg = Color(0xFFF8ECE8),
    HesitantHint = Color(0xFFC9A36B),
    PerfectHint = Color(0xFFBCD6CC),
    MapEmpty = Color(0xFFEAE2D0),
    WaveformBg = Color(0xFFF1EADC),
    WaveformPlayed = Color(0xFF6FA395),
    WaveformUnplayed = Color(0xFFDAD0BC),
    TawqitCurrentInk = Color(0xFF1F4F45),
    TaggedText = Color(0xFF9A917F),
    NowPlayingFill = Color(0xFFF1E1B4),
    AyahHighlightFill = Color(0xFFD6E8DF),
    PlayTile = Color(0xFF2A2620),
    PlayTileInk = Color(0xFFF5F0E6),
    KhatamGoldDeep = Color(0xFF9A732F),
    KhatamGoldBorder = Color(0xFFC99A45),
    KhatamProgressFill = Color(0xFFBF9038),
    KhatamToday = Color(0xFFE6C36C),
    KhatamCardTint = Color(0xFFFBF3DF),
    KhatamInsetTint = Color(0xFFF5ECD9),
    KhatamCardBorder = Color(0xFFEAD9B4),
    KhatamBorderSoft = Color(0xFFE0CFA3),
    KhatamRingTrack = Color(0xFFEEE2C6),
    KhatamUpcoming = Color(0xFFEBE3D2),
    ModalScrim = Color(0x6B1C1810),
    // Feedback — "Clay" (warm terracotta for "not quite"; never red)
    ClayText = Color(0xFFB25E3E),
    ClayTextSoft = Color(0xFF9A6A54),
    ClayBg = Color(0xFFF7E8DF),
    ClayBorder = Color(0xFFECD6C8),
    ClayChip = Color(0xFFF3D9CC),
    // Exercise UI accents
    ExerciseSelectedCard = Color(0xFFECF3F0),
    ArabicAnswerInk = Color(0xFF234E45),
    ExerciseSegmentedTrack = Color(0xFFEFE8D9),
    // Quranic keyboard
    KeyboardTray = Color(0xFFE4DAC8),
    KeyboardLetterKey = Color(0xFFFFFFFF),
    KeyboardSpecialKey = Color(0xFFCDC2AC),
    KeyboardHarakatBg = Color(0xFFF1E4C6),
    KeyboardHarakatKey = Color(0xFFFBF5E4),
    KeyboardHarakatBorder = Color(0xFFE8D7AC),
    KeyboardHarakatGlyph = Color(0xFF6E5320),
    KeyboardUthmaniBg = Color(0xFFDCE5DF),
    KeyboardUthmaniKey = Color(0xFFF1F5F1),
    KeyboardUthmaniBorder = Color(0xFFC7D6CC),
    KeyboardUthmaniGlyph = Color(0xFF2F5249),
    KeyboardWaqfRing = Color(0xFF8FB3A6),
    KeyboardWaqfBg = Color(0xFFE4EFE9),
    KeyboardPopoverBg = Color(0xFF2A2620),
    KeyboardPopoverKey = Color(0xFF3A352B),
)

val DarkPalette = Palette(
    Paper = Color(0xFF15120D),
    Surface = Color(0xFF1E1A13),
    SurfaceHero = Color(0xFF1B2E28),
    NavSurface = Color(0xFF1A1610),
    CardBorder = Color(0xFF2C2820),
    CardBorderHero = Color(0xFF294339),
    ChipBg = Color(0xFF2A251A),
    Ink = Color(0xFFE8E0CE),
    InkSecondary = Color(0xFFA89E84),
    InkSecondaryDark = Color(0xFFC2B8A0),
    InkMuted = Color(0xFF9A9079),
    InkFaint = Color(0xFF8C836F),
    InkFainter = Color(0xFF7C7560),
    Accent = Color(0xFF3E7D6E),
    AccentDeep = Color(0xFF74BBA4),
    AccentTint = Color(0xFF214038),
    AccentTint2 = Color(0xFF1E342D),
    AccentLight = Color(0xFF6FA395),
    OnAccent = Color(0xFFFBFAF5),
    Learning = Color(0xFFD8B45A),
    NotStarted = Color(0xFF38331F),
    GoldText = Color(0xFFD9A45C),
    GoldBg = Color(0xFF2A2318),
    SecondaryButtonBorder = Color(0xFF294339),
    Chevron = Color(0xFF6E6655),
    PageSurface = Color(0xFF1F1B14),
    Hairline = Color(0xFF2C2820),
    DockBorder = Color(0xFF2F2A20),
    HeaderRule = Color(0xFF3C3729),
    InkChrome = Color(0xFFA89E84),
    InkFooter = Color(0xFF7C7560),
    ConcealedInk = Color(0xFF56503F),
    ConcealedMedallion = Color(0xFF5C5544),
    StumbleAmber = Color(0xFFD9A45C),
    StumbleBorder = Color(0xFF463A26),
    StumbleBg = Color(0xFF2A2318),
    StumbleBgPressed = Color(0xFF332A1C),
    StumbleInk = Color(0xFFD9A45C),
    SessionBg = Color(0xFF15120D),
    LoopCardBorder = Color(0xFF2C2820),
    ControlBorder = Color(0xFF3A3528),
    ControlPressed = Color(0xFF2A251A),
    SegmentedTrack = Color(0xFF2A251A),
    SegmentedSelected = Color(0xFF34302A),
    TransportGlyph = Color(0xFFC2B8A0),
    WordHighlightBg = Color(0xFF274A40),
    WordHighlightInk = Color(0xFFCDE7DD),
    UpcomingWord = Color(0xFF5C5544),
    DashedNode = Color(0xFF5C5544),
    DashedLink = Color(0xFF3C3628),
    QueuedNumeral = Color(0xFF6E6655),
    ProgressTrack = Color(0xFF332E22),
    ForgotInk = Color(0xFFD98E7A),
    ForgotHint = Color(0xFFB07A6A),
    ForgotBorder = Color(0xFF5A3A32),
    ForgotBg = Color(0xFF2E1F1B),
    HesitantHint = Color(0xFFD9A45C),
    PerfectHint = Color(0xFFBCD6CC),
    MapEmpty = Color(0xFF38331F),
    WaveformBg = Color(0xFF221E16),
    WaveformPlayed = Color(0xFF6FA395),
    WaveformUnplayed = Color(0xFF3C3628),
    TawqitCurrentInk = Color(0xFFD8EAE2),
    TaggedText = Color(0xFF6E6655),
    NowPlayingFill = Color(0xFF3E351C),
    AyahHighlightFill = Color(0xFF23362F),
    PlayTile = Color(0xFF2E2A20),
    PlayTileInk = Color(0xFFEDE5D2),
    KhatamGoldDeep = Color(0xFFD9A45C),
    KhatamGoldBorder = Color(0xFFB78A48),
    KhatamProgressFill = Color(0xFFCB9A4A),
    KhatamToday = Color(0xFFEACB7C),
    KhatamCardTint = Color(0xFF2A2318),
    KhatamInsetTint = Color(0xFF241F15),
    KhatamCardBorder = Color(0xFF463A26),
    KhatamBorderSoft = Color(0xFF3C3221),
    KhatamRingTrack = Color(0xFF332E22),
    KhatamUpcoming = Color(0xFF38331F),
    ModalScrim = Color(0x99000000),
    // Feedback — "Clay" (warm terracotta for "not quite"; never red)
    ClayText = Color(0xFFD99A7C),
    ClayTextSoft = Color(0xFFC0937E),
    ClayBg = Color(0xFF2E211B),
    ClayBorder = Color(0xFF513A30),
    ClayChip = Color(0xFF3A271F),
    // Exercise UI accents
    ExerciseSelectedCard = Color(0xFF223A33),
    ArabicAnswerInk = Color(0xFF8FC3B4),
    ExerciseSegmentedTrack = Color(0xFF2A251A),
    // Quranic keyboard
    KeyboardTray = Color(0xFF1B1810),
    KeyboardLetterKey = Color(0xFF34302A),
    KeyboardSpecialKey = Color(0xFF26221B),
    KeyboardHarakatBg = Color(0xFF2E2716),
    KeyboardHarakatKey = Color(0xFF38311E),
    KeyboardHarakatBorder = Color(0xFF4A3F24),
    KeyboardHarakatGlyph = Color(0xFFE2CC8E),
    KeyboardUthmaniBg = Color(0xFF1E2E28),
    KeyboardUthmaniKey = Color(0xFF263A33),
    KeyboardUthmaniBorder = Color(0xFF35493F),
    KeyboardUthmaniGlyph = Color(0xFF9CC4B6),
    KeyboardWaqfRing = Color(0xFF6FA395),
    KeyboardWaqfBg = Color(0xFF1F342D),
    KeyboardPopoverBg = Color(0xFF221E18),
    KeyboardPopoverKey = Color(0xFF2E2A22),
)

object AlkahfColors {
    private val palette = mutableStateOf(LightPalette)

    fun setDark(dark: Boolean) {
        val target = if (dark) DarkPalette else LightPalette
        if (palette.value != target) palette.value = target
    }

    val Paper get() = palette.value.Paper
    val Surface get() = palette.value.Surface
    val SurfaceHero get() = palette.value.SurfaceHero
    val NavSurface get() = palette.value.NavSurface
    val CardBorder get() = palette.value.CardBorder
    val CardBorderHero get() = palette.value.CardBorderHero
    val ChipBg get() = palette.value.ChipBg
    val Ink get() = palette.value.Ink
    val InkSecondary get() = palette.value.InkSecondary
    val InkSecondaryDark get() = palette.value.InkSecondaryDark
    val InkMuted get() = palette.value.InkMuted
    val InkFaint get() = palette.value.InkFaint
    val InkFainter get() = palette.value.InkFainter
    val Accent get() = palette.value.Accent
    val AccentDeep get() = palette.value.AccentDeep
    val AccentTint get() = palette.value.AccentTint
    val AccentTint2 get() = palette.value.AccentTint2
    val AccentLight get() = palette.value.AccentLight
    val OnAccent get() = palette.value.OnAccent
    val Learning get() = palette.value.Learning
    val NotStarted get() = palette.value.NotStarted
    val GoldText get() = palette.value.GoldText
    val GoldBg get() = palette.value.GoldBg
    val SecondaryButtonBorder get() = palette.value.SecondaryButtonBorder
    val Chevron get() = palette.value.Chevron
    val PageSurface get() = palette.value.PageSurface
    val Hairline get() = palette.value.Hairline
    val DockBorder get() = palette.value.DockBorder
    val HeaderRule get() = palette.value.HeaderRule
    val InkChrome get() = palette.value.InkChrome
    val InkFooter get() = palette.value.InkFooter
    val ConcealedInk get() = palette.value.ConcealedInk
    val ConcealedMedallion get() = palette.value.ConcealedMedallion
    val StumbleAmber get() = palette.value.StumbleAmber
    val StumbleBorder get() = palette.value.StumbleBorder
    val StumbleBg get() = palette.value.StumbleBg
    val StumbleBgPressed get() = palette.value.StumbleBgPressed
    val StumbleInk get() = palette.value.StumbleInk
    val SessionBg get() = palette.value.SessionBg
    val LoopCardBorder get() = palette.value.LoopCardBorder
    val ControlBorder get() = palette.value.ControlBorder
    val ControlPressed get() = palette.value.ControlPressed
    val SegmentedTrack get() = palette.value.SegmentedTrack
    val SegmentedSelected get() = palette.value.SegmentedSelected
    val TransportGlyph get() = palette.value.TransportGlyph
    val WordHighlightBg get() = palette.value.WordHighlightBg
    val WordHighlightInk get() = palette.value.WordHighlightInk
    val UpcomingWord get() = palette.value.UpcomingWord
    val DashedNode get() = palette.value.DashedNode
    val DashedLink get() = palette.value.DashedLink
    val QueuedNumeral get() = palette.value.QueuedNumeral
    val ProgressTrack get() = palette.value.ProgressTrack
    val ForgotInk get() = palette.value.ForgotInk
    val ForgotHint get() = palette.value.ForgotHint
    val ForgotBorder get() = palette.value.ForgotBorder
    val ForgotBg get() = palette.value.ForgotBg
    val HesitantHint get() = palette.value.HesitantHint
    val PerfectHint get() = palette.value.PerfectHint
    val MapEmpty get() = palette.value.MapEmpty
    val WaveformBg get() = palette.value.WaveformBg
    val WaveformPlayed get() = palette.value.WaveformPlayed
    val WaveformUnplayed get() = palette.value.WaveformUnplayed
    val TawqitCurrentInk get() = palette.value.TawqitCurrentInk
    val TaggedText get() = palette.value.TaggedText
    val NowPlayingFill get() = palette.value.NowPlayingFill
    val AyahHighlightFill get() = palette.value.AyahHighlightFill
    val PlayTile get() = palette.value.PlayTile
    val PlayTileInk get() = palette.value.PlayTileInk
    val KhatamGoldDeep get() = palette.value.KhatamGoldDeep
    val KhatamGoldBorder get() = palette.value.KhatamGoldBorder
    val KhatamProgressFill get() = palette.value.KhatamProgressFill
    val KhatamToday get() = palette.value.KhatamToday
    val KhatamCardTint get() = palette.value.KhatamCardTint
    val KhatamInsetTint get() = palette.value.KhatamInsetTint
    val KhatamCardBorder get() = palette.value.KhatamCardBorder
    val KhatamBorderSoft get() = palette.value.KhatamBorderSoft
    val KhatamRingTrack get() = palette.value.KhatamRingTrack
    val KhatamUpcoming get() = palette.value.KhatamUpcoming
    val ModalScrim get() = palette.value.ModalScrim
    val ClayText get() = palette.value.ClayText
    val ClayTextSoft get() = palette.value.ClayTextSoft
    val ClayBg get() = palette.value.ClayBg
    val ClayBorder get() = palette.value.ClayBorder
    val ClayChip get() = palette.value.ClayChip
    val ExerciseSelectedCard get() = palette.value.ExerciseSelectedCard
    val ArabicAnswerInk get() = palette.value.ArabicAnswerInk
    val ExerciseSegmentedTrack get() = palette.value.ExerciseSegmentedTrack
    val KeyboardTray get() = palette.value.KeyboardTray
    val KeyboardLetterKey get() = palette.value.KeyboardLetterKey
    val KeyboardSpecialKey get() = palette.value.KeyboardSpecialKey
    val KeyboardHarakatBg get() = palette.value.KeyboardHarakatBg
    val KeyboardHarakatKey get() = palette.value.KeyboardHarakatKey
    val KeyboardHarakatBorder get() = palette.value.KeyboardHarakatBorder
    val KeyboardHarakatGlyph get() = palette.value.KeyboardHarakatGlyph
    val KeyboardUthmaniBg get() = palette.value.KeyboardUthmaniBg
    val KeyboardUthmaniKey get() = palette.value.KeyboardUthmaniKey
    val KeyboardUthmaniBorder get() = palette.value.KeyboardUthmaniBorder
    val KeyboardUthmaniGlyph get() = palette.value.KeyboardUthmaniGlyph
    val KeyboardWaqfRing get() = palette.value.KeyboardWaqfRing
    val KeyboardWaqfBg get() = palette.value.KeyboardWaqfBg
    val KeyboardPopoverBg get() = palette.value.KeyboardPopoverBg
    val KeyboardPopoverKey get() = palette.value.KeyboardPopoverKey
    // Reused existing tokens (exact-hex equivalents in the light palette):
    //   KeyboardHamzaBg        -> KhatamCardTint  (#FBF3DF)
    //   KeyboardHamzaRing      -> KhatamGoldBorder (#C99A45)
    //   KeyboardHamzaGlyph     -> KhatamGoldDeep  (#9A732F)
    //   KeyboardPopoverHighlight -> Accent        (#3E7D6E)
    //   KeyboardKeyInk         -> Ink             (#2A2620)
}
