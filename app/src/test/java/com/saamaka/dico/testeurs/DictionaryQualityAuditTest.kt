package com.saamaka.dico.testeurs

import com.saamaka.dico.testeurs.model.DictionaryEntry
import org.junit.Assert.assertEquals
import org.junit.Test

class DictionaryQualityAuditTest {
    private fun entry(
        id: Int,
        saamaka: String,
        french: String,
        category: String = "Animaux"
    ) = DictionaryEntry(
        id = id,
        french = french,
        saamaka = saamaka,
        categorie = category
    )

    @Test
    fun detectsDuplicatesMissingFieldsAndCategoryVariants() {
        val audit = auditDictionaryEntries(
            listOf(
                entry(1, "Womi", "homme", "Animaux"),
                entry(2, " womi ", " Homme ", "animaux"),
                entry(3, "", "vide", "Maison"),
                entry(4, "foo", "", "Maison")
            )
        )
        assertEquals(1, audit.exactDuplicateGroups)
        assertEquals(1, audit.missingFrench)
        assertEquals(1, audit.missingSaamaka)
        assertEquals(1, audit.categoryVariantGroups)
    }
}
