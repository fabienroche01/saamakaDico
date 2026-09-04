package com.saamaka.dico.testeurs

import java.io.File
import java.text.Normalizer
import java.security.MessageDigest
import java.util.Locale
import java.util.zip.ZipFile

data class TesterExportSource(val zipName: String, val tester: String)

data class ConsolidatedValidation(val entryId: Int, val sources: Set<TesterExportSource>)

data class ConsolidatedCorrection(
    val entryId: Int,
    val field: String,
    val proposedValue: String,
    val sources: Set<TesterExportSource>
)

data class CorrectionConflict(
    val entryId: Int,
    val field: String,
    val proposals: List<ConsolidatedCorrection>
)

data class ConsolidatedDeletion(
    val entryId: Int,
    val saamaka: String,
    val french: String,
    val sources: Set<TesterExportSource>,
    val requiresManualValidation: Boolean = true
)

data class ConsolidatedNewEntry(
    val localId: String,
    val saamaka: String,
    val french: String,
    val english: String,
    val dutch: String,
    val category: String,
    val audioFileName: String?,
    val sources: Set<TesterExportSource>
)

data class PossibleNewEntryDuplicate(
    val first: ConsolidatedNewEntry,
    val second: ConsolidatedNewEntry
)

enum class ConsolidatedAudioStatus { LINKED_DICTIONARY_ENTRY, LINKED_NEW_ENTRY, ORPHAN }

data class ConsolidatedAudio(
    val fileNames: Set<String>,
    val entryId: Int?,
    val newEntryLocalId: String?,
    val status: ConsolidatedAudioStatus,
    val sources: Set<TesterExportSource>
)

data class TesterExportConsolidationReport(
    val validations: List<ConsolidatedValidation>,
    val corrections: List<ConsolidatedCorrection>,
    val conflicts: List<CorrectionConflict>,
    val deletions: List<ConsolidatedDeletion>,
    val newEntries: List<ConsolidatedNewEntry>,
    val duplicateNewEntries: Int,
    val possibleDuplicateNewEntries: List<PossibleNewEntryDuplicate>,
    val audios: List<ConsolidatedAudio>,
    val errors: List<String>
) {
    val counters: Map<String, Int> = linkedMapOf(
        "validations" to validations.size,
        "corrections" to corrections.size,
        "conflicts" to conflicts.size,
        "deletions" to deletions.size,
        "newEntries" to newEntries.size,
        "duplicates" to duplicateNewEntries,
        "possibleDuplicates" to possibleDuplicateNewEntries.size,
        "audios" to audios.size,
        "errors" to errors.size
    )
}

/** Reads tester ZIP exports only. It never opens or modifies the dictionary database. */
class TesterExportConsolidator {
    fun consolidate(zipFiles: List<File>): TesterExportConsolidationReport {
        val parsed = zipFiles.map { parseZip(it) }
        val errors = parsed.flatMap { it.errors }

        val validations = parsed.flatMap { it.validations }
            .groupBy { it.entryId }
            .map { (entryId, values) -> ConsolidatedValidation(entryId, values.map { it.source }.toSet()) }
            .sortedBy { it.entryId }

        val corrections = parsed.flatMap { it.corrections }
            .groupBy { Triple(it.entryId, it.field, normalize(it.proposedValue)) }
            .map { (_, values) ->
                val first = values.first()
                ConsolidatedCorrection(first.entryId, first.field, first.proposedValue, values.map { it.source }.toSet())
            }
            .sortedWith(compareBy({ it.entryId }, { it.field }, { normalize(it.proposedValue) }))

        val conflicts = corrections.groupBy { it.entryId to it.field }
            .filterValues { it.size > 1 }
            .map { (key, proposals) -> CorrectionConflict(key.first, key.second, proposals) }
            .sortedWith(compareBy({ it.entryId }, { it.field }))

        val deletions = parsed.flatMap { it.deletions }
            .groupBy { Triple(it.entryId, normalize(it.saamaka), normalize(it.french)) }
            .map { (_, values) ->
                val first = values.first()
                ConsolidatedDeletion(first.entryId, first.saamaka, first.french, values.map { it.source }.toSet())
            }
            .sortedBy { it.entryId }

        val rawNewEntries = parsed.flatMap { it.newEntries }
        val groupedNewEntries = rawNewEntries.groupBy { newEntryKey(it) }
        val newEntries = groupedNewEntries.map { (_, values) ->
            val first = values.first()
            ConsolidatedNewEntry(
                first.localId, first.saamaka, first.french, first.english, first.dutch,
                first.category, first.audioFileName, values.map { it.source }.toSet()
            )
        }
        val duplicateCount = groupedNewEntries.values.sumOf { (it.size - 1).coerceAtLeast(0) }
        val possibleDuplicates = buildList {
            for (firstIndex in newEntries.indices) {
                for (secondIndex in firstIndex + 1 until newEntries.size) {
                    val first = newEntries[firstIndex]
                    val second = newEntries[secondIndex]
                    if (isPossibleDuplicate(first, second)) add(PossibleNewEntryDuplicate(first, second))
                }
            }
        }

        val audioNamesByNewEntry = rawNewEntries.mapNotNull { entry ->
            entry.audioFileName?.takeIf { it.isNotBlank() }?.let { it.lowercase(Locale.ROOT) to entry.localId }
        }.toMap()
        val rawAudios = parsed.flatMap { archive ->
            archive.audios.map { audio ->
                val dictionaryId = DICTIONARY_AUDIO.matchEntire(audio.fileName)?.groupValues?.get(1)?.toIntOrNull()
                val localId = audioNamesByNewEntry[audio.fileName.lowercase(Locale.ROOT)]
                val status = when {
                    dictionaryId != null -> ConsolidatedAudioStatus.LINKED_DICTIONARY_ENTRY
                    localId != null -> ConsolidatedAudioStatus.LINKED_NEW_ENTRY
                    else -> ConsolidatedAudioStatus.ORPHAN
                }
                ParsedConsolidatedAudio(audio.fileName, audio.digest, dictionaryId, localId, status, archive.source)
            }
        }
        val audios = rawAudios.groupBy { Triple(it.digest, it.entryId, it.newEntryLocalId) }
            .map { (_, values) ->
                val first = values.first()
                ConsolidatedAudio(
                    values.map { it.fileName }.toSet(), first.entryId, first.newEntryLocalId,
                    first.status, values.map { it.source }.toSet()
                )
            }

        return TesterExportConsolidationReport(
            validations, corrections, conflicts, deletions, newEntries,
            duplicateCount, possibleDuplicates, audios, errors
        )
    }

