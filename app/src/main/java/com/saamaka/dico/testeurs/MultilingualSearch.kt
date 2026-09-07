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
    Normalizer.normalize(
        value.trim()
            .replace(Regex("[‘’ʼ`]"), "'")
            .replace(Regex("[‐‑‒–—−]"), "-"),
        Normalizer.Form.NFD
    )
        .replace("\\p{Mn}+".toRegex(), "")
        .lowercase(Locale.ROOT)
        .replace(Regex("[.!?,;:]+$"), "")
        .trimEnd()
        .replace(Regex("\\s+"), " ")

internal fun accentInsensitiveGlob(value: String): String = buildString {
    normalizeMultilingualSearch(value).forEach { character ->
        append(
            when (character) {
                'a' -> "[aAàÀáÁâÂãÃäÄåÅāĀăĂąĄ]"
                'c' -> "[cCçÇćĆčČ]"
                'e' -> "[eEèÈéÉêÊëËēĒĕĔėĖęĘěĚ]"
                'i' -> "[iIìÌíÍîÎïÏĩĨīĪĭĬįĮı]"
                'n' -> "[nNñÑńŃňŇ]"
                'o' -> "[oOòÒóÓôÔõÕöÖøØōŌŏŎőŐ]"
                's' -> "[sSśŚšŠşŞ]"
                'u' -> "[uUùÙúÚûÛüÜũŨūŪŭŬůŮűŰųŲ]"
                'y' -> "[yYýÝÿŸ]"
                'z' -> "[zZźŹžŽżŻ]"
                '\'' -> "['‘’ʼ`]"
                '-' -> "[-‐‑‒–—−]"
                '*', '?', '[', ']' -> "[$character]"
                in 'a'..'z' -> "[$character${character.uppercaseChar()}]"
                else -> character.toString()
            }
        )
    }
}

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

internal fun multilingualSearchRankNormalized(text: String, normalizedQuery: String): Int {
    val normalizedText = normalizeMultilingualSearch(text)
    return when {
        normalizedText == normalizedQuery -> 0
        normalizedText.startsWith(normalizedQuery) -> 1
        normalizedText.contains(normalizedQuery) -> 2
        else -> 3
    }
}

private data class SearchRelevance(
    val rank: Int,
    val missingWordCount: Int = 0
)

private data class FuzzySearchRelevance(
    val distance: Int,
    val lexicalHeadPenalty: Int,
    val containerPenalty: Int,
    val lengthDelta: Int
)

private fun searchRelevance(text: String, normalizedQuery: String): SearchRelevance? {
    val normalizedText = normalizeMultilingualSearch(text)
    if (normalizedText.isBlank()) return null
    val queryWords = normalizedQuery.split(' ').filter(String::isNotBlank)
    if (queryWords.size <= 1) {
        val rank = multilingualSearchRank(normalizedText, normalizedQuery)
        return SearchRelevance(rank).takeIf { rank < 4 }
    }

    if (normalizedText == normalizedQuery) return SearchRelevance(0)
    if (normalizedText.startsWith(normalizedQuery)) return SearchRelevance(1)

    val textWords = normalizedText.split(' ').filter(String::isNotBlank)
    var nextIndex = 0
    val allWordsInOrder = queryWords.all { queryWord ->
        val found = textWords.indexOfFirstFrom(nextIndex) { it == queryWord }
        if (found >= 0) nextIndex = found + 1
        found >= 0
    }
    if (allWordsInOrder) return SearchRelevance(2)
    if (queryWords.all { it in textWords }) return SearchRelevance(3)

    val matchedWords = queryWords.count { queryWord ->
        textWords.any { textWord -> textWord.contains(queryWord) || queryWord.contains(textWord) }
    }
    return SearchRelevance(4, queryWords.size - matchedWords).takeIf { matchedWords > 0 }
}

private inline fun List<String>.indexOfFirstFrom(
    startIndex: Int,
    predicate: (String) -> Boolean
): Int {
    for (index in startIndex until size) if (predicate(this[index])) return index
    return -1
}

private fun fuzzyThreshold(length: Int): Int = when {
    length < 4 -> 0
    length < 8 -> 1
    else -> 2
}

private fun fuzzyComparable(value: String): String =
    normalizeMultilingualSearch(value)
        .replace(Regex("[^a-z0-9]+"), "")

