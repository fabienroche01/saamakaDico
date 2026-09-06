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
    suspend fun resolve(
        text: String,
        frenchToSaamaka: Boolean,
        accessLevel: AccessLevel
    ): PhraseTranslationPipelineResult {
        val clean = text.trim()
        val wordCount = normalizedInputWordCount(clean)
        if (wordCount < GRAMMATICAL_EXPRESSION_MINIMUM_WORDS) return fallback(clean, accessLevel)

        val primary = resolvePhrase(clean, frenchToSaamaka)
        val completeGrammar = primary?.takeUnless { it.isLexicalFallback() }
        val recognizedTwoWordStructure = wordCount == GRAMMATICAL_EXPRESSION_MINIMUM_WORDS &&
            isRecognizedTwoWordVerbStructure(clean, frenchToSaamaka)
        if (wordCount == GRAMMATICAL_EXPRESSION_MINIMUM_WORDS &&
            completeGrammar == null && !recognizedTwoWordStructure
        ) {
            return fallback(clean, accessLevel)
        }
        val grammatical = completeGrammar
            ?: resolveGrammaticalPartial?.invoke(clean, frenchToSaamaka)
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

        if (!consumeTrial(accessLevel)) return premiumRequired(result.text, accessLevel)
        return result.copy(trialConsumed = true, remainingTrials = remainingTrials(accessLevel))
    }

    suspend fun translate(
        text: String,
        frenchToSaamaka: Boolean,
        accessLevel: AccessLevel
    ): PhraseTranslationPipelineResult {
        val clean = text.trim()
        val wordCount = normalizedInputWordCount(clean)
        if (wordCount < GRAMMATICAL_EXPRESSION_MINIMUM_WORDS) return fallback(clean, accessLevel)

        if (wordCount == GRAMMATICAL_EXPRESSION_MINIMUM_WORDS) {
            val resolved = resolve(clean, frenchToSaamaka, accessLevel)
            if (resolved.wordByWordTranslation == null) return resolved
            return authorizePhraseAttempt(resolved, accessLevel)
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

    private fun isChargeableAttempt(result: PhraseTranslationPipelineResult): Boolean {
        val wordCount = normalizedInputWordCount(result.text)
        return wordCount >= PHRASE_MINIMUM_WORDS ||
            (wordCount == GRAMMATICAL_EXPRESSION_MINIMUM_WORDS &&
                result.wordByWordTranslation != null)
    }

    private fun PhraseTranslationResult.isLexicalFallback(): Boolean =
        kind == com.saamaka.dico.testeurs.model.PhraseTranslationKind.WORD_BY_WORD ||
            kind == com.saamaka.dico.testeurs.model.PhraseTranslationKind.PARTIAL

    private companion object {
        const val GRAMMATICAL_EXPRESSION_MINIMUM_WORDS = 2
        const val PHRASE_MINIMUM_WORDS = 3
    }
}

private fun isRecognizedTwoWordVerbStructure(text: String, frenchToSaamaka: Boolean): Boolean {
    if (!frenchToSaamaka) return false
    val units = frenchGrammaticalUnits(text)
    return normalizedInputWordCount(text) == 2 &&
        units.size >= 2 &&
        resolveAttestedFrenchSubject(units[0].lookup) != null &&
        FrenchVerbInflections.lemma(units[1].source) != null
}
