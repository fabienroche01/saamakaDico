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
    val disposition: PhraseTranslationDisposition,
    val trialConsumed: Boolean = false,
    val remainingTrials: Int
)

/** The single access/quota boundary around the dictionary phrase engine. */
internal class PhraseTranslationPipeline(
    private val resolvePhrase: suspend (String, Boolean) -> PhraseTranslationResult?,
    private val remainingTrials: (AccessLevel) -> Int,
    private val consumeTrial: (AccessLevel) -> Boolean
) {
    suspend fun resolve(
        text: String,
        frenchToSaamaka: Boolean,
        accessLevel: AccessLevel
    ): PhraseTranslationPipelineResult {
        val clean = text.trim()
        if (normalizedInputWordCount(clean) <= 1) return fallback(clean, accessLevel)
        if (isTrialLimited(accessLevel) && remainingTrials(accessLevel) <= 0) {
            return premiumRequired(clean, accessLevel)
        }

        val translation = resolvePhrase(clean, frenchToSaamaka)
            ?.takeIf {
                it.isComplete || (frenchToSaamaka && it.recognizedSegments.isNotEmpty())
            }
            ?: return fallback(clean, accessLevel)
        return PhraseTranslationPipelineResult(
            text = clean,
            translation = translation,
            disposition = PhraseTranslationDisposition.TRANSLATED,
            remainingTrials = remainingTrials(accessLevel)
        )
    }

    fun authorizePhraseAttempt(
        result: PhraseTranslationPipelineResult,
        accessLevel: AccessLevel,
        alreadyConsumed: Boolean = false
    ): PhraseTranslationPipelineResult {
        if (result.disposition == PhraseTranslationDisposition.PREMIUM_REQUIRED ||
            normalizedInputWordCount(result.text) <= 1 ||
            !isTrialLimited(accessLevel) ||
            alreadyConsumed
        ) return result

        if (!consumeTrial(accessLevel)) return premiumRequired(result.text, accessLevel)
        return result.copy(trialConsumed = true, remainingTrials = remainingTrials(accessLevel))
    }

    suspend fun translate(
        text: String,
        frenchToSaamaka: Boolean,
        accessLevel: AccessLevel,
        alreadyConsumed: Boolean = false
    ): PhraseTranslationPipelineResult {
        val clean = text.trim()
        if (normalizedInputWordCount(clean) <= 1) return fallback(clean, accessLevel)

        // The attempt is charged before any suspendable resolution work. A cancelled
        // search can therefore never bypass the persistent quota.
        val authorization = authorizePhraseAttempt(
            fallback(clean, accessLevel),
            accessLevel,
            alreadyConsumed
        )
        if (authorization.disposition == PhraseTranslationDisposition.PREMIUM_REQUIRED) {
            return authorization
        }

        val translation = resolvePhrase(clean, frenchToSaamaka)
            ?.takeIf {
                it.isComplete || (frenchToSaamaka && it.recognizedSegments.isNotEmpty())
            }
        return PhraseTranslationPipelineResult(
            text = clean,
            translation = translation,
            disposition = if (translation != null) {
                PhraseTranslationDisposition.TRANSLATED
            } else {
                PhraseTranslationDisposition.FALLBACK
            },
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
}
