package com.saamaka.dico.testeurs

import android.content.ContentValues
import android.database.sqlite.SQLiteDatabase
import java.io.File
import java.io.FileOutputStream
import java.util.UUID

data class ApprovedTesterCorrection(
    val entryId: Int,
    val field: String,
    val approvedValue: String
)

data class ApprovedTesterNewEntry(
    val localId: String,
    val saamaka: String,
    val french: String,
    val english: String,
    val dutch: String,
    val category: String
)

data class TesterChangeApproval(
    val validationEntryIds: Set<Int> = emptySet(),
    val corrections: List<ApprovedTesterCorrection> = emptyList(),
    val newEntries: List<ApprovedTesterNewEntry> = emptyList(),
    val deletionEntryIds: Set<Int> = emptySet(),
    val audios: List<ConsolidatedAudio> = emptyList()
)

data class TesterApplyReport(
    val outputDatabase: File?,
    val applied: List<String>,
    val ignored: List<String>,
    val errors: List<String>,
    val generatedEntryIds: Map<String, Int>
) {
    val succeeded: Boolean get() = outputDatabase != null && errors.isEmpty()
}

/**
 * Applies an explicit manual approval to a new database file. The source database is read and
 * copied only; it is never opened for writing and an existing output file is never overwritten.
 */
class TesterExportApplier {
    fun apply(
        sourceDatabase: File,
        outputDatabase: File,
        approval: TesterChangeApproval
    ): TesterApplyReport {
        val validationErrors = validateApproval(approval).toMutableList()
        if (!sourceDatabase.isFile) validationErrors += "Source dictionary database not found"
        if (outputDatabase.exists()) validationErrors += "Output database already exists"
        if (sourceDatabase.canonicalFile == outputDatabase.canonicalFile) {
            validationErrors += "Output database must be different from the source asset"
        }
        if (validationErrors.isNotEmpty()) {
            return TesterApplyReport(null, emptyList(), emptyList(), validationErrors, emptyMap())
        }

        outputDatabase.parentFile?.mkdirs()
        val temporary = File(
            outputDatabase.parentFile,
            ".${outputDatabase.name}.${UUID.randomUUID()}.tmp"
        )
        val applied = mutableListOf<String>()
        val ignored = mutableListOf<String>()
        val generatedIds = linkedMapOf<String, Int>()
        var database: SQLiteDatabase? = null

        return try {
            copyFileSafely(sourceDatabase, temporary)
            val writableDatabase = SQLiteDatabase.openDatabase(
                temporary.absolutePath,
                null,
                SQLiteDatabase.OPEN_READWRITE
            )
            database = writableDatabase
            verifySchema(writableDatabase)
            writableDatabase.beginTransaction()
            try {
                approval.validationEntryIds.sorted().forEach { entryId ->
                    requireEntry(writableDatabase, entryId)
                    val values = ContentValues().apply { put(COLUMN_VALIDATED, VALIDATED_VALUE) }
                    check(writableDatabase.update(TABLE, values, "$COLUMN_ID = ?", arrayOf(entryId.toString())) == 1)
                    applied += "VALIDATION entryId=$entryId"
                }

                approval.corrections.distinct().forEach { correction ->
                    if (canonicalField(correction.field) == COLUMN_CATEGORY && correction.approvedValue.isBlank()) {
                        ignored += "CORRECTION entryId=${correction.entryId} field=$COLUMN_CATEGORY: no proposed value"
                        return@forEach
                    }
                    requireEntry(writableDatabase, correction.entryId)
                    val column = when (correction.field.lowercase()) {
                        "french", "francais" -> COLUMN_FRENCH
                        "saamaka" -> COLUMN_SAAMAKA
                        "category", "categorie" -> COLUMN_CATEGORY
                        else -> error("Unsupported correction field: ${correction.field}")
                    }
                    val values = ContentValues().apply { put(column, correction.approvedValue) }
                    check(writableDatabase.update(TABLE, values, "$COLUMN_ID = ?", arrayOf(correction.entryId.toString())) == 1)
                    applied += "CORRECTION entryId=${correction.entryId} field=$column"
                }

                var nextId = nextEntryId(writableDatabase)
                approval.newEntries.forEach { entry ->
                    if (exactEntryExists(writableDatabase, entry)) {
                        ignored += "NEW_ENTRY localId=${entry.localId}: exact entry already exists"
                    } else {
                        val generatedId = nextId++
                        val values = ContentValues().apply {
                            put(COLUMN_ID, generatedId)
                            put(COLUMN_FRENCH, entry.french)
                            put(COLUMN_ENGLISH, entry.english)
                            put(COLUMN_DUTCH, entry.dutch)
                            put(COLUMN_SAAMAKA, entry.saamaka)
                            put(COLUMN_CATEGORY, entry.category)
                            put(COLUMN_EXAMPLE, "")
                            put(COLUMN_BIBLE_REFERENCE, "")
                            put(COLUMN_VALIDATED, VALIDATED_VALUE)
                            put(COLUMN_VALIDATED_EN, "")
                            put(COLUMN_VALIDATED_NL, "")
                        }
                        check(writableDatabase.insertOrThrow(TABLE, null, values) == generatedId.toLong())
                        generatedIds[entry.localId] = generatedId
                        applied += "NEW_ENTRY localId=${entry.localId} entryId=$generatedId"
                    }
                }

                approval.deletionEntryIds.sorted().forEach { entryId ->
                    requireEntry(writableDatabase, entryId)
                    check(writableDatabase.delete(TABLE, "$COLUMN_ID = ?", arrayOf(entryId.toString())) == 1)
                    applied += "DELETION entryId=$entryId"
                }

                approval.audios.forEach { audio ->
                    ignored += "AUDIO ${audio.fileNames.joinToString()}: SQLite has no audio column; package manually in res/raw"
                }

                writableDatabase.setTransactionSuccessful()
            } finally {
                writableDatabase.endTransaction()
            }
            verifyIntegrity(writableDatabase)
            writableDatabase.close()
            database = null

            check(temporary.renameTo(outputDatabase)) { "Unable to finalize output database" }
            TesterApplyReport(outputDatabase, applied, ignored, emptyList(), generatedIds)
        } catch (error: Exception) {
            runCatching { database?.close() }
            temporary.delete()
            TesterApplyReport(
                null,
                emptyList(),
                ignored,
                listOf(error.message ?: error.javaClass.simpleName),
                emptyMap()
            )
        } finally {
            runCatching { database?.close() }
            temporary.delete()
        }
    }

