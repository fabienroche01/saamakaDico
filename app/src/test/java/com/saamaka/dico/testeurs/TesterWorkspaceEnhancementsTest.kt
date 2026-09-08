package com.saamaka.dico.testeurs

import com.saamaka.dico.testeurs.model.DictionaryEntry
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class TesterWorkspaceEnhancementsTest {
    @Test
    fun filtersDistinguishTesterStates() {
        assertTrue(testerListFilterMatches(TesterListFilter.VALIDATED, true, false, false, false, false))
        assertTrue(testerListFilterMatches(TesterListFilter.CORRECTED, false, false, true, false, false))
        assertTrue(testerListFilterMatches(TesterListFilter.DELETION_PROPOSED, false, false, false, true, false))
        assertTrue(testerListFilterMatches(TesterListFilter.TO_REVIEW, false, false, false, false, true))
        assertTrue(testerListFilterMatches(TesterListFilter.UNTOUCHED, false, false, false, false, false))
        assertFalse(testerListFilterMatches(TesterListFilter.UNTOUCHED, false, true, false, false, false))
    }

    @Test
    fun categoryVariantsAreGroupedAndCounted() {
        val entries = listOf(
            DictionaryEntry(2, "a", "", "", "aa", "Animaux", ""),
            DictionaryEntry(3, "b", "", "", "bb", " animaux ", ""),
            DictionaryEntry(4, "c", "", "", "cc", "ANIMAUX", "")
        )
        val overview = buildCategoryOverviews(entries)
        assertEquals(1, overview.size)
        assertEquals(3, overview.single().count)
        assertEquals(3, overview.single().variants.size)
    }
}
