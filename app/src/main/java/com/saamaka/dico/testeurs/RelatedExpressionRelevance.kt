package com.saamaka.dico.testeurs

import com.saamaka.dico.testeurs.model.DictionaryEntry

private val irrelevantSearchWords = setOf(
    "a", "au", "aux", "de", "des", "du", "en", "et", "je", "la", "le", "les",
    "ma", "mes", "mon", "ne", "nous", "pas", "sa", "ses", "son", "ta", "tes", "ton",
    "tu", "un", "une", "vous"
)

internal fun relevantRelatedExpressions(
    input: String,
    candidates: List<DictionaryEntry>,
    languageCode: String?,
    limit: Int = 20
): List<DictionaryEntry> {
    val rawInputWords = normalizedWholeWords(input)
    val allInputWords = if (languageCode == null || languageCode == AppLanguage.FRENCH.code) {
        (rawInputWords + rawInputWords.mapNotNull(FrenchVerbInflections::lemma)).distinct()
    } else {
        rawInputWords
    }
    val meaningfulInputWords = allInputWords.filterNot { it in irrelevantSearchWords }.toSet()
        .ifEmpty { allInputWords.toSet() }
    if (meaningfulInputWords.isEmpty()) return emptyList()
    val attestedInputCategories = candidates.asSequence()
        .filter { entry ->
            searchableValues(entry, languageCode).any { value ->
                normalizeAttestedPhraseKey(value) in meaningfulInputWords
            }
        }
        .map { normalizeAttestedPhraseKey(it.categorie) }
        .filter(String::isNotBlank)
        .toSet()

    return candidates.asSequence()
        .mapNotNull { entry ->
            val candidateWords = searchableValues(entry, languageCode)
                .flatMap(::normalizedWholeWords)
                .toSet()
            val exactMatches = meaningfulInputWords.intersect(candidateWords)
            val synonymMatches = if (languageCode == null || languageCode == AppLanguage.FRENCH.code) {
                meaningfulInputWords.count { word ->
                    attestedFrenchSynonymsOf(word).any(candidateWords::contains)
                }
            } else {
                0
            }
            val sameAttestedCategory = normalizeAttestedPhraseKey(entry.categorie)
                .takeIf { it.isNotBlank() }
                ?.let(attestedInputCategories::contains) == true
            val score = exactMatches.size * EXACT_WORD_SCORE +
                synonymMatches * ATTESTED_SYNONYM_SCORE +
                if (sameAttestedCategory) ATTESTED_CATEGORY_SCORE else 0
            (entry to score).takeIf { score >= MIN_RELEVANCE_SCORE }
        }
        .sortedWith(compareByDescending<Pair<DictionaryEntry, Int>> { it.second }.thenBy { it.first.id })
        .map { it.first }
        .distinctBy(DictionaryEntry::id)
        .take(limit)
        .toList()
}

internal fun relatedCandidateSearchTerms(input: String, languageCode: String?): Set<String> {
    val words = normalizedWholeWords(input).filterNot { it in irrelevantSearchWords }
    val frenchLookup = languageCode == null || languageCode == AppLanguage.FRENCH.code
    val lemmas = if (frenchLookup) {
        words.mapNotNull(FrenchVerbInflections::lemma)
    } else {
        emptyList()
    }
    val synonyms = if (frenchLookup) {
        (words + lemmas).flatMap(::attestedFrenchSynonymsOf)
    } else {
        emptyList()
    }
    return (words + lemmas + synonyms).toSet()
}

private fun searchableValues(entry: DictionaryEntry, languageCode: String?): List<String> = when (languageCode) {
    AppLanguage.FRENCH.code -> listOf(entry.french)
    AppLanguage.ENGLISH.code -> listOf(entry.english)
    AppLanguage.DUTCH.code -> listOf(entry.dutch)
    AppLanguage.SAAMAKA.code -> listOf(entry.saamaka)
    else -> listOf(entry.saamaka, entry.french, entry.english, entry.dutch)
}

private fun normalizedWholeWords(text: String): List<String> = cleanPhraseInput(text)
    .split(Regex("\\s+"))
    .map(::normalizeAttestedPhraseKey)
    .filter(String::isNotBlank)

private const val EXACT_WORD_SCORE = 100
private const val ATTESTED_SYNONYM_SCORE = 80
private const val ATTESTED_CATEGORY_SCORE = 20
private const val MIN_RELEVANCE_SCORE = 80
