package com.saamaka.dico.testeurs

import com.saamaka.dico.testeurs.model.DictionaryEntry
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class MultilingualSearchTest {
    private val entries = listOf(
        entry(1, saamaka = "sun", french = "nager", english = "swim", dutch = "zwemmen"),
        entry(2, saamaka = "sönu", french = "soleil", english = "Sun", dutch = "zon"),
        entry(3, saamaka = "liba wosu", french = "maison haute", english = "high house", dutch = "hoog huis"),
        entry(4, saamaka = "wáta", french = "eau", english = "water", dutch = "water"),
        entry(5, saamaka = "foo", french = "", english = "", dutch = "")
    )

    @Test
    fun englishSunDoesNotLeakFromSaamaka() {
        val results = filterAndRankByLanguage(entries, "sun", "en")
        assertEquals(listOf(2), results.map { it.id })
        assertEquals("Sun", searchTextForLanguage(results.single(), "en"))
        assertFalse(results.any { it.id == 1 })
    }

    @Test
    fun eachSpecificFilterUsesOnlyItsOwnNonEmptyColumn() {
        assertEquals(listOf(1), filterAndRankByLanguage(entries, "sun", "srm").map { it.id })
        assertEquals(listOf(1), filterAndRankByLanguage(entries, "nager", "fr").map { it.id })
        assertEquals(listOf(3), filterAndRankByLanguage(entries, "high", "en").map { it.id })
        assertEquals(listOf(3), filterAndRankByLanguage(entries, "hoog", "nl").map { it.id })
        assertTrue(filterAndRankByLanguage(entries, "foo", "en").isEmpty())
    }

    @Test
    fun filtersDoNotMatchTextPresentOnlyInAnotherLanguage() {
        assertTrue(filterAndRankByLanguage(entries, "nager", "srm").isEmpty())
        assertTrue(filterAndRankByLanguage(entries, "zwemmen", "fr").isEmpty())
        assertTrue(filterAndRankByLanguage(entries, "liba", "en").isEmpty())
        assertTrue(filterAndRankByLanguage(entries, "soleil", "nl").isEmpty())
    }

    @Test
    fun allLanguagesCanFindFromEveryColumn() {
        fun all(query: String) = listOf("srm", "fr", "en", "nl")
            .flatMap { filterAndRankByLanguage(entries, query, it) }
            .distinctBy { it.id }

        assertTrue(all("wáta").any { it.id == 4 })
        assertTrue(all("eau").any { it.id == 4 })
        assertTrue(all("water").any { it.id == 4 })
        assertTrue(all("zon").any { it.id == 2 })
    }

    @Test
    fun normalizationAndRankingArePreserved() {
        val ranked = listOf(
            entry(10, english = "sunshine"),
            entry(11, english = "the sun rises"),
            entry(12, english = "midsummer sunbeam"),
            entry(13, english = "  SÚN  ")
        )
        assertEquals(
            listOf(13, 10, 11, 12),
            filterAndRankByLanguage(ranked, " sun ", "en").map { it.id }
        )
    }

    @Test
    fun resultCarriesTheLanguageThatMatched() {
        val swim = entries.first { it.id == 1 }
        val sun = entries.first { it.id == 2 }
        assertEquals(AppLanguage.ENGLISH, matchingLanguageForEntry(sun, "sun", AppLanguage.ENGLISH))
        assertEquals("Sun", searchTextForLanguage(sun, AppLanguage.ENGLISH.code))
        assertEquals(AppLanguage.FRENCH, matchingLanguageForEntry(swim, "nager", AppLanguage.FRENCH))
        assertEquals("nager", searchTextForLanguage(swim, AppLanguage.FRENCH.code))
        assertEquals(AppLanguage.DUTCH, matchingLanguageForEntry(swim, "zwemmen", AppLanguage.DUTCH))
        assertEquals("zwemmen", searchTextForLanguage(swim, AppLanguage.DUTCH.code))
        assertEquals(AppLanguage.SAAMAKA, matchingLanguageForEntry(swim, "sun", AppLanguage.SAAMAKA))
        assertEquals("sun", searchTextForLanguage(swim, AppLanguage.SAAMAKA.code))
        assertEquals(AppLanguage.ENGLISH, matchingLanguageForEntry(sun, "sun", null))
    }

    @Test
    fun saamakaSearchUsesInterfaceTranslationWithFrenchFallback() {
        val swim = entries.first { it.id == 1 }
        assertEquals(AppLanguage.ENGLISH, preferredTranslationLanguage(swim, UiLanguage.ENGLISH))
        assertEquals(AppLanguage.DUTCH, preferredTranslationLanguage(swim, UiLanguage.DUTCH))
        assertEquals(AppLanguage.FRENCH, preferredTranslationLanguage(swim, UiLanguage.SAAMAKA))
    }

    private fun entry(
        id: Int,
        saamaka: String = "srm-$id",
        french: String = "fr-$id",
        english: String = "en-$id",
        dutch: String = "nl-$id"
    ) = DictionaryEntry(
        id = id,
        french = french,
        english = english,
        dutch = dutch,
        saamaka = saamaka
    )
}
