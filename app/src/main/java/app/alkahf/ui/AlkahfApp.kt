package app.alkahf.ui

import androidx.activity.compose.BackHandler
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import android.content.res.Resources
import androidx.compose.ui.platform.LocalContext
import androidx.compose.runtime.rememberCoroutineScope
import app.alkahf.AlkahfApplication
import app.alkahf.R
import app.alkahf.data.HomeData
import app.alkahf.data.LoopPreset
import app.alkahf.data.MemorizationState
import app.alkahf.ui.components.AlkahfTab
import app.alkahf.ui.home.AyahMemorizationState
import app.alkahf.ui.home.DayActivity
import app.alkahf.ui.home.HomeScreen
import app.alkahf.ui.home.HomeUiState
import app.alkahf.ui.home.WeekDay
import app.alkahf.ui.library.LibraryScreen
import app.alkahf.ui.library.ReciterDownloadsScreen
import app.alkahf.ui.loop.LoopPlayerScreen
import app.alkahf.ui.mushaf.MushafScreen
import app.alkahf.ui.progress.ProgressScreen
import app.alkahf.ui.review.ReviewScreen
import app.alkahf.ui.tawqit.TawqitTaggingScreen
import kotlinx.coroutines.launch

private enum class AlkahfDestination {
    Home, Mushaf, Loop, Review, Progress, Library, ReciterDownloads, TawqitTagging, Settings
}

private fun buildHomeUiState(res: Resources, data: HomeData): HomeUiState {
    val names = data.review.names
    val sabaq = data.sabaq
    val drill = data.drill
    return HomeUiState(
        streakDays = data.streakDays,
        hasSabaq = sabaq != null,
        sabaqReference = sabaq?.let {
            res.getString(R.string.home_sabaq_reference, it.surahNameLatin, it.ayahFrom, it.ayahTo)
        } ?: "",
        sabaqAyahText = sabaq?.firstAyahText ?: "",
        sabaqAyahMarker = sabaq?.firstAyahMarker ?: "",
        sabaqAyahStates = sabaq?.states?.map { it.toHomeState() } ?: emptyList(),
        reviewPortionCount = data.review.count,
        reviewEstimatedMinutes = data.review.minutes,
        reviewPortionNames = names.take(4),
        reviewOverflowCount = (names.size - 4).coerceAtLeast(0),
        hasDrill = drill != null,
        drillPresetId = drill?.id ?: 0L,
        drillPresetTitle = drill?.let {
            res.getString(
                R.string.home_drill_title,
                it.reciterName,
                it.surahNameLatin,
                it.ayahFrom,
                it.ayahTo,
            )
        } ?: "",
        drillPresetDetail = drill?.let {
            res.getString(R.string.home_drill_detail, it.perAyah, it.perChain)
        } ?: "",
        weekSummary = res.getString(
            R.string.home_week_summary,
            data.week.daysPracticed,
            data.week.ayatThisWeek,
        ),
        weekDays = data.week.dayLetters.mapIndexed { index, letter ->
            val isToday = index == data.week.dayLetters.lastIndex
            WeekDay(
                letter = letter,
                activity = when {
                    isToday -> DayActivity.TODAY
                    data.week.practiced.getOrElse(index) { false } -> DayActivity.PRACTICED
                    else -> DayActivity.MISSED
                },
            )
        },
    )
}

private fun MemorizationState.toHomeState(): AyahMemorizationState = when (this) {
    MemorizationState.NOT_STARTED -> AyahMemorizationState.NOT_STARTED
    MemorizationState.LEARNING -> AyahMemorizationState.LEARNING
    MemorizationState.MEMORIZED -> AyahMemorizationState.MEMORIZED
    MemorizationState.STRONG -> AyahMemorizationState.STRONG
}

