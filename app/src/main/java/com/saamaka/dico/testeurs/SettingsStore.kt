package com.saamaka.dico.testeurs

import android.content.Context

enum class ThemeMode {
    SYSTEM,
    LIGHT,
    DARK
}

enum class TextSizeMode(val scale: Float) {
    SMALL(0.90f),
    NORMAL(1.00f),
    LARGE(1.18f)
}

class SettingsStore(context: Context) {
    private val preferences = context.getSharedPreferences(
        "saamaka_settings",
        Context.MODE_PRIVATE
    )

    fun themeMode(): ThemeMode =
        runCatching {
            ThemeMode.valueOf(
                preferences.getString(KEY_THEME, ThemeMode.SYSTEM.name)
                    ?: ThemeMode.SYSTEM.name
            )
        }.getOrDefault(ThemeMode.SYSTEM)

    fun setThemeMode(mode: ThemeMode) {
        preferences.edit()
            .putString(KEY_THEME, mode.name)
            .apply()
    }

    fun textSizeMode(): TextSizeMode =
        runCatching {
            TextSizeMode.valueOf(
                preferences.getString(KEY_TEXT_SIZE, TextSizeMode.NORMAL.name)
                    ?: TextSizeMode.NORMAL.name
            )
        }.getOrDefault(TextSizeMode.NORMAL)

    fun setTextSizeMode(mode: TextSizeMode) {
        preferences.edit()
            .putString(KEY_TEXT_SIZE, mode.name)
            .apply()
    }

    private companion object {
        const val KEY_THEME = "theme_mode"
        const val KEY_TEXT_SIZE = "text_size_mode"
    }
}