private fun sharesStableFuzzyPrefix(left: String, right: String): Boolean {
    val prefixLength = when {
        minOf(left.length, right.length) >= 6 -> 3
        minOf(left.length, right.length) >= 4 -> 2
        else -> 1
    }
    return left.take(prefixLength) == right.take(prefixLength)
}

private fun boundedLevenshtein(left: String, right: String, maxDistance: Int): Int? {
    if (kotlin.math.abs(left.length - right.length) > maxDistance) return null
    if (left == right) return 0
    if (maxDistance <= 0) return null

    var previous = IntArray(right.length + 1) { it }
    var current = IntArray(right.length + 1)

    for (i in 1..left.length) {
        current[0] = i
        var rowMinimum = current[0]
        for (j in 1..right.length) {
            val substitutionCost = if (left[i - 1] == right[j - 1]) 0 else 1
            current[j] = minOf(
                current[j - 1] + 1,
                previous[j] + 1,
                previous[j - 1] + substitutionCost
            )
            rowMinimum = minOf(rowMinimum, current[j])
        }
        if (rowMinimum > maxDistance) return null
        val swap = previous
        previous = current
        current = swap
    }

    return previous[right.length].takeIf { it <= maxDistance }
}

private fun fuzzySearchRelevance(text: String, normalizedQuery: String): FuzzySearchRelevance? {
    val queryComparable = fuzzyComparable(normalizedQuery)
    val normalizedText = normalizeMultilingualSearch(text)
    val textComparable = fuzzyComparable(normalizedText)
    if (queryComparable.isBlank() || textComparable.isBlank()) return null

    val threshold = fuzzyThreshold(queryComparable.length)
    if (threshold == 0) return null

    data class Candidate(
        val value: String,
        val containerPenalty: Int,
        val isFirstToken: Boolean
    )

    val words = normalizedText.split(' ').filter(String::isNotBlank)
    val candidates = buildList {
        add(Candidate(textComparable, 0, true))
        words
            .map(::fuzzyComparable)
            .filter { it.length >= 3 }
            .forEachIndexed { index, value ->
                add(Candidate(value, 1, index == 0))
            }
    }.distinctBy { Triple(it.value, it.containerPenalty, it.isFirstToken) }

    return candidates.mapNotNull { candidate ->
        if (!sharesStableFuzzyPrefix(queryComparable, candidate.value)) return@mapNotNull null

        val candidateThreshold = minOf(
            threshold,
            fuzzyThreshold(maxOf(queryComparable.length, candidate.value.length))
        )
        boundedLevenshtein(queryComparable, candidate.value, candidateThreshold)?.let { distance ->
            val lexicalHeadPenalty = when {
                candidate.containerPenalty == 0 && words.size == 1 -> 0
                candidate.isFirstToken && Regex("[,;/]").containsMatchIn(text) -> 0
                candidate.isFirstToken -> 1
                else -> 2
            }
            FuzzySearchRelevance(
                distance = distance,
                lexicalHeadPenalty = lexicalHeadPenalty,
                containerPenalty = candidate.containerPenalty,
                lengthDelta = kotlin.math.abs(queryComparable.length - candidate.value.length)
            )
        }
    }.minWithOrNull(
        compareBy<FuzzySearchRelevance> { it.distance }
            .thenBy { it.lexicalHeadPenalty }
            .thenBy { it.containerPenalty }
            .thenBy { it.lengthDelta }
    )
}

internal fun filterAndRankAcrossLanguages(
    entries: List<DictionaryEntry>,
    normalizedQuery: String,
    languageCodes: List<String> = listOf("srm", "fr", "en", "nl"),
    limit: Int = 100
): List<DictionaryEntry> {
    if (normalizedQuery.isBlank()) return emptyList()
    return entries.asSequence()
        .mapNotNull { entry ->
            val relevance = languageCodes.mapNotNull { languageCode ->
                searchRelevance(searchTextForLanguage(entry, languageCode), normalizedQuery)
            }.minWithOrNull(compareBy<SearchRelevance> { it.rank }.thenBy { it.missingWordCount })
            relevance?.let { entry to it }
        }
        .distinctBy { it.first.id }
        .sortedWith(
            compareBy<Pair<DictionaryEntry, SearchRelevance>> { it.second.rank }
                .thenBy { it.second.missingWordCount }
                .thenBy { it.first.id }
        )
        .take(limit)
        .map { it.first }
        .toList()
}

