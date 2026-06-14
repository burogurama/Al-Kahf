package app.alkahf

import android.app.Application
import app.alkahf.data.QuranRepository
import app.alkahf.ui.theme.KfgqpcHafs
import app.alkahf.ui.theme.KfgqpcWarsh
import app.alkahf.ui.theme.quranFont

class AlkahfApplication : Application() {
    val repository: QuranRepository by lazy { QuranRepository(this) }

    override fun onCreate() {
        super.onCreate()
        // Pick the mushaf font for the active riwāyah before any UI is built.
        quranFont = if (repository.riwayah == "warsh") KfgqpcWarsh else KfgqpcHafs
    }
}
