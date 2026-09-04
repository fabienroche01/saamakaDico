package com.saamaka.dico.testeurs

import android.content.Context

class LearningTrialStore(context: Context) {
    private val preferences = context.getSharedPreferences(
        "learning_trials",
        Context.MODE_PRIVATE
    )

    private val maxTrials = 10

    fun usedTrials(): Int = preferences
        .getInt(KEY_USED_TRIALS, 0)
        .coerceIn(0, maxTrials)

    fun remainingTrials(): Int = (maxTrials - usedTrials()).coerceAtLeast(0)

    fun useTrial(): Boolean {
        val used = usedTrials()
        if (used >= maxTrials) return false

        preferences.edit()
            .putInt(KEY_USED_TRIALS, used + 1)
            .apply()
        return true
    }

    private companion object {
        const val KEY_USED_TRIALS = "used_trials"
    }
}
