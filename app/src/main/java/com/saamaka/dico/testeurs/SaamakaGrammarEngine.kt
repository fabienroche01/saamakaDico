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
        val complements = resolveComplements(words.drop(6)) ?: return null
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
        val complements = resolveComplements(words.drop(3)) ?: return null
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
        val verb = resolveSafeVerb(words[2]) ?: return null
        val complements = resolveComplements(words.drop(3)) ?: return null
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
        val complements = resolveComplements(words.drop(4)) ?: return null
        return grammatical(
            words,
            listOf(subject, "an", verb.saamaka) + complements.map { it.second.saamaka },
            listOf(words[2] to verb) + complements
        )
    }

    private fun translateSafeAction(words: List<String>, subject: String): PhraseTranslationResult? {
        val verb = resolveSafeVerb(words[1]) ?: return null
        val complements = resolveComplements(words.drop(2)) ?: return null
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

    private fun resolveComplements(words: List<String>): List<Pair<String, FrenchFallbackResolution>>? {
        if (words.isEmpty()) return emptyList()

        val resolved = mutableListOf<Pair<String, FrenchFallbackResolution>>()
        var index = 0

        while (index < words.size) {
            var match: Pair<String, FrenchFallbackResolution>? = null

            // Prefer the longest exact attested expression. This keeps entries such as
            // multiword nouns/expressions intact instead of forcing a word-by-word split.
            for (endExclusive in words.size downTo index + 1) {
                val candidate = words.subList(index, endExclusive).joinToString(" ")
                val resolution = resolveExact(candidate) ?: continue
                match = candidate to resolution
                break
            }

            val resolvedMatch = match ?: return null
            resolved += resolvedMatch
            index += resolvedMatch.first.split(' ').size
        }

        return resolved
    }

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
