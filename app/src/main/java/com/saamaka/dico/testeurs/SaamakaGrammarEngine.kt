package com.saamaka.dico.testeurs

import com.saamaka.dico.testeurs.model.PhraseTranslationKind
import com.saamaka.dico.testeurs.model.PhraseTranslationResult
import com.saamaka.dico.testeurs.model.RecognizedPhraseSegment
import com.saamaka.dico.testeurs.model.TranslationReliability

/**
 * Grammar-line only.
 *
 * The first, lexical/word-by-word line is deliberately outside this class. Saamaka
 * lexical material still comes from [resolveFrenchWord]; only grammatical morphemes
 * explicitly attested in the project grammar are generated here.
 */
internal class SaamakaGrammarEngine(
    private val resolveFrenchWord: (String) -> FrenchFallbackResolution?
) {
    private enum class PredicateType { STATE, DYNAMIC, OTHER }
    private enum class CopularPredicateType { STATE, LOCATIVE, NOMINAL }

    /** Strict order: NEG + Time + Mode + Aspect. */
    private data class TamMarkers(
        val negative: Boolean = false,
        val past: Boolean = false,
        val future: Boolean = false,
        val potential: Boolean = false,
        val imperfective: Boolean = false
    ) {
        fun asSaamaka(): List<String> = buildList {
            if (negative) add("á")
            if (past) add("bi")
            if (future) add("o")
            if (potential) add("sa")
            if (imperfective) add("ta")
        }
    }

    private data class OrderedComplements(
        val core: List<Pair<String, FrenchFallbackResolution>> = emptyList(),
        val mannerOrMeans: List<Pair<String, FrenchFallbackResolution>> = emptyList(),
        val location: List<Pair<String, FrenchFallbackResolution>> = emptyList(),
        val time: List<Pair<String, FrenchFallbackResolution>> = emptyList()
    ) {
        val all: List<Pair<String, FrenchFallbackResolution>>
            get() = core + mannerOrMeans + location + time

        val output: List<String>
            get() = all.map { it.second.saamaka }
    }

    private data class PossessivePrefix(
        val tokens: List<String>,
        val pluralPossessed: Boolean
    )

    fun translate(text: String): PhraseTranslationResult? {
        val clean = normalizeSurface(text)
        if (clean.isBlank()) return null

        translateQuestion(clean)?.let { return it }
        translateComplexSentence(clean)?.let { return it }
        translatePresenter(clean)?.let { return it }
        return translateSimpleClause(clean)
    }

    // -------------------------------------------------------------------------
    // Questions
    // -------------------------------------------------------------------------

    private fun translateQuestion(text: String): PhraseTranslationResult? {
        val trimmed = text.trim()
        if (!trimmed.endsWith("?")) return null

        val withoutQuestionMark = trimmed.removeSuffix("?").trim()
        val lower = normalizeAttestedPhraseKey(withoutQuestionMark)

        if (lower == "qui est la" || lower == "qui est là") {
            return syntheticGrammar(trimmed, "Ambé dɛ aálá ?")
        }
        if (lower == "qu'est-ce que c'est" || lower == "qu est ce que c est") {
            return syntheticGrammar(trimmed, "Andí da dí sã akí ?")
        }
        if (lower == "comment vas-tu" || lower == "comment vas tu") {
            return syntheticGrammar(trimmed, "Unfá i dɛ ?")
        }

        val estCeQue = Regex("^est[- ]ce que\\s+", RegexOption.IGNORE_CASE)
        if (estCeQue.containsMatchIn(withoutQuestionMark)) {
            val declarative = withoutQuestionMark.replace(estCeQue, "").trim()
            val translated = translateClauseWithoutQuestion(declarative) ?: return null
            return translated.copy(translation = "${translated.translation} ?")
        }

        val prefix = questionPrefix(lower)
        if (prefix != null) {
            val (frenchPrefix, saamakaPrefix) = prefix
            val remainder = withoutQuestionMark.drop(frenchPrefix.length).trim()
            if (remainder.isBlank()) return null
            val normalizedRemainder = normalizeFrenchInversion(remainder)
            val translated = translateClauseWithoutQuestion(normalizedRemainder) ?: return null
            return translated.copy(translation = "$saamakaPrefix ${translated.translation} ?")
        }

        val translated = translateClauseWithoutQuestion(normalizeFrenchInversion(withoutQuestionMark)) ?: return null
        return translated.copy(translation = "${translated.translation} ?")
    }

    private fun questionPrefix(normalized: String): Pair<String, String>? = when {
        normalized.startsWith("qu'est-ce que ") -> "qu'est-ce que" to "Andí"
        normalized.startsWith("qu est ce que ") -> "qu est ce que" to "Andí"
        normalized.startsWith("qu'") -> "qu'" to "Andí"
        normalized.startsWith("quoi ") -> "quoi" to "Andí"
        normalized.startsWith("que ") -> "que" to "Andí"
        normalized.startsWith("ou ") || normalized.startsWith("où ") -> normalized.substringBefore(' ') to "Ún kamía"
        normalized.startsWith("pourquoi ") -> "pourquoi" to "Fa andí"
        normalized.startsWith("comment ") -> "comment" to "Unfá"
        normalized.startsWith("quand ") -> "quand" to "Unte"
        normalized.startsWith("qui ") -> "qui" to "Ambé"
        else -> null
    }

    private fun normalizeFrenchInversion(text: String): String {
        val clean = text.trim()
        Regex("^([\\p{L}’']+)-t-(il|elle)(.*)$", RegexOption.IGNORE_CASE).matchEntire(clean)?.let {
            return "${it.groupValues[2]} ${it.groupValues[1]}${it.groupValues[3]}".trim()
        }
        Regex("^([\\p{L}’']+)-(tu|il|elle|nous|vous|ils|elles)(.*)$", RegexOption.IGNORE_CASE).matchEntire(clean)?.let {
            return "${it.groupValues[2]} ${it.groupValues[1]}${it.groupValues[3]}".trim()
        }
        return clean
    }

    // -------------------------------------------------------------------------
    // Complex clauses / subordination
    // -------------------------------------------------------------------------

    private fun translateComplexSentence(text: String): PhraseTranslationResult? {
        val clean = text.trim().removeSuffix(".").trim()

        splitOnce(clean, " parce que ")?.let { (main, subordinate) ->
            val left = translateClauseWithoutQuestion(main) ?: return null
            val right = translateClauseWithoutQuestion(subordinate) ?: return null
            return combineGrammar(clean, "${left.translation} bika ${right.translation}", left, right)
        }

        if (normalizeAttestedPhraseKey(clean).startsWith("quand ")) {
            val body = clean.substringAfter(' ').trim()
            splitAtComma(body)?.let { (subordinate, main) ->
                val left = translateClauseWithoutQuestion(subordinate) ?: return null
                val right = translateClauseWithoutQuestion(main) ?: return null
                return combineGrammar(clean, "Te ${left.translation}, ${right.translation}", left, right)
            }
        }

        if (normalizeAttestedPhraseKey(clean).startsWith("si ")) {
            val body = clean.substringAfter(' ').trim()
            splitAtComma(body)?.let { (condition, main) ->
                val left = translateClauseWithoutQuestion(condition) ?: return null
                val right = translateClauseWithoutQuestion(main) ?: return null
                return combineGrammar(clean, "Ee ${left.translation}, ${right.translation}", left, right)
            }
        }

        val queIndex = indexOfWordSequence(clean, " que ")
        if (queIndex >= 0) {
            val main = clean.substring(0, queIndex).trim()
            val subordinate = clean.substring(queIndex + 5).trim()
            val matrixLemma = matrixVerbLemma(main)
            if (matrixLemma in FACTUAL_COMPLEMENT_LEMMAS) {
                val left = translateSimpleClause(main) ?: return null
                val right = translateClauseWithoutQuestion(subordinate) ?: return null
                return combineGrammar(clean, "${left.translation} táa ${right.translation}", left, right)
            }
            if (matrixLemma in VOLITIVE_COMPLEMENT_LEMMAS) {
                val left = translateMatrixClause(main) ?: return null
                val right = translateBareClause(subordinate) ?: return null
                return combineGrammar(clean, "${left.translation} fu ${right.translation}", left, right)
            }
        }

        translateRelative(clean)?.let { return it }
        return null
    }

    private fun translateRelative(text: String): PhraseTranslationResult? {
        val qui = indexOfWordSequence(text, " qui ")
        val que = indexOfWordSequence(text, " que ")
        val index = listOf(qui, que).filter { it >= 0 }.minOrNull() ?: return null
        val connector = if (index == qui) "qui" else "que"
        val headText = text.substring(0, index).trim()
        val relativeText = text.substring(index + connector.length + 2).trim()
        val head = resolveNominalPhrase(tokenizeFrench(headText)) ?: return null

        val relative = if (connector == "qui") {
            translateRelativeSubjectClause(relativeText)
        } else {
            translateClauseWithoutQuestion(relativeText)
        } ?: return null

        return syntheticGrammar(text, "${head.saamaka} di ${relative.translation}")
    }

    private fun translateRelativeSubjectClause(text: String): PhraseTranslationResult? {
        val words = tokenizeFrench(text)
        if (words.isEmpty()) return null
        val parsed = resolveVerbPhrase(words) ?: return null
        val ordered = resolveOrderedComplements(words.drop(parsed.consumedWords), parsed.lemma == "manger") ?: return null
        val markers = tamForVerb(parsed.verbSource, parsed.lemma) ?: return null
        return syntheticGrammar(text, (markers.asSaamaka() + parsed.output + ordered.output).joinToString(" "))
    }

    private fun translateMatrixClause(text: String): PhraseTranslationResult? {
        val words = tokenizeFrench(text)
        if (words.size != 2) return translateSimpleClause(text)
        val subject = resolveAttestedFrenchSubject(words[0]) ?: return null
        val lemma = grammarLemma(words[1]) ?: return null
        val verb = resolveGrammarVerb(words[1], lemma) ?: return null
        val markers = tamForVerb(words[1], lemma) ?: TamMarkers()
        return grammatical(words, listOf(subject) + markers.asSaamaka() + verb.saamaka, listOf(words[1] to verb))
    }

    /** Volitive complements use a bare subordinate verb. */
    private fun translateBareClause(text: String): PhraseTranslationResult? {
        val words = tokenizeFrench(text)
        if (words.size < 2) return null
        val subject = resolveAttestedFrenchSubject(words[0]) ?: return null
        val parsed = resolveVerbPhrase(words.drop(1)) ?: return null
        val ordered = resolveOrderedComplements(words.drop(1 + parsed.consumedWords), parsed.lemma == "manger") ?: return null
        return grammatical(words, listOf(subject) + parsed.output + ordered.output, parsed.lexicalSegments + ordered.all)
    }

    private fun matrixVerbLemma(text: String): String? {
        val words = tokenizeFrench(text)
        if (words.size < 2 || resolveAttestedFrenchSubject(words[0]) == null) return null
        return grammarLemma(words[1])
    }

    // -------------------------------------------------------------------------
    // Presenter "c'est"
    // -------------------------------------------------------------------------

    private fun translatePresenter(text: String): PhraseTranslationResult? {
        val normalized = normalizeAttestedPhraseKey(text)
        val patterns = listOf(
            "ce n'est pas " to Pair(listOf("á", "dɛ"), "ce n'est pas"),
            "ce n etait pas " to Pair(listOf("á", "bi", "dɛ"), "ce n'était pas"),
            "ce n'était pas " to Pair(listOf("á", "bi", "dɛ"), "ce n'était pas"),
            "c'etait " to Pair(listOf("a", "bi", "dɛ"), "c'était"),
            "c'était " to Pair(listOf("a", "bi", "dɛ"), "c'était"),
            "ce sera " to Pair(listOf("a", "o", "dɛ"), "ce sera"),
            "c'est " to Pair(listOf("da"), "c'est")
        )
        val match = patterns.firstOrNull { normalized.startsWith(normalizeAttestedPhraseKey(it.first)) } ?: return null
        val sourcePrefix = match.second.second
        val predicateText = text.drop(sourcePrefix.length).trim()
        val nominal = resolveNominalPhrase(tokenizeFrench(predicateText)) ?: return null
        return syntheticGrammar(text, (match.second.first + nominal.saamaka).joinToString(" "))
    }

    // -------------------------------------------------------------------------
    // Simple clause
    // -------------------------------------------------------------------------

    private fun translateClauseWithoutQuestion(text: String): PhraseTranslationResult? =
        translateComplexSentence(text) ?: translatePresenter(text) ?: translateSimpleClause(text)

    private fun translateSimpleClause(text: String): PhraseTranslationResult? {
        val words = tokenizeFrench(text)
        if (words.size < 2) return null

        // French may front a time adverb; Saamaka canonical order puts it last.
        resolveTimeAdverb(words.first())?.let { time ->
            val restWords = words.drop(1)
            val rest = restWords.joinToString(" ")
            val translated = translateSimpleClause(rest) ?: return null
            val subject = restWords.firstOrNull()?.let(::resolveAttestedFrenchSubject)
            val futureMotion = normalizeAttestedPhraseKey(words.first()) == "demain" &&
                restWords.size >= 2 && grammarLemma(restWords[1]) == "aller" &&
                grammarTense(restWords[1]) == FrenchVerbTense.PRESENT && subject != null
            val adjusted = if (futureMotion) {
                translated.translation.replaceFirst("$subject ta ", "$subject o ")
            } else translated.translation
            return translated.copy(translation = "$adjusted ${time.saamaka}")
        }

        val subject = resolveAttestedFrenchSubject(words.first()) ?: return null
        return translateProgressive(words, subject)
            ?: translateNegation(words, subject)
            ?: translateCopular(words, subject)
            ?: translatePotential(words, subject)
            ?: translateNearFuture(words, subject)
            ?: translateVouloir(words, subject)
            ?: translateReflexive(words, subject)
            ?: translateDoubleObject(words, subject)
            ?: translateSafeAction(words, subject)
    }

    private fun translateProgressive(words: List<String>, subject: String): PhraseTranslationResult? {
        if (words.size < 6 ||
            grammarLemma(words[1]) != "être" ||
            grammarTense(words[1]) != FrenchVerbTense.PRESENT ||
            words.subList(2, 5).map(::normalizeAttestedPhraseKey) != listOf("en", "train", "de")
        ) return null
        val tail = words.drop(5)
        val parsed = resolveVerbPhrase(tail) ?: return null
        val ordered = resolveOrderedComplements(tail.drop(parsed.consumedWords), parsed.lemma == "manger") ?: return null
        val markers = TamMarkers(imperfective = true)
        return grammatical(words, listOf(subject) + markers.asSaamaka() + parsed.output + ordered.output, parsed.lexicalSegments + ordered.all)
    }

    private fun translateNearFuture(words: List<String>, subject: String): PhraseTranslationResult? {
        if (words.size < 3 || grammarLemma(words[1]) != "aller" || grammarTense(words[1]) != FrenchVerbTense.PRESENT) return null
        val tail = words.drop(2)
        val parsed = resolveVerbPhrase(tail) ?: return null
        val ordered = resolveOrderedComplements(tail.drop(parsed.consumedWords), parsed.lemma == "manger") ?: return null
        return grammatical(words, listOf(subject, "o") + parsed.output + ordered.output, parsed.lexicalSegments + ordered.all)
    }

    private fun translatePotential(words: List<String>, subject: String): PhraseTranslationResult? {
        if (words.size < 3 || grammarLemma(words[1]) != "pouvoir" || grammarTense(words[1]) != FrenchVerbTense.PRESENT) return null
        val tail = words.drop(2)
        val parsed = resolveVerbPhrase(tail) ?: return null
        val ordered = resolveOrderedComplements(tail.drop(parsed.consumedWords), parsed.lemma == "manger") ?: return null
        return grammatical(words, listOf(subject, "sa") + parsed.output + ordered.output, parsed.lexicalSegments + ordered.all)
    }

    private fun translateVouloir(words: List<String>, subject: String): PhraseTranslationResult? {
        if (words.size < 3 || grammarLemma(words[1]) != "vouloir" || grammarTense(words[1]) != FrenchVerbTense.PRESENT) return null
        val vouloir = resolveGrammarVerb(words[1], "vouloir") ?: return null
        val tail = words.drop(2)
        val parsed = resolveVerbPhrase(tail) ?: return null
        val ordered = resolveOrderedComplements(tail.drop(parsed.consumedWords), parsed.lemma == "manger") ?: return null
        return grammatical(words, listOf(subject, vouloir.saamaka) + parsed.output + ordered.output, listOf(words[1] to vouloir) + parsed.lexicalSegments + ordered.all)
    }

    // -------------------------------------------------------------------------
    // Three-way French "être": state / location / equation
    // -------------------------------------------------------------------------

    private fun translateCopular(words: List<String>, subject: String): PhraseTranslationResult? {
        if (words.size < 3 || grammarLemma(words[1]) != "être") return null
        val predicateWords = words.drop(2)
        val type = classifyCopularPredicate(predicateWords)
        val tense = grammarTense(words[1]) ?: return null

        return when (type) {
            CopularPredicateType.STATE -> {
                val state = resolveStatePredicate(predicateWords) ?: return null
                val markers = when (tense) {
                    FrenchVerbTense.PRESENT -> TamMarkers()
                    FrenchVerbTense.PAST -> TamMarkers(past = true)
                    FrenchVerbTense.FUTURE -> TamMarkers(future = true)
                    else -> return null
                }
                grammatical(words, listOf(subject) + markers.asSaamaka() + state.saamaka, listOf(predicateWords.joinToString(" ") to state))
            }
            CopularPredicateType.LOCATIVE -> {
                val location = resolveLocativePredicate(predicateWords) ?: return null
                val markers = when (tense) {
                    FrenchVerbTense.PRESENT -> TamMarkers()
                    FrenchVerbTense.PAST -> TamMarkers(past = true)
                    FrenchVerbTense.FUTURE -> TamMarkers(future = true)
                    else -> return null
                }
                grammatical(words, listOf(subject) + markers.asSaamaka() + listOf("dɛ", location.saamaka), listOf(predicateWords.joinToString(" ") to location))
            }
            CopularPredicateType.NOMINAL -> {
                val nominal = resolveNominalPhrase(predicateWords) ?: return null
                when (tense) {
                    FrenchVerbTense.PRESENT -> grammatical(words, listOf(subject, "da", nominal.saamaka), listOf(predicateWords.joinToString(" ") to nominal))
                    FrenchVerbTense.PAST -> grammatical(words, listOf(subject, "bi", "dɛ", nominal.saamaka), listOf(predicateWords.joinToString(" ") to nominal))
                    FrenchVerbTense.FUTURE -> grammatical(words, listOf(subject, "o", "dɛ", nominal.saamaka), listOf(predicateWords.joinToString(" ") to nominal))
                    else -> null
                }
            }
        }
    }

    private fun classifyCopularPredicate(words: List<String>): CopularPredicateType {
        if (words.isEmpty()) return CopularPredicateType.STATE
        val first = normalizeAttestedPhraseKey(words.first())
        if (first in LOCATIVE_FRENCH_PREPOSITIONS) return CopularPredicateType.LOCATIVE
        if (first in NOMINAL_DETERMINERS || possessivePrefix(first) != null || first in DEMONSTRATIVE_DETERMINERS) {
            return CopularPredicateType.NOMINAL
        }
        return CopularPredicateType.STATE
    }

    private fun resolveStatePredicate(words: List<String>): FrenchFallbackResolution? {
        if (words.size != 1) return null
        return resolveNominalLexeme(words.first())
    }

    private fun resolveLocativePredicate(words: List<String>): FrenchFallbackResolution? {
        if (words.size < 2) return null
        val prep = normalizeAttestedPhraseKey(words.first())
        if (prep !in LOCATIVE_FRENCH_PREPOSITIONS) return null
        val body = words.drop(1).toMutableList()
        if (prep == "au") body.add(0, "le")
        if (prep == "aux") body.add(0, "les")

        val nominal = if (body.map(::normalizeAttestedPhraseKey) == listOf("la", "maison")) {
            resolveNominalLexeme("maison")
        } else {
            resolveNominalPhrase(body)
        } ?: return null
        return syntheticResolution(words.joinToString(" "), "a ${nominal.saamaka}")
    }

    // -------------------------------------------------------------------------
    // Negation: á + bi + o + sa + ta + predicate
    // -------------------------------------------------------------------------

    private fun translateNegation(words: List<String>, subject: String): PhraseTranslationResult? {
        if (words.size < 4 || normalizeAttestedPhraseKey(words[1]) != "ne") return null
        val pasIndex = (2 until words.size).firstOrNull { normalizeAttestedPhraseKey(words[it]) == "pas" } ?: return null
        if (pasIndex < 3) return null
        val beforePas = words.subList(2, pasIndex)
        val afterPas = words.drop(pasIndex + 1)

        if (beforePas.size == 1 && grammarLemma(beforePas[0]) == "être") {
            val predicateType = classifyCopularPredicate(afterPas)
            val tense = grammarTense(beforePas[0]) ?: return null
            val baseMarkers = when (tense) {
                FrenchVerbTense.PRESENT -> TamMarkers(negative = true)
                FrenchVerbTense.PAST -> TamMarkers(negative = true, past = true)
                FrenchVerbTense.FUTURE -> TamMarkers(negative = true, future = true)
                else -> return null
            }
            return when (predicateType) {
                CopularPredicateType.STATE -> {
                    val state = resolveStatePredicate(afterPas) ?: return null
                    grammatical(words, listOf(subject) + baseMarkers.asSaamaka() + state.saamaka, listOf(afterPas.joinToString(" ") to state))
                }
                CopularPredicateType.LOCATIVE -> {
                    val location = resolveLocativePredicate(afterPas) ?: return null
                    grammatical(words, listOf(subject) + baseMarkers.asSaamaka() + listOf("dɛ", location.saamaka), listOf(afterPas.joinToString(" ") to location))
                }
                CopularPredicateType.NOMINAL -> {
                    val nominal = resolveNominalPhrase(afterPas) ?: return null
                    grammatical(words, listOf(subject) + baseMarkers.asSaamaka() + listOf("dɛ", nominal.saamaka), listOf(afterPas.joinToString(" ") to nominal))
                }
            }
        }

        if (beforePas.size == 1 && grammarLemma(beforePas[0]) == "pouvoir") {
            val parsed = resolveVerbPhrase(afterPas) ?: return null
            val ordered = resolveOrderedComplements(afterPas.drop(parsed.consumedWords), parsed.lemma == "manger") ?: return null
            return grammatical(words, listOf(subject, "á", "sa") + parsed.output + ordered.output, parsed.lexicalSegments + ordered.all)
        }

        if (beforePas.size == 1 && grammarLemma(beforePas[0]) == "vouloir") {
            val vouloir = resolveGrammarVerb(beforePas[0], "vouloir") ?: return null
            val parsed = resolveVerbPhrase(afterPas) ?: return null
            val ordered = resolveOrderedComplements(afterPas.drop(parsed.consumedWords), parsed.lemma == "manger") ?: return null
            return grammatical(words, listOf(subject, "á", vouloir.saamaka) + parsed.output + ordered.output, listOf(beforePas[0] to vouloir) + parsed.lexicalSegments + ordered.all)
        }

        val parsed = resolveVerbPhrase(beforePas) ?: return null
        if (parsed.consumedWords != beforePas.size) return null
        val markers = tamForVerb(parsed.verbSource, parsed.lemma, negative = true) ?: return null
        val ordered = resolveOrderedComplements(afterPas, parsed.lemma == "manger") ?: return null
        return grammatical(words, listOf(subject) + markers.asSaamaka() + parsed.output + ordered.output, parsed.lexicalSegments + ordered.all)
    }

    // -------------------------------------------------------------------------
    // Reflexive / reciprocal
    // -------------------------------------------------------------------------

    private fun translateReflexive(words: List<String>, subject: String): PhraseTranslationResult? {
        if (words.size < 3) return null
        val clitic = normalizeAttestedPhraseKey(words[1])
        if (clitic !in REFLEXIVE_CLITICS) return null
        val expectedObject = subjectObjectPronoun(words[0]) ?: return null
        if (!reflexiveCliticMatchesSubject(clitic, words[0])) return null
        val verbSource = words[2]
        val lemma = grammarLemma(verbSource) ?: return null
        val verb = resolveGrammarVerb(verbSource, lemma) ?: return null
        val markers = tamForVerb(verbSource, lemma, forceImperfectiveForPresent = lemma in RECIPROCAL_LEMMAS && subject in setOf("u", "unu", "de")) ?: return null
        val ordered = resolveOrderedComplements(words.drop(3), lemma == "manger") ?: return null

        val reflexiveObject = when {
            lemma in PHYSICAL_REFLEXIVE_LEMMAS -> "$expectedObject sinkii"
            lemma in RECIPROCAL_LEMMAS && subject in setOf("u", "unu", "de") -> "di ún ku di ún"
            else -> "$expectedObject seéi"
        }
        val reflexive = syntheticResolution(words[1], reflexiveObject)
        return grammatical(
            words,
            listOf(subject) + markers.asSaamaka() + verb.saamaka + reflexiveObject + ordered.output,
            listOf(verbSource to verb, words[1] to reflexive) + ordered.all
        )
    }

    // -------------------------------------------------------------------------
    // Double object: S + V + indirect beneficiary + direct object
    // -------------------------------------------------------------------------

    private fun translateDoubleObject(words: List<String>, subject: String): PhraseTranslationResult? {
        if (words.size < 5) return null
        val lemma = grammarLemma(words[1]) ?: return null
        if (lemma !in DOUBLE_OBJECT_LEMMAS) return null
        val separator = (2 until words.size).firstOrNull { normalizeAttestedPhraseKey(words[it]) == "a" || words[it] == "à" } ?: return null
        if (separator <= 2 || separator >= words.lastIndex) return null
        val verb = resolveGrammarVerb(words[1], lemma) ?: return null
        val direct = resolveNominalPhrase(words.subList(2, separator)) ?: return null
        val indirectWords = words.drop(separator + 1)
        val indirect = resolveObjectOrNominal(indirectWords) ?: return null
        val markers = tamForVerb(words[1], lemma) ?: return null
        return grammatical(
            words,
            listOf(subject) + markers.asSaamaka() + listOf(verb.saamaka, indirect.saamaka, direct.saamaka),
            listOf(words[1] to verb, indirectWords.joinToString(" ") to indirect, words.subList(2, separator).joinToString(" ") to direct)
        )
    }

    // -------------------------------------------------------------------------
    // Ordinary SVO action + canonical peripheral order
    // -------------------------------------------------------------------------

    private fun translateSafeAction(words: List<String>, subject: String): PhraseTranslationResult? {
        val tail = words.drop(1)
        val parsed = resolveVerbPhrase(tail) ?: return null
        val ordered = resolveOrderedComplements(tail.drop(parsed.consumedWords), parsed.lemma == "manger") ?: return null
        var markers = tamForVerb(parsed.verbSource, parsed.lemma) ?: return null
        if (parsed.lemma == "aller" && ordered.time.any { normalizeAttestedPhraseKey(it.first) == "demain" }) {
            markers = TamMarkers(future = true)
        }
        return grammatical(words, listOf(subject) + markers.asSaamaka() + parsed.output + ordered.output, parsed.lexicalSegments + ordered.all)
    }

    private fun tamForVerb(
        verbSource: String,
        lemma: String,
        negative: Boolean = false,
        forceImperfectiveForPresent: Boolean = false
    ): TamMarkers? {
        val type = predicateType(lemma)
        return when (grammarTense(verbSource)) {
            FrenchVerbTense.FUTURE -> TamMarkers(negative = negative, future = true)
            FrenchVerbTense.PRESENT -> when {
                forceImperfectiveForPresent -> TamMarkers(negative = negative, imperfective = true)
                type == PredicateType.STATE -> TamMarkers(negative = negative)
                type == PredicateType.DYNAMIC -> TamMarkers(negative = negative, imperfective = true)
                else -> null
            }
            FrenchVerbTense.PAST -> when (type) {
                PredicateType.STATE -> TamMarkers(negative = negative, past = true)
                PredicateType.DYNAMIC -> TamMarkers(negative = negative, past = true, imperfective = true)
                PredicateType.OTHER -> null
            }
            else -> null
        }
    }

    private fun predicateType(lemma: String): PredicateType = when (normalizeAttestedPhraseKey(lemma)) {
        "aimer", "voir", "savoir", "connaitre", "connaître", "croire", "penser" -> PredicateType.STATE
        in GRAMMAR_DYNAMIC_LEMMAS -> PredicateType.DYNAMIC
        else -> PredicateType.OTHER
    }

    // -------------------------------------------------------------------------
    // Verb phrase and personal pronouns
    // -------------------------------------------------------------------------

    private data class ParsedVerbPhrase(
        val verbSource: String,
        val verb: FrenchFallbackResolution,
        val lemma: String,
        val objectSource: String? = null,
        val objectPronoun: FrenchFallbackResolution? = null,
        val consumedWords: Int
    ) {
        val output: List<String> get() = listOf(verb.saamaka) + listOfNotNull(objectPronoun?.saamaka)
        val lexicalSegments: List<Pair<String, FrenchFallbackResolution>>
            get() = listOf(verbSource to verb) + listOfNotNull(objectSource?.let { source -> objectPronoun?.let { source to it } })
    }

    private fun resolveVerbPhrase(words: List<String>): ParsedVerbPhrase? {
        resolveNaturalObjectClitic(words)?.let { parsed ->
            return ParsedVerbPhrase(
                parsed.verbSource,
                parsed.verb,
                grammarLemma(parsed.verbSource) ?: normalizeAttestedPhraseKey(parsed.verbSource),
                parsed.objectSource,
                parsed.objectPronoun,
                parsed.consumedWords
            )
        }
        val verbSource = words.firstOrNull() ?: return null
        val lemma = grammarLemma(verbSource) ?: return null
        val verb = resolveGrammarVerb(verbSource, lemma) ?: return null
        return ParsedVerbPhrase(verbSource, verb, lemma, consumedWords = 1)
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
        val detachedPronoun = when (normalizeAttestedPhraseKey(first)) {
            "me" -> "moi"
            "te" -> "toi"
            "le", "la" -> "lui"
            "nous" -> "nous"
            "vous" -> "vous"
            "les" -> "eux"
            else -> null
        }
        if (detachedPronoun != null && words.size >= 2) {
            val lemma = grammarLemma(words[1]) ?: return null
            val verb = resolveGrammarVerb(words[1], lemma) ?: return null
            val pronoun = resolveAttestedObjectPronoun(detachedPronoun) ?: return null
            return NaturalObjectClitic(first, pronoun, words[1], verb, 2)
        }

        val attached = listOf("m'" to "moi", "t'" to "toi", "l'" to "lui")
            .firstOrNull { (prefix, _) -> first.startsWith(prefix) && first.length > prefix.length }
            ?: return null
        val verbSource = first.removePrefix(attached.first)
        val lemma = grammarLemma(verbSource) ?: return null
        val verb = resolveGrammarVerb(verbSource, lemma) ?: return null
        val pronoun = resolveAttestedObjectPronoun(attached.second) ?: return null
        return NaturalObjectClitic(attached.first, pronoun, verbSource, verb, 1)
    }

    private fun resolveAttestedObjectPronoun(word: String): FrenchFallbackResolution? {
        val saamaka = when (normalizeAttestedPhraseKey(word)) {
            "je", "me", "moi" -> "mi"
            "tu", "te", "toi" -> "i"
            "il", "elle", "lui", "le", "la" -> "ɛn"
            "nous" -> "u"
            "vous" -> "unu"
            "ils", "elles", "eux", "les" -> "de"
            else -> return null
        }
        return pronounResolution(word, saamaka)
    }

    private fun resolveAttestedStrongPronoun(word: String): FrenchFallbackResolution? {
        val saamaka = when (normalizeAttestedPhraseKey(word)) {
            "moi" -> "mií"
            "toi" -> "ií"
            "lui", "elle" -> "hɛ̃́"
            "nous" -> "uú"
            "vous" -> "unú"
            "eux", "elles" -> "déé"
            else -> return null
        }
        return pronounResolution(word, saamaka)
    }

    private fun subjectObjectPronoun(subject: String): String? = when (normalizeAttestedPhraseKey(subject)) {
        "je" -> "mi"
        "tu" -> "i"
        "il", "elle" -> "ɛn"
        "nous" -> "u"
        "vous" -> "unu"
        "ils", "elles" -> "de"
        else -> null
    }

    private fun reflexiveCliticMatchesSubject(clitic: String, subject: String): Boolean = when (normalizeAttestedPhraseKey(subject)) {
        "je" -> clitic == "me"
        "tu" -> clitic == "te"
        "il", "elle" -> clitic == "se"
        "nous" -> clitic == "nous"
        "vous" -> clitic == "vous"
        "ils", "elles" -> clitic == "se"
        else -> false
    }

    // -------------------------------------------------------------------------
    // Canonical complement order: OI > OD > manner/means > location > time
    // -------------------------------------------------------------------------

    private fun resolveOrderedComplements(words: List<String>, allowFrenchPartitive: Boolean = false): OrderedComplements? {
        if (words.isEmpty()) return OrderedComplements()
        val remaining = words.toMutableList()
        val time = mutableListOf<Pair<String, FrenchFallbackResolution>>()
        val mannerOrMeans = mutableListOf<Pair<String, FrenchFallbackResolution>>()
        val location = mutableListOf<Pair<String, FrenchFallbackResolution>>()

        if (remaining.isNotEmpty()) {
            resolveTimeAdverb(remaining.last())?.let {
                time += remaining.last() to it
                remaining.removeAt(remaining.lastIndex)
            }
        }
        if (remaining.isNotEmpty()) {
            resolveMannerAdverb(remaining.last())?.let {
                mannerOrMeans += remaining.last() to it
                remaining.removeAt(remaining.lastIndex)
            }
        }

        val snapshot = remaining.toList()
        val withIndex = snapshot.indexOfFirst { normalizeAttestedPhraseKey(it) == "avec" }
        val locativeIndex = snapshot.indexOfFirst { normalizeAttestedPhraseKey(it) in LOCATIVE_FRENCH_PREPOSITIONS }
        val peripheralIndexes = listOf(withIndex, locativeIndex).filter { it >= 0 }
        val coreEnd = peripheralIndexes.minOrNull() ?: snapshot.size

        if (withIndex >= 0) {
            val nextBoundary = listOf(locativeIndex).filter { it > withIndex }.minOrNull() ?: snapshot.size
            val body = snapshot.subList(withIndex + 1, nextBoundary)
            if (body.isEmpty()) return null
            val value = resolveObjectOrNominal(body) ?: return null
            val source = snapshot.subList(withIndex, nextBoundary).joinToString(" ")
            mannerOrMeans += source to syntheticResolution(source, "ku ${value.saamaka}")
        }

        if (locativeIndex >= 0) {
            val nextBoundary = listOf(withIndex).filter { it > locativeIndex }.minOrNull() ?: snapshot.size
            val locativeWords = snapshot.subList(locativeIndex, nextBoundary)
            val value = resolveLocativePredicate(locativeWords) ?: return null
            val source = locativeWords.joinToString(" ")
            location += source to value
        }

        val coreWords = snapshot.take(coreEnd)
        val core = resolveComplements(coreWords, allowFrenchPartitive) ?: return null
        return OrderedComplements(core, mannerOrMeans, location, time)
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
                    match = source to syntheticResolution(source, "fu ${strong.saamaka}")
                    consumed = 2
                }
            }

            if (match == null) {
                for (end in words.size downTo index + 1) {
                    val slice = words.subList(index, end)
                    val nominal = resolveNominalPhrase(slice) ?: continue
                    match = slice.joinToString(" ") to nominal
                    consumed = end - index
                    break
                }
            }

            if (match == null && allowFrenchPartitive) {
                val normalized = normalizeAttestedPhraseKey(words[index])
                if (normalized == "du" || normalized == "des") {
                    for (end in words.size downTo index + 2) {
                        val noun = resolveNominalPhrase(words.subList(index + 1, end)) ?: continue
                        val source = words.subList(index, end).joinToString(" ")
                        match = source to syntheticResolution(source, noun.saamaka)
                        consumed = end - index
                        break
                    }
                } else if (normalized == "de" && index + 2 < words.size && normalizeAttestedPhraseKey(words[index + 1]) in setOf("la", "le", "les")) {
                    for (end in words.size downTo index + 3) {
                        val noun = resolveNominalPhrase(words.subList(index + 2, end)) ?: continue
                        val source = words.subList(index, end).joinToString(" ")
                        match = source to syntheticResolution(source, noun.saamaka)
                        consumed = end - index
                        break
                    }
                }
            }

            val found = match ?: return null
            resolved += found
            index += consumed
        }
        return resolved
    }

    // -------------------------------------------------------------------------
    // Nominal groups
    // [Article/Possessive] + [Number/Quantity] + [Adjective] + N + [akí/álá]
    // -------------------------------------------------------------------------

    private fun resolveNominalPhrase(inputWords: List<String>): FrenchFallbackResolution? {
        if (inputWords.isEmpty()) return null
        val words = inputWords.toMutableList()
        var suffix: String? = null

        if (words.isNotEmpty()) {
            val last = normalizeAttestedPhraseKey(words.last())
            if (last in setOf("ci", "ici")) {
                suffix = "akí"
                words.removeAt(words.lastIndex)
            } else if (last in setOf("la", "là", "la-bas", "là-bas")) {
                suffix = "álá"
                words.removeAt(words.lastIndex)
            }
        }
        if (words.isEmpty()) return null

        val prefix = mutableListOf<String>()
        var index = 0
        val first = normalizeAttestedPhraseKey(words[0])

        possessivePrefix(first)?.let { possessive ->
            prefix += possessive.tokens
            if (possessive.pluralPossessed) prefix += "dée"
            index++
        } ?: run {
            when (first) {
                "le", "la" -> { prefix += "dí"; index++ }
                "les" -> { prefix += "dée"; index++ }
                "un", "une" -> { prefix += "wán"; index++ }
                "des" -> index++
                "du" -> index++
                "ce", "cet", "cette" -> {
                    if (suffix == null) return null
                    prefix += "dí"; index++
                }
                "ces" -> {
                    if (suffix == null) return null
                    prefix += "dée"; index++
                }
            }
        }

        val quantity = mutableListOf<String>()
        if (index < words.size) {
            when (normalizeAttestedPhraseKey(words[index])) {
                "deux" -> { quantity += "tuu"; index++ }
                "trois" -> { quantity += "drii"; index++ }
                "beaucoup" -> {
                    quantity += "hánti"
                    index++
                    if (index < words.size && normalizeAttestedPhraseKey(words[index]) == "de") index++
                }
            }
        }

        if (index >= words.size) return null
        val lexicalWords = words.drop(index)

        if (prefix.isEmpty() && quantity.isEmpty() && suffix == null) {
            if (lexicalWords.size == 1) return resolveNominalLexeme(lexicalWords[0])
            resolveNominalLexeme(lexicalWords.joinToString(" "))?.let { return it }
        }

        var adjective: FrenchFallbackResolution? = null
        var nounWords = lexicalWords
        if (lexicalWords.size >= 2) {
            adjectiveLemma(lexicalWords.first())?.let { lemma ->
                adjective = resolveNominalLexeme(lemma)
                if (adjective != null) nounWords = lexicalWords.drop(1)
            }
            if (adjective == null) {
                adjectiveLemma(lexicalWords.last())?.let { lemma ->
                    adjective = resolveNominalLexeme(lemma)
                    if (adjective != null) nounWords = lexicalWords.dropLast(1)
                }
            }
        }

        val noun = resolveNominalLexeme(nounWords.joinToString(" ")) ?: return null
        val output = prefix + quantity + listOfNotNull(adjective?.saamaka) + noun.saamaka + listOfNotNull(suffix)
        return syntheticResolution(inputWords.joinToString(" "), output.joinToString(" "))
    }

    private fun possessivePrefix(normalized: String): PossessivePrefix? = when (normalized) {
        "mon", "ma" -> PossessivePrefix(listOf("mi"), false)
        "mes" -> PossessivePrefix(listOf("mi"), true)
        "ton", "ta" -> PossessivePrefix(listOf("i"), false)
        "tes" -> PossessivePrefix(listOf("i"), true)
        "son", "sa" -> PossessivePrefix(listOf("ɛn"), false)
        "ses" -> PossessivePrefix(listOf("ɛn"), true)
        "notre" -> PossessivePrefix(listOf("di", "u"), false)
        "nos" -> PossessivePrefix(listOf("di", "u"), true)
        "votre" -> PossessivePrefix(listOf("unu"), false)
        "vos" -> PossessivePrefix(listOf("unu"), true)
        "leur" -> PossessivePrefix(listOf("de"), false)
        "leurs" -> PossessivePrefix(listOf("de"), true)
        else -> null
    }

    private fun adjectiveLemma(word: String): String? = when (normalizeAttestedPhraseKey(word)) {
        "nouveau", "nouvelle", "nouveaux", "nouvelles" -> "nouveau"
        "blanc", "blanche", "blancs", "blanches" -> "blanc"
        "grand", "grande", "grands", "grandes" -> "grand"
        "vert", "verte", "verts", "vertes" -> "vert"
        else -> null
    }

    private fun resolveObjectOrNominal(words: List<String>): FrenchFallbackResolution? {
        if (words.size == 1) resolveAttestedObjectPronoun(words[0])?.let { return it }
        return resolveNominalPhrase(words)
    }

    private fun resolveNominalLexeme(word: String): FrenchFallbackResolution? {
        val direct = resolveFrenchWord(word) ?: return null
        return direct.takeIf {
            it.kind in setOf(FrenchResolutionKind.EXACT, FrenchResolutionKind.INFLECTION, FrenchResolutionKind.EXPRESSION_DERIVED) &&
                it.saamaka.isNotBlank()
        }
    }

    // -------------------------------------------------------------------------
    // Lexical helpers
    // -------------------------------------------------------------------------

    private fun resolveGrammarVerb(form: String, lemma: String): FrenchFallbackResolution? {
        val normalizedLemma = normalizeAttestedPhraseKey(lemma)
        if (normalizedLemma !in GRAMMAR_SAFE_LEMMAS) return null
        return resolveFrenchWord(form)?.takeIf {
            normalizeAttestedPhraseKey(it.matchedFrench) == normalizedLemma &&
                it.kind in setOf(FrenchResolutionKind.EXACT, FrenchResolutionKind.INFLECTION)
        } ?: resolveFrenchWord(lemma)?.takeIf {
            normalizeAttestedPhraseKey(it.matchedFrench) == normalizedLemma && it.kind == FrenchResolutionKind.EXACT
        }
    }

    private fun grammarLemma(form: String): String? =
        FrenchVerbInflections.lemma(form) ?: EXTRA_LEMMA_BY_FORM[normalizeAttestedPhraseKey(form)]

    private fun grammarTense(form: String): FrenchVerbTense? =
        FrenchVerbInflections.tense(form) ?: EXTRA_TENSE_BY_FORM[normalizeAttestedPhraseKey(form)]

    private fun resolveTimeAdverb(word: String): FrenchFallbackResolution? = when (normalizeAttestedPhraseKey(word)) {
        "demain" -> syntheticResolution(word, "amánu")
        "hier" -> syntheticResolution(word, "esíde")
        else -> null
    }

    private fun resolveMannerAdverb(word: String): FrenchFallbackResolution? = when (normalizeAttestedPhraseKey(word)) {
        "bien", "joliment" -> syntheticResolution(word, "hánsi")
        else -> null
    }

    private fun pronounResolution(word: String, saamaka: String) =
        FrenchFallbackResolution(word, word, saamaka, FrenchResolutionKind.EXACT, 1300, emptyList())

    private fun syntheticResolution(source: String, saamaka: String) =
        FrenchFallbackResolution(source, source, saamaka, FrenchResolutionKind.EXACT, 1300, emptyList())

    private fun grammatical(
        sourceWords: List<String>,
        output: List<String>,
        lexicalSegments: List<Pair<String, FrenchFallbackResolution>>
    ) = PhraseTranslationResult(
        translation = output.joinToString(" "),
        recognizedSegments = listOf(
            RecognizedPhraseSegment(sourceWords.first(), output.first(), matchedSource = sourceWords.first())
        ) + lexicalSegments.map { (source, resolution) ->
            RecognizedPhraseSegment(source, resolution.saamaka, matchedSource = resolution.matchedFrench, alternatives = resolution.alternatives)
        },
        untranslatedSegments = emptyList(),
        isComplete = true,
        reliability = TranslationReliability.HIGH,
        kind = PhraseTranslationKind.GRAMMATICAL
    )

    private fun syntheticGrammar(source: String, output: String) = PhraseTranslationResult(
        translation = output,
        recognizedSegments = listOf(RecognizedPhraseSegment(source, output, matchedSource = source)),
        untranslatedSegments = emptyList(),
        isComplete = true,
        reliability = TranslationReliability.HIGH,
        kind = PhraseTranslationKind.GRAMMATICAL
    )

    private fun combineGrammar(
        source: String,
        output: String,
        vararg parts: PhraseTranslationResult
    ) = PhraseTranslationResult(
        translation = output,
        recognizedSegments = parts.flatMap { it.recognizedSegments },
        untranslatedSegments = emptyList(),
        isComplete = true,
        reliability = TranslationReliability.HIGH,
        kind = PhraseTranslationKind.GRAMMATICAL
    )

    private fun splitOnce(text: String, delimiter: String): Pair<String, String>? {
        val index = text.indexOf(delimiter, ignoreCase = true)
        if (index < 0) return null
        return text.substring(0, index).trim() to text.substring(index + delimiter.length).trim()
    }

    private fun splitAtComma(text: String): Pair<String, String>? {
        val index = text.indexOf(',')
        if (index < 0) return null
        val left = text.substring(0, index).trim()
        val right = text.substring(index + 1).trim()
        if (left.isBlank() || right.isBlank()) return null
        return left to right
    }

    private fun indexOfWordSequence(text: String, sequence: String): Int =
        text.lowercase().indexOf(sequence.lowercase())

    private fun normalizeSurface(text: String): String = text
        .replace('’', '\'')
        .replace(Regex("\\bqu'(?=(il|elle|ils|elles|on)\\b)", RegexOption.IGNORE_CASE), "que ")
        .replace(Regex("\\s+"), " ")
        .trim()

    private fun tokenizeFrench(text: String): List<String> = cleanPhraseInput(normalizeSurface(text))
        .lowercase()
        .replace(Regex("\\bj'"), "je ")
        .replace(Regex("\\bn'"), "ne ")
        .replace(Regex("\\bm'(?=\\p{L})"), "me ")
        .replace(Regex("\\bt'(?=\\p{L})"), "te ")
        .replace(Regex("\\bs'(?=\\p{L})"), "se ")
        .replace(Regex("\\bl'(?=\\p{L})"), "le ")
        .replace(Regex("-ci\\b"), " ci")
        .replace(Regex("-là\\b"), " là")
        .replace(Regex("-la\\b"), " la")
        .replace(Regex("[,.!?;:]+$"), "")
        .replace(Regex("\\s+"), " ")
        .trim()
        .split(' ')
        .filter(String::isNotBlank)

    private companion object {
        val FACTUAL_COMPLEMENT_LEMMAS = setOf("savoir", "dire", "croire", "penser", "voir")
        val VOLITIVE_COMPLEMENT_LEMMAS = setOf("vouloir", "demander")
        val DOUBLE_OBJECT_LEMMAS = setOf("donner")
        val PHYSICAL_REFLEXIVE_LEMMAS = setOf("laver", "blesser", "habiller")
        val RECIPROCAL_LEMMAS = setOf("voir", "regarder")
        val REFLEXIVE_CLITICS = setOf("me", "te", "se", "nous", "vous")

        val LOCATIVE_FRENCH_PREPOSITIONS = setOf("a", "à", "au", "aux", "dans", "sur")
        val NOMINAL_DETERMINERS = setOf("le", "la", "les", "un", "une", "des", "du")
        val DEMONSTRATIVE_DETERMINERS = setOf("ce", "cet", "cette", "ces")

        val GRAMMAR_DYNAMIC_LEMMAS = setOf(
            "manger", "dormir", "aider", "marcher", "ecrire", "écrire", "aller", "venir",
            "partir", "travailler", "boire", "courir", "parler", "dire", "donner", "regarder",
            "acheter", "appeler", "pleurer", "laver", "blesser", "habiller", "faire"
        )

        val GRAMMAR_SAFE_LEMMAS = GRAMMAR_DYNAMIC_LEMMAS + setOf(
            "aimer", "voir", "savoir", "connaitre", "connaître", "croire", "penser",
            "vouloir", "pouvoir", "devoir", "être"
        )

        val EXTRA_LEMMA_BY_FORM = buildMap {
            fun add(lemma: String, forms: String) {
                forms.split(' ').filter(String::isNotBlank).forEach { put(normalizeAttestedPhraseKey(it), lemma) }
            }
            add("venir", "vienne viennes venions veniez viennent")
            add("pleurer", "pleure pleures pleurons pleurez pleurent pleurais pleurait pleurions pleuriez pleuraient pleurerai pleureras pleurera pleurerons pleurerez pleureront")
            add("laver", "lave laves lavons lavez lavent lavais lavait lavions laviez lavaient laverai laveras lavera laverons laverez laveront")
            add("blesser", "blesse blesses blessons blessez blessent blessais blessait blessions blessiez blessaient blesserai blesseras blessera blesserons blesserez blesseront")
            add("habiller", "habille habilles habillons habillez habillent habillais habillait habillions habilliez habillaient habillerai habilleras habillera habillerons habillerez habilleront")
        }

        val EXTRA_TENSE_BY_FORM = buildMap {
            fun add(tense: FrenchVerbTense, forms: String) {
                forms.split(' ').filter(String::isNotBlank).forEach { put(normalizeAttestedPhraseKey(it), tense) }
            }
            add(FrenchVerbTense.OTHER, "vienne viennes venions veniez viennent")
            add(FrenchVerbTense.PRESENT, "pleure pleures pleurons pleurez pleurent lave laves lavons lavez lavent blesse blesses blessons blessez blessent habille habilles habillons habillez habillent")
            add(FrenchVerbTense.PAST, "pleurais pleurait pleurions pleuriez pleuraient lavais lavait lavions laviez lavaient blessais blessait blessions blessiez blessaient habillais habillait habillions habilliez habillaient")
            add(FrenchVerbTense.FUTURE, "pleurerai pleureras pleurera pleurerons pleurerez pleureront laverai laveras lavera laverons laverez laveront blesserai blesseras blessera blesserons blesserez blesseront habillerai habilleras habillera habillerons habillerez habilleront")
        }
    }
}
