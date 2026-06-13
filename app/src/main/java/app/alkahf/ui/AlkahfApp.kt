package app.alkahf.ui

import androidx.activity.compose.BackHandler
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import androidx.compose.runtime.rememberCoroutineScope
import app.alkahf.AlkahfApplication
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
    Home, Mushaf, Loop, Review, Progress, Library, ReciterDownloads, TawqitTagging
}

private fun buildHomeUiState(data: HomeData, preset: LoopPreset): HomeUiState {
    val names = data.review.names
    return HomeUiState(
        streakDays = data.streakDays,
        sabaqReference = data.sabaq.reference,
        sabaqAyahText = data.sabaq.firstAyahText,
        sabaqAyahMarker = data.sabaq.firstAyahMarker,
        sabaqAyahStates = data.sabaq.states.map { it.toHomeState() },
        reviewPortionCount = data.review.count,
        reviewEstimatedMinutes = data.review.minutes,
        reviewPortionNames = names.take(4),
        reviewOverflowCount = (names.size - 4).coerceAtLeast(0),
        drillPresetTitle = "${preset.reciterName} · ${preset.surahNameLatin} ${preset.ayahFrom}–${preset.ayahTo}",
        drillPresetDetail = "Cumulative chain · ${preset.perAyah}× each · ${preset.perChain}× chain",
        weekSummary = "${data.week.daysPracticed} of 7 days · ${data.week.ayatThisWeek} ayat",
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
fun AlkahfApp() {
    var destination by remember { mutableStateOf(AlkahfDestination.Home) }
    // Set when the Mushaf is opened for a specific surah (the sabaq) or page
    // (from the Progress map); both null means resume the last page read.
    var mushafTargetSurah by remember { mutableStateOf<Int?>(null) }
    var mushafTargetPage by remember { mutableStateOf<Int?>(null) }
    // The preset to open in the Loop player; null means the default preset.
    var loopPresetId by remember { mutableStateOf<Long?>(null) }
    // The reciter whose surahs are being managed.
    var manageReciter by remember { mutableStateOf<app.alkahf.data.ReciterStatus?>(null) }
    // The Tawqīt track being tagged (a fresh draft or an existing track).
    var tawqitDraft by remember { mutableStateOf<app.alkahf.data.TawqitTrack?>(null) }
    val repository = (LocalContext.current.applicationContext as AlkahfApplication).repository
    val scope = rememberCoroutineScope()

    fun openMushaf(surah: Int?, page: Int?) {
        mushafTargetSurah = surah
        mushafTargetPage = page
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

    val homeState by produceState(initialValue = HomeUiState(), destination) {
        if (destination == AlkahfDestination.Home) {
            value = buildHomeUiState(repository.homeData(), repository.defaultPreset())
        }
    }

    when (destination) {
        AlkahfDestination.Home -> HomeScreen(
            state = homeState,
            onOpenMushaf = { openMushaf(null, null) },
            onOpenSabaq = { openMushaf(repository.sabaqSurah, null) },
            onOpenLoop = {
                loopPresetId = null
                destination = AlkahfDestination.Loop
            },
            onOpenReview = { destination = AlkahfDestination.Review },
            onOpenProgress = { destination = AlkahfDestination.Progress },
            onOpenLibrary = { destination = AlkahfDestination.Library },
        )
        AlkahfDestination.Mushaf -> {
            BackHandler { destination = AlkahfDestination.Home }
            MushafScreen(
                startSurah = mushafTargetSurah,
                startPage = mushafTargetPage,
                onBack = { destination = AlkahfDestination.Home },
            )
        }
        AlkahfDestination.Loop -> {
            BackHandler { destination = AlkahfDestination.Home }
            LoopPlayerScreen(
                presetId = loopPresetId,
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
                    destination = AlkahfDestination.Loop
                },
                onNewPreset = {
                    loopPresetId = null
                    destination = AlkahfDestination.Loop
                },
                onManageReciter = { reciter ->
                    manageReciter = reciter
                    destination = AlkahfDestination.ReciterDownloads
                },
                onSelectTab = ::onTab,
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
