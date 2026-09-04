package com.saamaka.dico.testeurs

import android.content.Context

enum class LearningActivity {
    QUIZ,
    REVIEW,
    PHRASES,
    GAMES
}

class LearningTrialStore(context: Context) {
    private val preferences = context.getSharedPreferences(
        "learning_trials",
        Context.MODE_PRIVATE
    )

    fun remainingTrials(accessLevel: AccessLevel, activity: LearningActivity): Int {
        val maximum = maximumTrials(accessLevel) ?: return 0
        return (maximum - usedTrials(accessLevel, activity, maximum)).coerceAtLeast(0)
    }

    fun useTrial(accessLevel: AccessLevel, activity: LearningActivity): Boolean {
        val maximum = maximumTrials(accessLevel) ?: return true
        val used = usedTrials(accessLevel, activity, maximum)
        if (used >= maximum) return false

        preferences.edit()
            .putInt(counterKey(accessLevel, activity), used + 1)
            .apply()
        return true
    }

    private fun usedTrials(
        accessLevel: AccessLevel,
        activity: LearningActivity,
        maximum: Int
    ): Int {
        return preferences
            .getInt(counterKey(accessLevel, activity), 0)
            .coerceIn(0, maximum)
    }

    private fun maximumTrials(accessLevel: AccessLevel): Int? = when (accessLevel) {
        AccessLevel.GUEST -> 5
        AccessLevel.FREE_ACCOUNT -> 10
        AccessLevel.PREMIUM,
        AccessLevel.TESTER -> null
    }

    private fun counterKey(accessLevel: AccessLevel, activity: LearningActivity): String = when (accessLevel) {
        AccessLevel.GUEST -> when (activity) {
            LearningActivity.QUIZ -> KEY_GUEST_QUIZ_USED
            LearningActivity.REVIEW -> KEY_GUEST_REVIEW_USED
            LearningActivity.PHRASES -> KEY_GUEST_PHRASES_USED
            LearningActivity.GAMES -> KEY_GUEST_GAMES_USED
        }
        AccessLevel.FREE_ACCOUNT -> when (activity) {
            LearningActivity.QUIZ -> KEY_FREE_QUIZ_USED
            LearningActivity.REVIEW -> KEY_FREE_REVIEW_USED
            LearningActivity.PHRASES -> KEY_FREE_PHRASES_USED
            LearningActivity.GAMES -> KEY_FREE_GAMES_USED
        }
        AccessLevel.PREMIUM,
        AccessLevel.TESTER -> error("Unlimited access does not use a learning trial counter")
    }

    private companion object {
        const val KEY_GUEST_QUIZ_USED = "guest_quiz_used"
        const val KEY_GUEST_REVIEW_USED = "guest_review_used"
        const val KEY_GUEST_PHRASES_USED = "guest_phrases_used"
        const val KEY_GUEST_GAMES_USED = "guest_games_used"
        const val KEY_FREE_QUIZ_USED = "free_quiz_used"
        const val KEY_FREE_REVIEW_USED = "free_review_used"
        const val KEY_FREE_PHRASES_USED = "free_phrases_used"
        const val KEY_FREE_GAMES_USED = "free_games_used"
    }
}
