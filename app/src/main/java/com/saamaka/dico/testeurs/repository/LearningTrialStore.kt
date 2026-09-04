package com.saamaka.dico.testeurs

import android.content.Context

class LearningTrialStore(context: Context) {
    private val preferences = context.getSharedPreferences(
        "learning_trials",
        Context.MODE_PRIVATE
    )

    fun remainingTrials(accessLevel: AccessLevel): Int {
        val maximum = maximumTrials(accessLevel) ?: return 0
        return (maximum - usedTrials(accessLevel, maximum)).coerceAtLeast(0)
    }

    fun useTrial(accessLevel: AccessLevel): Boolean {
        val maximum = maximumTrials(accessLevel) ?: return true
        val used = usedTrials(accessLevel, maximum)
        if (used >= maximum) return false

        preferences.edit()
            .putInt(counterKey(accessLevel), used + 1)
            .apply()
        return true
    }

    private fun usedTrials(accessLevel: AccessLevel, maximum: Int): Int {
        val key = counterKey(accessLevel)
        val defaultValue = if (
            accessLevel == AccessLevel.FREE_ACCOUNT &&
            !preferences.contains(key)
        ) {
            preferences.getInt(LEGACY_USED_TRIALS, 0)
        } else {
            0
        }
        return preferences.getInt(key, defaultValue).coerceIn(0, maximum)
    }

    private fun maximumTrials(accessLevel: AccessLevel): Int? = when (accessLevel) {
        AccessLevel.GUEST -> 5
        AccessLevel.FREE_ACCOUNT -> 10
        AccessLevel.PREMIUM,
        AccessLevel.TESTER -> null
    }

    private fun counterKey(accessLevel: AccessLevel): String = when (accessLevel) {
        AccessLevel.GUEST -> KEY_GUEST_USED_TRIALS
        AccessLevel.FREE_ACCOUNT -> KEY_FREE_ACCOUNT_USED_TRIALS
        AccessLevel.PREMIUM,
        AccessLevel.TESTER -> error("Unlimited access does not use a learning trial counter")
    }

    private companion object {
        const val KEY_GUEST_USED_TRIALS = "guest_used_trials"
        const val KEY_FREE_ACCOUNT_USED_TRIALS = "free_account_used_trials"
        const val LEGACY_USED_TRIALS = "used_trials"
    }
}
