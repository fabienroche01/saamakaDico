package com.saamaka.dico.testeurs


internal enum class FrenchResolutionKind(val detailLabel: String) {
    EXACT("correspondance exacte"),
    ELISION("élision française normalisée"),
    INFLECTION("flexion grammaticale normalisée"),
    SYNONYM("formulation proche utilisée"),
    SPELLING("correction orthographique")
}

internal data class FrenchTranslationCandidate(
    val french: String,
    val saamaka: String,
    val validationStatus: String
)

internal data class FrenchFallbackResolution(
    val requested: String,
    val matchedFrench: String,
    val saamaka: String,
    val kind: FrenchResolutionKind,
    val score: Int,
    val alternatives: List<String>
) {
    val detail: String
        get() = "$requested → $matchedFrench : ${kind.detailLabel}"
}

private val attestedFrenchSynonyms = mapOf(
    "pere" to setOf("papa"),
    "papa" to setOf("pere")
)

internal fun attestedFrenchSynonymsOf(word: String): Set<String> =
    attestedFrenchSynonyms[normalizeAttestedPhraseKey(word)].orEmpty()

internal fun resolveAttestedFrenchSubject(subject: String): String? =
    when (normalizeAttestedPhraseKey(subject)) {
        "je" -> "mi"
        "tu" -> "i"
        "il", "elle" -> "a"
        "nous" -> "u"
        "vous" -> "unu"
        "ils", "elles" -> "de"
        else -> null
    }

internal class FrenchFallbackResolver(
    candidates: List<FrenchTranslationCandidate>
) {
    private data class IndexedCandidate(
        val value: FrenchTranslationCandidate,
        val normalizedFrench: String
    )

    private val indexedCandidates = candidates.mapNotNull { candidate ->
        val normalizedFrench = normalizeAttestedPhraseKey(candidate.french)
        candidate.takeIf {
            normalizedFrench.isNotBlank() && it.saamaka.isNotBlank()
        }?.let { IndexedCandidate(it, normalizedFrench) }
    }
    private val exactIndex = indexedCandidates.groupBy { it.normalizedFrench }
    private val spellingIndex = indexedCandidates.groupBy {
        spellingBucket(it.normalizedFrench)
    }

    fun resolve(word: String): FrenchFallbackResolution? {
        val normalizedWord = normalizeAttestedPhraseKey(word)
        if (normalizedWord.isBlank()) return null

        exact(normalizedWord, word)?.let { return it }

        safeElisionForms(normalizedWord).forEach { normalizedForm ->
            bestCandidate(normalizedForm)?.let { candidate ->
                return resolution(word, candidate, FrenchResolutionKind.ELISION, 850)
            }
        }

        safeInflectionForms(normalizedWord).forEach { normalizedForm ->
            bestCandidate(normalizedForm)?.let { candidate ->
                return resolution(word, candidate, FrenchResolutionKind.INFLECTION, 800)
            }
        }

        attestedFrenchSynonyms[normalizedWord].orEmpty().forEach { synonym ->
            bestCandidate(synonym)?.let { candidate ->
                return resolution(word, candidate, FrenchResolutionKind.SYNONYM, 600)
            }
        }

        if (normalizedWord.length < MIN_SPELLING_LENGTH) return null

        val spellingCandidates = buildList {
            for (length in (normalizedWord.length - 1)..(normalizedWord.length + 1)) {
                if (length > 0) {
                    addAll(
                        spellingIndex[normalizedWord.first() to length]
                            .orEmpty()
                            .take(MAX_SPELLING_CANDIDATES)
                    )
                }
            }
        }.take(MAX_SPELLING_CANDIDATES)

        val spelling = spellingCandidates
            .asSequence()
            .map { it to levenshteinDistance(normalizedWord, it.normalizedFrench) }
            .filter { (_, distance) -> distance == 1 }
            .maxByOrNull { (candidate, distance) ->
                validationScore(candidate.value.validationStatus) - distance
            }
            ?.first?.value

        return spelling?.let {
            resolution(word, it, FrenchResolutionKind.SPELLING, 400)
        }?.takeIf { it.score >= MIN_ACCEPTED_SCORE }
    }

    private fun exact(normalizedWord: String, requested: String): FrenchFallbackResolution? =
        bestCandidate(normalizedWord)?.let {
            resolution(requested, it, FrenchResolutionKind.EXACT, 1000)
        }

    private fun bestCandidate(normalizedFrench: String): FrenchTranslationCandidate? =
        exactIndex[normalizedFrench]
            .orEmpty()
            .maxByOrNull { validationScore(it.value.validationStatus) }
            ?.value

    private fun resolution(
        requested: String,
        candidate: FrenchTranslationCandidate,
        kind: FrenchResolutionKind,
        relationScore: Int
    ): FrenchFallbackResolution {
        val score = relationScore + validationScore(candidate.validationStatus)
        val rankedTranslations = exactIndex[normalizeAttestedPhraseKey(candidate.french)]
            .orEmpty()
            .sortedByDescending { validationScore(it.value.validationStatus) }
            .flatMap { splitTranslationAlternatives(it.value.saamaka) }
            .distinctBy(::normalizeAttestedPhraseKey)
        val primary = splitTranslationAlternatives(candidate.saamaka).firstOrNull()
            ?: return FrenchFallbackResolution(
                requested, candidate.french, candidate.saamaka.trim(), kind, score, emptyList()
            )

        return FrenchFallbackResolution(
            requested = requested,
            matchedFrench = candidate.french,
            saamaka = primary,
            kind = kind,
            score = score,
            alternatives = rankedTranslations.filterNot {
                normalizeAttestedPhraseKey(it) == normalizeAttestedPhraseKey(primary)
            }
        )
    }

    private fun splitTranslationAlternatives(value: String): List<String> = value
        .split(Regex("\\s*[,;/]\\s*"))
        .map(String::trim)
        .filter(String::isNotBlank)

    private fun safeInflectionForms(word: String): List<String> = buildList {
        FrenchVerbInflections.lemma(word)?.let(::add)
        if (word.length > 3 && word.endsWith("s") && !word.endsWith("ss")) {
            add(word.dropLast(1))
        }
    }.distinct()

    private fun safeElisionForms(word: String): List<String> = buildList {
        if ((word.startsWith("l'") || word.startsWith("d'")) && word.length > 2) {
            add(word.substring(2))
        }
    }

    private fun validationScore(status: String): Int = when {
        status.trim().equals("O", ignoreCase = true) -> 300
        status.trim().equals("D", ignoreCase = true) -> 100
        else -> 200
    }

    private fun levenshteinDistance(left: String, right: String): Int {
        if (left == right) return 0
        if (kotlin.math.abs(left.length - right.length) > 1) return 2
        var previous = IntArray(right.length + 1) { it }
        for (i in left.indices) {
            val current = IntArray(right.length + 1)
            current[0] = i + 1
            for (j in right.indices) {
                current[j + 1] = minOf(
                    current[j] + 1,
                    previous[j + 1] + 1,
                    previous[j] + if (left[i] == right[j]) 0 else 1
                )
            }
            previous = current
        }
        return previous[right.length]
    }

    private fun spellingBucket(normalizedFrench: String): Pair<Char, Int> =
        normalizedFrench.first() to normalizedFrench.length

    private companion object {
        const val MAX_SPELLING_CANDIDATES = 128
        const val MIN_SPELLING_LENGTH = 5
        const val MIN_ACCEPTED_SCORE = 650
    }
}
