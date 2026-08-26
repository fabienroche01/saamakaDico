package com.saamaka.dico.testeurs

import com.saamaka.dico.testeurs.model.DictionaryEntry
import com.saamaka.dico.testeurs.model.PhraseTranslationResult

enum class LocalMatchProvenance(val label: String) {
    DICTIONARY("Correspondance du dictionnaire"),
    LOCAL_CORRECTION("Correction locale"),
    ATTESTED_EXPRESSION("Expression attestée")
}

data class LocalExactMatch(
    val source: String,
    val translation: String,
    val provenance: LocalMatchProvenance
)

data class UnifiedLocalSearchResult(
    val exactMatch: LocalExactMatch?,
    val usefulEntries: List<DictionaryEntry>
)

internal fun normalizedInputWordCount(text: String): Int =
    cleanPhraseInput(text)
        .split(Regex("\\s+"))
        .count { it.isNotBlank() }

internal fun unifiedSearchButtonLabel(text: String): String =
    if (normalizedInputWordCount(text) <= 1) {
        "Rechercher"
    } else {
        "Rechercher / Traduire"
    }

internal fun shouldOfferPremiumTranslation(
    text: String,
    result: UnifiedLocalSearchResult?
): Boolean = normalizedInputWordCount(text) > 1 &&
    result != null &&
    result.exactMatch == null

internal fun shouldConsumeTrialAfterPremiumResult(
    accessLevel: AccessLevel,
    remainingTrials: Int,
    result: PhraseTranslationResult?,
    alreadyConsumedForRequest: Boolean
): Boolean = accessLevel == AccessLevel.FREE_ACCOUNT &&
    remainingTrials > 0 &&
    result != null &&
    !alreadyConsumedForRequest
