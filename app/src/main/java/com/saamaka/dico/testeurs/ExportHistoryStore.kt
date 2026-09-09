package com.saamaka.dico.testeurs

import android.app.Activity
import android.content.Context
import org.json.JSONArray
import org.json.JSONObject
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.security.MessageDigest
import java.util.zip.ZipFile
import com.saamaka.dico.testeurs.repository.CorrectionStore
import com.saamaka.dico.testeurs.repository.DeletionProposalStore
import com.saamaka.dico.testeurs.repository.NewEntryProposalStore

class ExportHistoryStore(
    private val context: Context
) {

    private fun sha256(file: File): String {
        val digest = MessageDigest.getInstance("SHA-256")

        file.inputStream().use { input ->
            val buffer = ByteArray(8192)

            while (true) {
                val read = input.read(buffer)

                if (read <= 0) {
                    break
                }

                digest.update(buffer, 0, read)
            }
        }

        return digest
            .digest()
            .joinToString("") { byte ->
                "%02x".format(byte)
            }
    }
    private val historyFile: File
        get() = File(
            context.filesDir,
            "tester_export_history.json"
        )

    fun markCreated(
        testerName: String,
        file: File
    ) {
        // Keep a durable copy before any local work can be removed.
        val archive = archiveFile(file)
        archive.parentFile?.let { directory ->
            check(directory.isDirectory || directory.mkdirs()) {
                "Impossible de conserver le ZIP."
            }
        }
        file.copyTo(archive, overwrite = true)
        check(sha256(archive) == sha256(file)) { "Copie du ZIP incomplète." }
        addEvent(
            testerName = testerName,
            file = file,
            status = "CREATED"
        )
    }

    fun hasSameExportAlreadyBeenShared(file: File): Boolean {
        val currentHash = sha256(file)
        val history = readHistory()

        for (i in 0 until history.length()) {
            val item = history.optJSONObject(i) ?: continue

            val status = item.optString("status")
            val savedHash = item.optString("sha256")

            if (
                status == "SHARE_LAUNCHED" &&
                savedHash.isNotBlank() &&
                savedHash == currentHash
            ) {
                return true
            }
        }

        return false
    }
    fun lastSharedAtMillis(): Long {
        val history = readHistory()
        val formatter = SimpleDateFormat(
            "yyyy-MM-dd'T'HH:mm:ss",
            Locale.ROOT
        )

        var latest = 0L

        for (i in 0 until history.length()) {
            val item = history.optJSONObject(i) ?: continue
            if (item.optString("status") != "SHARE_LAUNCHED") continue

            val timestamp = item.optString("timestamp")
            val parsed = runCatching {
                formatter.parse(timestamp)?.time ?: 0L
            }.getOrDefault(0L)

            if (parsed > latest) latest = parsed
        }

        return latest
    }

    fun markShareLaunched(
        testerName: String,
        file: File
    ) {
        addEvent(
            testerName = testerName,
            file = file,
            status = "SHARE_LAUNCHED"
        )
        if (clearExportedTesterWork(file)) {
            (context as? Activity)?.recreate()
        }
    }

    fun pendingExportText(): String = buildString {
        appendLine(ValidationStore(context).exportText())
        appendLine()
        appendLine(ReviewStore(context).exportText())
        appendLine()
        appendLine(CorrectionStore(context).exportText())
        appendLine()
        appendLine(DeletionProposalStore(context).exportText())
        appendLine()
        appendLine(NewEntryProposalStore(context).exportText())
        appendLine()
    }

    private fun archiveFile(file: File): File =
        File(File(context.filesDir, "tester_exports"), "${sha256(file)}_${file.name}")

    private fun clearExportedTesterWork(file: File): Boolean {
        val archive = archiveFile(file)
        if (!archive.isFile || sha256(archive) != sha256(file)) return false

        // Re-sharing an old ZIP must not clear work added since its creation.
        val exportedAudio = ZipFile(archive).use { zip ->
            val textEntry = zip.getEntry("export.txt") ?: return false
            val exportedText = zip.getInputStream(textEntry)
                .bufferedReader(Charsets.UTF_8).use { it.readText() }
            if (!exportedText.startsWith(pendingExportText() + "SAAMAKA DICO — AUDIOS TESTEURS")) return false

            File(context.filesDir, "audio").listFiles().orEmpty().filter { audio ->
                val entry = zip.getEntry("audio/${audio.name}")
                audio.isFile && audio.extension.equals("m4a", ignoreCase = true) &&
                    entry != null && !entry.isDirectory &&
                    zip.getInputStream(entry).use { input ->
                        val digest = MessageDigest.getInstance("SHA-256")
                        val buffer = ByteArray(8192)
                        var count = input.read(buffer)
                        while (count != -1) {
                            digest.update(buffer, 0, count)
                            count = input.read(buffer)
                        }
                        digest.digest().joinToString("") { "%02x".format(it) } == sha256(audio)
                    }
            }
        }

        context.getSharedPreferences("saamaka_validations", Context.MODE_PRIVATE)
            .edit().clear().apply()
        ReviewStore(context).clearAll()
        // Remove only contribution keys; preserve the tester identity.
        context.getSharedPreferences("saamaka_corrections", Context.MODE_PRIVATE)
            .edit().remove("corrections").apply()
        context.getSharedPreferences("saamaka_deletion_proposals", Context.MODE_PRIVATE)
            .edit().remove("proposals").apply()
        context.getSharedPreferences("saamaka_new_entry_proposals", Context.MODE_PRIVATE)
            .edit().remove("proposals").apply()

        exportedAudio.forEach { audio -> runCatching { audio.delete() } }
        return true
    }

    private fun addEvent(
        testerName: String,
        file: File,
        status: String
    ) {

        val history = readHistory()

        val item = JSONObject().apply {
            put("testerName", testerName.trim())
            put("fileName", file.name)
            put("status", status)
            put("timestamp", now())
            put("sha256", sha256(file))
        }

        history.put(item)

        val maxEvents = 200

        val trimmedHistory =
            if (history.length() > maxEvents) {
                JSONArray().apply {
                    val startIndex = history.length() - maxEvents

                    for (i in startIndex until history.length()) {
                        put(history.get(i))
                    }
                }
            } else {
                history
            }

        val tempHistoryFile = File(
            historyFile.parentFile,
            "${historyFile.name}.tmp"
        )

        try {
            tempHistoryFile.writeText(
                trimmedHistory.toString(2),
                Charsets.UTF_8
            )

            if (!tempHistoryFile.exists() || tempHistoryFile.length() <= 0L) {
                throw IllegalStateException(
                    "Impossible de sauvegarder l'historique des exports."
                )
            }

            if (historyFile.exists()) {
                historyFile.delete()
            }

            if (!tempHistoryFile.renameTo(historyFile)) {
                tempHistoryFile.copyTo(
                    target = historyFile,
                    overwrite = true
                )

                tempHistoryFile.delete()
            }

        } catch (e: Exception) {
            tempHistoryFile.delete()
            throw e
        }
    }

    fun hasAlreadyBeenShared(file: File): Boolean {
        val history = readHistory()

        for (i in 0 until history.length()) {
            val item = history.optJSONObject(i) ?: continue

            val fileName = item.optString("fileName")
            val status = item.optString("status")

            if (
                fileName == file.name &&
                status == "SHARE_LAUNCHED"
            ) {
                return true
            }
        }

        return false
    }
    private fun readHistory(): JSONArray {
        if (!historyFile.exists()) {
            return JSONArray()
        }

        return try {
            JSONArray(
                historyFile.readText(Charsets.UTF_8)
            )
        } catch (e: Exception) {
            JSONArray()
        }
    }

    private fun now(): String =
        SimpleDateFormat(
            "yyyy-MM-dd'T'HH:mm:ss",
            Locale.ROOT
        ).format(Date())
}