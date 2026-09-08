package com.saamaka.dico.testeurs

import org.junit.Assert.assertEquals
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
    }
}