    private fun parseZip(file: File): ParsedArchive {
        val fallbackSource = TesterExportSource(file.name, testerFromZipName(file.name))
        return try {
            ZipFile(file).use { zip ->
                val exportEntry = zip.getEntry(EXPORT_FILE)
                    ?: return ParsedArchive(fallbackSource, errors = listOf("${file.name}: export.txt missing"))
                val text = zip.getInputStream(exportEntry).bufferedReader(Charsets.UTF_8).use { it.readText() }
                val blocks = text.split(SEPARATOR).map { it.trim() }.filter { it.isNotBlank() }
                val audios = zip.entries().asSequence()
                    .filter { !it.isDirectory && it.name.startsWith("audio/") }
                    .map { entry ->
                        ParsedAudio(
                            entry.name.substringAfterLast('/'),
                            zip.getInputStream(entry).use { input ->
                                val digest = MessageDigest.getInstance("SHA-256")
                                val buffer = ByteArray(DEFAULT_BUFFER_SIZE)
                                while (true) {
                                    val read = input.read(buffer)
                                    if (read < 0) break
                                    digest.update(buffer, 0, read)
                                }
                                digest.digest().joinToString("") { "%02x".format(it) }
                            }
                        )
                    }
                    .toList()
                parseBlocks(fallbackSource, blocks).copy(audios = audios)
            }
        } catch (error: Exception) {
            ParsedArchive(fallbackSource, errors = listOf("${file.name}: ${error.message ?: error.javaClass.simpleName}"))
        }
    }

    private fun parseBlocks(fallbackSource: TesterExportSource, blocks: List<String>): ParsedArchive {
        val validations = mutableListOf<ParsedValidation>()
        val corrections = mutableListOf<ParsedCorrection>()
        val deletions = mutableListOf<ParsedDeletion>()
        val newEntries = mutableListOf<ParsedNewEntry>()

        blocks.forEach { block ->
            val values = block.lineSequence().mapNotNull { line ->
                val separator = line.indexOf(':')
                if (separator < 0) null else line.substring(0, separator).trim() to line.substring(separator + 1).trim()
            }.toMap()
            val tester = values["Correcteur"] ?: values["Testeur"] ?: fallbackSource.tester
            val source = fallbackSource.copy(tester = tester.ifBlank { fallbackSource.tester })
            val entryId = (values["ID mot"] ?: values["ID du mot"] ?: values["ID de l'entrée"])?.toIntOrNull()

            when {
                values["Type"] == "VALIDATED" && entryId != null -> validations += ParsedValidation(entryId, source)
                values["Type"] == "CORRECTED" && entryId != null -> addCorrections(entryId, values, source, corrections)
                entryId != null && (values.containsKey("Français proposé") || values.containsKey("Saamaka proposé")) ->
                    addCorrections(entryId, values, source, corrections)
                values.containsKey("Identifiant local") -> {
                    newEntries += ParsedNewEntry(
                        values["Identifiant local"].orEmpty(), values["Saamaka"].orEmpty(),
                        values["Français"].orEmpty(), values["English"].orEmpty(),
                        values["Nederlands"].orEmpty(), values["Catégorie"].orEmpty(),
                        values["Fichier audio"]?.takeUnless { it == "Aucun" }, source
                    )
                }
                values.containsKey("ID de l'entrée") && entryId != null -> {
                    deletions += ParsedDeletion(entryId, values["Saamaka"].orEmpty(), values["Français"].orEmpty(), source)
                }
            }
        }
        return ParsedArchive(fallbackSource, validations, corrections, deletions, newEntries)
    }

