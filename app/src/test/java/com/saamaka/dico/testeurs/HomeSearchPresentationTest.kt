package com.saamaka.dico.testeurs

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class HomeSearchPresentationTest {
    @Test
    fun actualHomeSearchShowsPhraseCtaForUnknownMultiWordInput() {
        val state = homeSearchPresentation("  je   suis ton pere  ", 0, false)

        assertTrue(state.showPhraseCta)
        assertFalse(state.showNoResult)
    }

    @Test
    fun actualHomeSearchKeepsNoResultForOneUnknownWord() {
        val state = homeSearchPresentation("inconnu ", 0, false)

        assertFalse(state.showPhraseCta)
        assertTrue(state.showNoResult)
    }

    @Test
    fun actualHomeSearchDoesNotOfferPremiumForCompleteLocalExpression() {
        val state = homeSearchPresentation("passe le bonjour", 0, true)

        assertFalse(state.showPhraseCta)
        assertFalse(state.showNoResult)
    }

    @Test
    fun changingTextRecomputesAndClearsPreviousPhraseDecision() {
        assertTrue(homeSearchPresentation("phrase vraiment inconnue", 0, false).showPhraseCta)
        assertFalse(homeSearchPresentation("mot", 1, true).showPhraseCta)
    }

    @Test
    fun twoWordsRemainAFreeExpressionSearch() {
        val state = homeSearchPresentation("bonjour maman", 0, false)

        assertFalse(state.showPhraseCta)
        assertFalse(state.showNoResult)
    }
}
