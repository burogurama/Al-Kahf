package app.alkahf

import android.content.Context
import android.content.res.Configuration
import java.util.Locale

/**
 * Applies the in-app language choice ("system" | "en" | "ar") to a Context.
 *
 * The language is read directly from SharedPreferences because [apply] runs in
 * [android.app.Activity.attachBaseContext], before the Application (and thus the
 * repository) is fully available.
 */
object LocaleManager {
    private const val PREFS = "alkahf_prefs"
    private const val KEY_LANGUAGE = "app_language"
    private const val DEFAULT = "system"

    fun language(context: Context): String =
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
            .getString(KEY_LANGUAGE, DEFAULT) ?: DEFAULT

    /**
     * Wraps [base] in a configuration context for the chosen language. When the
     * language is "system" the base context is returned unchanged so the device
     * locale (and its layout direction) is honoured.
     */
    fun apply(base: Context): Context {
        val language = language(base)
        if (language == DEFAULT) return base
        val locale = Locale(language)
        Locale.setDefault(locale)
        val config = Configuration(base.resources.configuration)
        config.setLocale(locale)
        config.setLayoutDirection(locale)
        return base.createConfigurationContext(config)
    }
}
