package app.alkahf.ui

import androidx.activity.compose.BackHandler
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import app.alkahf.AlkahfApplication
import app.alkahf.ui.components.AlkahfTab
import app.alkahf.ui.home.HomeScreen
import app.alkahf.ui.home.HomeUiState
import app.alkahf.ui.library.LibraryScreen
import app.alkahf.ui.library.ReciterDownloadsScreen
import app.alkahf.ui.loop.LoopPlayerScreen
import app.alkahf.ui.mushaf.MushafScreen
import app.alkahf.ui.progress.ProgressScreen
import app.alkahf.ui.review.ReviewScreen
import app.alkahf.ui.tawqit.TawqitTaggingScreen
import androidx.compose.runtime.rememberCoroutineScope
import kotlinx.coroutines.launch

private enum class AlkahfDestination {
    Home, Mushaf, Loop, Review, Progress, Library, ReciterDownloads, TawqitTagging
}

/** The current sabaq surah; becomes dynamic once sabaq state is user-configurable. */
private const val SABAQ_SURAH = 18

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
            val preset = repository.defaultPreset()
            value = HomeUiState(
                drillPresetTitle =
                    "${preset.reciterName} · ${preset.surahNameLatin} ${preset.ayahFrom}–${preset.ayahTo}",
                drillPresetDetail =
                    "Cumulative chain · ${preset.perAyah}× each · ${preset.perChain}× chain",
            )
        }
    }

    when (destination) {
        AlkahfDestination.Home -> HomeScreen(
            state = homeState,
            onOpenMushaf = { openMushaf(null, null) },
            onOpenSabaq = { openMushaf(SABAQ_SURAH, null) },
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
