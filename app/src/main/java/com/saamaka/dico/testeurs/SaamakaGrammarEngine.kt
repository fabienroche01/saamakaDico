package com.saamaka.dico.testeurs

import com.saamaka.dico.testeurs.model.PhraseTranslationKind
import com.saamaka.dico.testeurs.model.PhraseTranslationResult
import com.saamaka.dico.testeurs.model.RecognizedPhraseSegment
import com.saamaka.dico.testeurs.model.TranslationReliability

/** Grammar-line only. Lexical Saamaka always comes from attested dictionary data. */
internal class SaamakaGrammarEngine(
    private val resolveFrenchWord: (String) -> FrenchFallbackResolution?
) {
    private enum class PredicateType { STATE, DYNAMIC, OTHER }

    private data class TamMarkers(
        val past: Boolean = false,
        val future: Boolean = false,
        val potential: Boolean = false,
        val imperfective: Boolean = false
    ) {
        fun asSaamaka(): List<String> = buildList {
            if (past) add("bi")
            if (future) add("o")
            if (potential) add("sa")
            if (imperfective) add("ta")
        }
    }

    fun translate(text: String): PhraseTranslationResult? {
        val words = tokenizeFrench(text)
        if (words.size < 2) return null
        val subject = resolveAttestedFrenchSubject(words.first()) ?: return null

        return translateProgressive(words, subject)
            ?: translateNegation(words, subject)
            ?: translateCopularState(words, subject)
            ?: translateNearFuture(words, subject)
            ?: translatePotential(words, subject)
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
        val complements = resolveComplements(tail.drop(parsed.consumedWords), parsed.lemma == "manger") ?: return null
        return grammatical(words, listOf(subject) + TamMarkers(imperfective = true).asSaamaka() + parsed.output + complements.map { it.second.saamaka }, parsed.lexicalSegments + complements)
    }

    private fun translateNearFuture(words: List<String>, subject: String): PhraseTranslationResult? {
        if (words.size < 3 || FrenchVerbInflections.lemma(words[1]) != "aller" || FrenchVerbInflections.tense(words[1]) != FrenchVerbTense.PRESENT) return null
        val tail = words.drop(2)
        val parsed = resolveVerbPhrase(tail) ?: return null
        val complements = resolveComplements(tail.drop(parsed.consumedWords), parsed.lemma == "manger") ?: return null
        return grammatical(words, listOf(subject) + TamMarkers(future = true).asSaamaka() + parsed.output + complements.map { it.second.saamaka }, parsed.lexicalSegments + complements)
    }

    private fun translatePotential(words: List<String>, subject: String): PhraseTranslationResult? {
        if (words.size < 3 || FrenchVerbInflections.lemma(words[1]) != "pouvoir" || FrenchVerbInflections.tense(words[1]) != FrenchVerbTense.PRESENT) return null
        val tail = words.drop(2)
        val parsed = resolveVerbPhrase(tail) ?: return null
        val complements = resolveComplements(tail.drop(parsed.consumedWords), parsed.lemma == "manger") ?: return null
        return grammatical(words, listOf(subject) + TamMarkers(potential = true).asSaamaka() + parsed.output + complements.map { it.second.saamaka }, parsed.lexicalSegments + complements)
    }

    private fun translateVouloir(words: List<String>, subject: String): PhraseTranslationResult? {
        if (words.size < 3 || FrenchVerbInflections.lemma(words[1]) != "vouloir" || FrenchVerbInflections.tense(words[1]) != FrenchVerbTense.PRESENT) return null
        val vouloir = resolveLemma(words[1], "vouloir") ?: return null
        val tail = words.drop(2)
        val parsed = resolveVerbPhrase(tail) ?: return null
        val complements = resolveComplements(tail.drop(parsed.consumedWords), parsed.lemma == "manger") ?: return null
        return grammatical(words, listOf(subject, vouloir.saamaka) + parsed.output + complements.map { it.second.saamaka }, listOf(words[1] to vouloir) + parsed.lexicalSegments + complements)
    }

    private fun translateCopularState(words: List<String>, subject: String): PhraseTranslationResult? {
        if (words.size < 3 || FrenchVerbInflections.lemma(words[1]) != "être") return null
        val predicate = resolveComplements(words.drop(2)) ?: return null
        if (predicate.isEmpty()) return null
        val markers = when (FrenchVerbInflections.tense(words[1])) {
            FrenchVerbTense.PRESENT -> TamMarkers()
            FrenchVerbTense.PAST -> TamMarkers(past = true)
            FrenchVerbTense.FUTURE -> TamMarkers(future = true)
            else -> return null
        }
        return grammatical(words, listOf(subject) + markers.asSaamaka() + predicate.map { it.second.saamaka }, predicate)
    }

    private fun translateNegation(words: List<String>, subject: String): PhraseTranslationResult? {
        if (words.size < 4 || normalizeAttestedPhraseKey(words[1]) != "ne") return null
        val pasIndex = (2 until words.size).firstOrNull { normalizeAttestedPhraseKey(words[it]) == "pas" } ?: return null
        if (pasIndex < 3) return null
        val beforePas = words.subList(2, pasIndex)
        val afterPas = words.drop(pasIndex + 1)

        if (beforePas.size == 1 && FrenchVerbInflections.lemma(beforePas[0]) == "vouloir") {
            if (afterPas.isEmpty()) return null
            val vouloir = resolveLemma(beforePas[0], "vouloir") ?: return null
            val parsed = resolveVerbPhrase(afterPas) ?: return null
            val complements = resolveComplements(afterPas.drop(parsed.consumedWords), parsed.lemma == "manger") ?: return null
            return grammatical(words, listOf(subject, "an", vouloir.saamaka) + parsed.output + complements.map { it.second.saamaka }, listOf(beforePas[0] to vouloir) + parsed.lexicalSegments + complements)
        }

        if (beforePas.size == 1 && FrenchVerbInflections.lemma(beforePas[0]) == "être") {
            val predicate = resolveComplements(afterPas) ?: return null
            if (predicate.isEmpty()) return null
            return grammatical(words, listOf(subject, "an") + predicate.map { it.second.saamaka }, predicate)
        }

        val parsed = resolveVerbPhrase(beforePas) ?: return null
        if (parsed.consumedWords != beforePas.size) return null
        val complements = resolveComplements(afterPas, parsed.lemma == "manger") ?: return null
        return grammatical(words, listOf(subject, "an") + parsed.output + complements.map { it.second.saamaka }, parsed.lexicalSegments + complements)
    }

    private fun translateSafeAction(words: List<String>, subject: String): PhraseTranslationResult? {
        val tail = words.drop(1)
        val parsed = resolveVerbPhrase(tail) ?: return null
        val complements = resolveComplements(tail.drop(parsed.consumedWords), parsed.lemma == "manger") ?: return null
        val predicateType = predicateType(parsed.lemma)
        val markers = when (FrenchVerbInflections.tense(parsed.verbSource)) {
            FrenchVerbTense.FUTURE -> TamMarkers(future = true)
            FrenchVerbTense.PRESENT -> when (predicateType) {
                PredicateType.STATE -> TamMarkers()
                PredicateType.DYNAMIC -> TamMarkers(imperfective = true)
                PredicateType.OTHER -> return null
            }
            FrenchVerbTense.PAST -> when (predicateType) {
                PredicateType.STATE -> TamMarkers(past = true)
                PredicateType.DYNAMIC -> TamMarkers(past = true, imperfective = true)
                PredicateType.OTHER -> return null
            }
            else -> return null
        }
        return grammatical(words, listOf(subject) + markers.asSaamaka() + parsed.output + complements.map { it.second.saamaka }, parsed.lexicalSegments + complements)
    }

    private fun predicateType(lemma: String): PredicateType = when (normalizeAttestedPhraseKey(lemma)) {
        "aimer", "voir" -> PredicateType.STATE
        "manger", "dormir", "aider", "marcher", "écrire" -> PredicateType.DYNAMIC
        else -> PredicateType.OTHER
    }

    private data class ParsedVerbPhrase(val verbSource: String, val verb: FrenchFallbackResolution, val lemma: String, val objectSource: String? = null, val objectPronoun: FrenchFallbackResolution? = null, val consumedWords: Int) {
        val output: List<String> get() = listOf(verb.saamaka) + listOfNotNull(objectPronoun?.saamaka)
        val lexicalSegments: List<Pair<String, FrenchFallbackResolution>> get() = listOf(verbSource to verb) + listOfNotNull(objectSource?.let { source -> objectPronoun?.let { source to it } })
    }

    private fun resolveVerbPhrase(words: List<String>): ParsedVerbPhrase? {
        resolveNaturalObjectClitic(words)?.let { parsed ->
            return ParsedVerbPhrase(parsed.verbSource, parsed.verb, FrenchVerbInflections.lemma(parsed.verbSource) ?: normalizeAttestedPhraseKey(parsed.verbSource), parsed.objectSource, parsed.objectPronoun, parsed.consumedWords)
        }
        val verbSource = words.firstOrNull() ?: return null
        val verb = resolveSafeVerb(verbSource) ?: return null
        return ParsedVerbPhrase(verbSource, verb, FrenchVerbInflections.lemma(verbSource) ?: normalizeAttestedPhraseKey(verbSource), consumedWords = 1)
    }

    private data class NaturalObjectClitic(val objectSource: String, val objectPronoun: FrenchFallbackResolution, val verbSource: String, val verb: FrenchFallbackResolution, val consumedWords: Int)

    private fun resolveNaturalObjectClitic(words: List<String>): NaturalObjectClitic? {
        if (words.isEmpty()) return null
        val first = words[0]
        val detachedPronoun = when (normalizeAttestedPhraseKey(first)) {
            "me" -> "moi"; "te" -> "toi"; "le", "la" -> "lui"; "nous" -> "nous"; "vous" -> "vous"; "les" -> "eux"; else -> null
        }
        if (detachedPronoun != null && words.size >= 2) {
            val verb = resolveSafeVerb(words[1]) ?: return null
            val pronoun = resolveAttestedComplementPronoun(detachedPronoun) ?: return null
            return NaturalObjectClitic(first, pronoun, words[1], verb, 2)
        }
        val attached = listOf("m'" to "moi", "t'" to "toi", "l'" to "lui").firstOrNull { (prefix, _) -> first.startsWith(prefix) && first.length > prefix.length } ?: return null
        val verbSource = first.removePrefix(attached.first)
        val verb = resolveSafeVerb(verbSource) ?: return null
        val pronoun = resolveAttestedComplementPronoun(attached.second) ?: return null
        return NaturalObjectClitic(attached.first, pronoun, verbSource, verb, 1)
    }

    private fun resolveComplements(words: List<String>, allowFrenchPartitive: Boolean = false): List<Pair<String, FrenchFallbackResolution>>? {
        if (words.isEmpty()) return emptyList()
        val resolved = mutableListOf<Pair<String, FrenchFallbackResolution>>()
        var index = 0
        while (index < words.size) {
            var match: Pair<String, FrenchFallbackResolution>? = null
            var consumed = 0
            if (normalizeAttestedPhraseKey(words[index]) == "pour" && index + 1 < words.size) {
                resolveAttestedStrongPronoun(words[index + 1])?.let { strong ->
                    val source = words.subList(index, index + 2).joinToString(" ")
                    match = source to strong.copy(requested = source, matchedFrench = source, saamaka = "fu ${strong.saamaka}")
                    consumed = 2
                }
            }
            if (match == null && index + 1 < words.size) {
                resolveAttestedPossessivePronoun(words[index])?.let { possessor ->
                    for (end in words.size downTo index + 2) {
                        val nounSource = words.subList(index + 1, end).joinToString(" ")
                        val noun = resolveExactLexeme(nounSource) ?: continue
                        val source = words.subList(index, end).joinToString(" ")
                        match = source to noun.copy(requested = source, matchedFrench = source, saamaka = "$possessor ${noun.saamaka}")
                        consumed = end - index
                        break
                    }
                }
            }
            if (match == null) {
                for (end in words.size downTo index + 1) {
                    val candidate = words.subList(index, end).joinToString(" ")
                    val resolution = resolveExact(candidate) ?: continue
                    match = candidate to resolution
                    consumed = end - index
                    break
                }
            }
            if (match == null && isFrenchNominalDeterminer(words[index]) && index + 1 < words.size) {
                for (end in words.size downTo index + 2) {
                    val lexical = words.subList(index + 1, end).joinToString(" ")
                    val resolution = resolveExact(lexical) ?: continue
                    match = words.subList(index, end).joinToString(" ") to resolution
                    consumed = end - index
                    break
                }
            }
            if (match == null && allowFrenchPartitive && normalizeAttestedPhraseKey(words[index]) == "de" && index + 2 < words.size && normalizeAttestedPhraseKey(words[index + 1]) in setOf("la", "le", "les")) {
                for (end in words.size downTo index + 3) {
                    val lexical = words.subList(index + 2, end).joinToString(" ")
                    val resolution = resolveExact(lexical) ?: continue
                    match = words.subList(index, end).joinToString(" ") to resolution
                    consumed = end - index
                    break
                }
            }
            resolved += match ?: return null
            index += consumed
        }
        return resolved
    }

    private fun isFrenchNominalDeterminer(word: String) = normalizeAttestedPhraseKey(word) in setOf("le", "la", "les", "un", "une", "des", "du")

    private fun resolveSafeVerb(form: String): FrenchFallbackResolution? {
        val lemma = FrenchVerbInflections.lemma(form) ?: normalizeAttestedPhraseKey(form)
        if (!FrenchVerbInflections.canComposeFromAttestedTranslation(lemma)) return null
        return resolveLemma(form, lemma)
    }

    private fun resolveLemma(form: String, lemma: String): FrenchFallbackResolution? {
        val normalized = normalizeAttestedPhraseKey(lemma)
        return resolveFrenchWord(form)?.takeIf { normalizeAttestedPhraseKey(it.matchedFrench) == normalized && it.kind in setOf(FrenchResolutionKind.EXACT, FrenchResolutionKind.INFLECTION) }
            ?: resolveFrenchWord(lemma)?.takeIf { normalizeAttestedPhraseKey(it.matchedFrench) == normalized && it.kind == FrenchResolutionKind.EXACT }
    }

    private fun resolveExactLexeme(word: String): FrenchFallbackResolution? = resolveFrenchWord(word)?.takeIf { it.kind == FrenchResolutionKind.EXACT && normalizeAttestedPhraseKey(it.matchedFrench) == normalizeAttestedPhraseKey(word) }
    private fun resolveExact(word: String): FrenchFallbackResolution? = resolveExactLexeme(word) ?: resolveAttestedComplementPronoun(word)

    private fun resolveAttestedComplementPronoun(word: String): FrenchFallbackResolution? {
        val saamaka = when (normalizeAttestedPhraseKey(word)) {
            "moi", "me" -> "mi"; "toi", "te" -> "i"; "lui", "elle", "le", "la" -> "ën"; "nous" -> "u"; "vous" -> "unu"; "eux", "elles", "les" -> "de"; else -> return null
        }
        return pronounResolution(word, saamaka)
    }

    private fun resolveAttestedStrongPronoun(word: String): FrenchFallbackResolution? {
        val saamaka = when (normalizeAttestedPhraseKey(word)) {
            "moi" -> "mí"; "toi" -> "í"; "lui", "elle" -> "hën"; "nous" -> "ú"; "vous" -> "únu"; "eux", "elles" -> "dé"; else -> return null
        }
        return pronounResolution(word, saamaka)
    }

    private fun resolveAttestedPossessivePronoun(word: String): String? = when (normalizeAttestedPhraseKey(word)) {
        "mon", "ma", "mes" -> "mi"; "ton", "ta", "tes" -> "i"; "son", "sa", "ses" -> "ën"; "notre", "nos" -> "u"; "votre", "vos" -> "unu"; "leur", "leurs" -> "de"; else -> null
    }

    private fun pronounResolution(word: String, saamaka: String) = FrenchFallbackResolution(word, word, saamaka, FrenchResolutionKind.EXACT, 1300, emptyList())

    private fun grammatical(sourceWords: List<String>, output: List<String>, lexicalSegments: List<Pair<String, FrenchFallbackResolution>>) = PhraseTranslationResult(
        translation = output.joinToString(" "),
        recognizedSegments = listOf(RecognizedPhraseSegment(sourceWords.first(), output.first(), matchedSource = sourceWords.first())) + lexicalSegments.map { (source, resolution) -> RecognizedPhraseSegment(source, resolution.saamaka, matchedSource = resolution.matchedFrench, alternatives = resolution.alternatives) },
        untranslatedSegments = emptyList(), isComplete = true, reliability = TranslationReliability.HIGH, kind = PhraseTranslationKind.GRAMMATICAL
    )

    private fun tokenizeFrench(text: String): List<String> = cleanPhraseInput(text).lowercase().replace(Regex("\\bj'"), "je ").replace(Regex("\\bn'"), "ne ").replace(Regex("\\s+"), " ").trim().split(' ').filter(String::isNotBlank)
}
