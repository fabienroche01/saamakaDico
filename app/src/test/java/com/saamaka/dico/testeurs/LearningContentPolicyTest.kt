package com.saamaka.dico.testeurs

import com.saamaka.dico.testeurs.model.DictionaryEntry
import org.junit.Assert.assertEquals
import org.junit.Test

class LearningContentPolicyTest {
    private fun entry(id: Int, valide: String = "O", category: String = "Base") = DictionaryEntry(id = id, french = "mot$id", english = "", dutch = "", saamaka = "w$id", categorie = category, valide = valide)
    @Test fun trustedSelectionRejectsDoubtfulLocalChangesDeletionAndEmptyCategory() {
        val result = trustedLearningEntries(
            listOf(entry(1), entry(2, "D"), entry(3), entry(4), entry(5, category = "")),
            correctedEntryIds = setOf(3), deletionProposalEntryIds = setOf(4)
        )
        assertEquals(listOf(1), result.map { it.id })
    }
}
