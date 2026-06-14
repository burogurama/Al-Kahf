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
import app.alkahf.notify.ReminderScheduler
import app.alkahf.ui.AlkahfApp
import app.alkahf.ui.theme.AlkahfTheme
import app.alkahf.ui.theme.ThemeMode

class MainActivity : ComponentActivity() {
    // Bumped whenever a "Listen" reminder action launches the activity, so the
    // composition navigates into the sabaq drill and starts playing.
    private var playDrillSignal = mutableIntStateOf(0)

    override fun attachBaseContext(newBase: Context) {
        super.attachBaseContext(LocaleManager.apply(newBase))
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        // Keep the reminder channel and alarms alive across app updates / installs.
        HifzNotifier.ensureChannel(this)
        ReminderScheduler.reschedule(this)
        if (intent?.getBooleanExtra(EXTRA_PLAY_DRILL, false) == true) playDrillSignal.intValue++

        val repository = (application as AlkahfApplication).repository
        setContent {
            var themeMode by remember { mutableStateOf(toThemeMode(repository.themeMode)) }
            AlkahfTheme(mode = themeMode) {
                AlkahfApp(
                    playDrillSignal = playDrillSignal.intValue,
                    onThemeModeChange = { mode ->
                        themeMode = mode
                        repository.themeMode = mode.name.lowercase()
                    },
                    onLanguageChange = { language ->
                        repository.appLanguage = language
                        recreate()
                    },
                )
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        if (intent.getBooleanExtra(EXTRA_PLAY_DRILL, false)) playDrillSignal.intValue++
    }

    private fun toThemeMode(value: String): ThemeMode = when (value) {
        "light" -> ThemeMode.LIGHT
        "dark" -> ThemeMode.DARK
        else -> ThemeMode.SYSTEM
    }

    companion object {
        const val EXTRA_PLAY_DRILL = "app.alkahf.extra.PLAY_DRILL"
    }
}
