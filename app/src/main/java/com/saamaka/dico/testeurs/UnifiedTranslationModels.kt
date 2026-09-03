package com.saamaka.dico.testeurs

import com.saamaka.dico.testeurs.model.DictionaryEntry
import com.saamaka.dico.testeurs.model.PhraseTranslationResult

enum class LocalMatchProvenance(val label: String) {
    DICTIONARY("Correspondance exacte du dictionnaire"),
    LOCAL_CORRECTION("Correction locale"),
    ATTESTED_EXPRESSION("Traduction construite avec une règle validée")
}

internal fun relatedExpressionLabel(input: String, candidate: String): String {
    val inputWords = cleanPhraseInput(input).split(Regex("\\s+")).filter { it.isNotBlank() }
    val candidateWords = cleanPhraseInput(candidate).split(Regex("\\s+")).filter { it.isNotBlank() }
    val normalizedInput = inputWords.map(::normalizeAttestedPhraseKey)
    val normalizedCandidate = candidateWords.map(::normalizeAttestedPhraseKey)
    if (normalizedInput.isEmpty() || normalizedCandidate.isEmpty()) return "Expression proche"
    return when {
        normalizedCandidate == normalizedInput -> "Correspondance exacte du dictionnaire"
        normalizedCandidate.size > normalizedInput.size &&
            normalizedCandidate.windowed(normalizedInput.size).any { it == normalizedInput } ->
            "Expression proche — contient des mots supplémentaires"
        normalizedCandidate.size < normalizedInput.size &&
            normalizedInput.windowed(normalizedCandidate.size).any { it == normalizedCandidate } ->
            "Proposition incomplète"
        else -> "Expression proche"
    }
}

data class LocalExactMatch(
    val source: String,
    val translation: String,
    val provenance: LocalMatchProvenance,
    val entry: DictionaryEntry? = null
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