    private fun addCorrections(
        entryId: Int,
        values: Map<String, String>,
        source: TesterExportSource,
        output: MutableList<ParsedCorrection>
    ) {
        values["Français proposé"]?.takeIf { it.isNotBlank() }?.let {
            output += ParsedCorrection(entryId, "french", it, source)
        }
        values["Saamaka proposé"]?.takeIf { it.isNotBlank() }?.let {
            output += ParsedCorrection(entryId, "saamaka", it, source)
        }
    }

    private fun newEntryKey(entry: ParsedNewEntry) = listOf(
        entry.saamaka, entry.french, entry.english, entry.dutch, entry.category
    ).joinToString("|") { normalize(it) }

    private fun isPossibleDuplicate(first: ConsolidatedNewEntry, second: ConsolidatedNewEntry): Boolean {
        val saamakaFirst = normalize(first.saamaka)
        val saamakaSecond = normalize(second.saamaka)
        val frenchFirst = normalize(first.french)
        val frenchSecond = normalize(second.french)
        if (saamakaFirst.isBlank() || saamakaSecond.isBlank()) return false
        val saamakaClose = editDistance(saamakaFirst, saamakaSecond) <= 2
        val frenchClose = frenchFirst.isNotBlank() && frenchSecond.isNotBlank() &&
            editDistance(frenchFirst, frenchSecond) <= 2
        return saamakaClose && (frenchClose || frenchFirst == frenchSecond)
    }

    private fun editDistance(first: String, second: String): Int {
        var previous = IntArray(second.length + 1) { it }
        first.forEachIndexed { firstIndex, firstChar ->
            val current = IntArray(second.length + 1)
            current[0] = firstIndex + 1
            second.forEachIndexed { secondIndex, secondChar ->
                current[secondIndex + 1] = minOf(
                    current[secondIndex] + 1,
                    previous[secondIndex + 1] + 1,
                    previous[secondIndex] + if (firstChar == secondChar) 0 else 1
                )
            }
            previous = current
        }
        return previous[second.length]
    }

    private fun normalize(value: String): String = Normalizer.normalize(
        value.trim().lowercase(Locale.ROOT), Normalizer.Form.NFD
    ).replace("\\p{Mn}+".toRegex(), "").replace(Regex("\\s+"), " ")

    private fun testerFromZipName(name: String): String = name
        .removeSuffix(".zip").removePrefix("SaamakaDico_")
        .replace(Regex("_\\d{4}-\\d{2}-\\d{2}_\\d{4}(?:_\\d+)?$"), "")
        .ifBlank { "unknown" }

    private data class ParsedValidation(val entryId: Int, val source: TesterExportSource)
    private data class ParsedCorrection(val entryId: Int, val field: String, val proposedValue: String, val source: TesterExportSource)
    private data class ParsedDeletion(val entryId: Int, val saamaka: String, val french: String, val source: TesterExportSource)
    private data class ParsedNewEntry(
        val localId: String, val saamaka: String, val french: String, val english: String,
        val dutch: String, val category: String, val audioFileName: String?, val source: TesterExportSource
    )
    private data class ParsedAudio(val fileName: String, val digest: String)
    private data class ParsedConsolidatedAudio(
        val fileName: String,
        val digest: String,
        val entryId: Int?,
        val newEntryLocalId: String?,
        val status: ConsolidatedAudioStatus,
        val source: TesterExportSource
    )
    private data class ParsedArchive(
        val source: TesterExportSource,
        val validations: List<ParsedValidation> = emptyList(),
        val corrections: List<ParsedCorrection> = emptyList(),
        val deletions: List<ParsedDeletion> = emptyList(),
        val newEntries: List<ParsedNewEntry> = emptyList(),
        val audios: List<ParsedAudio> = emptyList(),
        val errors: List<String> = emptyList()
    )

    private companion object {
        const val EXPORT_FILE = "export.txt"
        val SEPARATOR = Regex("(?m)^-{3,}\\s*$")
        val DICTIONARY_AUDIO = Regex("^srm_(\\d+)_.*\\.m4a$", RegexOption.IGNORE_CASE)
    }
}