@Composable
fun AlkahfApp(
    playDrillSignal: Int = 0,
    onThemeModeChange: (app.alkahf.ui.theme.ThemeMode) -> Unit = {},
    onLanguageChange: (String) -> Unit = {},
) {
    var destination by remember { mutableStateOf(AlkahfDestination.Home) }
    // Set when the Mushaf is opened for a specific surah (the sabaq) or page
    // (from the Progress map); both null means resume the last page read.
    var mushafTargetSurah by remember { mutableStateOf<Int?>(null) }
    var mushafTargetPage by remember { mutableStateOf<Int?>(null) }
    // The preset to open in the Loop player; null means the default preset.
    var loopPresetId by remember { mutableStateOf<Long?>(null) }
    // True when the Loop player should open straight into the new-preset editor.
    var loopNewPreset by remember { mutableStateOf(false) }
    var manageReciter by remember { mutableStateOf<app.alkahf.data.ReciterStatus?>(null) }
    // The Tawqīt track being tagged (a fresh draft or an existing track).
    var tawqitDraft by remember { mutableStateOf<app.alkahf.data.TawqitTrack?>(null) }
    // The sabaq range to highlight + scroll to when the Mushaf opens for it.
    var mushafHighlight by remember { mutableStateOf<app.alkahf.data.AyahRange?>(null) }
    val repository = (LocalContext.current.applicationContext as AlkahfApplication).repository
    val scope = rememberCoroutineScope()

    fun openMushaf(surah: Int?, page: Int?, highlight: app.alkahf.data.AyahRange? = null) {
        mushafTargetSurah = surah
        mushafTargetPage = page
        mushafHighlight = highlight
        destination = AlkahfDestination.Mushaf
    }

    fun onTab(tab: AlkahfTab) {
        when (tab) {
            AlkahfTab.TODAY -> destination = AlkahfDestination.Home
            AlkahfTab.MUSHAF -> openMushaf(null, null)
            AlkahfTab.REVIEW -> destination = AlkahfDestination.Review
            AlkahfTab.PROGRESS -> destination = AlkahfDestination.Progress
            AlkahfTab.LIBRARY -> destination = AlkahfDestination.Library
        }
    }

    val resources = LocalContext.current.resources
    // Bumped to reload the Home dashboard after an in-place change (e.g. marking
    // the sabaq memorized) that doesn't switch destination.
    var homeRefresh by remember { mutableStateOf(0) }
    val homeState by produceState(initialValue = HomeUiState(), destination, homeRefresh) {
        if (destination == AlkahfDestination.Home) {
            value = buildHomeUiState(resources, repository.homeData())
        }
    }

    // A reminder's "Listen" action drops the user straight into the sabaq drill.
    // Opening the Loop player applies the preset, which starts playback (so the
    // drill is already reciting); loopPresetId null → the default sabaq drill.
    LaunchedEffect(playDrillSignal) {
        if (playDrillSignal > 0) {
            loopPresetId = null
            loopNewPreset = false
            destination = AlkahfDestination.Loop
        }
    }

    when (destination) {
        AlkahfDestination.Home -> HomeScreen(
            state = homeState,
            onOpenMushaf = { openMushaf(null, null) },
            onOpenSabaq = { openMushaf(null, null, highlight = repository.sabaqRange) },
            // (sabaqRange is null when there's no sabaq → opens the Mushaf normally)
            onMarkSabaq = {
                scope.launch {
                    repository.markSabaqMemorized()
                    homeRefresh++
                }
            },
            onOpenLoop = {
                loopPresetId = homeState.drillPresetId.takeIf { it != 0L }
                loopNewPreset = false
                destination = AlkahfDestination.Loop
            },
            onOpenReview = { destination = AlkahfDestination.Review },
            onOpenProgress = { destination = AlkahfDestination.Progress },
            onOpenLibrary = { destination = AlkahfDestination.Library },
            onOpenSettings = { destination = AlkahfDestination.Settings },
        )
        AlkahfDestination.Mushaf -> {
            BackHandler { destination = AlkahfDestination.Home }
            MushafScreen(
                startSurah = mushafTargetSurah,
                startPage = mushafTargetPage,
                highlightRange = mushafHighlight,
                onBack = { destination = AlkahfDestination.Home },
                onImportReciter = { reciter ->
                    manageReciter = reciter
                    destination = AlkahfDestination.ReciterDownloads
                },
            )
        }
        AlkahfDestination.Loop -> {
            BackHandler { destination = AlkahfDestination.Home }
            LoopPlayerScreen(
                presetId = loopPresetId,
                newPreset = loopNewPreset,
                onBack = { destination = AlkahfDestination.Home },
            )
        }
        AlkahfDestination.Review -> {
            BackHandler { destination = AlkahfDestination.Home }
            ReviewScreen(onBack = { destination = AlkahfDestination.Home })
        }
        AlkahfDestination.Progress -> {
            BackHandler { destination = AlkahfDestination.Home }
            ProgressScreen(
                onOpenPage = { page -> openMushaf(null, page) },
                onSelectTab = ::onTab,
            )
        }
        AlkahfDestination.Library -> {
            BackHandler { destination = AlkahfDestination.Home }
            LibraryScreen(
                onOpenPreset = { id ->
                    loopPresetId = id
                    loopNewPreset = false
                    destination = AlkahfDestination.Loop
                },
                onNewPreset = {
                    loopPresetId = null
                    loopNewPreset = true
                    destination = AlkahfDestination.Loop
                },
                onManageReciter = { reciter ->
                    manageReciter = reciter
                    destination = AlkahfDestination.ReciterDownloads
                },
                onSelectTab = ::onTab,
            )
        }
        AlkahfDestination.Settings -> {
            BackHandler { destination = AlkahfDestination.Home }
            app.alkahf.ui.settings.SettingsScreen(
                onBack = { destination = AlkahfDestination.Home },
                onThemeModeChange = onThemeModeChange,
                onLanguageChange = onLanguageChange,
            )
        }
        AlkahfDestination.TawqitTagging -> {
            BackHandler { destination = AlkahfDestination.ReciterDownloads }
            tawqitDraft?.let { draft ->
                TawqitTaggingScreen(
                    draft = draft,
                    onSaved = { destination = AlkahfDestination.ReciterDownloads },
                    onClose = { destination = AlkahfDestination.ReciterDownloads },
                )
            }
        }
        AlkahfDestination.ReciterDownloads -> {
            BackHandler { destination = AlkahfDestination.Library }
            val reciter = manageReciter
            if (reciter != null) {
                ReciterDownloadsScreen(
                    reciterKey = reciter.key,
                    reciterName = reciter.displayName,
                    isImported = reciter.isImported,
                    onTimeSurah = { surah ->
                        scope.launch {
                            tawqitDraft = repository.tawqitDraftForImport(reciter.key, surah)
                            if (tawqitDraft != null) destination = AlkahfDestination.TawqitTagging
                        }
                    },
                    onBack = { destination = AlkahfDestination.Library },
                )
            }
        }
    }
}
