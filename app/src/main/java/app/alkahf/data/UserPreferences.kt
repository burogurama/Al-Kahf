package app.alkahf.data

import android.content.Context

/**
 * Typed access to the app's SharedPreferences: UI settings, the current sabaq
 * range, the active reciter and riwāyah, reminder configuration, and the
 * Mushaf's resume state. The single owner of every preference key — the rest of
 * the app reads and writes settings through here rather than touching prefs.
 */
class UserPreferences(context: Context) {
    private val prefs = context.getSharedPreferences("alkahf_prefs", Context.MODE_PRIVATE)

    /** Last page open in the Mushaf, so reading resumes where the user left off. */
    var lastMushafPage: Int?
        get() = prefs.getInt(KEY_LAST_MUSHAF_PAGE, 0).takeIf { it in 1..QuranRepository.PAGE_COUNT }
        set(value) {
            prefs.edit().putInt(KEY_LAST_MUSHAF_PAGE, value ?: 0).apply()
        }

    /** Whether the Mushaf was last in hide/self-test mode, so it reopens the same way. */
    var lastMushafHideMode: Boolean
        get() = prefs.getBoolean(KEY_LAST_HIDE_MODE, true)
        set(value) {
            prefs.edit().putBoolean(KEY_LAST_HIDE_MODE, value).apply()
        }

    /** The current sabaq range, or null when no sabaq is active. */
    val sabaqRange: AyahRange?
        get() {
            val surah = prefs.getInt(KEY_SABAQ_SURAH, 0)
            if (surah == 0) return null
            return AyahRange(surah, prefs.getInt(KEY_SABAQ_FROM, 1), prefs.getInt(KEY_SABAQ_TO, 5))
        }

    fun setSabaq(surah: Int, from: Int, to: Int) {
        prefs.edit()
            .putInt(KEY_SABAQ_SURAH, surah)
            .putInt(KEY_SABAQ_FROM, from)
            .putInt(KEY_SABAQ_TO, to)
            .apply()
    }

    /** New āyāt introduced per day; also the sabaq section length. */
    val sectionLength: Int get() = prefs.getInt(KEY_NEW_PER_DAY, 5).coerceIn(1, 20)

    /** "hafs" (default) | "warsh". Drives the Qur'an DB, font, and reciter list. */
    var riwayah: Riwayah
        get() = Riwayah.fromKey(prefs.getString(KEY_RIWAYAH, null))
        set(value) {
            prefs.edit().putString(KEY_RIWAYAH, value.key).apply()
        }

    /** The stored active-reciter path, or null when none has been chosen. */
    val activeReciterRaw: String? get() = prefs.getString(KEY_ACTIVE_RECITER, null)

    fun setActiveReciter(path: String) {
        prefs.edit().putString(KEY_ACTIVE_RECITER, path).apply()
    }

    /** "light" | "dark" | "system" (default). */
    var themeMode: String
        get() = prefs.getString(KEY_THEME_MODE, "system") ?: "system"
        set(value) {
            prefs.edit().putString(KEY_THEME_MODE, value).apply()
        }

    /** In-app UI language: "system" (default) | "en" | "ar". */
    var appLanguage: String
        get() = prefs.getString(KEY_APP_LANGUAGE, "system") ?: "system"
        set(value) {
            prefs.edit().putString(KEY_APP_LANGUAGE, value).apply()
        }

    val arabicTextSizePt: Int get() = prefs.getInt(KEY_ARABIC_SIZE, 24)
    val dailyBudgetMin: Int get() = prefs.getInt(KEY_DAILY_BUDGET, 15)
    val reviewPacing: ReviewPacing
        get() = runCatching { ReviewPacing.valueOf(prefs.getString(KEY_PACING, "STANDARD")!!) }
            .getOrDefault(ReviewPacing.STANDARD)
    val autoLowerOnStumble: Boolean get() = prefs.getBoolean(KEY_AUTO_LOWER, true)

    /** Whether daily hifz reminders are armed. */
    val remindersEnabled: Boolean get() = prefs.getBoolean(KEY_REMINDERS_ON, false)

    /** Reminder times as minutes after midnight, ascending and de-duplicated. */
    val reminderTimes: List<Int>
        get() = (prefs.getString(KEY_REMINDER_TIMES, null) ?: DEFAULT_REMINDER_TIMES)
            .split(',')
            .mapNotNull { it.trim().toIntOrNull()?.takeIf { m -> m in 0..1439 } }
            .distinct()
            .sorted()

    fun settings(): SettingsData = SettingsData(
        themeMode = themeMode,
        appLanguage = appLanguage,
        arabicTextSizePt = arabicTextSizePt,
        dailyBudgetMin = dailyBudgetMin,
        newPerDay = prefs.getInt(KEY_NEW_PER_DAY, 5),
        reviewPacing = reviewPacing,
        autoLowerOnStumble = autoLowerOnStumble,
        keepScreenOn = prefs.getBoolean(KEY_KEEP_SCREEN_ON, true),
        backgroundAudio = prefs.getBoolean(KEY_BACKGROUND_AUDIO, true),
        remindersEnabled = remindersEnabled,
        reminderTimes = reminderTimes,
    )

    fun updateSettings(s: SettingsData) {
        prefs.edit()
            .putString(KEY_THEME_MODE, s.themeMode)
            .putInt(KEY_ARABIC_SIZE, s.arabicTextSizePt)
            .putInt(KEY_DAILY_BUDGET, s.dailyBudgetMin)
            .putInt(KEY_NEW_PER_DAY, s.newPerDay)
            .putString(KEY_PACING, s.reviewPacing.name)
            .putBoolean(KEY_AUTO_LOWER, s.autoLowerOnStumble)
            .putBoolean(KEY_KEEP_SCREEN_ON, s.keepScreenOn)
            .putBoolean(KEY_BACKGROUND_AUDIO, s.backgroundAudio)
            .putBoolean(KEY_REMINDERS_ON, s.remindersEnabled)
            .putString(
                KEY_REMINDER_TIMES,
                s.reminderTimes.distinct().sorted().joinToString(","),
            )
            .apply()
    }

    companion object {
        private const val KEY_LAST_MUSHAF_PAGE = "last_mushaf_page"
        private const val KEY_LAST_HIDE_MODE = "last_mushaf_hide_mode"
        private const val KEY_ACTIVE_RECITER = "active_reciter"
        private const val KEY_THEME_MODE = "theme_mode"
        private const val KEY_APP_LANGUAGE = "app_language"
        private const val KEY_ARABIC_SIZE = "arabic_size_pt"
        private const val KEY_DAILY_BUDGET = "daily_budget_min"
        private const val KEY_NEW_PER_DAY = "new_per_day"
        private const val KEY_PACING = "review_pacing"
        private const val KEY_AUTO_LOWER = "auto_lower_stumble"
        private const val KEY_KEEP_SCREEN_ON = "keep_screen_on"
        private const val KEY_BACKGROUND_AUDIO = "background_audio"
        private const val KEY_SABAQ_SURAH = "sabaq_surah"
        private const val KEY_SABAQ_FROM = "sabaq_from"
        private const val KEY_SABAQ_TO = "sabaq_to"
        private const val KEY_RIWAYAH = "riwayah"
        private const val KEY_REMINDERS_ON = "reminders_enabled"
        private const val KEY_REMINDER_TIMES = "reminder_times"
        // A single morning nudge by default (07:30 = 450 minutes).
        private const val DEFAULT_REMINDER_TIMES = "450"
    }
}
