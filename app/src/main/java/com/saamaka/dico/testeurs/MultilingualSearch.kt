package com.saamaka.dico.testeurs

import com.saamaka.dico.testeurs.model.DictionaryEntry
import java.text.Normalizer
import java.util.Locale

internal fun searchTextForLanguage(entry: DictionaryEntry, languageCode: String): String =
    when (languageCode) {
        "srm" -> entry.saamaka
        "en" -> entry.english
        "nl" -> entry.dutch
        else -> entry.french
    }

internal fun normalizeMultilingualSearch(value: String): String =
    Normalizer.normalize(value.trim(), Normalizer.Form.NFD)
        .replace("\\p{Mn}+".toRegex(), "")
        .lowercase(Locale.ROOT)
        .replace(Regex("\\s+"), " ")

internal fun multilingualSearchRank(text: String, query: String): Int {
    val normalizedText = normalizeMultilingualSearch(text)
    val normalizedQuery = normalizeMultilingualSearch(query)
    return when {
        normalizedText == normalizedQuery -> 0
        normalizedText.startsWith(normalizedQuery) -> 1
        normalizedText.split(' ').contains(normalizedQuery) -> 2
        normalizedText.contains(normalizedQuery) -> 3
        else -> 4
    }
}

internal fun filterAndRankByLanguage(
    entries: List<DictionaryEntry>,
    query: String,
    languageCode: String,
    limit: Int = 100
): List<DictionaryEntry> {
    val normalizedQuery = normalizeMultilingualSearch(query)
    if (normalizedQuery.isBlank()) return emptyList()
    return entries.asSequence()
        .filter { searchTextForLanguage(it, languageCode).isNotBlank() }
        .filter {
            normalizeMultilingualSearch(searchTextForLanguage(it, languageCode))
                .contains(normalizedQuery)
        }
        .distinctBy { it.id }
        .sortedWith(
            compareBy<DictionaryEntry> {
                multilingualSearchRank(searchTextForLanguage(it, languageCode), normalizedQuery)
            }.thenBy {
                normalizeMultilingualSearch(searchTextForLanguage(it, languageCode))
            }
        )
        .take(limit)
        .toList()
}
