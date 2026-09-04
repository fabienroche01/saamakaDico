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
    private val remainingTrials: () -> Int,
    private val consumeTrial: () -> Boolean
) {
    suspend fun resolve(
        text: String,
        frenchToSaamaka: Boolean,
        accessLevel: AccessLevel
    ): PhraseTranslationPipelineResult {
        val clean = text.trim()
        if (normalizedInputWordCount(clean) <= 1) return fallback(clean)
        if (accessLevel == AccessLevel.GUEST) return premiumRequired(clean)
        if (accessLevel == AccessLevel.FREE_ACCOUNT && remainingTrials() <= 0) {
            return premiumRequired(clean)
        }

        val translation = resolvePhrase(clean, frenchToSaamaka)
            ?.takeIf { it.isComplete }
            ?: return fallback(clean)
        return PhraseTranslationPipelineResult(
            text = clean,
            translation = translation,
            disposition = PhraseTranslationDisposition.TRANSLATED,
            remainingTrials = remainingTrials()
        )
    }

    fun authorizeSuccessfulTranslation(
        result: PhraseTranslationPipelineResult,
        accessLevel: AccessLevel,
        alreadyConsumed: Boolean = false
    ): PhraseTranslationPipelineResult {
        if (result.disposition != PhraseTranslationDisposition.TRANSLATED ||
            accessLevel != AccessLevel.FREE_ACCOUNT || alreadyConsumed
        ) return result

        if (!consumeTrial()) return premiumRequired(result.text)
        return result.copy(trialConsumed = true, remainingTrials = remainingTrials())
    }

    suspend fun translate(
        text: String,
        frenchToSaamaka: Boolean,
        accessLevel: AccessLevel
    ): PhraseTranslationPipelineResult = authorizeSuccessfulTranslation(
        resolve(text, frenchToSaamaka, accessLevel),
        accessLevel
    )

    private fun fallback(text: String) = PhraseTranslationPipelineResult(
        text = text,
        disposition = PhraseTranslationDisposition.FALLBACK,
        remainingTrials = remainingTrials()
    )

    private fun premiumRequired(text: String) = PhraseTranslationPipelineResult(
        text = text,
        disposition = PhraseTranslationDisposition.PREMIUM_REQUIRED,
        remainingTrials = remainingTrials()
    )
}
