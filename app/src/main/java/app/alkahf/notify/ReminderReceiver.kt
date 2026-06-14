package app.alkahf.notify

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import app.alkahf.AlkahfApplication
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
                CoroutineScope(Dispatchers.IO).launch {
                    try {
                        HifzNotifier.showReminder(context, app.repository.sabaqReference())
                        // One-shot alarm just fired — re-arm the whole set.
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