internal fun filterAndRankByLanguage(
    entries: List<DictionaryEntry>,
    query: String,
    languageCode: String,
    limit: Int = 100
): List<DictionaryEntry> {
    val normalizedQuery = normalizeMultilingualSearch(query)
    return filterAndRankByLanguageNormalized(entries, normalizedQuery, languageCode, limit)
}

internal fun filterAndRankByLanguageNormalized(
    entries: List<DictionaryEntry>,
    normalizedQuery: String,
    languageCode: String,
    limit: Int = 100
): List<DictionaryEntry> {
    if (normalizedQuery.isBlank()) return emptyList()
    return entries.asSequence()
        .filter { searchTextForLanguage(it, languageCode).isNotBlank() }
        .mapNotNull { entry ->
            searchRelevance(searchTextForLanguage(entry, languageCode), normalizedQuery)?.let { entry to it }
        }
        .distinctBy { it.first.id }
        .sortedWith(
            compareBy<Pair<DictionaryEntry, SearchRelevance>> { it.second.rank }
                .thenBy { it.second.missingWordCount }
                .thenBy { normalizeMultilingualSearch(searchTextForLanguage(it.first, languageCode)) }
        )
        .take(limit)
        .map { it.first }
        .toList()
}

internal fun filterAndRankFuzzyAcrossLanguages(
    entries: List<DictionaryEntry>,
    normalizedQuery: String,
    languageCodes: List<String> = listOf("srm", "fr", "en", "nl"),
    limit: Int = 20
): List<DictionaryEntry> {
    if (normalizedQuery.isBlank()) return emptyList()
    return entries.asSequence()
        .mapNotNull { entry ->
            val relevance = languageCodes.mapNotNull { languageCode ->
                fuzzySearchRelevance(searchTextForLanguage(entry, languageCode), normalizedQuery)
            }.minWithOrNull(
                compareBy<FuzzySearchRelevance> { it.distance }
                    .thenBy { it.lexicalHeadPenalty }
                    .thenBy { it.containerPenalty }
                    .thenBy { it.lengthDelta }
            )
            relevance?.let { entry to it }
        }
        .distinctBy { it.first.id }
        .sortedWith(
            compareBy<Pair<DictionaryEntry, FuzzySearchRelevance>> { it.second.distance }
                .thenBy { it.second.lexicalHeadPenalty }
                .thenBy { it.second.containerPenalty }
                .thenBy { it.second.lengthDelta }
                .thenBy { it.first.id }
        )
        .take(minOf(limit, 8))
        .map { it.first }
        .toList()
}

internal fun filterAndRankFuzzyByLanguage(
    entries: List<DictionaryEntry>,
    normalizedQuery: String,
    languageCode: String,
    limit: Int = 20
): List<DictionaryEntry> {
    if (normalizedQuery.isBlank()) return emptyList()
    return entries.asSequence()
        .filter { searchTextForLanguage(it, languageCode).isNotBlank() }
        .mapNotNull { entry ->
            fuzzySearchRelevance(searchTextForLanguage(entry, languageCode), normalizedQuery)?.let { entry to it }
        }
        .distinctBy { it.first.id }
        .sortedWith(
            compareBy<Pair<DictionaryEntry, FuzzySearchRelevance>> { it.second.distance }
                .thenBy { it.second.lexicalHeadPenalty }
                .thenBy { it.second.containerPenalty }
                .thenBy { it.second.lengthDelta }
                .thenBy { normalizeMultilingualSearch(searchTextForLanguage(it.first, languageCode)) }
        )
        .take(minOf(limit, 8))
        .map { it.first }
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

internal fun effectiveTranslationLanguage(
    entry: DictionaryEntry,
    preferredLanguage: AppLanguage
): AppLanguage {
    val available = AppLanguage.entries.filter {
        searchTextForLanguage(entry, it.code).isNotBlank()
    }
    return preferredLanguage.takeIf { it in available }
        ?: AppLanguage.FRENCH.takeIf { it in available }
        ?: available.firstOrNull()
        ?: AppLanguage.SAAMAKA
}
