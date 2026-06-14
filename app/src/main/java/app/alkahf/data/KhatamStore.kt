package app.alkahf.data

import app.alkahf.data.khatam.KhatamMath
import app.alkahf.data.user.KhatamEntity
import app.alkahf.data.user.UserDao
import java.time.LocalDate

/**
 * Today's khatam portion resolved for a reminder notification: the juzʼ and the
 * localized first/last sūrah names with their āyah numbers. Rendered outside
 * Compose, so the names are already resolved for the requested language.
 */
data class KhatamPortionRef(
    val juz: Int,
    val surahFromName: String, val ayahFrom: Int,
    val surahToName: String, val ayahTo: Int,
)

/**
 * The single active khatam (a full cover-to-cover Qur'an recitation programmed
 * at a chosen daily pace), mirroring the single sabaq-drill pattern: at most one
 * active row. Owns the khatam table; resolves today's portion through
 * [QuranTextStore]'s juzʼ division map. All derived numbers come from the pure
 * [KhatamMath].
 */
class KhatamStore(
    private val userDao: UserDao,
    private val quranText: QuranTextStore,
    private val settings: UserPreferences,
) {
    /**
     * The active khatam as a fully-resolved [KhatamState] (counters + derived
     * day/pace/finish values + today's [KhatamPortion]), or null when none is
     * active. Today's portion is resolved in the khatam's own riwāyah so its page
     * span matches that reading.
     */
    suspend fun activeKhatam(): KhatamState? {
        val entity = userDao.activeKhatam() ?: return null
        return entity.toState()
    }

    /**
     * Programs a new active khatam starting today with no units logged, with its
     * daily reminder. Any existing active khatam is removed first so there is only
     * ever one. The reminder is stored on the entity (display truth) and mirrored
     * into prefs (the synchronous scheduling source) so the two never disagree.
     */
    suspend fun programKhatam(
        pace: Int,
        reminderEnabled: Boolean = true,
        reminderMinute: Int = DEFAULT_REMINDER_MINUTE,
    ): KhatamState {
        userDao.deleteActiveKhatam()
        val entity = KhatamEntity(
            pace = pace.coerceAtLeast(1),
            startDate = LocalDate.now().toEpochDay(),
            unitsCompleted = 0,
            reminderEnabled = reminderEnabled,
            reminderTime = reminderMinute,
            riwayah = quranText.riwayah.key,
            active = true,
        )
        val id = userDao.insertKhatam(entity)
        settings.setKhatamReminderMirror(reminderEnabled, reminderMinute)
        return entity.copy(id = id).toState()
    }

    /**
     * Updates the active khatam's reminder (entity + prefs mirror). A no-op when
     * there is no active khatam.
     */
    suspend fun setKhatamReminder(enabled: Boolean, minute: Int) {
        val entity = userDao.activeKhatam() ?: return
        userDao.updateKhatam(entity.copy(reminderEnabled = enabled, reminderTime = minute))
        settings.setKhatamReminderMirror(enabled, minute)
    }

    /**
     * Today's portion resolved for a reminder notification (juzʼ + localized
     * sūrah range), using the khatam's own riwāyah. Null when there's no active
     * khatam or it is complete.
     */
    suspend fun khatamPortionReminderRef(arabic: Boolean): KhatamPortionRef? {
        val entity = userDao.activeKhatam() ?: return null
        if (entity.unitsCompleted >= KhatamMath.TOTAL_UNITS) return null
        val juz = KhatamMath.todaysPortionJuz(entity.unitsCompleted)
        val portion = quranText.khatamPortion(juz, Riwayah.fromKey(entity.riwayah)) ?: return null
        return KhatamPortionRef(
            juz = portion.juz,
            surahFromName = quranText.surahName(portion.surahFrom, arabic),
            ayahFrom = portion.ayahFrom,
            surahToName = quranText.surahName(portion.surahTo, arabic),
            ayahTo = portion.ayahTo,
        )
    }

    /**
     * Logs today's portion: advances by one juzʼ, adds that juzʼ's page count to
     * [KhatamEntity.pagesRead], updates the streak (a fresh increment when logged
     * on a new day — consecutive day extends it, a gap restarts it at 1), and
     * stamps [KhatamEntity.lastLoggedDate]. A no-op once all 30 units are done.
     * Returns the recomputed state, or null when there is no active khatam.
     */
    suspend fun logTodayPortion(): KhatamState? {
        val entity = userDao.activeKhatam() ?: return null
        if (entity.unitsCompleted >= KhatamMath.TOTAL_UNITS) return entity.toState()
        val today = LocalDate.now().toEpochDay()
        val portion = quranText.khatamPortion(
            KhatamMath.todaysPortionJuz(entity.unitsCompleted),
            Riwayah.fromKey(entity.riwayah),
        )
        val streak = when (entity.lastLoggedDate) {
            null -> 1
            today -> entity.streakDays.coerceAtLeast(1) // already logged today: keep
            today - 1 -> entity.streakDays + 1 // consecutive day
            else -> 1 // a gap restarts the streak
        }
        val updated = entity.copy(
            unitsCompleted = entity.unitsCompleted + 1,
            pagesRead = entity.pagesRead + (portion?.pageCount ?: 0),
            lastLoggedDate = today,
            streakDays = streak,
        )
        userDao.updateKhatam(updated)
        return updated.toState()
    }

    /** Removes the active khatam (cancel / delete) and clears its reminder mirror. Idempotent. */
    suspend fun cancelKhatam() {
        userDao.deleteActiveKhatam()
        settings.setKhatamReminderMirror(enabled = false, minute = DEFAULT_REMINDER_MINUTE)
    }

    /**
     * Re-derives the scheduling prefs mirror from the active khatam entity — the
     * single source of truth — so the two can't drift. Called at startup: covers a
     * mirror that was reset (e.g. prefs cleared while the DB persisted) or never
     * written. Disables the mirror when no khatam is active.
     */
    suspend fun reconcileKhatamReminder() {
        val entity = userDao.activeKhatam()
        if (entity == null) {
            settings.setKhatamReminderMirror(enabled = false, minute = DEFAULT_REMINDER_MINUTE)
        } else {
            settings.setKhatamReminderMirror(
                enabled = entity.reminderEnabled,
                minute = entity.reminderTime ?: DEFAULT_REMINDER_MINUTE,
            )
        }
    }

    private suspend fun KhatamEntity.toState(): KhatamState {
        val today = LocalDate.now().toEpochDay()
        val currentDay = KhatamMath.currentDay(startDate, today)
        val todaysJuz = KhatamMath.todaysPortionJuz(unitsCompleted)
        val isComplete = unitsCompleted >= KhatamMath.TOTAL_UNITS
        val khatamRiwayah = Riwayah.fromKey(riwayah)
        return KhatamState(
            pace = pace,
            startEpochDay = startDate,
            unitsCompleted = unitsCompleted,
            totalUnits = KhatamMath.TOTAL_UNITS,
            currentDay = currentDay,
            todaysPortionJuz = todaysJuz,
            todaysPortion = if (isComplete) null else quranText.khatamPortion(todaysJuz, khatamRiwayah),
            finishEpochDay = KhatamMath.derivedFinishDate(startDate, pace),
            paceStatus = KhatamMath.paceStatus(unitsCompleted, currentDay, pace),
            streakDays = streakDays,
            pagesRead = pagesRead,
            timeReadSeconds = timeReadSeconds,
            ringFraction = KhatamMath.ringFraction(unitsCompleted),
            percent = KhatamMath.percent(unitsCompleted),
            reminderEnabled = reminderEnabled,
            reminderTime = reminderTime,
            riwayah = khatamRiwayah,
            isComplete = isComplete,
        )
    }

    companion object {
        /** Default khatam reminder time: after Fajr (05:10 = 310 minutes). */
        const val DEFAULT_REMINDER_MINUTE = 310
    }
}
