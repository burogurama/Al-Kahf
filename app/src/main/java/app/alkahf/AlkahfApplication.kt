package app.alkahf

import android.app.Application
import app.alkahf.data.QuranRepository

class AlkahfApplication : Application() {
    val repository: QuranRepository by lazy { QuranRepository(this) }
}
