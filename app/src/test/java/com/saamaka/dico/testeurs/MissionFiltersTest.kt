package com.saamaka.dico.testeurs

import com.saamaka.dico.testeurs.model.DictionaryEntry
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class MissionFiltersTest {
    private val entries = listOf(
        entry(1, "École", "skoro", "D"),
        entry(2, "Maison", "", ""),
        entry(3, "Forêt", "busi", "O"),
        entry(4, "Ecolier", "skoro mii", ""),
        entry(5, "menacer", "poi", " d ")
    )
    private val classifications = classifyMissionEntries(
        entries = entries,
        completedIds = setOf(4)
    )

    @Test
    fun filtersUseExistingStatusesAndPreserveOrder() {
        assertEquals(listOf(1, 2, 3, 4, 5), ids(MissionFilter.ALL))
        assertEquals(listOf(1, 5), ids(MissionFilter.DOUBTFUL))
        assertEquals(listOf(2), ids(MissionFilter.TO_COMPLETE))
        assertEquals(listOf(3, 4), ids(MissionFilter.COMPLETED))
    }

    @Test
    fun displayedDoubtfulMenacerRemainsInDoubtfulSearchResults() {
        val menacer = entries.last()
        assertEquals(
            MissionVisualStatus.DOUBTFUL,
            classifications.getValue(menacer.id).visualStatus
        )

        val results = filterMissionEntries(
            entries = entries,
            filter = MissionFilter.DOUBTFUL,
            query = "menacer",
            classifications = classifications
        )

        assertEquals(listOf(menacer), results)
        assertTrue(results.single().saamaka.isNotBlank())
    }

    @Test
    fun searchIsCaseAndAccentInsensitiveAndCombinesWithFilter() {
        assertEquals(listOf(1, 4), ids(MissionFilter.ALL, "ECO"))
        assertEquals(listOf(1), ids(MissionFilter.DOUBTFUL, "éCO"))
        assertEquals(listOf(3), ids(MissionFilter.ALL, "FORET"))
    }

    @Test
    fun completeDoubtfulEssoufflerCanBeValidated() {
        val entry = entry(10, "essouffler", "hanse", "D")
        val classification = classifyMissionEntry(entry, hasLocalValidation = false)
        val policy = missionValidationPolicy(entry, classification)

        assertEquals(MissionVisualStatus.DOUBTFUL, classification.visualStatus)
        assertTrue(policy.canValidate)
        assertEquals(false, policy.correctionIsPrimary)
    }

    @Test
    fun discuterWithoutSaamakaRequiresCorrection() {
        val entry = entry(11, "discuter", "", "")
        val classification = classifyMissionEntry(entry, hasLocalValidation = false)
        val policy = missionValidationPolicy(entry, classification)

        assertEquals(MissionVisualStatus.TO_COMPLETE, classification.visualStatus)
        assertEquals(false, policy.canValidate)
        assertTrue(policy.correctionIsPrimary)
        assertEquals("Traduction saamaka manquante", policy.missingMessage)
    }

    @Test
    fun validatedEntryCannotBeValidatedTwiceButCanStillBeCorrected() {
        val entry = entry(12, "mot", "woto", "D")
        val classification = classifyMissionEntry(entry, hasLocalValidation = true)
        val policy = missionValidationPolicy(entry, classification)

        assertEquals(MissionVisualStatus.ALREADY_VALIDATED, classification.visualStatus)
        assertEquals(false, policy.canValidate)
        assertEquals(false, policy.correctionIsPrimary)
    }

    private fun ids(filter: MissionFilter, query: String = "") =
        filterMissionEntries(entries, filter, query, classifications).map { it.id }

    private fun entry(id: Int, french: String, saamaka: String, valide: String) =
        DictionaryEntry(
            id = id,
            french = french,
            english = "",
            dutch = "",
            saamaka = saamaka,
            categorie = "Test",
            valide = valide
        )
}
