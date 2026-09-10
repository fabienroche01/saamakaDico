package com.saamaka.dico.testeurs

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class SaamakaGrammarEngineTest {
    private val resolver = FrenchFallbackResolver(
        listOf(
            candidate("malade", "síki"),
            candidate("seul", "wanwan"),
            candidate("vouloir", "kë"),
            candidate("devoir", "da"),
            candidate("aimer", "lobi"),
            candidate("manger", "Makandi", "O"),
            candidate("dormir", "duumí"),
            candidate("voir", "si"),
            candidate("aider", "heepi"),
            candidate("mère", "mama")
        )
    )
    private val engine = SaamakaGrammarEngine(resolver::resolve)

    @Test
    fun appliesAttestedCopulaMarkersWithoutTranslatingEtre() {
        assertEquals("mi síki", engine.translate("je suis malade")?.translation)
        assertEquals("i wanwan", engine.translate("tu es seul")?.translation)
        assertEquals("a síki", engine.translate("elle est malade")?.translation)
        assertEquals("a síki", engine.translate("il est malade")?.translation)
        assertEquals("mi bi síki", engine.translate("j’étais malade")?.translation)
        assertEquals("mi an síki", engine.translate("je ne suis pas malade")?.translation)
    }

    @Test
    fun appliesAttestedAspectAndModalPatterns() {
        assertEquals("mi o Makandi", engine.translate("je vais manger")?.translation)
        assertEquals("i o duumí", engine.translate("tu vas dormir")?.translation)
        assertEquals("mi kë Makandi", engine.translate("je veux manger")?.translation)
        assertEquals("i kë duumí", engine.translate("tu veux dormir")?.translation)
        assertEquals("mi ta Makandi", engine.translate("je suis en train de manger")?.translation)
        assertEquals("mi ta da duumí", engine.translate("je dois dormir")?.translation)
        assertEquals("mi an lobi duumí", engine.translate("je n’aime pas dormir")?.translation)
    }

    @Test
    fun grammarLineDistinguishesThirdPersonSubjectFromObject() {
        assertEquals("a ta si mi", engine.translate("elle me voit")?.translation)
        assertEquals("mi ta si ën", engine.translate("je la vois")?.translation)
        assertEquals("mi ta si ën", engine.translate("je le vois")?.translation)
        assertEquals("mi ta heepi ën", engine.translate("je l’aide")?.translation)
        assertEquals("mi an si ën", engine.translate("je ne la vois pas")?.translation)
    }

    @Test
    fun grammarLineKeepsWeakObjectPronounsForOtherPersons() {
        assertEquals("mi ta si i", engine.translate("je te vois")?.translation)
        assertEquals("mi ta si u", engine.translate("je nous vois")?.translation)
        assertEquals("mi ta si unu", engine.translate("je vous vois")?.translation)
        assertEquals("mi ta si de", engine.translate("je les vois")?.translation)
    }

    @Test
    fun grammarLineUsesDependentPronounForPossession() {
        assertEquals("mi ta si i mama", engine.translate("je vois ta mère")?.translation)
        assertEquals("mi ta si ën mama", engine.translate("je vois sa mère")?.translation)
    }

    @Test
    fun grammarLineUsesStrongPronounAfterPour() {
        assertEquals("mi ta Makandi fu mí", engine.translate("je mange pour moi")?.translation)
        assertEquals("mi ta Makandi fu hën", engine.translate("je mange pour elle")?.translation)
    }

    @Test
    fun neverInventsAnUnknownOrUnsafeLexeme() {
        assertNull(engine.translate("je fais dormir"))
        assertNull(engine.translate("je peux dormir"))
        assertNull(engine.translate("je veux téléporter"))
        assertNull(engine.translate("on veut dormir"))
    }

    @Test
    fun frenchInflectionsOnlyExposeLemmaAndTense() {
        assertEquals("être", FrenchVerbInflections.lemma("suis"))
        assertEquals(FrenchVerbTense.PRESENT, FrenchVerbInflections.tense("suis"))
        assertEquals(FrenchVerbTense.PAST, FrenchVerbInflections.tense("étais"))
        assertEquals("aller", FrenchVerbInflections.lemma("vas"))
        assertEquals(FrenchVerbTense.PRESENT, FrenchVerbInflections.tense("vas"))
    }

    private fun candidate(french: String, saamaka: String, validation: String = "") =
        FrenchTranslationCandidate(french, saamaka, validation)
}
