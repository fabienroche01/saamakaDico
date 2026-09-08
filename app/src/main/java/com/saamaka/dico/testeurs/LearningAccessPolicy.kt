package com.saamaka.dico.testeurs

internal fun learningActivityForSection(section: String): LearningActivity? = when (section) {
    "QUIZ" -> LearningActivity.QUIZ
    "WORDS", "FAVORITES", "WORD_OF_DAY", "AUDIO" -> LearningActivity.REVIEW
    "PHRASES" -> LearningActivity.PHRASES
    "GAMES" -> LearningActivity.GAMES
    else -> null
}

internal fun hasLearningSectionAccess(
    accessLevel: AccessLevel,
    section: String,
    remainingTrials: Map<LearningActivity, Int>
): Boolean {
    if (accessLevel == AccessLevel.PREMIUM || accessLevel == AccessLevel.TESTER) return true
    val activity = learningActivityForSection(section) ?: return true
    return (remainingTrials[activity] ?: 0) > 0
}
