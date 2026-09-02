package com.saamaka.dico.testeurs

import android.content.Context

/** Stores only the interface language; tester identity and mission data stay separate. */
class UiLanguageStore(context: Context) {
    private val preferences = context.getSharedPreferences(PREFERENCES_NAME, Context.MODE_PRIVATE)

    fun load(): UiLanguage = decode(preferences.getString(KEY_LANGUAGE, null))

    fun save(language: UiLanguage) {
        preferences.edit().putString(KEY_LANGUAGE, language.name).apply()
    }

    companion object {
        internal fun decode(stored: String?): UiLanguage = stored
            ?.let { value -> UiLanguage.entries.firstOrNull { it.name == value } }
            ?: UiLanguage.FRENCH

        const val PREFERENCES_NAME = "dicosaam_ui_preferences"
        const val KEY_LANGUAGE = "interface_language"
    }
}
