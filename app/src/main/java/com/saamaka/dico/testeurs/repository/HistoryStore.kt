package com.saamaka.dico.testeurs.repository

import android.content.Context

class HistoryStore(context: Context) {
    private val preferences = context.getSharedPreferences(
        "saamaka_history",
        Context.MODE_PRIVATE
    )

    fun add(id: Int) {
        val updated = ids()
            .filterNot { it == id }
            .toMutableList()
            .apply { add(0, id) }
            .take(MAX_HISTORY)

        preferences.edit()
            .putString(KEY_IDS, updated.joinToString(","))
            .apply()
    }

    fun ids(): List<Int> =
        preferences.getString(KEY_IDS, "")
            .orEmpty()
            .split(",")
            .mapNotNull { it.toIntOrNull() }

    fun clear() {
        preferences.edit().remove(KEY_IDS).apply()
    }

    private companion object {
        const val KEY_IDS = "history_ids"
        const val MAX_HISTORY = 20
    }
}