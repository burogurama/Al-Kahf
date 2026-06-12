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

private enum class AlkahfDestination { Home, Mushaf, Loop }

@Composable
fun AlkahfApp() {
    var destination by rememberSaveable { mutableStateOf(AlkahfDestination.Home) }
    when (destination) {
        AlkahfDestination.Home -> HomeScreen(
            onOpenMushaf = { destination = AlkahfDestination.Mushaf },
            onOpenLoop = { destination = AlkahfDestination.Loop },
        )
        AlkahfDestination.Mushaf -> {
            BackHandler { destination = AlkahfDestination.Home }
            MushafScreen(onBack = { destination = AlkahfDestination.Home })
        }
        AlkahfDestination.Loop -> {
            BackHandler { destination = AlkahfDestination.Home }
            LoopPlayerScreen(onBack = { destination = AlkahfDestination.Home })
        }
    }
}
