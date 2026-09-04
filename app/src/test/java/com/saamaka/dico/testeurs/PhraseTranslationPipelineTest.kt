package com.saamaka.dico.testeurs

import com.saamaka.dico.testeurs.model.PhraseTranslationKind
import com.saamaka.dico.testeurs.model.PhraseTranslationResult
import com.saamaka.dico.testeurs.model.RecognizedPhraseSegment
import com.saamaka.dico.testeurs.model.TranslationReliability
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Test

class PhraseTranslationPipelineTest {
    @Test
    fun homeAndTranslateUseTheSamePhraseDecisionForAllRequiredCases() = runBlocking {
        val translations = mapOf(
            "je veux manger" to "mi kë Makandi",
            "tu veux dormir" to "i kë duumí",
            "il veut dormir" to "a kë duumí",
            "je dois dormir" to "mi ta da duumí",
            "je vais manger" to "mi o Makandi"
        )

        (translations.keys + "phrase inconnue").forEach { input ->
            fun pipeline() = PhraseTranslationPipeline(
                resolvePhrase = { text, _ -> translations[text]?.let(::complete) },
                remainingTrials = { 3 },
                consumeTrial = { true }
            )

            val homePipeline = pipeline()
            val home = homePipeline.authorizeSuccessfulTranslation(
                homePipeline.resolve(input, true, AccessLevel.PREMIUM),
                AccessLevel.PREMIUM
            )
            val translate = pipeline().translate(input, true, AccessLevel.PREMIUM)

            assertEquals("Disposition différente pour $input", translate.disposition, home.disposition)
            assertEquals("Traduction différente pour $input", translate.translation, home.translation)
        }
    }

    @Test
    fun freePhraseConsumesTheExistingQuotaOnlyAfterACompleteTranslation() = runBlocking {
        var remaining = 2
        val pipeline = PhraseTranslationPipeline(
            resolvePhrase = { text, _ -> if (text == "je veux manger") complete("mi kë Makandi") else null },
            remainingTrials = { remaining },
            consumeTrial = {
                if (remaining <= 0) false else {
                    remaining--
                    true
                }
            }
        )

        val translated = pipeline.translate("je veux manger", true, AccessLevel.FREE_ACCOUNT)
        val unknown = pipeline.translate("phrase inconnue", true, AccessLevel.FREE_ACCOUNT)
        val word = pipeline.translate("manger", true, AccessLevel.FREE_ACCOUNT)

        assertEquals(PhraseTranslationDisposition.TRANSLATED, translated.disposition)
        assertEquals(true, translated.trialConsumed)
        assertEquals(PhraseTranslationDisposition.FALLBACK, unknown.disposition)
        assertEquals(PhraseTranslationDisposition.FALLBACK, word.disposition)
        assertEquals(1, remaining)
    }

    private fun complete(translation: String) = PhraseTranslationResult(
        translation = translation,
        recognizedSegments = listOf(RecognizedPhraseSegment("source", translation)),
        untranslatedSegments = emptyList(),
        isComplete = true,
        reliability = TranslationReliability.HIGH,
        kind = PhraseTranslationKind.GRAMMATICAL
    )
}
