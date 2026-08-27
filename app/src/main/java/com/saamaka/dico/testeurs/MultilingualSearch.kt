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

internal fun matchingLanguageForEntry(
    entry: DictionaryEntry,
    query: String,
    filteredLanguage: AppLanguage?,
    preferredLanguage: AppLanguage = AppLanguage.FRENCH
): AppLanguage {
    if (filteredLanguage != null) return filteredLanguage
    val normalizedQuery = normalizeMultilingualSearch(query)
    val candidates = AppLanguage.entries.filter {
        normalizeMultilingualSearch(searchTextForLanguage(entry, it.code)).contains(normalizedQuery)
    }
    return candidates.minWithOrNull(
        compareBy<AppLanguage> {
            multilingualSearchRank(searchTextForLanguage(entry, it.code), normalizedQuery)
        }.thenBy { if (it == preferredLanguage) 0 else 1 }
            .thenBy { it.ordinal }
    ) ?: preferredLanguage
}

internal fun preferredTranslationLanguage(
    entry: DictionaryEntry,
    uiLanguage: UiLanguage
): AppLanguage {
    val preferred = when (uiLanguage) {
        UiLanguage.FRENCH -> AppLanguage.FRENCH
        UiLanguage.ENGLISH -> AppLanguage.ENGLISH
        UiLanguage.DUTCH -> AppLanguage.DUTCH
        UiLanguage.SAAMAKA -> AppLanguage.FRENCH
    }
    val available = listOf(AppLanguage.FRENCH, AppLanguage.ENGLISH, AppLanguage.DUTCH)
        .filter { searchTextForLanguage(entry, it.code).isNotBlank() }
    return preferred.takeIf { it in available }
        ?: AppLanguage.FRENCH.takeIf { it in available }
        ?: available.firstOrNull()
        ?: AppLanguage.SAAMAKA
}
