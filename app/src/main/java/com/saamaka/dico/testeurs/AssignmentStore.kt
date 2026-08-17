package com.saamaka.dico.testeurs

import android.content.Context
import com.saamaka.dico.testeurs.model.DictionaryEntry

class AssignmentStore(context: Context) {

    private val preferences = context.getSharedPreferences(
        "saamaka_assignment",
        Context.MODE_PRIVATE
    )

    fun isConfigured(): Boolean =
        preferences.getBoolean(KEY_CONFIGURED, false)

    fun testerNumber(): Int =
        preferences.getInt(KEY_TESTER_NUMBER, 1).coerceIn(1, TOTAL_TESTERS)

    fun totalTesters(): Int = TOTAL_TESTERS

    fun selectTester(testerNumber: Int) {
        preferences.edit()
            .putInt(
                KEY_TESTER_NUMBER,
                testerNumber.coerceIn(1, TOTAL_TESTERS)
            )
            .putBoolean(KEY_CONFIGURED, true)
            .apply()
    }

    fun assignedCategory(): String =
        preferences.getString(KEY_CATEGORY, "").orEmpty()

    fun selectCategory(category: String) {
        preferences.edit()
            .putString(KEY_CATEGORY, category.trim())
            .apply()
    }

    fun resetProfile() {
        preferences.edit().clear().apply()
    }

    companion object {
        const val TOTAL_TESTERS = 12

        private const val KEY_TESTER_NUMBER = "tester_number"
        private const val KEY_CONFIGURED = "configured"
        private const val KEY_CATEGORY = "category"
    }
}