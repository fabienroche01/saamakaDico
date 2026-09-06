package com.saamaka.dico.testeurs

import java.io.File
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class UiLocalizationTest {
    @Test
    fun everyCatalogEntryResolvesInAllFourLanguages() {
        UiLanguage.entries.forEach { language ->
            val strings = stringsFor(language)
            UiCopyKey.entries.forEach { key ->
                assertTrue("Blank $key for $language", strings.ui(key).isNotBlank())
            }
        }
    }

    @Test
    fun importantLabelsAreLocalizedInAllFourLanguages() {
        val expected = mapOf(
            UiLanguage.FRENCH to listOf("Accueil", "Apprendre", "Recherche", "Restaurer les achats"),
            UiLanguage.ENGLISH to listOf("Home", "Learn", "Search", "Restore purchases"),
            UiLanguage.DUTCH to listOf("Start", "Leren", "Zoeken", "Aankopen herstellen"),
            UiLanguage.SAAMAKA to listOf("Wosu", "Learn", "Suku", "Restore purchases")
        )

        expected.forEach { (language, labels) ->
            val strings = stringsFor(language)
            assertEquals(labels[0], strings.ui(UiCopyKey.HOME))
            assertEquals(labels[1], strings.ui(UiCopyKey.LEARN))
            assertEquals(labels[2], strings.search)
            assertEquals(labels[3], strings.ui(UiCopyKey.RESTORE_PURCHASES))
        }
    }

    @Test
    fun uncertainSaamakaCopyIsExplicitlyTrackedAndFallsBackToEnglish() {
        assertTrue(UiCopyKey.RESTORE_PURCHASES in saamakaCopyAwaitingValidation)
        assertEquals(
            stringsFor(UiLanguage.ENGLISH).ui(UiCopyKey.RESTORE_PURCHASES),
            stringsFor(UiLanguage.SAAMAKA).ui(UiCopyKey.RESTORE_PURCHASES)
        )
        assertFalse(UiCopyKey.HOME in saamakaCopyAwaitingValidation)
    }

    @Test
    fun grammaticalTranslationLabelIsLocalizedWithSaamakaEnglishFallback() {
        assertEquals("Traduction grammaticale", stringsFor(UiLanguage.FRENCH).ui(UiCopyKey.GRAMMATICAL_TRANSLATION))
        assertEquals("Grammatical translation", stringsFor(UiLanguage.ENGLISH).ui(UiCopyKey.GRAMMATICAL_TRANSLATION))
        assertEquals("Grammaticale vertaling", stringsFor(UiLanguage.DUTCH).ui(UiCopyKey.GRAMMATICAL_TRANSLATION))
        assertEquals(
            "Traduction grammaticale partielle",
            stringsFor(UiLanguage.FRENCH).ui(UiCopyKey.GRAMMATICAL_PARTIAL_TRANSLATION)
        )
        assertEquals(
            "Grammatical translation",
            stringsFor(UiLanguage.SAAMAKA).ui(UiCopyKey.GRAMMATICAL_TRANSLATION)
        )
    }

    @Test
    fun lexicalFallbackLabelsAreLocalizedWithSaamakaEnglishFallback() {
        assertEquals("Traduction mot à mot", stringsFor(UiLanguage.FRENCH).ui(UiCopyKey.WORD_BY_WORD_TRANSLATION))
        assertEquals("Word-for-word translation", stringsFor(UiLanguage.ENGLISH).ui(UiCopyKey.WORD_BY_WORD_TRANSLATION))
        assertEquals("Woord-voor-woordvertaling", stringsFor(UiLanguage.DUTCH).ui(UiCopyKey.WORD_BY_WORD_TRANSLATION))
        assertEquals("Word-for-word translation", stringsFor(UiLanguage.SAAMAKA).ui(UiCopyKey.WORD_BY_WORD_TRANSLATION))
        assertEquals("Traduction partielle", stringsFor(UiLanguage.FRENCH).ui(UiCopyKey.PARTIAL_TRANSLATION))
        assertEquals("Partial translation", stringsFor(UiLanguage.SAAMAKA).ui(UiCopyKey.PARTIAL_TRANSLATION))
    }

    @Test
    fun storedLanguageIsRestoredWithoutTouchingTesterData() {
        assertEquals(UiLanguage.ENGLISH, UiLanguageStore.decode("ENGLISH"))
        assertEquals(UiLanguage.DUTCH, UiLanguageStore.decode("DUTCH"))
        assertEquals(UiLanguage.SAAMAKA, UiLanguageStore.decode("SAAMAKA"))
        assertEquals(UiLanguage.FRENCH, UiLanguageStore.decode("unknown"))
    }

    @Test
    fun uiFilesDoNotReintroduceKnownFrenchComposeLiterals() {
        val projectRoot = generateSequence(File(".").canonicalFile) { it.parentFile }
            .first { File(it, "app/src/main").isDirectory }
        val roots = listOf(
            File(projectRoot, "app/src/main/java/com/saamaka/dico/testeurs/MainActivity.kt"),
            File(projectRoot, "app/src/main/java/com/saamaka/dico/testeurs/ui")
        )
        val forbidden = listOf(
            "Text(\"Accueil\")",
            "Text(\"Apprendre\")",
            "Text(\"Restaurer les achats\")",
            "Text(\"Gérer mon abonnement\")",
            "Text(\"Traduire cette phrase\")",
            "Text(\"Aucune expression complète trouvée dans le dictionnaire\")",
            "contentDescription = \"Rechercher\"",
            "contentDescription = \"Écouter\"",
            "Text(\"Testeur\")",
            "Text(\"Exporter mon travail\")",
            "Text(\"Catégorie\")",
            "contentDescription = \"Retirer des favoris\"",
            "contentDescription = \"Retirer de l’historique\""
        )
        val files = roots.flatMap { root ->
            if (root.isDirectory) root.walkTopDown().filter { it.extension == "kt" }.toList()
            else listOf(root)
        }

        files.forEach { file ->
            val source = file.readText()
            forbidden.forEach { literal ->
                assertFalse(
                    "Hard-coded French UI text in ${file.path}: $literal",
                    source.contains(literal)
                )
            }
        }
    }
}
