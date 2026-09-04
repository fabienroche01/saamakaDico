package com.saamaka.dico.testeurs

import org.junit.Assert.assertEquals
import org.junit.Test
import java.io.File

class TesterReleasePackageBuilderTest {
    @Test
    fun `uses generated id for approved new entry audio`() {
        val audio = approvedAudio(entryId = null, localId = "local-1", fileName = "new_local-1_Ana.m4a")

        val decision = TesterReleasePackageBuilder.classifyAudios(
            listOf(audio),
            generatedEntryIds = mapOf("local-1" to 12000),
            existingEntryIds = setOf(12000)
        ).single()

        assertEquals(12000, decision.targetEntryId)
        assertEquals(ReleaseAudioDisposition.READY, decision.disposition)
    }

    @Test
    fun `marks every audio targeting the same id as conflict`() {
        val decisions = TesterReleasePackageBuilder.classifyAudios(
            listOf(
                approvedAudio(77, null, "srm_77_Ana.m4a"),
                approvedAudio(77, null, "srm_77_Bob.m4a")
            ),
            generatedEntryIds = emptyMap(),
            existingEntryIds = setOf(77)
        )

        assertEquals(2, decisions.size)
        assertEquals(2, decisions.count { it.disposition == ReleaseAudioDisposition.CONFLICT })
    }

    @Test
    fun `keeps unresolved and missing ids as orphan`() {
        val decisions = TesterReleasePackageBuilder.classifyAudios(
            listOf(
                approvedAudio(null, "missing-local", "new_missing.m4a"),
                approvedAudio(99, null, "srm_99_Ana.m4a")
            ),
            generatedEntryIds = emptyMap(),
            existingEntryIds = emptySet()
        )

        assertEquals(2, decisions.count { it.disposition == ReleaseAudioDisposition.ORPHAN })
    }

    private fun approvedAudio(entryId: Int?, localId: String?, fileName: String): ApprovedTesterAudio {
        val zip = File("SaamakaDico_Ana_2026-01-01_1200.zip")
        return ApprovedTesterAudio(
            audio = ConsolidatedAudio(
                fileNames = setOf(fileName),
                entryId = entryId,
                newEntryLocalId = localId,
                status = if (entryId != null) {
                    ConsolidatedAudioStatus.LINKED_DICTIONARY_ENTRY
                } else {
                    ConsolidatedAudioStatus.LINKED_NEW_ENTRY
                },
                sources = setOf(TesterExportSource(zip.name, "Ana"))
            ),
            sourceZip = zip,
            sourceFileName = fileName
        )
    }
}
