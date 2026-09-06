package com.saamaka.dico.testeurs

import com.saamaka.dico.testeurs.model.PhraseTranslationResult
import com.saamaka.dico.testeurs.model.PhraseTranslationKind
import com.saamaka.dico.testeurs.model.RecognizedPhraseSegment
import com.saamaka.dico.testeurs.model.TranslationReliability

internal fun cleanPhraseInput(value: String): String = value
    .replace('’', '\'')
    .replace(Regex("[.,;:!?…\"“”()\\[\\]{}]"), " ")
    .replace(Regex("\\s+"), " ")
    .trim()

internal fun assembleAttestedPhrase(
    text: String,
    maxExpressionWords: Int = 8,
    highReliabilityMatch: (RecognizedPhraseSegment) -> Boolean = { true },
    lookupExact: (String) -> RecognizedPhraseSegment?
): PhraseTranslationResult? {
    val words = cleanPhraseInput(text)
        .split(Regex("\\s+"))
        .filter { it.isNotBlank() }

    if (words.isEmpty()) return null

    val recognized = mutableListOf<RecognizedPhraseSegment>()
    val missing = mutableListOf<String>()
    val proposalParts = mutableListOf<String>()
    var index = 0

    while (index < words.size) {
        var match: RecognizedPhraseSegment? = null
        var consumed = 0
        val maxChunk = minOf(maxExpressionWords, words.size - index)

        for (size in maxChunk downTo 1) {
            val source = words.subList(index, index + size).joinToString(" ")
            val resolved = lookupExact(source)
            if (resolved != null && resolved.translation.isNotBlank()) {
                match = resolved.copy(source = source)
                consumed = size
                break
            }
        }

        if (match != null) {
            recognized += match
            proposalParts += match.translation
            index += consumed
        } else {
            missing += words[index]
            index++
        }
    }

    val complete = missing.isEmpty() && recognized.isNotEmpty()
    return PhraseTranslationResult(
        translation = proposalParts.joinToString(" ")
            .ifBlank { "Aucune proposition locale disponible" },
        recognizedSegments = recognized,
        untranslatedSegments = missing,
        isComplete = complete,
        reliability = when {
            !complete -> TranslationReliability.LOW
            recognized.size == 1 && highReliabilityMatch(recognized.single()) ->
                TranslationReliability.HIGH
            else -> TranslationReliability.MEDIUM
        }
    )
}

/** Marks a DB-backed lexical assembly without presenting it as proven grammar. */
internal fun PhraseTranslationResult.asFrenchLexicalFallback(): PhraseTranslationResult? {
    if (recognizedSegments.isEmpty()) return null
    return copy(
        reliability = if (isComplete) TranslationReliability.MEDIUM else TranslationReliability.LOW,
        kind = if (isComplete) PhraseTranslationKind.WORD_BY_WORD else PhraseTranslationKind.PARTIAL
    )
}

internal fun assembleWordByWordPhrase(
    text: String,
    lookupToken: (String) -> RecognizedPhraseSegment?
): PhraseTranslationResult? {
    val tokens = cleanPhraseInput(text)
        .split(Regex("\\s+"))
        .filter(String::isNotBlank)
    if (tokens.isEmpty()) return null

    val recognized = mutableListOf<RecognizedPhraseSegment>()
    val missing = mutableListOf<String>()
    val output = tokens.map { token ->
        lookupToken(token)?.takeIf { it.translation.isNotBlank() }?.also(recognized::add)
            ?.translation
            ?: "[$token ?]".also { missing += token }
    }
    return PhraseTranslationResult(
        translation = output.joinToString(" • "),
        recognizedSegments = recognized,
        untranslatedSegments = missing,
        isComplete = missing.isEmpty(),
        reliability = if (missing.isEmpty()) TranslationReliability.MEDIUM else TranslationReliability.LOW,
        kind = if (missing.isEmpty()) PhraseTranslationKind.WORD_BY_WORD else PhraseTranslationKind.PARTIAL
    )
}

internal data class FrenchGrammaticalUnit(
    val source: String,
    val lookup: String
)

internal fun frenchGrammaticalUnits(text: String): List<FrenchGrammaticalUnit> =
    cleanPhraseInput(text)
        .split(Regex("\\s+"))
        .filter(String::isNotBlank)
        .flatMap { token ->
            val normalized = token.lowercase().replace('’', '\'')
            val contraction = listOf(
                "j'" to "je", "n'" to "ne", "d'" to "de", "qu'" to "que",
                "m'" to "me", "t'" to "te", "s'" to "se", "c'" to "ce"
            ).firstOrNull { normalized.startsWith(it.first) && normalized.length > it.first.length }
            if (contraction == null) {
                listOf(FrenchGrammaticalUnit(token, FrenchVerbInflections.lemma(token) ?: token))
            } else {
                val remainder = normalized.removePrefix(contraction.first)
                listOf(
                    FrenchGrammaticalUnit(contraction.first, contraction.second),
                    FrenchGrammaticalUnit(remainder, FrenchVerbInflections.lemma(remainder) ?: remainder)
                )
            }
        }

internal fun assemblePartialGrammaticalPhrase(
    text: String,
    lookupUnit: (FrenchGrammaticalUnit) -> RecognizedPhraseSegment?
): PhraseTranslationResult? {
    val units = frenchGrammaticalUnits(text)
    if (units.isEmpty()) return null
    val recognized = mutableListOf<RecognizedPhraseSegment>()
    val missing = mutableListOf<String>()
    val output = units.map { unit ->
        lookupUnit(unit)?.takeIf { it.translation.isNotBlank() }?.also(recognized::add)
            ?.translation
            ?: "[${unit.lookup} ?]".also { missing += unit.lookup }
    }
    if (recognized.isEmpty()) return null
    return PhraseTranslationResult(
        translation = output.joinToString(" "),
        recognizedSegments = recognized,
        untranslatedSegments = missing,
        isComplete = false,
        reliability = TranslationReliability.LOW,
        kind = PhraseTranslationKind.PARTIAL
    )
}
