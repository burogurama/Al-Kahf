package app.alkahf.notify

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import app.alkahf.AlkahfApplication
import app.alkahf.LocaleManager
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

/**
 * Fires the reminder notification when an alarm goes off, and re-arms the alarm
 * set on boot, clock changes, or after each firing (the alarms are one-shot).
 */
class ReminderReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        when (intent.action) {
            ReminderScheduler.ACTION_FIRE -> {
                val app = context.applicationContext as AlkahfApplication
                val pending = goAsync()
                // The notification renders in the in-app language; resolve the
                // sūrah name to match (Arabic when the effective locale is ar).
                val arabic = LocaleManager.apply(context).resources
                    .configuration.locales[0].language == "ar"
                CoroutineScope(Dispatchers.IO).launch {
                    try {
                        HifzNotifier.showReminder(context, app.repository.sabaqReference(arabic))
                        // One-shot alarm just fired — re-arm the whole set.
                        ReminderScheduler.reschedule(context)
                    } finally {
                        pending.finish()
                    }
                }
            }
            ReminderScheduler.ACTION_FIRE_KHATAM -> {
                val app = context.applicationContext as AlkahfApplication
                val pending = goAsync()
                val arabic = LocaleManager.apply(context).resources
                    .configuration.locales[0].language == "ar"
                CoroutineScope(Dispatchers.IO).launch {
                    try {
                        // Skip posting when there's no active/complete khatam, but
                        // still re-arm (the one-shot alarm just fired).
                        val ref = app.repository.khatamPortionReminderRef(arabic)
                        if (ref != null) KhatamNotifier.showReminder(context, ref)
                        ReminderScheduler.reschedule(context)
                    } finally {
                        pending.finish()
                    }
                }
            }
            else -> ReminderScheduler.reschedule(context) // boot / time / timezone change
        }
    }
}
