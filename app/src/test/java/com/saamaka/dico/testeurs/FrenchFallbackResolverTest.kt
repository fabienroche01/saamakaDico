package com.saamaka.dico.testeurs

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import com.saamaka.dico.testeurs.model.RecognizedPhraseSegment

class FrenchFallbackResolverTest {
    @Test
    fun fatherCanUsePapaOnlyWhenNoExactFatherEntryExists() {
        val result = resolver(candidate("papa", "LOCAL_PAPA", "O")).resolve("père")!!

        assertEquals(FrenchResolutionKind.SYNONYM, result.kind)
        assertEquals("papa", result.matchedFrench)
        assertEquals("LOCAL_PAPA", result.saamaka)
    }

    @Test
    fun conjugatedVerbUsesOnlyAnExplicitSafeInfinitive() {
        val result = resolver(candidate("dormir", "LOCAL_DORMIR", "O")).resolve("dors")!!

        assertEquals(FrenchResolutionKind.INFLECTION, result.kind)
        assertEquals("dormir", result.matchedFrench)
    }

    @Test
    fun lightTypingErrorIsCorrectedButNotCalledASynonym() {
        val result = resolver(candidate("bonjour", "LOCAL_BONJOUR", "O")).resolve("bonjor")!!

        assertEquals(FrenchResolutionKind.SPELLING, result.kind)
        assertEquals("bonjour", result.matchedFrench)
    }

    @Test
    fun unrelatedWordsAreNotTreatedAsSynonyms() {
        val result = resolver(candidate("chien", "LOCAL_CHIEN", "O")).resolve("chat")

        assertNull(result)
    }

    @Test
    fun exactFrenchEntryWinsBeforeSynonymFallback() {
        val result = resolver(
            candidate("père", "LOCAL_PERE", ""),
            candidate("papa", "LOCAL_PAPA", "O")
        ).resolve("père")!!

        assertEquals(FrenchResolutionKind.EXACT, result.kind)
        assertEquals("LOCAL_PERE", result.saamaka)
    }

    @Test
    fun validatedCandidateWinsAndDoubtfulIsLastResort() {
        val result = resolver(
            candidate("bonjour", "DOUBTFUL", "D"),
            candidate("bonjour", "VALIDATED", "O")
        ).resolve("bonjour")!!

        assertEquals("VALIDATED", result.saamaka)
    }

    @Test
    fun multipleTranslationsInsertOnlyOnePrimaryCandidate() {
        val result = resolver(
            candidate("père", "tàta, päa", ""),
            candidate("père", "tata", "D")
        ).resolve("père")!!

        assertEquals("tàta", result.saamaka)
        assertEquals(listOf("päa"), result.alternatives)
    }

    @Test
    fun shortWordsNeverTriggerUnrelatedSpellingSubstitutions() {
        assertNull(resolver(candidate("son", "UNRELATED", "O")).resolve("ton"))
    }

    @Test
    fun observedSentenceCannotExpandIntoUnalignedCandidateSequence() {
        val resolver = resolver(
            candidate("je", "mi", "O"),
            candidate("être", "dë", "O"),
            candidate("père", "tàta, päa", ""),
            candidate("son", "azobi boto hayon-hayon", "O")
        )
        val result = assembleAttestedPhrase("je suis ton père") { segment ->
            if (segment.contains(' ')) return@assembleAttestedPhrase null
            resolver.resolve(segment)?.let {
                RecognizedPhraseSegment(
                    source = segment,
                    translation = it.saamaka,
                    matchedSource = it.matchedFrench,
                    alternatives = it.alternatives
                )
            }
        }!!

        assertEquals("mi tàta", result.translation)
        assertEquals(listOf("suis", "ton"), result.untranslatedSegments)
        assertEquals(2, result.recognizedSegments.size)
    }

    private fun resolver(vararg candidates: FrenchTranslationCandidate) =
        FrenchFallbackResolver(candidates.toList())

    private fun candidate(french: String, saamaka: String, status: String) =
        FrenchTranslationCandidate(french, saamaka, status)
}
