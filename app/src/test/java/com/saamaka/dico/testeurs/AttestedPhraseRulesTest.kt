package com.saamaka.dico.testeurs

import com.saamaka.dico.testeurs.model.TranslationReliability
import com.saamaka.dico.testeurs.model.RecognizedPhraseSegment
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class AttestedPhraseRulesTest {
    @Test
    fun validatedGreetingWordIsAvailableInBothDirections() {
        assertEquals("odi", findAttestedPhraseRule("bonjour", frenchToSaamaka = true))
        assertEquals("bonjour", findAttestedPhraseRule("ODI!", frenchToSaamaka = false))
    }

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

    @Test
    fun greetingWithPersonPreservesTheNameAndDoesNotAddFromMe() {
        assertEquals(
            "Da Fusia odi",
            findAttestedPhraseRule("passe le bonjour à Fusia", frenchToSaamaka = true)
        )
        assertEquals(
            "Da Antoine odi",
            findAttestedPhraseRule("PASSE LE BONJOUR A Antoine!", frenchToSaamaka = true)
        )
        assertEquals(
            "Da L’Éa odi",
            findAttestedPhraseRule("Passe le bonjour à L’Éa.", frenchToSaamaka = true)
        )
    }

    @Test
    fun greetingWithPersonAddsFromMeOnlyWhenItIsPresent() {
        assertEquals(
            "Da Fusia odi da mi",
            findAttestedPhraseRule(
                "Passe le bonjour à Fusia de ma part",
                frenchToSaamaka = true
            )
        )
        assertEquals(
            "Da Fusia odi",
            findAttestedPhraseRule("Passe le bonjour à Fusia", frenchToSaamaka = true)
        )
    }

    @Test
    fun reverseGreetingWithPersonPreservesTheNameAndOptionalSuffix() {
        assertEquals(
            "Passe le bonjour à Fusia",
            findAttestedPhraseRule("Da Fusia odi", frenchToSaamaka = false)
        )
        assertEquals(
            "Passe le bonjour à Fusia de ma part",
            findAttestedPhraseRule("Da Fusia odi da mi", frenchToSaamaka = false)
        )
        assertEquals(
            "Passe le bonjour à Antoine",
            findAttestedPhraseRule("DA Antoine ODI!", frenchToSaamaka = false)
        )
    }

    @Test
    fun longerUnattestedExpressionIsNotAcceptedAsAnExactRule() {
        assertNull(
            findAttestedPhraseRule(
                "Passe le bonjour à Fusia demain",
                frenchToSaamaka = true
            )
        )
        assertNull(findAttestedPhraseRule("Da Fusia odi da mi demain", frenchToSaamaka = false))
    }
}