    private fun requireEntry(database: SQLiteDatabase, entryId: Int) {
        database.rawQuery(
            "SELECT 1 FROM $TABLE WHERE $COLUMN_ID = ? LIMIT 1",
            arrayOf(entryId.toString())
        ).use { cursor ->
            check(cursor.moveToFirst()) { "Dictionary entry $entryId does not exist" }
        }
    }

    private fun nextEntryId(database: SQLiteDatabase): Int = database.rawQuery(
        "SELECT COALESCE(MAX($COLUMN_ID), 0) + 1 FROM $TABLE",
        null
    ).use { cursor ->
        check(cursor.moveToFirst())
        cursor.getInt(0)
    }

    private fun exactEntryExists(database: SQLiteDatabase, entry: ApprovedTesterNewEntry): Boolean {
        val columns = listOf(COLUMN_SAAMAKA, COLUMN_FRENCH, COLUMN_ENGLISH, COLUMN_DUTCH, COLUMN_CATEGORY)
        val values = listOf(entry.saamaka, entry.french, entry.english, entry.dutch, entry.category)
        val where = columns.joinToString(" AND ") { "LOWER(TRIM(COALESCE($it, ''))) = LOWER(TRIM(?))" }
        return database.rawQuery("SELECT 1 FROM $TABLE WHERE $where LIMIT 1", values.toTypedArray()).use {
            it.moveToFirst()
        }
    }

    private fun verifySchema(database: SQLiteDatabase) {
        val columns = database.rawQuery("PRAGMA table_info($TABLE)", null).use { cursor ->
            buildSet {
                val nameIndex = cursor.getColumnIndexOrThrow("name")
                while (cursor.moveToNext()) add(cursor.getString(nameIndex))
            }
        }
        check(REQUIRED_COLUMNS.all { it in columns }) { "Unsupported dictionary database schema" }
    }

    private fun verifyIntegrity(database: SQLiteDatabase) {
        database.rawQuery("PRAGMA quick_check", null).use { cursor ->
            check(cursor.moveToFirst() && cursor.getString(0).equals("ok", ignoreCase = true)) {
                "Generated dictionary database failed integrity check"
            }
        }
    }

    private fun copyFileSafely(source: File, destination: File) {
        source.inputStream().use { input ->
            FileOutputStream(destination).use { output ->
                input.copyTo(output)
                output.fd.sync()
            }
        }
    }

    companion object {
        fun validateApproval(approval: TesterChangeApproval): List<String> {
            val errors = mutableListOf<String>()
            approval.corrections.groupBy { it.entryId to canonicalField(it.field) }
                .filterValues { corrections -> corrections.map { it.approvedValue }.distinct().size > 1 }
                .keys
                .forEach { (entryId, field) ->
                    errors += "Conflicting approved corrections for entryId=$entryId field=$field"
                }
            approval.newEntries.groupBy { it.localId }
                .filterValues { entries -> entries.distinct().size > 1 }
                .keys
                .forEach { localId -> errors += "Multiple approved new entries use localId=$localId" }
            approval.newEntries.filter { it.localId.isBlank() }.forEach {
                errors += "Approved new entry has no localId"
            }
            return errors
        }

        private fun canonicalField(field: String): String = when (field.lowercase()) {
            "french", "francais" -> COLUMN_FRENCH
            "category", "categorie" -> COLUMN_CATEGORY
            else -> field.lowercase()
        }

        private const val TABLE = "dictionnaire"
        private const val COLUMN_ID = "id"
        private const val COLUMN_FRENCH = "francais"
        private const val COLUMN_ENGLISH = "english"
        private const val COLUMN_DUTCH = "nederlands"
        private const val COLUMN_SAAMAKA = "saamaka"
        private const val COLUMN_CATEGORY = "categorie"
        private const val COLUMN_EXAMPLE = "exemple"
        private const val COLUMN_BIBLE_REFERENCE = "reference_biblique"
        private const val COLUMN_VALIDATED = "valide"
        private const val COLUMN_VALIDATED_EN = "valide_en_sa"
        private const val COLUMN_VALIDATED_NL = "valide_nl_sa"
        private const val VALIDATED_VALUE = "O"
        private val REQUIRED_COLUMNS = setOf(
            COLUMN_ID, COLUMN_FRENCH, COLUMN_ENGLISH, COLUMN_DUTCH, COLUMN_SAAMAKA,
            COLUMN_CATEGORY, COLUMN_EXAMPLE, COLUMN_BIBLE_REFERENCE, COLUMN_VALIDATED,
            COLUMN_VALIDATED_EN, COLUMN_VALIDATED_NL
        )
    }
}
