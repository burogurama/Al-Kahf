package app.alkahf

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import app.alkahf.ui.AlkahfApp
import app.alkahf.ui.theme.AlkahfTheme
import app.alkahf.ui.theme.ThemeMode

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        val repository = (application as AlkahfApplication).repository
        setContent {
            var themeMode by remember { mutableStateOf(toThemeMode(repository.themeMode)) }
            AlkahfTheme(mode = themeMode) {
                AlkahfApp(
                    themeMode = themeMode,
                    onThemeModeChange = { mode ->
                        themeMode = mode
                        repository.themeMode = mode.name.lowercase()
                    },
                )
            }
        }
    }

    private fun toThemeMode(value: String): ThemeMode = when (value) {
        "light" -> ThemeMode.LIGHT
        "dark" -> ThemeMode.DARK
        else -> ThemeMode.SYSTEM
    }
}
