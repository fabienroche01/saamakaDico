package com.saamaka.dico.testeurs

import android.content.Context
import org.json.JSONArray
import org.json.JSONObject
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.security.MessageDigest

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
    fun markShareLaunched(
        testerName: String,
        file: File
    ) {
        addEvent(
            testerName = testerName,
            file = file,
            status = "SHARE_LAUNCHED"
        )
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