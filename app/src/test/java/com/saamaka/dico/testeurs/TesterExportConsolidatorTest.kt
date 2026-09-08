package com.saamaka.dico.testeurs

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File
import java.nio.file.Files
import java.util.zip.ZipEntry
import java.util.zip.ZipOutputStream

class TesterExportConsolidatorTest {
    @Test
    fun `groups validations and reports conflicting corrections`() {
        val first = zip(
            "SaamakaDico_Ana_2026-01-01_1200.zip",
            review(12, "VALIDATED", "Ana") + "\n" + review(42, "CORRECTED", "Ana", saamaka = "odi")
        )
        val second = zip(
            "SaamakaDico_Bob_2026-01-01_1201.zip",
            review(12, "VALIDATED", "Bob") + "\n" + review(42, "CORRECTED", "Bob", saamaka = "odii")
        )

        val report = TesterExportConsolidator().consolidate(listOf(first, second))

        assertEquals(1, report.validations.size)
        assertEquals(2, report.validations.single().sources.size)
        assertEquals(2, report.corrections.size)
        assertEquals(1, report.conflicts.size)
        assertEquals("saamaka", report.conflicts.single().field)
    }

    @Test
    fun `marks exact and possible new entry duplicates`() {
        val exact = newEntry("one", "odi", "bonjour", "new_one_Ana.m4a", "Ana")
        val same = newEntry("two", "ODI", "Bonjour", "new_two_Bob.m4a", "Bob")
        val close = newEntry("three", "odii", "bonjour", null, "Cara")
        val report = TesterExportConsolidator().consolidate(
            listOf(
                zip("SaamakaDico_Ana_2026-01-01_1200.zip", exact),
                zip("SaamakaDico_Bob_2026-01-01_1201.zip", same),
                zip("SaamakaDico_Cara_2026-01-01_1202.zip", close)
            )
        )

        assertEquals(2, report.newEntries.size)
        assertEquals(1, report.duplicateNewEntries)
        assertEquals(1, report.possibleDuplicateNewEntries.size)
    }

    @Test
    fun `links known audio and keeps deletions manual`() {
        val export = """
            SAAMAKA DICO — PROPOSITIONS DE SUPPRESSION
            Proposition 1
            ID de l'entrée : 77
            Saamaka : odi
            Français : bonjour
            Testeur : Ana
            ---
        """.trimIndent()
        val report = TesterExportConsolidator().consolidate(
            listOf(
                zip(
                    "SaamakaDico_Ana_2026-01-01_1200.zip",
                    export,
                    mapOf("audio/srm_77_Ana.m4a" to byteArrayOf(1, 2), "audio/unknown.m4a" to byteArrayOf(3))
                )
            )
        )

        assertTrue(report.deletions.single().requiresManualValidation)
        assertEquals(1, report.audios.count { it.status == ConsolidatedAudioStatus.LINKED_DICTIONARY_ENTRY })
        assertEquals(1, report.audios.count { it.status == ConsolidatedAudioStatus.ORPHAN })
    }

    @Test
    fun `preserves category corrections and reports category conflicts`() {
        val first = zip(
            "SaamakaDico_Ana_2026-01-01_1200.zip",
            review(42, "CORRECTED", "Ana", category = "Animal")
        )
        val second = zip(
            "SaamakaDico_Bob_2026-01-01_1201.zip",
            review(42, "CORRECTED", "Bob", category = "Alimentation")
        )

        val report = TesterExportConsolidator().consolidate(listOf(first, second))

        assertEquals(2, report.corrections.count { it.field == "category" })
        assertEquals("category", report.conflicts.single().field)
    }

    private fun review(
        id: Int,
        type: String,
        tester: String,
        saamaka: String? = null,
        category: String? = null
    ) = """
        SAAMAKA DICO — REVUES LINGUISTIQUES
        Action 1
        ID mot : $id
        Français : bonjour
        Saamaka : odi
        Type : $type
        Correcteur : $tester
        ${saamaka?.let { "Saamaka proposé : $it" }.orEmpty()}
        ${category?.let { "Catégorie proposée : $it" }.orEmpty()}
        ------------------------------
    """.trimIndent()

    private fun newEntry(localId: String, saamaka: String, french: String, audio: String?, tester: String) = """
        SAAMAKA DICO — NOUVELLES ENTRÉES PROPOSÉES
        Proposition 1
        Identifiant local : $localId
        Saamaka : $saamaka
        Français : $french
        Catégorie : salutation
        Testeur : $tester
        Fichier audio : ${audio ?: "Aucun"}
        ---
    """.trimIndent()

    private fun zip(name: String, export: String, audio: Map<String, ByteArray> = emptyMap()): File {
        val file = Files.createTempDirectory("tester-export-test").resolve(name).toFile()
        file.deleteOnExit()
        ZipOutputStream(file.outputStream()).use { zip ->
            zip.putNextEntry(ZipEntry("export.txt"))
            zip.write(export.toByteArray(Charsets.UTF_8))
            zip.closeEntry()
            audio.forEach { (path, bytes) ->
                zip.putNextEntry(ZipEntry(path))
                zip.write(bytes)
                zip.closeEntry()
            }
        }
        return file
    }
}
