package com.saamaka.dico.testeurs

import com.saamaka.dico.testeurs.model.PhraseTranslationKind
import com.saamaka.dico.testeurs.model.PhraseTranslationResult
import com.saamaka.dico.testeurs.model.RecognizedPhraseSegment
import com.saamaka.dico.testeurs.model.TranslationReliability

/**
 * Applies only the attested French -> Saamaka structures. Lexical translations always
 * come from [resolveFrenchWord]; this class contains no French phrase translations.
 */
internal class SaamakaGrammarEngine(
    private val resolveFrenchWord: (String) -> FrenchFallbackResolution?
) {
    fun translate(text: String): PhraseTranslationResult? {
        val words = tokenizeFrench(text)
        if (words.size < 2) return null
        val subject = resolveAttestedFrenchSubject(words.first()) ?: return null

        return translateProgressive(words, subject)
            ?: translateNegation(words, subject)
            ?: translateCopularState(words, subject)
            ?: translateNearFuture(words, subject)
            ?: translateVouloir(words, subject)
            ?: translateSafeAction(words, subject)
    }

    private fun translateProgressive(words: List<String>, subject: String): PhraseTranslationResult? {
        if (words.size < 6 ||
            FrenchVerbInflections.lemma(words[1]) != "être" ||
            FrenchVerbInflections.tense(words[1]) != FrenchVerbTense.PRESENT ||
            words.subList(2, 5).map(::normalizeAttestedPhraseKey) != listOf("en", "train", "de")
        ) return null

        val verb = resolveSafeVerb(words[5]) ?: return null
        val verbLemma = FrenchVerbInflections.lemma(words[5]) ?: normalizeAttestedPhraseKey(words[5])
        val complements = resolveComplements(
            words.drop(6),
            allowFrenchPartitive = verbLemma == "manger"
        ) ?: return null
        return grammatical(
            words,
            listOf(subject, "ta", verb.saamaka) + complements.map { it.second.saamaka },
            listOf(words[5] to verb) + complements
        )
    }

    private fun translateNearFuture(words: List<String>, subject: String): PhraseTranslationResult? {
        if (words.size < 3 ||
            FrenchVerbInflections.lemma(words[1]) != "aller" ||
            FrenchVerbInflections.tense(words[1]) != FrenchVerbTense.PRESENT
        ) return null

        val verb = resolveSafeVerb(words[2]) ?: return null
        val verbLemma = FrenchVerbInflections.lemma(words[2]) ?: normalizeAttestedPhraseKey(words[2])
        val complements = resolveComplements(
            words.drop(3),
            allowFrenchPartitive = verbLemma == "manger"
        ) ?: return null
        return grammatical(
            words,
            listOf(subject, "o", verb.saamaka) + complements.map { it.second.saamaka },
            listOf(words[2] to verb) + complements
        )
    }

    private fun translateVouloir(words: List<String>, subject: String): PhraseTranslationResult? {
        if (words.size < 3 ||
            FrenchVerbInflections.lemma(words[1]) != "vouloir" ||
            FrenchVerbInflections.tense(words[1]) != FrenchVerbTense.PRESENT
        ) return null
        val vouloir = resolveLemma(words[1], "vouloir") ?: return null

        resolveNaturalObjectClitic(words.drop(2))?.let { parsed ->
            val verbLemma = FrenchVerbInflections.lemma(parsed.verbSource)
                ?: normalizeAttestedPhraseKey(parsed.verbSource)
            val complements = resolveComplements(
                words.drop(2 + parsed.consumedWords),
                allowFrenchPartitive = verbLemma == "manger"
            ) ?: return null
            return grammatical(
                words,
                listOf(subject, vouloir.saamaka, parsed.verb.saamaka, parsed.objectPronoun.saamaka) +
                    complements.map { it.second.saamaka },
                listOf(
                    words[1] to vouloir,
                    parsed.verbSource to parsed.verb,
                    parsed.objectSource to parsed.objectPronoun
                ) + complements
            )
        }

        val verb = resolveSafeVerb(words[2]) ?: return null
        val verbLemma = FrenchVerbInflections.lemma(words[2]) ?: normalizeAttestedPhraseKey(words[2])
        val complements = resolveComplements(
            words.drop(3),
            allowFrenchPartitive = verbLemma == "manger"
        ) ?: return null
        return grammatical(
            words,
            listOf(subject, vouloir.saamaka, verb.saamaka) + complements.map { it.second.saamaka },
            listOf(words[1] to vouloir, words[2] to verb) + complements
        )
    }

    private fun translateCopularState(words: List<String>, subject: String): PhraseTranslationResult? {
        if (words.size != 3 || FrenchVerbInflections.lemma(words[1]) != "être") return null
        val state = resolveExact(words[2]) ?: return null
        val marker = when (FrenchVerbInflections.tense(words[1])) {
            FrenchVerbTense.PRESENT -> emptyList()
            FrenchVerbTense.PAST -> listOf("bi")
            else -> return null
        }
        return grammatical(words, listOf(subject) + marker + state.saamaka, listOf(words[2] to state))
    }

    private fun translateNegation(words: List<String>, subject: String): PhraseTranslationResult? {
        if (words.size < 4 ||
            normalizeAttestedPhraseKey(words[1]) != "ne" ||
            normalizeAttestedPhraseKey(words[3]) != "pas"
        ) return null

        val lemma = FrenchVerbInflections.lemma(words[2]) ?: words[2]
        if (lemma == "être") {
            if (words.size != 5) return null
            val state = resolveExact(words[4]) ?: return null
            return grammatical(words, listOf(subject, "an", state.saamaka), listOf(words[4] to state))
        }

        val verb = resolveSafeVerb(words[2]) ?: return null
        val complements = resolveComplements(
            words.drop(4),
            allowFrenchPartitive = normalizeAttestedPhraseKey(lemma) == "manger"
        ) ?: return null
        return grammatical(
            words,
            listOf(subject, "an", verb.saamaka) + complements.map { it.second.saamaka },
            listOf(words[2] to verb) + complements
        )
    }

    private fun translateSafeAction(words: List<String>, subject: String): PhraseTranslationResult? {
        val verb = resolveSafeVerb(words[1]) ?: return null
        val verbLemma = FrenchVerbInflections.lemma(words[1]) ?: normalizeAttestedPhraseKey(words[1])
        val complements = resolveComplements(
            words.drop(2),
            allowFrenchPartitive = verbLemma == "manger"
        ) ?: return null
        val markers = when (FrenchVerbInflections.tense(words[1])) {
            FrenchVerbTense.FUTURE -> listOf("o")
            FrenchVerbTense.PRESENT -> listOf("ta")
            FrenchVerbTense.PAST -> emptyList()
            else -> return null
        }
        return grammatical(
            words,
            listOf(subject) + markers + verb.saamaka + complements.map { it.second.saamaka },
            listOf(words[1] to verb) + complements
        )
    }

    private data class NaturalObjectClitic(
        val objectSource: String,
        val objectPronoun: FrenchFallbackResolution,
        val verbSource: String,
        val verb: FrenchFallbackResolution,
        val consumedWords: Int
    )

    private fun resolveNaturalObjectClitic(words: List<String>): NaturalObjectClitic? {
        if (words.isEmpty()) return null

        val first = words[0]
        val normalizedFirst = normalizeAttestedPhraseKey(first)
        val detachedPronoun = when (normalizedFirst) {
            "me" -> "moi"
            "te" -> "toi"
            "le", "la" -> "lui"
            "nous" -> "nous"
            "vous" -> "vous"
            "les" -> "eux"
            else -> null
        }
        if (detachedPronoun != null && words.size >= 2) {
            val verb = resolveSafeVerb(words[1]) ?: return null
            val pronoun = resolveAttestedComplementPronoun(detachedPronoun) ?: return null
            return NaturalObjectClitic(
                objectSource = first,
                objectPronoun = pronoun,
                verbSource = words[1],
                verb = verb,
                consumedWords = 2
            )
        }

        val attached = listOf(
            "m'" to "moi",
            "t'" to "toi",
            "l'" to "lui"
        ).firstOrNull { (prefix, _) -> first.startsWith(prefix) && first.length > prefix.length }
            ?: return null
        val verbSource = first.removePrefix(attached.first)
        val verb = resolveSafeVerb(verbSource) ?: return null
        val pronoun = resolveAttestedComplementPronoun(attached.second) ?: return null
        return NaturalObjectClitic(
            objectSource = attached.first.dropLast(1),
            objectPronoun = pronoun,
            verbSource = verbSource,
            verb = verb,
            consumedWords = 1
        )
    }

    private fun resolveComplements(
        words: List<String>,
        allowFrenchPartitive: Boolean = false
    ): List<Pair<String, FrenchFallbackResolution>>? {
        if (words.isEmpty()) return emptyList()

        val resolved = mutableListOf<Pair<String, FrenchFallbackResolution>>()
        var index = 0

        while (index < words.size) {
            var match: Pair<String, FrenchFallbackResolution>? = null
            var consumedWordCount = 0

            // Prefer the longest exact attested expression. This keeps entries such as
            // multiword nouns/expressions intact instead of forcing a word-by-word split.
            for (endExclusive in words.size downTo index + 1) {
                val candidate = words.subList(index, endExclusive).joinToString(" ")
                val resolution = resolveExact(candidate) ?: continue
                match = candidate to resolution
                consumedWordCount = endExclusive - index
                break
            }

            // French articles are source-side grammar. When no exact multiword entry exists,
            // ignore the determiner only if the following lexical noun/expression is itself
            // attested. No Saamaka article is invented here.
            if (match == null && isFrenchNominalDeterminer(words[index]) && index + 1 < words.size) {
                for (endExclusive in words.size downTo index + 1) {
                    val lexicalCandidate = words.subList(index + 1, endExclusive).joinToString(" ")
                    val resolution = resolveExact(lexicalCandidate) ?: continue
                    val sourceCandidate = words.subList(index, endExclusive).joinToString(" ")
                    match = sourceCandidate to resolution
                    consumedWordCount = endExclusive - index
                    break
                }
            }

            // With manger, French partitives such as "de la nourriture" are source-side
            // determiners. Strip only the French partitive sequence and only when the
            // remaining noun/expression is attested. This avoids treating "de" as disposable
            // for verbs where it may carry lexical meaning.
            if (
                match == null &&
                allowFrenchPartitive &&
                normalizeAttestedPhraseKey(words[index]) == "de" &&
                index + 2 < words.size &&
                normalizeAttestedPhraseKey(words[index + 1]) in setOf("la", "le", "les")
            ) {
                for (endExclusive in words.size downTo index + 2) {
                    val lexicalCandidate = words.subList(index + 2, endExclusive).joinToString(" ")
                    val resolution = resolveExact(lexicalCandidate) ?: continue
                    val sourceCandidate = words.subList(index, endExclusive).joinToString(" ")
                    match = sourceCandidate to resolution
                    consumedWordCount = endExclusive - index
                    break
                }
            }

            val resolvedMatch = match ?: return null
            resolved += resolvedMatch
            index += consumedWordCount
        }

        return resolved
    }

    private fun isFrenchNominalDeterminer(word: String): Boolean =
        normalizeAttestedPhraseKey(word) in setOf("le", "la", "les", "un", "une", "des", "du")

    private fun resolveSafeVerb(form: String): FrenchFallbackResolution? {
        val lemma = FrenchVerbInflections.lemma(form) ?: normalizeAttestedPhraseKey(form)
        if (!FrenchVerbInflections.canComposeFromAttestedTranslation(lemma)) return null
        return resolveLemma(form, lemma)
    }

    private fun resolveLemma(form: String, lemma: String): FrenchFallbackResolution? =
        resolveFrenchWord(form)?.takeIf {
            normalizeAttestedPhraseKey(it.matchedFrench) == normalizeAttestedPhraseKey(lemma) &&
                it.kind in setOf(FrenchResolutionKind.EXACT, FrenchResolutionKind.INFLECTION)
        }

    private fun resolveExact(word: String): FrenchFallbackResolution? =
        resolveFrenchWord(word)?.takeIf {
            it.kind == FrenchResolutionKind.EXACT &&
                normalizeAttestedPhraseKey(it.matchedFrench) == normalizeAttestedPhraseKey(word)
        } ?: resolveAttestedComplementPronoun(word)

    private fun resolveAttestedComplementPronoun(word: String): FrenchFallbackResolution? {
        val normalized = normalizeAttestedPhraseKey(word)
        val saamaka = when (normalized) {
            "moi" -> "mi"
            "toi" -> "i"
            "lui", "elle" -> "a"
            "nous" -> "u"
            "vous" -> "unu"
            "eux", "elles" -> "de"
            else -> return null
        }
        return FrenchFallbackResolution(
            requested = word,
            matchedFrench = word,
            saamaka = saamaka,
            kind = FrenchResolutionKind.EXACT,
            score = 1300,
            alternatives = emptyList()
        )
    }

    private fun grammatical(
        sourceWords: List<String>,
        output: List<String>,
        lexicalSegments: List<Pair<String, FrenchFallbackResolution>>
    ): PhraseTranslationResult = PhraseTranslationResult(
        translation = output.joinToString(" "),
        recognizedSegments = listOf(
            RecognizedPhraseSegment(sourceWords.first(), output.first(), matchedSource = sourceWords.first())
        ) + lexicalSegments.map { (source, resolution) ->
            RecognizedPhraseSegment(
                source = source,
                translation = resolution.saamaka,
                matchedSource = resolution.matchedFrench,
                alternatives = resolution.alternatives
            )
        },
        untranslatedSegments = emptyList(),
        isComplete = true,
        reliability = TranslationReliability.HIGH,
        kind = PhraseTranslationKind.GRAMMATICAL
    )

    private fun tokenizeFrench(text: String): List<String> = cleanPhraseInput(text)
        .lowercase()
        .replace(Regex("\\bj'"), "je ")
        .replace(Regex("\\bn'"), "ne ")
        .replace(Regex("\\s+"), " ")
        .trim()
        .split(' ')
        .filter(String::isNotBlank)
}
