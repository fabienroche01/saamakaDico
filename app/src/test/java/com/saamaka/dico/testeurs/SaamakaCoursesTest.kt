package com.saamaka.dico.testeurs

import com.saamaka.dico.testeurs.model.DictionaryEntry
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class SaamakaCoursesTest {
    @Test
    fun `course content uses only validated matching dictionary entries`() {
        val lesson = SaamakaCourseLesson("food", UiCopyKey.FOOD, setOf("Alimentation"))
        val valid = DictionaryEntry(236, "Aubergine", saamaka = "boulase", categorie = "Alimentation", valide = "O")
        val unvalidated = valid.copy(id = 237, valide = "D")
        val unrelated = DictionaryEntry(243, "Aujourd'hui", saamaka = "tidé", categorie = "Temps", valide = "O")

        assertEquals(listOf(valid), courseEntries(lesson, listOf(unvalidated, unrelated, valid)))
    }

    @Test
    fun `lesson completion is stable and preserves prior progress`() {
        val first = completedLessonsAfter(emptySet(), "family")
        val second = completedLessonsAfter(first, "food")
        val repeated = completedLessonsAfter(second, "family")

        assertEquals(setOf("family", "food"), repeated)
    }

    @Test
    fun `primary navigation destinations remain unchanged`() {
        val primary = MainTab.entries.take(5)

        assertEquals(listOf(MainTab.HOME, MainTab.SEARCH, MainTab.FAVORITES, MainTab.LEARN, MainTab.MORE), primary)
        assertFalse(MainTab.entries.any { it.name == "COURSES" })
        assertTrue(saamakaCourseLevels.flatMap { it.lessons }.size == 16)
    }
}
