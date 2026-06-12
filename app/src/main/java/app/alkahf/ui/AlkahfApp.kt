package app.alkahf.ui

import androidx.activity.compose.BackHandler
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import app.alkahf.AlkahfApplication
import app.alkahf.ui.components.AlkahfTab
import app.alkahf.ui.home.HomeScreen
import app.alkahf.ui.home.HomeUiState
import app.alkahf.ui.loop.LoopPlayerScreen
import app.alkahf.ui.mushaf.MushafScreen
import app.alkahf.ui.progress.ProgressScreen
import app.alkahf.ui.review.ReviewScreen

private enum class AlkahfDestination { Home, Mushaf, Loop, Review, Progress }

/** The current sabaq surah; becomes dynamic once sabaq state is user-configurable. */
private const val SABAQ_SURAH = 18

@Composable
fun AlkahfApp() {
    var destination by rememberSaveable { mutableStateOf(AlkahfDestination.Home) }
    // Set when the Mushaf is opened for a specific surah (the sabaq) or page
    // (from the Progress map); both null means resume the last page read.
    var mushafTargetSurah by rememberSaveable { mutableStateOf<Int?>(null) }
    var mushafTargetPage by rememberSaveable { mutableStateOf<Int?>(null) }
    val repository = (LocalContext.current.applicationContext as AlkahfApplication).repository
    when (destination) {
        AlkahfDestination.Home -> HomeScreen(
            state = remember(destination) {
                repository.loopPreset?.let { preset ->
                    HomeUiState(
                        drillPresetTitle =
                            "${preset.reciterName} · ${preset.surahNameLatin} ${preset.ayahFrom}–${preset.ayahTo}",
                        drillPresetDetail =
                            "Cumulative chain · ${preset.perAyah}× each · ${preset.perChain}× chain",
                    )
                } ?: HomeUiState()
            },
            onOpenMushaf = {
                mushafTargetSurah = null
                mushafTargetPage = null
                destination = AlkahfDestination.Mushaf
            },
            onOpenSabaq = {
                mushafTargetSurah = SABAQ_SURAH
                mushafTargetPage = null
                destination = AlkahfDestination.Mushaf
            },
            onOpenLoop = { destination = AlkahfDestination.Loop },
            onOpenReview = { destination = AlkahfDestination.Review },
            onOpenProgress = { destination = AlkahfDestination.Progress },
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
            LoopPlayerScreen(onBack = { destination = AlkahfDestination.Home })
        }
        AlkahfDestination.Review -> {
            BackHandler { destination = AlkahfDestination.Home }
            ReviewScreen(onBack = { destination = AlkahfDestination.Home })
        }
        AlkahfDestination.Progress -> {
            BackHandler { destination = AlkahfDestination.Home }
            ProgressScreen(
                onOpenPage = { page ->
                    mushafTargetSurah = null
                    mushafTargetPage = page
                    destination = AlkahfDestination.Mushaf
                },
                onSelectTab = { tab ->
                    destination = when (tab) {
                        AlkahfTab.TODAY -> AlkahfDestination.Home
                        AlkahfTab.MUSHAF -> {
                            mushafTargetSurah = null
                            mushafTargetPage = null
                            AlkahfDestination.Mushaf
                        }
                        AlkahfTab.REVIEW -> AlkahfDestination.Review
                        else -> AlkahfDestination.Progress
                    }
                },
            )
        }
    }
}
