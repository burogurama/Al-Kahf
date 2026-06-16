package app.alkahf

import android.content.Context
import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import app.alkahf.notify.HifzNotifier
import app.alkahf.notify.KhatamNotifier
import app.alkahf.notify.ReminderScheduler
import androidx.compose.runtime.CompositionLocalProvider
import app.alkahf.ui.AlkahfApp
import app.alkahf.ui.theme.AlkahfTheme
import app.alkahf.ui.theme.LocalQuranFont
import app.alkahf.ui.theme.ThemeChoice
import app.alkahf.ui.theme.quranFontFor

class MainActivity : ComponentActivity() {
    // Bumped whenever a "Listen" reminder action launches the activity, so the
    // composition navigates into the sabaq drill and starts playing.
    private var playDrillSignal = mutableIntStateOf(0)
    // Bumped whenever a khatam reminder launches the activity, so the composition
    // navigates into the Khatam tracker.
    private var openKhatamSignal = mutableIntStateOf(0)

    override fun attachBaseContext(newBase: Context) {
        super.attachBaseContext(LocaleManager.apply(newBase))
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        // Keep the reminder channel and alarms alive across app updates / installs.
        HifzNotifier.ensureChannel(this)
        KhatamNotifier.ensureChannel(this)
        ReminderScheduler.reschedule(this)
        if (intent?.getBooleanExtra(EXTRA_PLAY_DRILL, false) == true) playDrillSignal.intValue++
        if (intent?.getBooleanExtra(EXTRA_OPEN_KHATAM, false) == true) openKhatamSignal.intValue++

        val repository = (application as AlkahfApplication).repository
        setContent {
            var themeChoice by remember { mutableStateOf(toThemeChoice(repository.themeMode)) }
            AlkahfTheme(choice = themeChoice) {
                CompositionLocalProvider(LocalQuranFont provides quranFontFor(repository.riwayah)) {
                    AlkahfApp(
                        playDrillSignal = playDrillSignal.intValue,
                        openKhatamSignal = openKhatamSignal.intValue,
                        onThemeChange = { choice ->
                            themeChoice = choice
                            repository.themeMode = choice.name.lowercase()
                        },
                        onLanguageChange = { language ->
                            repository.appLanguage = language
                            recreate()
                        },
                    )
                }
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        if (intent.getBooleanExtra(EXTRA_PLAY_DRILL, false)) playDrillSignal.intValue++
        if (intent.getBooleanExtra(EXTRA_OPEN_KHATAM, false)) openKhatamSignal.intValue++
    }

    private fun toThemeChoice(value: String): ThemeChoice = when (value) {
        "light" -> ThemeChoice.LIGHT
        "dark" -> ThemeChoice.DARK
        "rose" -> ThemeChoice.ROSE
        else -> ThemeChoice.SYSTEM
    }

    companion object {
        const val EXTRA_PLAY_DRILL = "app.alkahf.extra.PLAY_DRILL"
        const val EXTRA_OPEN_KHATAM = "app.alkahf.extra.OPEN_KHATAM"
    }
}
