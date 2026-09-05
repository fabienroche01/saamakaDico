package com.saamaka.dico.testeurs

import com.saamaka.dico.testeurs.model.DictionaryEntry
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class RelatedExpressionRelevanceTest {
    @Test
    fun tonPapaNeverMatchesBatonBySubstring() {
        val results = relevantRelatedExpressions(
            input = "ton papa",
            candidates = listOf(
                entry(1, "bâton"),
                entry(2, "papa"),
                entry(3, "le papa arrive")
            ),
            languageCode = "fr"
        )

        assertEquals(listOf(2, 3), results.map { it.id })
    }

    @Test
    fun usesOnlyAttestedSynonymsAndReturnsEmptyWithoutRelevantWholeWords() {
        val synonym = relevantRelatedExpressions(
            input = "père",
            candidates = listOf(entry(1, "papa"), entry(2, "repère")),
            languageCode = "fr"
        )
        val unrelated = relevantRelatedExpressions(
            input = "luxe",
            candidates = listOf(entry(3, "bûche"), entry(4, "lumière")),
            languageCode = "fr"
        )

        assertEquals(listOf(1), synonym.map { it.id })
        assertTrue(unrelated.isEmpty())
        assertEquals(setOf("pere", "papa"), relatedCandidateSearchTerms("père", "fr"))
    }

    private fun entry(id: Int, french: String) = DictionaryEntry(
        id = id,
        french = french,
        saamaka = "attesté"
    )
}
