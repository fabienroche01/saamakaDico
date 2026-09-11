package com.saamaka.dico.testeurs

import org.junit.Assert.assertEquals
import org.junit.Test

class FrenchDictionarySearchAliasTest {

    @Test
    fun avoirConjugationsResolveToInfinitive() {
        assertEquals("avoir", normalizeMultilingualSearch("j’ai"))
        assertEquals("avoir", normalizeMultilingualSearch("tu as"))
        assertEquals("avoir", normalizeMultilingualSearch("nous avons"))
        assertEquals("avoir", normalizeMultilingualSearch("ils ont"))
    }

    @Test
    fun etreConjugationsResolveToInfinitive() {
        assertEquals("etre", normalizeMultilingualSearch("je suis"))
        assertEquals("etre", normalizeMultilingualSearch("vous êtes"))
        assertEquals("etre", normalizeMultilingualSearch("elles sont"))
    }

    @Test
    fun allerConjugationsResolveToInfinitive() {
        assertEquals("aller", normalizeMultilingualSearch("je vais"))
        assertEquals("aller", normalizeMultilingualSearch("nous allons"))
        assertEquals("aller", normalizeMultilingualSearch("ils vont"))
    }

    @Test
    fun unrelatedQueriesStayUntouched() {
        assertEquals("moi", normalizeMultilingualSearch("moi"))
        assertEquals("maison", normalizeMultilingualSearch("maison"))
        assertEquals("a", normalizeMultilingualSearch("a"))
    }
}
