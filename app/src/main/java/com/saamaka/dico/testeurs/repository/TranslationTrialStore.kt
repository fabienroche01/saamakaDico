package com.saamaka.dico.testeurs

import android.content.Context

class TranslationTrialStore(context: Context) {
    private val prefs = context.getSharedPreferences("translation_trials", Context.MODE_PRIVATE)

    init {
        migrateLegacyCounterIfNeeded()
    }

    fun usedTrials(accessLevel: AccessLevel): Int = maximumTrials(accessLevel)?.let { maximum ->
        prefs.getInt(counterKey(accessLevel), 0).coerceIn(0, maximum)
    } ?: 0

    fun remainingTrials(accessLevel: AccessLevel): Int = maximumTrials(accessLevel)?.let { maximum ->
        (maximum - usedTrials(accessLevel)).coerceAtLeast(0)
    } ?: Int.MAX_VALUE

    fun canUseTrial(accessLevel: AccessLevel): Boolean =
        maximumTrials(accessLevel) == null || remainingTrials(accessLevel) > 0

    fun useTrial(accessLevel: AccessLevel): Boolean {
        val maximum = maximumTrials(accessLevel) ?: return true
        val used = usedTrials(accessLevel)
        if (used >= maximum) return false
        prefs.edit().putInt(counterKey(accessLevel), used + 1).apply()
        return true
    }

    fun resetTrials() {
        prefs.edit()
            .putInt(GUEST_USED_TRIALS, 0)
            .putInt(FREE_ACCOUNT_USED_TRIALS, 0)
            .apply()
    }

    private fun migrateLegacyCounterIfNeeded() {
        if (!prefs.contains(LEGACY_USED_TRIALS)) return
        val legacyUsed = prefs.getInt(LEGACY_USED_TRIALS, 0).coerceAtLeast(0)
        val editor = prefs.edit()
        if (!prefs.contains(GUEST_USED_TRIALS)) {
            editor.putInt(GUEST_USED_TRIALS, legacyUsed.coerceAtMost(requireNotNull(translationTrialLimit(AccessLevel.GUEST))))
        }
        if (!prefs.contains(FREE_ACCOUNT_USED_TRIALS)) {
            editor.putInt(
                FREE_ACCOUNT_USED_TRIALS,
                legacyUsed.coerceAtMost(requireNotNull(translationTrialLimit(AccessLevel.FREE_ACCOUNT)))
            )
        }
        editor.apply()
    }

    private fun maximumTrials(accessLevel: AccessLevel): Int? = translationTrialLimit(accessLevel)

    private fun counterKey(accessLevel: AccessLevel): String = when (accessLevel) {
        AccessLevel.GUEST -> GUEST_USED_TRIALS
        AccessLevel.FREE_ACCOUNT -> FREE_ACCOUNT_USED_TRIALS
        AccessLevel.PREMIUM, AccessLevel.TESTER -> error("Unlimited access has no translation trial counter")
    }

    private companion object {
        const val LEGACY_USED_TRIALS = "used_trials"
        const val GUEST_USED_TRIALS = "guest_used_trials"
        const val FREE_ACCOUNT_USED_TRIALS = "free_account_used_trials"
    }
}
