package com.saamaka.dico.testeurs

internal data class HomeSearchPresentation(
    val normalizedText: String,
    val wordCount: Int,
    val showPhraseCta: Boolean,
    val showNoResult: Boolean
)

internal fun homeSearchPresentation(
    text: String,
    localResultCount: Int,
    hasExactCompleteMatch: Boolean,
    hasStructuredPhraseResult: Boolean = false
): HomeSearchPresentation {
    val normalized = cleanPhraseInput(text)
    val wordCount = normalizedInputWordCount(normalized)
    val hasAnyLocalResult = localResultCount > 0 || hasExactCompleteMatch || hasStructuredPhraseResult
    return HomeSearchPresentation(
        normalizedText = normalized,
        wordCount = wordCount,
        showPhraseCta = wordCount >= 3 && !hasExactCompleteMatch,
        showNoResult = wordCount <= 1 && !hasAnyLocalResult
    )
}
