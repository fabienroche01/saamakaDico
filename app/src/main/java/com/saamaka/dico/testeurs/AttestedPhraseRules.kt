package com.saamaka.dico.testeurs

import java.text.Normalizer
import java.util.Locale

internal data class AttestedPhraseRule(
    val french: String,
    val saamaka: String
)

private val attestedPhraseRules = listOf(
    // Correspondance validée humainement ; ce n'est pas une règle générale
    // de conjugaison ni une déduction construite depuis « saluer ».
    AttestedPhraseRule(
        french = "passe le bonjour",
        saamaka = "da odi"
    )
)

internal fun findAttestedPhraseRule(
    text: String,
    frenchToSaamaka: Boolean
): String? {
    val normalizedText = normalizeAttestedPhraseKey(text)

    return attestedPhraseRules.firstNotNullOfOrNull { rule ->
        val source = if (frenchToSaamaka) rule.french else rule.saamaka
        val translation = if (frenchToSaamaka) rule.saamaka else rule.french
        translation.takeIf {
            normalizeAttestedPhraseKey(source) == normalizedText
        }
    }
}

internal fun normalizeAttestedPhraseKey(value: String): String =
    Normalizer.normalize(cleanPhraseInput(value), Normalizer.Form.NFD)
        .replace("\\p{Mn}+".toRegex(), "")
        .lowercase(Locale.ROOT)
