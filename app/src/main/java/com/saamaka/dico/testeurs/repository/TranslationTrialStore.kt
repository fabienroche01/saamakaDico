package com.saamaka.dico.testeurs

import android.content.Context

class TranslationTrialStore(
    context: Context
) {
    private val prefs = context.getSharedPreferences(
        "translation_trials",
        Context.MODE_PRIVATE
    )

    private val maxTrials = 3

    fun usedTrials(): Int {
        return prefs.getInt("used_trials", 0)
            .coerceIn(0, maxTrials)
    }

    fun remainingTrials(): Int {
        return (maxTrials - usedTrials())
            .coerceAtLeast(0)
    }

    fun canUseTrial(): Boolean {
        return remainingTrials() > 0
    }

    fun useTrial(): Boolean {
        val used = usedTrials()

        if (used >= maxTrials) {
            return false
        }

        prefs.edit()
            .putInt("used_trials", used + 1)
            .apply()

        return true
    }

    fun resetTrials() {
        prefs.edit()
            .putInt("used_trials", 0)
            .apply()
    }
}