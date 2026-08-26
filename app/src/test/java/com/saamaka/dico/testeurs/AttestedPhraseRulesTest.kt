package com.saamaka.dico.testeurs

import com.saamaka.dico.testeurs.model.TranslationReliability
import com.saamaka.dico.testeurs.model.RecognizedPhraseSegment
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class AttestedPhraseRulesTest {
    @Test
    fun validatedGreetingExpressionIsFoundExactly() {
        assertEquals(
            "da odi",
            findAttestedPhraseRule("passe le bonjour", frenchToSaamaka = true)
        )
    }

    @Test
    fun validatedGreetingIgnoresCaseFinalPunctuationAndMultipleSpaces() {
        assertEquals(
            "da odi",
            findAttestedPhraseRule("  Passe   le bonjour. ", frenchToSaamaka = true)
        )
    }

    @Test
    fun validatedGreetingIsAHighReliabilityCompleteExpression() {
        val result = assembleAttestedPhrase("Passe le bonjour.") { segment ->
            findAttestedPhraseRule(segment, frenchToSaamaka = true)?.let {
                RecognizedPhraseSegment(segment, it)
            }
        }!!

        assertEquals("da odi", result.translation)
        assertEquals(TranslationReliability.HIGH, result.reliability)
        assertTrue(result.untranslatedSegments.isEmpty())
    }

    @Test
    fun reverseAttestedExpressionIsAvailableWithoutGeneralization() {
        assertEquals(
            "passe le bonjour",
            findAttestedPhraseRule("DA ODI!", frenchToSaamaka = false)
        )
        assertEquals(null, findAttestedPhraseRule("passe le salut", true))
    }
}
