package com.saamaka.dico.testeurs

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class CorrectionProposalTest {
    @Test
    fun `records only fields whose proposed value changed`() {
        assertEquals("nouveau français", proposedCorrectionValue("français", "nouveau français"))
        assertEquals("", proposedCorrectionValue("saamaka", "saamaka"))
        assertEquals("Alimentation", proposedCorrectionValue("Animal", "Alimentation"))
    }

    @Test
    fun `supports french saamaka category and combined corrections independently`() {
        fun changes(french: String, saamaka: String, category: String) = listOf(
            proposedCorrectionValue("ancien français", french),
            proposedCorrectionValue("ancien saamaka", saamaka),
            proposedCorrectionValue("Animal", category)
        )

        assertEquals(listOf("nouveau français", "", ""), changes("nouveau français", "ancien saamaka", "Animal"))
        assertEquals(listOf("", "nouveau saamaka", ""), changes("ancien français", "nouveau saamaka", "Animal"))
        assertEquals(listOf("", "", "Alimentation"), changes("ancien français", "ancien saamaka", "Alimentation"))
        assertEquals(listOf("nouveau français", "", "Alimentation"), changes("nouveau français", "ancien saamaka", "Alimentation"))
    }

    @Test
    fun `old correction proposal has no category correction`() {
        val oldFormat = CorrectionProposal(
            id = 1L,
            entryId = 2,
            frenchCurrent = "bonjour",
            saamakaCurrent = "odi",
            frenchProposed = "salut",
            saamakaProposed = "",
            comment = "",
            testerName = "Tester",
            createdAt = 3L
        )

        assertEquals("", oldFormat.categoryCurrent)
        assertEquals("", oldFormat.categoryProposed)
        assertTrue(oldFormat.hasChanges())
    }

    @Test
    fun `category only is a real change and unchanged proposal is not`() {
        val categoryOnly = proposal(categoryCurrent = "Animal", categoryProposed = "Alimentation")
        val unchanged = proposal()

        assertTrue(categoryOnly.hasChanges())
        assertFalse(unchanged.hasChanges())
    }

    @Test
    fun `category correction is counted and exported`() {
        val categoryOnly = proposal(categoryCurrent = "Animal", categoryProposed = "Alimentation")
        val unchanged = proposal(id = 2L)
        val realCorrections = listOf(categoryOnly, unchanged).filter(CorrectionProposal::hasChanges)
        val export = correctionExportText(listOf(categoryOnly, unchanged))

        assertEquals(1, realCorrections.size)
        assertTrue(export.contains("Catégorie actuelle : Animal"))
        assertTrue(export.contains("Catégorie proposée : Alimentation"))
    }

    private fun proposal(
        id: Long = 1L,
        frenchProposed: String = "",
        saamakaProposed: String = "",
        categoryCurrent: String = "Animal",
        categoryProposed: String = ""
    ) = CorrectionProposal(
        id = id,
        entryId = 2,
        frenchCurrent = "bonjour",
        saamakaCurrent = "odi",
        frenchProposed = frenchProposed,
        saamakaProposed = saamakaProposed,
        comment = "",
        testerName = "Tester",
        createdAt = 3L,
        categoryCurrent = categoryCurrent,
        categoryProposed = categoryProposed
    )
}
