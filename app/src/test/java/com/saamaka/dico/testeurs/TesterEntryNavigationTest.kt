package com.saamaka.dico.testeurs

import com.saamaka.dico.testeurs.model.DictionaryEntry
import org.junit.Assert.assertEquals
import org.junit.Test

class TesterEntryNavigationTest {
    @Test
    fun `skips deletion proposals while preserving forward position`() {
        val entries = (1..5).map(::entry)

        assertEquals(3, nextTesterEntry(entries, 1, emptySet(), setOf(1, 2))?.id)
        assertEquals(4, nextTesterEntry(entries, 2, emptySet(), setOf(1, 2, 3))?.id)
    }

    @Test
    fun `current filtered entry still advances from its all entries position`() {
        val entries = (1..4).map(::entry)

        assertEquals(3, nextTesterEntry(entries, 2, setOf(2), setOf(1))?.id)
    }

    @Test
    fun `wraps only after reaching the end`() {
        val entries = (1..4).map(::entry)

        assertEquals(2, nextTesterEntry(entries, 4, emptySet(), setOf(1))?.id)
    }

    @Test
    fun `skips blank and invalid dictionary entries`() {
        val entries = listOf(
            entry(1),
            entry(2, french = ""),
            entry(3, saamaka = "#NAME?"),
            entry(4)
        )

        assertEquals(4, nextTesterEntry(entries, 1, emptySet(), emptySet())?.id)
    }

    private fun entry(
        id: Int,
        french: String = "français $id",
        saamaka: String = "saamaka $id"
    ) = DictionaryEntry(
        id = id,
        french = french,
        english = "",
        dutch = "",
        saamaka = saamaka,
        categorie = ""
    )
}
