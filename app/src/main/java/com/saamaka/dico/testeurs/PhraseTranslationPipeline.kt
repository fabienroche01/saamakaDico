package com.saamaka.dico.testeurs

import com.saamaka.dico.testeurs.model.PhraseTranslationResult

enum class PhraseTranslationDisposition {
    TRANSLATED,
    FALLBACK,
    PREMIUM_REQUIRED
}

data class PhraseTranslationPipelineResult(
    val text: String,
    val translation: PhraseTranslationResult? = null,
    val wordByWordTranslation: PhraseTranslationResult? = null,
    val grammaticalTranslation: PhraseTranslationResult? = null,
    val disposition: PhraseTranslationDisposition,
    val trialConsumed: Boolean = false,
    val remainingTrials: Int
)

/** The single access/quota boundary around the dictionary phrase engine. */
internal class PhraseTranslationPipeline(
    private val resolvePhrase: suspend (String, Boolean) -> PhraseTranslationResult?,
    private val resolveWordByWord: (suspend (String, Boolean) -> PhraseTranslationResult?)? = null,
    private val resolveGrammaticalPartial: (suspend (String, Boolean) -> PhraseTranslationResult?)? = null,
    private val remainingTrials: (AccessLevel) -> Int,
    private val consumeTrial: (AccessLevel) -> Boolean
) {
    /**
     * Prevents an Enter key repeat / fast double submit from spending several trials
     * for the exact same unchanged phrase. A different phrase becomes chargeable as
     * usual; coming back to the first phrase later is a new attempt.
     */
    private var lastConsumedAttemptKey: String? = null

    suspend fun resolve(
        text: String,
        frenchToSaamaka: Boolean,
        accessLevel: AccessLevel
    ): PhraseTranslationPipelineResult {
        val clean = text.trim()
        val wordCount = normalizedInputWordCount(clean)
        val recognizedShortStructure = isRecognizedShortVerbStructure(clean, frenchToSaamaka)
        if (wordCount < PHRASE_MINIMUM_WORDS && !recognizedShortStructure) {
            return fallback(clean, accessLevel)
        }

        val primary = resolvePhrase(clean, frenchToSaamaka)
        val completeGrammar = primary?.takeUnless { it.isLexicalFallback() }
        val grammatical = (completeGrammar
            ?: resolveGrammaticalPartial?.invoke(clean, frenchToSaamaka))
            ?.asCompleteGrammarWhenFullyResolved()
        val wordByWord = resolveWordByWord?.invoke(clean, frenchToSaamaka)
            ?: primary?.takeIf { it.isLexicalFallback() }
        val translation = grammatical ?: wordByWord?.takeIf { it.recognizedSegments.isNotEmpty() }
        return PhraseTranslationPipelineResult(
            text = clean,
            translation = translation,
            wordByWordTranslation = wordByWord,
            grammaticalTranslation = grammatical,
            disposition = if (translation != null) {
                PhraseTranslationDisposition.TRANSLATED
            } else {
                PhraseTranslationDisposition.FALLBACK
            },
            remainingTrials = remainingTrials(accessLevel)
        )
    }

    fun authorizePhraseAttempt(
        result: PhraseTranslationPipelineResult,
        accessLevel: AccessLevel,
        alreadyConsumed: Boolean = false
    ): PhraseTranslationPipelineResult {
        if (result.disposition == PhraseTranslationDisposition.PREMIUM_REQUIRED ||
            !isChargeableAttempt(result) ||
            !isTrialLimited(accessLevel) ||
            alreadyConsumed
        ) return result

        val attemptKey = quotaAttemptKey(result.text, accessLevel)
        synchronized(this) {
            if (lastConsumedAttemptKey == attemptKey) {
                return result.copy(
                    trialConsumed = false,
                    remainingTrials = remainingTrials(accessLevel)
                )
            }

            if (!consumeTrial(accessLevel)) return premiumRequired(result.text, accessLevel)
            lastConsumedAttemptKey = attemptKey
        }

        return result.copy(trialConsumed = true, remainingTrials = remainingTrials(accessLevel))
    }

    suspend fun translate(
        text: String,
        frenchToSaamaka: Boolean,
        accessLevel: AccessLevel
    ): PhraseTranslationPipelineResult {
        val clean = text.trim()
        val wordCount = normalizedInputWordCount(clean)
        val recognizedShortStructure = isRecognizedShortVerbStructure(clean, frenchToSaamaka)
        if (wordCount < PHRASE_MINIMUM_WORDS && !recognizedShortStructure) {
            return fallback(clean, accessLevel)
        }

        // Recognized short French structures are deliberately free. They can be
        // previewed and explicitly submitted without consuming the 3+ word quota.
        if (wordCount < PHRASE_MINIMUM_WORDS) {
            return resolve(clean, frenchToSaamaka, accessLevel)
        }

        // The attempt is charged before any suspendable resolution work. A cancelled
        // search can therefore never bypass the persistent quota.
        val authorization = authorizePhraseAttempt(
            fallback(clean, accessLevel),
            accessLevel
        )
        if (authorization.disposition == PhraseTranslationDisposition.PREMIUM_REQUIRED) {
            return authorization
        }

        return resolve(clean, frenchToSaamaka, accessLevel).copy(
            trialConsumed = authorization.trialConsumed,
            remainingTrials = remainingTrials(accessLevel)
        )
    }

    private fun quotaAttemptKey(text: String, accessLevel: AccessLevel): String =
        "${accessLevel.name}:${normalizeAttestedPhraseKey(cleanPhraseInput(text))}"

    private fun fallback(text: String, accessLevel: AccessLevel) = PhraseTranslationPipelineResult(
        text = text,
        disposition = PhraseTranslationDisposition.FALLBACK,
        remainingTrials = remainingTrials(accessLevel)
    )

    private fun premiumRequired(text: String, accessLevel: AccessLevel) = PhraseTranslationPipelineResult(
        text = text,
        disposition = PhraseTranslationDisposition.PREMIUM_REQUIRED,
        remainingTrials = remainingTrials(accessLevel)
    )

    private fun isTrialLimited(accessLevel: AccessLevel): Boolean =
        translationTrialLimit(accessLevel) != null

    private fun isChargeableAttempt(result: PhraseTranslationPipelineResult): Boolean =
        normalizedInputWordCount(result.text) >= PHRASE_MINIMUM_WORDS

    private fun PhraseTranslationResult.isLexicalFallback(): Boolean =
        kind == com.saamaka.dico.testeurs.model.PhraseTranslationKind.WORD_BY_WORD ||
            kind == com.saamaka.dico.testeurs.model.PhraseTranslationKind.PARTIAL

    private fun PhraseTranslationResult.asCompleteGrammarWhenFullyResolved(): PhraseTranslationResult =
        if (isComplete && kind == com.saamaka.dico.testeurs.model.PhraseTranslationKind.PARTIAL) {
            copy(kind = com.saamaka.dico.testeurs.model.PhraseTranslationKind.GRAMMATICAL)
        } else {
            this
        }

    private companion object {
        const val PHRASE_MINIMUM_WORDS = 3
    }
}

internal fun isRecognizedShortVerbStructure(text: String, frenchToSaamaka: Boolean = true): Boolean {
    if (!frenchToSaamaka || normalizedInputWordCount(text) > 2) return false

    val units = frenchGrammaticalUnits(text)
    if (units.size < 2 || resolveAttestedFrenchSubject(units.first().lookup) == null) return false

    // Simple short form: "je veux", "tu dors", etc.
    if (units.size == 2 && FrenchVerbInflections.lemma(units[1].source) != null) {
        return true
    }

    // Elided object clitic counts as one typed word in French:
    // "je t'aide", "je l'aime", "je m'aide". frenchGrammaticalUnits expands
    // the apostrophe to subject + clitic + verb, so inspect the final verb unit.
    if (units.size == 3 &&
        normalizeAttestedPhraseKey(units[1].source) in setOf("me", "te", "le", "la") &&
        FrenchVerbInflections.lemma(units[2].source) != null
    ) {
        return true
    }

    return false
}

internal fun shouldAnalyzeAsPhrase(text: String, frenchToSaamaka: Boolean = true): Boolean =
    normalizedInputWordCount(text) >= 3 || isRecognizedShortVerbStructure(text, frenchToSaamaka)
