package com.saamaka.dico.testeurs

import java.text.Normalizer
import java.util.Locale

internal data class AttestedPhraseRule(
    val french: String,
    val saamaka: String
)

private val attestedPhraseRules = listOf(
    AttestedPhraseRule(
        french = "bonjour",
        saamaka = "odi"
    ),
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
    findAttestedGreetingWithPerson(text, frenchToSaamaka)?.let { return it }
    val normalizedText = normalizeAttestedPhraseKey(text)

    return attestedPhraseRules.firstNotNullOfOrNull { rule ->
        val source = if (frenchToSaamaka) rule.french else rule.saamaka
        val translation = if (frenchToSaamaka) rule.saamaka else rule.french
        translation.takeIf {
            normalizeAttestedPhraseKey(source) == normalizedText
        }
    }
}

private fun findAttestedGreetingWithPerson(
    text: String,
    frenchToSaamaka: Boolean
): String? {
    val originalWords = cleanAttestedInputPreservingNames(text)
        .split(Regex("\\s+"))
        .filter { it.isNotBlank() }
    val normalizedWords = originalWords.map(::normalizeAttestedPhraseKey)

    if (frenchToSaamaka) {
        val prefix = listOf("passe", "le", "bonjour", "a")
        if (normalizedWords.take(prefix.size) != prefix) return null
        var personEnd = originalWords.size
        val hasFromMe = normalizedWords.takeLast(3) == listOf("de", "ma", "part")
        if (hasFromMe) personEnd -= 3
        val personWords = originalWords.subList(prefix.size, personEnd)
        if (personWords.size != 1) return null
        val person = personWords.single()
        return "Da $person odi" + if (hasFromMe) " da mi" else ""
    }

    if (normalizedWords.size < 3 || normalizedWords.first() != "da") return null
    val hasFromMe = normalizedWords.takeLast(2) == listOf("da", "mi")
    val effectiveEnd = originalWords.size - if (hasFromMe) 2 else 0
    if (effectiveEnd < 3 || normalizedWords[effectiveEnd - 1] != "odi") return null
    val personWords = originalWords.subList(1, effectiveEnd - 1)
    if (personWords.size != 1) return null
    val person = personWords.single()
    return "Passe le bonjour à $person" + if (hasFromMe) " de ma part" else ""
}

private fun cleanAttestedInputPreservingNames(value: String): String = value
    .replace(Regex("[.,;:!?…\"“”()\\[\\]{}]"), " ")
    .replace(Regex("\\s+"), " ")
    .trim()

internal fun normalizeAttestedPhraseKey(value: String): String =
    Normalizer.normalize(cleanPhraseInput(value), Normalizer.Form.NFD)
        .replace("\\p{Mn}+".toRegex(), "")
        .lowercase(Locale.ROOT)
