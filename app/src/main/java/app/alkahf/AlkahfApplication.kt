package app.alkahf

import android.app.Application
import app.alkahf.data.QuranRepository
import app.alkahf.notify.ReminderScheduler
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

class AlkahfApplication : Application() {
    val repository: QuranRepository by lazy { QuranRepository(this) }

    override fun onCreate() {
        super.onCreate()
        // Warm the sūrah-name cache off the main thread so the first screen can
        // resolve names synchronously (no first-frame flash of bare numbers).
        CoroutineScope(SupervisorJob()).launch { repository.preloadSurahNames() }
        // Re-derive the khatam reminder scheduling mirror from its entity (the
        // source of truth) in case the two drifted, then re-arm alarms from the
        // reconciled state — covers a prefs reset or a mirror that was never written.
        CoroutineScope(SupervisorJob()).launch {
            repository.reconcileKhatamReminder()
            ReminderScheduler.reschedule(this@AlkahfApplication)
        }
    }
}
