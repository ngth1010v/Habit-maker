package app.habitmaker.util

import android.content.Context
import android.content.res.Configuration
import java.util.Locale

/**
 * The user's chosen app language, read synchronously from `attachBaseContext` so the very first
 * frame renders in the right language.
 */
object LocalePrefs {
    private const val PREFS_NAME = "habitmaker_prefs"
    private const val KEY_LOCALE = "locale"

    /** BCP-47 language tag ("en", "vi"), or "" to follow the system locale. */
    fun get(context: Context): String =
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE).getString(KEY_LOCALE, "").orEmpty()

    fun set(context: Context, languageTag: String) {
        // commit, not apply: the activity is recreated right after and must read the new value.
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE).edit().putString(KEY_LOCALE, languageTag).commit()
    }

    /** Wraps [context] with the stored locale applied. No-op when following the system locale. */
    fun wrap(context: Context): Context {
        val tag = get(context)
        if (tag.isEmpty()) return context
        val locale = Locale.forLanguageTag(tag)
        Locale.setDefault(locale)
        val config = Configuration(context.resources.configuration).apply { setLocale(locale) }
        return context.createConfigurationContext(config)
    }
}
