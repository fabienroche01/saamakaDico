package com.saamaka.dico.testeurs

import android.database.sqlite.SQLiteDatabase
import java.io.File
import java.io.FileOutputStream
import java.util.UUID
import java.util.zip.ZipFile

data class ApprovedTesterAudio(
    val audio: ConsolidatedAudio,
    val sourceZip: File,
    val sourceFileName: String
)

data class TesterReleasePackageRequest(
    val generatedDatabase: File,
    val approval: TesterChangeApproval,
    val approvedAudios: List<ApprovedTesterAudio>,
    val applyReport: TesterApplyReport,
    val outputDirectory: File
)

enum class ReleaseAudioDisposition { READY, ORPHAN, CONFLICT }

data class ReleaseAudioDecision(
    val approvedAudio: ApprovedTesterAudio,
    val targetEntryId: Int?,
    val disposition: ReleaseAudioDisposition,
    val reason: String
)

data class TesterReleasePackageResult(
    val packageDirectory: File?,
    val databaseFile: File?,
    val reportFile: File?,
    val audioDecisions: List<ReleaseAudioDecision>,
    val errors: List<String>
) {
    val succeeded: Boolean get() = packageDirectory != null && errors.isEmpty()
}

/** Builds a standalone integration package and never writes to project assets or resources. */
class TesterReleasePackageBuilder {
    fun build(request: TesterReleasePackageRequest): TesterReleasePackageResult {
        val errors = validateRequest(request).toMutableList()
        if (errors.isNotEmpty()) return failed(errors)

        val temporaryDirectory = File(
            request.outputDirectory.parentFile,
            ".${request.outputDirectory.name}.${UUID.randomUUID()}.tmp"
        )
        return try {
            check(temporaryDirectory.mkdirs()) { "Unable to create temporary package directory" }
            val packagedDatabase = File(temporaryDirectory, DATABASE_FILE_NAME)
            copyFileSafely(request.generatedDatabase, packagedDatabase)

            val existingIds = readEntryIds(packagedDatabase)
            val decisions = classifyAudios(
                request.approvedAudios,
                request.applyReport.generatedEntryIds,
                existingIds
            )
            val audioDirectory = File(temporaryDirectory, AUDIO_DIRECTORY_NAME)
            decisions.filter { it.disposition == ReleaseAudioDisposition.READY }.forEach { decision ->
                if (!audioDirectory.exists()) check(audioDirectory.mkdirs()) { "Unable to create audio directory" }
                val target = File(audioDirectory, "srm_${decision.targetEntryId}.m4a")
                check(!target.exists()) { "Audio output already exists: ${target.name}" }
                copyApprovedAudio(decision.approvedAudio, target)
            }

            val reportFile = File(temporaryDirectory, REPORT_FILE_NAME)
            check(!reportFile.exists()) { "Release report already exists" }
            reportFile.writeText(buildReport(request, decisions), Charsets.UTF_8)

            check(!request.outputDirectory.exists()) { "Output package already exists" }
            check(temporaryDirectory.renameTo(request.outputDirectory)) {
                "Unable to finalize tester release package"
            }
            TesterReleasePackageResult(
                request.outputDirectory,
                File(request.outputDirectory, DATABASE_FILE_NAME),
                File(request.outputDirectory, REPORT_FILE_NAME),
                decisions,
                emptyList()
            )
        } catch (error: Exception) {
            runCatching { cleanTemporaryDirectory(temporaryDirectory, request.outputDirectory) }
            failed(listOf(error.message ?: error.javaClass.simpleName))
        } finally {
            runCatching { cleanTemporaryDirectory(temporaryDirectory, request.outputDirectory) }
        }
    }

    private fun validateRequest(request: TesterReleasePackageRequest): List<String> = buildList {
        if (!request.applyReport.succeeded) add("Tester apply report is not successful")
        if (!request.generatedDatabase.isFile) add("Generated database not found")
        if (request.outputDirectory.exists()) add("Output package already exists")
        if (request.applyReport.outputDatabase?.canonicalFile != request.generatedDatabase.canonicalFile) {
            add("Generated database does not match the apply report")
        }
        request.approvedAudios.forEach { approved ->
            if (!approved.sourceZip.isFile) add("Audio source ZIP not found: ${approved.sourceZip.name}")
            if (approved.sourceFileName !in approved.audio.fileNames) {
                add("Audio file was not part of the approved consolidated item: ${approved.sourceFileName}")
            }
            if (approved.audio.sources.none { it.zipName == approved.sourceZip.name }) {
                add("Audio ZIP was not part of the approved consolidated item: ${approved.sourceZip.name}")
            }
        }
    }

    private fun copyApprovedAudio(approved: ApprovedTesterAudio, target: File) {
        ZipFile(approved.sourceZip).use { zip ->
            val entry = zip.getEntry("audio/${approved.sourceFileName}")
                ?: error("Approved audio missing from ZIP: ${approved.sourceFileName}")
            check(!entry.isDirectory) { "Approved audio is not a file: ${approved.sourceFileName}" }
            zip.getInputStream(entry).use { input ->
                FileOutputStream(target).use { output ->
                    input.copyTo(output)
                    output.fd.sync()
                }
            }
            check(target.length() > 0L) { "Approved audio is empty: ${approved.sourceFileName}" }
        }
    }

