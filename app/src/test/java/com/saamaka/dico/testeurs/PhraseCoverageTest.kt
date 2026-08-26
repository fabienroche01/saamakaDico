package com.saamaka.dico.testeurs

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import com.saamaka.dico.testeurs.model.TranslationReliability
import com.saamaka.dico.testeurs.model.RecognizedPhraseSegment

class PhraseCoverageTest {
    @Test
    fun fullyRecognizedPhraseProducesCompleteTranslation() {
        val result = assemble("bonjour ami", mapOf("bonjour" to "odi", "ami" to "mati"))!!

        assertTrue(result.isComplete)
        assertEquals("odi mati", result.translation)
        assertEquals(TranslationReliability.MEDIUM, result.reliability)
        assertTrue(result.untranslatedSegments.isEmpty())
    }

    @Test
    fun longestAttestedExpressionHasPriorityAndIsNotSplitAgain() {
        val result = assemble(
            "bon jour ami",
            mapOf("bon jour" to "odi", "bon" to "bun", "jour" to "daka", "ami" to "mati")
        )!!

        assertEquals(listOf("bon jour", "ami"), result.recognizedSegments.map { it.source })
        assertEquals("odi mati", result.translation)
    }

    @Test
    fun completeAttestedExpressionHasHighReliability() {
        val result = assemble("bon jour", mapOf("bon jour" to "odi"))!!

        assertEquals("odi", result.translation)
        assertEquals(TranslationReliability.HIGH, result.reliability)
    }

    @Test
    fun partiallyRecognizedPhraseKeepsEveryMissingElementVisible() {
        val result = assemble("je suis ton père", mapOf("je" to "mi"))!!

        assertFalse(result.isComplete)
        assertEquals("mi", result.translation)
        assertEquals(TranslationReliability.LOW, result.reliability)
        assertEquals(listOf("je"), result.recognizedSegments.map { it.source })
        assertEquals(listOf("suis", "ton", "père"), result.untranslatedSegments)
        assertTrue(result.shareableText().contains("Proposition approximative"))
        assertFalse(result.shareableText().trim() == "mi")
    }

    @Test
    fun totallyUnknownPhraseReportsEveryElement() {
        val result = assemble("alpha beta", emptyMap())!!

        assertFalse(result.isComplete)
        assertEquals("Aucune proposition locale disponible", result.translation)
        assertTrue(result.recognizedSegments.isEmpty())
        assertEquals(listOf("alpha", "beta"), result.untranslatedSegments)
        assertEquals(TranslationReliability.LOW, result.reliability)
    }

    @Test
    fun accentsApostrophesPunctuationCaseAndSpacesAreHandled() {
        val result = assemble("  L’école,   écoute ! ", mapOf("l'école" to "A", "écoute" to "B"))!!

        assertTrue(result.isComplete)
        assertEquals("A B", result.translation)
        assertEquals(TranslationReliability.MEDIUM, result.reliability)
        assertEquals(listOf("L'école", "écoute"), result.recognizedSegments.map { it.source })
    }

    private fun assemble(text: String, dictionary: Map<String, String>) =
        assembleAttestedPhrase(text) { candidate ->
            dictionary[candidate.lowercase()]?.let {
                RecognizedPhraseSegment(candidate, it)
            }
        }
}
