package com.saamaka.dico.testeurs

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import com.saamaka.dico.testeurs.model.PhraseTranslationResult
import com.saamaka.dico.testeurs.model.TranslationReliability

class UnifiedTranslationModelsTest {
    @Test
    fun simpleWordUsesSearchLabel() {
        assertEquals("Rechercher", unifiedSearchButtonLabel("père"))
    }

    @Test
    fun phraseUsesCombinedLabel() {
        assertEquals("Rechercher / Traduire", unifiedSearchButtonLabel("je suis"))
        assertEquals(2, normalizedInputWordCount("  je   suis   "))
    }

    @Test
    fun exactExpressionNeverOffersPremiumTranslation() {
        val result = UnifiedLocalSearchResult(
            exactMatch = LocalExactMatch(
                source = "passe le bonjour",
                translation = "da odi",
                provenance = LocalMatchProvenance.ATTESTED_EXPRESSION
            ),
            usefulEntries = emptyList()
        )

        assertFalse(shouldOfferPremiumTranslation("Passe   le bonjour.", result))
        assertEquals(
            "Traduction construite avec une règle validée",
            result.exactMatch?.provenance?.label
        )
    }

    @Test
    fun relatedExpressionsAreClearlyDistinguishedFromExactAndIncompleteResults() {
        assertEquals(
            "Correspondance exacte du dictionnaire",
            relatedExpressionLabel("Passe le bonjour à Fusia", "passe le bonjour a Fusia")
        )
        assertEquals(
            "Expression proche — contient des mots supplémentaires",
            relatedExpressionLabel(
                "Passe le bonjour à Fusia",
                "Passe le bonjour à Fusia de ma part"
            )
        )
        assertEquals(
            "Proposition incomplète",
            relatedExpressionLabel("Passe le bonjour à Fusia", "bonjour à Fusia")
        )
    }

    @Test
    fun unknownPhraseOffersPremiumOnlyAfterLocalSearchCompleted() {
        assertFalse(shouldOfferPremiumTranslation("phrase inconnue", null))
        assertTrue(
            shouldOfferPremiumTranslation(
                "phrase inconnue",
                UnifiedLocalSearchResult(exactMatch = null, usefulEntries = emptyList())
            )
        )
    }

    @Test
    fun unknownSingleWordNeverOffersPremiumTranslation() {
        assertFalse(
            shouldOfferPremiumTranslation(
                "inconnu   ",
                UnifiedLocalSearchResult(exactMatch = null, usefulEntries = emptyList())
            )
        )
    }

    @Test
    fun quickSearchGrammarRespectsPremiumAccessRules() {
        assertTrue(canResolveGrammarInQuickSearch(AccessLevel.TESTER))
        assertTrue(canResolveGrammarInQuickSearch(AccessLevel.PREMIUM))
        assertFalse(canResolveGrammarInQuickSearch(AccessLevel.GUEST))
        assertFalse(canResolveGrammarInQuickSearch(AccessLevel.FREE_ACCOUNT))
    }

    @Test
    fun grammaticalQuickSearchResultDoesNotOfferFallbackTranslation() {
        val result = UnifiedLocalSearchResult(
            exactMatch = LocalExactMatch(
                source = "tu veux dormir",
                translation = "i kë duumí",
                provenance = LocalMatchProvenance.GRAMMATICAL
            ),
            usefulEntries = emptyList()
        )

        assertFalse(shouldOfferPremiumTranslation("tu veux dormir", result))
        assertEquals("Traduction grammaticale", result.exactMatch?.provenance?.label)
    }

    @Test
    fun directionIndependentPolicyDoesNotConsumeOrHideLocalMatch() {
        val reverseMatch = UnifiedLocalSearchResult(
            LocalExactMatch("da odi", "passe le bonjour", LocalMatchProvenance.ATTESTED_EXPRESSION),
            emptyList()
        )

        assertFalse(shouldOfferPremiumTranslation("da odi", reverseMatch))
    }

    @Test
    fun premiumTrialIsConsumedOnlyAfterOneSuccessfulExplicitRequest() {
        val phraseResult = PhraseTranslationResult(
            translation = "proposition",
            recognizedSegments = emptyList(),
            untranslatedSegments = listOf("phrase", "inconnue"),
            isComplete = false,
            reliability = TranslationReliability.LOW
        )

        assertFalse(shouldConsumeTrialAfterPremiumResult(AccessLevel.FREE_ACCOUNT, 3, null, false))
        assertTrue(shouldConsumeTrialAfterPremiumResult(AccessLevel.FREE_ACCOUNT, 3, phraseResult, false))
        assertFalse(shouldConsumeTrialAfterPremiumResult(AccessLevel.FREE_ACCOUNT, 3, phraseResult, true))
        assertFalse(shouldConsumeTrialAfterPremiumResult(AccessLevel.PREMIUM, 3, phraseResult, false))
    }
}
