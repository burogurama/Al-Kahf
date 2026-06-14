package app.alkahf.notify

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import app.alkahf.AlkahfApplication
import java.util.Calendar

/**
 * Arms one daily alarm per configured reminder time.
 *
 * Inexact [AlarmManager.setAndAllowWhileIdle] alarms are used so no exact-alarm
 * permission is required; a hifz nudge tolerates the few minutes of Doze drift.
 * Each alarm is one-shot, so the receiver re-arms the whole set every time one
 * fires (and on boot / clock changes).
 */
object ReminderScheduler {
    const val ACTION_FIRE = "app.alkahf.action.HIFZ_REMINDER"

    /** Upper bound on how many reminder slots we manage (and cancel). */
    const val MAX_SLOTS = 12

    /** Cancels every reminder alarm, then re-arms them from the saved config. */
    fun reschedule(context: Context) {
        val app = context.applicationContext as AlkahfApplication
        val am = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        // Always clear all slots first so removed/disabled times don't linger.
        for (slot in 0 until MAX_SLOTS) am.cancel(pendingIntent(context, slot, null))

        if (!app.repository.remindersEnabled) return
        app.repository.reminderTimes.take(MAX_SLOTS).forEachIndexed { slot, minute ->
            am.setAndAllowWhileIdle(
                AlarmManager.RTC_WAKEUP,
                nextTriggerMillis(minute),
                pendingIntent(context, slot, minute),
            )
        }
    }

    private fun nextTriggerMillis(minuteOfDay: Int): Long {
        val now = System.currentTimeMillis()
        val next = Calendar.getInstance().apply {
            set(Calendar.HOUR_OF_DAY, minuteOfDay / 60)
            set(Calendar.MINUTE, minuteOfDay % 60)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }
        if (next.timeInMillis <= now) next.add(Calendar.DAY_OF_MONTH, 1)
        return next.timeInMillis
    }

    private fun pendingIntent(context: Context, slot: Int, minute: Int?): PendingIntent {
        val intent = Intent(context, ReminderReceiver::class.java).setAction(ACTION_FIRE)
        // Extras are ignored when matching for cancellation; the slot request code
        // is what identifies the alarm.
        return PendingIntent.getBroadcast(
            context,
            slot,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )
    }
}
