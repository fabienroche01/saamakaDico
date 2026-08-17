package com.saamaka.dico.testeurs

import android.content.Context
import java.io.File
import android.media.MediaRecorder
import android.media.MediaPlayer
import java.io.FileOutputStream
import java.util.zip.ZipEntry
import java.util.zip.ZipOutputStream
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
class AudioStore(
    private val context: Context
) {
    private var recorder: MediaRecorder? = null
    private var currentFile: File? = null
    private var player: MediaPlayer? = null

    fun audioFile(
        entryId: Int,
        testerName: String
    ): File {

        val audioDir = File(
            context.filesDir,
            "audio"
        )

        if (!audioDir.exists()) {
            audioDir.mkdirs()
        }

        val safeName = testerName
            .trim()
            .replace("[^A-Za-z0-9_-]".toRegex(), "_")
            .ifBlank { "inconnu" }

        return File(
            audioDir,
            "srm_${entryId}_${safeName}.m4a"
        )
    }

    fun startRecording(
        entryId: Int,
        testerName: String
    ) {
        val file = audioFile(entryId, testerName)

        currentFile = file

        recorder = MediaRecorder(context).apply {
            setAudioSource(MediaRecorder.AudioSource.MIC)
            setOutputFormat(MediaRecorder.OutputFormat.MPEG_4)
            setAudioEncoder(MediaRecorder.AudioEncoder.AAC)
            setOutputFile(file.absolutePath)

            prepare()
            start()
        }
    }

    fun stopRecording() {
        recorder?.apply {
            stop()
            release()
        }

        recorder = null
        currentFile = null
    }

    fun hasAudio(
        entryId: Int,
        testerName: String
    ): Boolean {
        return audioFile(entryId, testerName).exists()
    }

    fun deleteAudio(
        entryId: Int,
        testerName: String
    ): Boolean {
        val file = audioFile(entryId, testerName)

        return if (file.exists()) {
            file.delete()
        } else {
            true
        }
    }

    fun listAudioFiles(): List<File> {
        val audioDir = File(
            context.filesDir,
            "audio"
        )

        if (!audioDir.exists()) {
            return emptyList()
        }

        return audioDir
            .listFiles()
            ?.filter { file ->
                file.isFile &&
                        file.extension.equals("m4a", ignoreCase = true)
            }
            ?.sortedBy { it.name }
            ?: emptyList()
    }

    fun exportAudioSummary(): String {
        val files = listAudioFiles()

        if (files.isEmpty()) {
            return "SAAMAKA DICO — AUDIOS TESTEURS\nAucun audio enregistré."
        }

        return buildString {
            appendLine("SAAMAKA DICO — AUDIOS TESTEURS")
            appendLine("Nombre d'audios : ${files.size}")
            appendLine()

            files.forEachIndexed { index, file ->
                val nameWithoutExtension = file.nameWithoutExtension
                val parts = nameWithoutExtension.split("_")

                val entryId = parts.getOrNull(1).orEmpty()
                val tester = parts.drop(2).joinToString("_").ifBlank {
                    "Non renseigné"
                }

                appendLine("Audio ${index + 1}")
                appendLine("ID mot : $entryId")
                appendLine("Locuteur : $tester")
                appendLine("Fichier : ${file.name}")
                appendLine("Taille : ${file.length()} octets")
                appendLine("------------------------------")
            }
        }
    }

    fun hasOfficialAudio(
        entryId: Int
    ): Boolean {
        val resId = context.resources.getIdentifier(
            "srm_$entryId",
            "raw",
            context.packageName
        )

        return resId != 0
    }

    fun playOfficialAudio(
        entryId: Int
    ) {
        val resId = context.resources.getIdentifier(
            "srm_$entryId",
            "raw",
            context.packageName
        )

        if (resId == 0) {
            return
        }

        player?.release()

        player = MediaPlayer.create(
            context,
            resId
        )?.apply {

            setOnCompletionListener {
                it.release()
                player = null
            }

            start()
        }
    }
    fun playAudio(
        entryId: Int,
        testerName: String
    ) {
        val file = audioFile(entryId, testerName)

        if (!file.exists()) {
            return
        }

        player?.release()

        player = MediaPlayer().apply {
            setDataSource(file.absolutePath)

            setOnCompletionListener {
                it.release()
                player = null
            }

            prepare()
            start()
        }
    }

    fun createTesterExportZip(
        testerName: String,
        exportText: String
    ): File {

        val safeName = testerName
            .trim()
            .replace("[^A-Za-z0-9_-]".toRegex(), "_")
            .ifBlank { "testeur" }

        val exportDir = File(
            context.cacheDir,
            "exports"
        )

        if (!exportDir.exists()) {
            exportDir.mkdirs()
        }

        val dateTime = SimpleDateFormat(
            "yyyy-MM-dd_HHmm",
            Locale.ROOT
        ).format(Date())

        val zipFile = File(
            exportDir,
            "SaamakaDico_${safeName}_${dateTime}.zip"
        )

        ZipOutputStream(
            FileOutputStream(zipFile)
        ).use { zip ->

            // 1. Validations + corrections + résumé audio
            zip.putNextEntry(
                ZipEntry("export.txt")
            )

            zip.write(
                exportText.toByteArray(Charsets.UTF_8)
            )

            zip.closeEntry()

            // 2. Fichiers audio du testeur
            val testerSuffix = "_${safeName}.m4a"

            listAudioFiles()
                .filter { file ->
                    file.name.endsWith(
                        testerSuffix,
                        ignoreCase = true
                    )
                }
                .forEach { audioFile ->

                    zip.putNextEntry(
                        ZipEntry(
                            "audio/${audioFile.name}"
                        )
                    )

                    audioFile.inputStream().use { input ->
                        input.copyTo(zip)
                    }

                    zip.closeEntry()
                }
        }

        return zipFile
    }
    fun stopPlayback() {
        player?.release()
        player = null
    }
}