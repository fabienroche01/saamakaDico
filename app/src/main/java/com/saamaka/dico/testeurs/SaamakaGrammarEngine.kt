package com.saamaka.dico.testeurs

import com.saamaka.dico.testeurs.model.PhraseTranslationKind
import com.saamaka.dico.testeurs.model.PhraseTranslationResult
import com.saamaka.dico.testeurs.model.RecognizedPhraseSegment
import com.saamaka.dico.testeurs.model.TranslationReliability

/**
 * Applies only the attested French -> Saamaka structures. Lexical translations always
 * come from [resolveFrenchWord]; this class contains no invented Saamaka phrase lexicon.
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

        val tail = words.drop(5)
        val parsed = resolveVerbPhrase(tail) ?: return null
        val complements = resolveComplements(
            tail.drop(parsed.consumedWords),
            allowFrenchPartitive = parsed.lemma == "manger"
        ) ?: return null
        return grammatical(
            words,
            listOf(subject, "ta") + parsed.output + complements.map { it.second.saamaka },
            parsed.lexicalSegments + complements
        )
    }

    private fun translateNearFuture(words: List<String>, subject: String): PhraseTranslationResult? {
        if (words.size < 3 ||
            FrenchVerbInflections.lemma(words[1]) != "aller" ||
            FrenchVerbInflections.tense(words[1]) != FrenchVerbTense.PRESENT
        ) return null

        val tail = words.drop(2)
        val parsed = resolveVerbPhrase(tail) ?: return null
        val complements = resolveComplements(
            tail.drop(parsed.consumedWords),
            allowFrenchPartitive = parsed.lemma == "manger"
        ) ?: return null
        return grammatical(
            words,
            listOf(subject, "o") + parsed.output + complements.map { it.second.saamaka },
            parsed.lexicalSegments + complements
        )
    }

    private fun translateVouloir(words: List<String>, subject: String): PhraseTranslationResult? {
        if (words.size < 3 ||
            FrenchVerbInflections.lemma(words[1]) != "vouloir" ||
            FrenchVerbInflections.tense(words[1]) != FrenchVerbTense.PRESENT
        ) return null
        val vouloir = resolveLemma(words[1], "vouloir") ?: return null
        val tail = words.drop(2)
        val parsed = resolveVerbPhrase(tail) ?: return null
        val complements = resolveComplements(
            tail.drop(parsed.consumedWords),
            allowFrenchPartitive = parsed.lemma == "manger"
        ) ?: return null
        return grammatical(
            words,
            listOf(subject, vouloir.saamaka) + parsed.output + complements.map { it.second.saamaka },
            listOf(words[1] to vouloir) + parsed.lexicalSegments + complements
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
        if (words.size < 4 || normalizeAttestedPhraseKey(words[1]) != "ne") return null
        val pasIndex = (2 until words.size).firstOrNull {
            normalizeAttestedPhraseKey(words[it]) == "pas"
        } ?: return null
        if (pasIndex < 3) return null

        val beforePas = words.subList(2, pasIndex)
        val afterPas = words.drop(pasIndex + 1)

        // "je ne veux pas ..." : negate vouloir, then compose only an attested safe verb phrase.
        if (beforePas.size == 1 && FrenchVerbInflections.lemma(beforePas[0]) == "vouloir") {
            if (afterPas.isEmpty()) return null
            val vouloir = resolveLemma(beforePas[0], "vouloir") ?: return null
            val parsed = resolveVerbPhrase(afterPas) ?: return null
            val complements = resolveComplements(
                afterPas.drop(parsed.consumedWords),
                allowFrenchPartitive = parsed.lemma == "manger"
            ) ?: return null
            return grammatical(
                words,
                listOf(subject, "an", vouloir.saamaka) + parsed.output +
                    complements.map { it.second.saamaka },
                listOf(beforePas[0] to vouloir) + parsed.lexicalSegments + complements
            )
        }

        // French copular state: "je ne suis pas malade" -> subject + an + attested state.
        if (beforePas.size == 1 && FrenchVerbInflections.lemma(beforePas[0]) == "être") {
            if (afterPas.size != 1) return null
            val state = resolveExact(afterPas[0]) ?: return null
            return grammatical(
                words,
                listOf(subject, "an", state.saamaka),
                listOf(afterPas[0] to state)
            )
        }

        // Handles both "je ne mange pas" and natural clitics such as
        // "je ne te vois pas" / "je ne t'aide pas".
        val parsed = resolveVerbPhrase(beforePas) ?: return null
        if (parsed.consumedWords != beforePas.size) return null
        val complements = resolveComplements(
            afterPas,
            allowFrenchPartitive = parsed.lemma == "manger"
        ) ?: return null
        return grammatical(
            words,
            listOf(subject, "an") + parsed.output + complements.map { it.second.saamaka },
            parsed.lexicalSegments + complements
        )
    }

    private fun translateSafeAction(words: List<String>, subject: String): PhraseTranslationResult? {
        val tail = words.drop(1)
        val parsed = resolveVerbPhrase(tail) ?: return null
        val complements = resolveComplements(
            tail.drop(parsed.consumedWords),
            allowFrenchPartitive = parsed.lemma == "manger"
        ) ?: return null
        val markers = when (FrenchVerbInflections.tense(parsed.verbSource)) {
            FrenchVerbTense.FUTURE -> listOf("o")
            FrenchVerbTense.PRESENT -> listOf("ta")
            FrenchVerbTense.PAST -> emptyList()
            else -> return null
        }
        return grammatical(
            words,
            listOf(subject) + markers + parsed.output + complements.map { it.second.saamaka },
            parsed.lexicalSegments + complements
        )
    }

    private data class ParsedVerbPhrase(
        val verbSource: String,
        val verb: FrenchFallbackResolution,
        val lemma: String,
        val objectSource: String? = null,
        val objectPronoun: FrenchFallbackResolution? = null,
        val consumedWords: Int
    ) {
        val output: List<String>
            get() = listOf(verb.saamaka) + listOfNotNull(objectPronoun?.saamaka)

        val lexicalSegments: List<Pair<String, FrenchFallbackResolution>>
            get() = listOf(verbSource to verb) + listOfNotNull(
                objectSource?.let { source -> objectPronoun?.let { source to it } }
            )
    }

    private fun resolveVerbPhrase(words: List<String>): ParsedVerbPhrase? {
        resolveNaturalObjectClitic(words)?.let { parsed ->
            return ParsedVerbPhrase(
                verbSource = parsed.verbSource,
                verb = parsed.verb,
                lemma = FrenchVerbInflections.lemma(parsed.verbSource)
                    ?: normalizeAttestedPhraseKey(parsed.verbSource),
                objectSource = parsed.objectSource,
                objectPronoun = parsed.objectPronoun,
                consumedWords = parsed.consumedWords
            )
        }
        val verbSource = words.firstOrNull() ?: return null
        val verb = resolveSafeVerb(verbSource) ?: return null
        return ParsedVerbPhrase(
            verbSource = verbSource,
            verb = verb,
            lemma = FrenchVerbInflections.lemma(verbSource) ?: normalizeAttestedPhraseKey(verbSource),
            consumedWords = 1
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
            objectSource = attached.first,
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

            // Longest exact attested expression first.
            for (endExclusive in words.size downTo index + 1) {
                val candidate = words.subList(index, endExclusive).joinToString(" ")
                val resolution = resolveExact(candidate) ?: continue
                match = candidate to resolution
                consumedWordCount = endExclusive - index
                break
            }

            // French nominal determiners are stripped only when the lexical noun/expression
            // that follows is itself attested. No Saamaka article is fabricated.
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

            // For manger only, French partitives such as "de la ..." are source-side
            // determiners and can be stripped when the remaining noun/expression is attested.
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

    private fun resolveLemma(form: String, lemma: String): FrenchFallbackResolution? {
        val normalizedLemma = normalizeAttestedPhraseKey(lemma)
        return resolveFrenchWord(form)?.takeIf {
            normalizeAttestedPhraseKey(it.matchedFrench) == normalizedLemma &&
                it.kind in setOf(FrenchResolutionKind.EXACT, FrenchResolutionKind.INFLECTION)
        } ?: resolveFrenchWord(lemma)?.takeIf {
            normalizeAttestedPhraseKey(it.matchedFrench) == normalizedLemma &&
                it.kind == FrenchResolutionKind.EXACT
        }
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