    private fun readEntryIds(databaseFile: File): Set<Int> {
        val database = SQLiteDatabase.openDatabase(
            databaseFile.absolutePath,
            null,
            SQLiteDatabase.OPEN_READONLY
        )
        return try {
            database.rawQuery("SELECT id FROM dictionnaire", null).use { cursor ->
                buildSet { while (cursor.moveToNext()) add(cursor.getInt(0)) }
            }
        } finally {
            database.close()
        }
    }

    private fun buildReport(
        request: TesterReleasePackageRequest,
        decisions: List<ReleaseAudioDecision>
    ): String = buildString {
        appendLine("SAAMAKA DICO — TESTER RELEASE PACKAGE")
        appendLine("DB prepared: $DATABASE_FILE_NAME")
        appendLine()
        appendLine("Applied changes:")
        request.applyReport.applied.forEach { appendLine("- $it") }
        appendLine("Ignored changes:")
        request.applyReport.ignored.forEach { appendLine("- $it") }
        appendLine()
        appendLine("Generated IDs:")
        request.applyReport.generatedEntryIds.forEach { (localId, entryId) ->
            appendLine("- $localId -> $entryId")
        }
        appendLine()
        appendLine("Audio ready:")
        decisions.filter { it.disposition == ReleaseAudioDisposition.READY }.forEach {
            appendLine("- ${it.approvedAudio.sourceZip.name}/${it.approvedAudio.sourceFileName} -> srm_${it.targetEntryId}.m4a")
        }
        appendLine("Audio orphan:")
        decisions.filter { it.disposition == ReleaseAudioDisposition.ORPHAN }.forEach {
            appendLine("- ${it.approvedAudio.sourceZip.name}/${it.approvedAudio.sourceFileName}: ${it.reason}")
        }
        appendLine("Audio conflict:")
        decisions.filter { it.disposition == ReleaseAudioDisposition.CONFLICT }.forEach {
            appendLine("- ${it.approvedAudio.sourceZip.name}/${it.approvedAudio.sourceFileName}: ${it.reason}")
        }
        appendLine()
        appendLine("Approved validation IDs: ${request.approval.validationEntryIds.sorted().joinToString()}")
        appendLine("Approved corrections: ${request.approval.corrections.size}")
        appendLine("Approved new entries: ${request.approval.newEntries.size}")
        appendLine("Approved deletion IDs: ${request.approval.deletionEntryIds.sorted().joinToString()}")
    }

    private fun copyFileSafely(source: File, destination: File) {
        source.inputStream().use { input ->
            FileOutputStream(destination).use { output ->
                input.copyTo(output)
                output.fd.sync()
            }
        }
    }

    private fun cleanTemporaryDirectory(temporary: File, intendedOutput: File) {
        if (!temporary.exists()) return
        val intendedParent = intendedOutput.absoluteFile.parentFile?.canonicalFile ?: return
        check(temporary.canonicalFile.parentFile == intendedParent) {
            "Refusing to clean a temporary directory outside the package destination"
        }
        temporary.deleteRecursively()
    }

    private fun failed(errors: List<String>) = TesterReleasePackageResult(
        null, null, null, emptyList(), errors
    )

    companion object {
        fun classifyAudios(
            approvedAudios: List<ApprovedTesterAudio>,
            generatedEntryIds: Map<String, Int>,
            existingEntryIds: Set<Int>
        ): List<ReleaseAudioDecision> {
            val targets = approvedAudios.map { approved ->
                approved.audio.entryId
                    ?: approved.audio.newEntryLocalId?.let(generatedEntryIds::get)
            }
            val conflicts = targets.filterNotNull()
                .groupingBy { it }.eachCount().filterValues { it > 1 }.keys

            return approvedAudios.mapIndexed { index, approved ->
                val target = targets[index]
                when {
                    target == null -> ReleaseAudioDecision(
                        approved, null, ReleaseAudioDisposition.ORPHAN,
                        "No certain dictionary ID"
                    )
                    target !in existingEntryIds -> ReleaseAudioDecision(
                        approved, target, ReleaseAudioDisposition.ORPHAN,
                        "Target dictionary ID does not exist in prepared DB"
                    )
                    target in conflicts -> ReleaseAudioDecision(
                        approved, target, ReleaseAudioDisposition.CONFLICT,
                        "Multiple approved audios target entry ID $target"
                    )
                    else -> ReleaseAudioDecision(
                        approved, target, ReleaseAudioDisposition.READY,
                        "Certain dictionary ID"
                    )
                }
            }
        }

        const val DATABASE_FILE_NAME = "SaamakaDico_v11_25.db"
        const val AUDIO_DIRECTORY_NAME = "audios"
        const val REPORT_FILE_NAME = "tester-release-report.txt"
    }
}
