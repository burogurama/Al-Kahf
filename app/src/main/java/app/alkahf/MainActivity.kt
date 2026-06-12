package app.alkahf

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import app.alkahf.ui.AlkahfApp
import app.alkahf.ui.theme.AlkahfTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            AlkahfTheme {
                AlkahfApp()
            }
        }
    }
}
