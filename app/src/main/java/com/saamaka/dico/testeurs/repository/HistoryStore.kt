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

    fun remove(id: Int): Int? {
        val updated = ids().toMutableList()
        val previousIndex = updated.indexOf(id)

        if (previousIndex < 0) {
            return null
        }

        updated.removeAt(previousIndex)
        save(updated)
        return previousIndex
    }

    fun restore(id: Int, index: Int) {
        val updated = ids()
            .filterNot { it == id }
            .toMutableList()

        updated.add(index.coerceIn(0, updated.size), id)
        save(updated.take(MAX_HISTORY))
    }

    private fun save(ids: List<Int>) {
        preferences.edit()
            .putString(KEY_IDS, ids.joinToString(","))
            .apply()
    }

    private companion object {
        const val KEY_IDS = "history_ids"
        const val MAX_HISTORY = 20
    }
}
