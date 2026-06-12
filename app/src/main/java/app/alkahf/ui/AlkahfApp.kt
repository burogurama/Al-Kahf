package app.alkahf.ui

import androidx.activity.compose.BackHandler
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import app.alkahf.ui.home.HomeScreen
import app.alkahf.ui.loop.LoopPlayerScreen
import app.alkahf.ui.mushaf.MushafScreen
import app.alkahf.ui.review.ReviewScreen

private enum class AlkahfDestination { Home, Mushaf, Loop, Review }

@Composable
fun AlkahfApp() {
    var destination by rememberSaveable { mutableStateOf(AlkahfDestination.Home) }
    when (destination) {
        AlkahfDestination.Home -> HomeScreen(
            onOpenMushaf = { destination = AlkahfDestination.Mushaf },
            onOpenLoop = { destination = AlkahfDestination.Loop },
            onOpenReview = { destination = AlkahfDestination.Review },
        )
        AlkahfDestination.Mushaf -> {
            BackHandler { destination = AlkahfDestination.Home }
            MushafScreen(onBack = { destination = AlkahfDestination.Home })
        }
        AlkahfDestination.Loop -> {
            BackHandler { destination = AlkahfDestination.Home }
            LoopPlayerScreen(onBack = { destination = AlkahfDestination.Home })
        }
        AlkahfDestination.Review -> {
            BackHandler { destination = AlkahfDestination.Home }
            ReviewScreen(onBack = { destination = AlkahfDestination.Home })
        }
    }
}
