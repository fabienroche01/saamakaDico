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
import android.widget.Toast

class AudioStore(
    private val context: Context,
    private var strings: AppStrings
) {
    private var recorder: MediaRecorder? = null
    private var currentFile: File? = null
    private var player: MediaPlayer? = null

    fun updateStrings(value: AppStrings) {
        strings = value
    }

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
        startRecordingTo(audioFile(entryId, testerName))
    }

    fun proposedEntryAudioFile(localId: String, testerName: String): File {
        val audioDir = File(context.filesDir, "audio")
        if (!audioDir.exists()) audioDir.mkdirs()
        val safeTester = testerName.trim()
            .replace("[^A-Za-z0-9_-]".toRegex(), "_")
            .ifBlank { "inconnu" }
        val safeId = localId.replace("[^A-Za-z0-9-]".toRegex(), "_")
        return File(audioDir, "new_${safeId}_${safeTester}.m4a")
    }

    fun startProposedEntryRecording(localId: String, testerName: String) {
        startRecordingTo(proposedEntryAudioFile(localId, testerName))
    }

    fun hasProposedEntryAudio(localId: String, testerName: String): Boolean =
        proposedEntryAudioFile(localId, testerName).let { it.exists() && it.length() > 0L }

    fun deleteProposedEntryAudio(localId: String, testerName: String): Boolean {
        val file = proposedEntryAudioFile(localId, testerName)
        return !file.exists() || file.delete()
    }

    fun playProposedEntryAudio(
        localId: String,
        testerName: String,
        onFinished: () -> Unit
    ) {
        val file = proposedEntryAudioFile(localId, testerName)
        if (!file.exists() || file.length() <= 0L) {
            Toast.makeText(context, strings.ui(UiCopyKey.AUDIO_NOT_FOUND, file.name), Toast.LENGTH_LONG).show()
            onFinished()
            return
        }
        try {
            stopPlayback()
            val newPlayer = MediaPlayer().apply {
                setDataSource(file.absolutePath)
                setOnPreparedListener { it.start() }
                setOnCompletionListener {
                    it.release()
                    if (player === it) player = null
                    onFinished()
                }
                setOnErrorListener { mediaPlayer, _, _ ->
                    mediaPlayer.release()
                    if (player === mediaPlayer) player = null
                    onFinished()
                    true
                }
            }
            player = newPlayer
            newPlayer.prepareAsync()
        } catch (error: Exception) {
            stopPlayback()
            onFinished()
            Toast.makeText(
                context,
                strings.ui(
                    UiCopyKey.AUDIO_READ_FAILED,
                    error.message ?: strings.ui(UiCopyKey.UNKNOWN_ERROR)
                ),
                Toast.LENGTH_LONG
            ).show()
        }
    }

    private fun startRecordingTo(file: File) {

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
            Toast.makeText(
                context,
                strings.ui(UiCopyKey.AUDIO_NOT_FOUND, file.name),
                Toast.LENGTH_LONG
            ).show()
            return
        }

        if (file.length() <= 0L) {
            Toast.makeText(
                context,
                strings.ui(UiCopyKey.AUDIO_EMPTY, file.name),
                Toast.LENGTH_LONG
            ).show()
            return
        }

        try {
            player?.release()
            player = null

            val newPlayer = MediaPlayer()

            newPlayer.setDataSource(file.absolutePath)

            newPlayer.setOnPreparedListener { mediaPlayer ->
                Toast.makeText(
                    context,
                    strings.ui(UiCopyKey.PLAYING_PRONUNCIATION),
                    Toast.LENGTH_SHORT
                ).show()

                mediaPlayer.start()
            }

            newPlayer.setOnCompletionListener { mediaPlayer ->
                mediaPlayer.release()

                if (player === mediaPlayer) {
                    player = null
                }
            }

            newPlayer.setOnErrorListener { mediaPlayer, what, extra ->
                mediaPlayer.release()

                if (player === mediaPlayer) {
                    player = null
                }

                Toast.makeText(
                    context,
                    strings.ui(UiCopyKey.AUDIO_ERROR, what, extra),
                    Toast.LENGTH_LONG
                ).show()

                true
            }

            player = newPlayer
            newPlayer.prepareAsync()

        } catch (e: Exception) {
            player?.release()
            player = null

            Toast.makeText(
                context,
                strings.ui(
                    UiCopyKey.AUDIO_READ_FAILED,
                    e.message ?: strings.ui(UiCopyKey.UNKNOWN_ERROR)
                ),
                Toast.LENGTH_LONG
            ).show()
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

        require(exportText.isNotBlank()) {
            "Impossible de créer l'export : aucune donnée à exporter."
        }

        val exportDir = File(
            context.cacheDir,
            "exports"
        )

        if (!exportDir.exists() && !exportDir.mkdirs()) {
            throw IllegalStateException(
                "Impossible de créer le dossier d'export."
            )
        }

        val dateTime = SimpleDateFormat(
            "yyyy-MM-dd_HHmm",
            Locale.ROOT
        ).format(Date())

        // On conserve le format de nom V12.
        // Si un fichier du même nom existe déjà, on évite de l'écraser.
        var zipFile = File(
            exportDir,
            "SaamakaDico_${safeName}_${dateTime}.zip"
        )

        var counter = 2

        while (zipFile.exists()) {
            zipFile = File(
                exportDir,
                "SaamakaDico_${safeName}_${dateTime}_$counter.zip"
            )
            counter++
        }

        // Le ZIP est d'abord créé sous forme temporaire.
        // Il ne devient partageable qu'une fois complètement terminé.
        val tempFile = File(
            exportDir,
            "${zipFile.name}.tmp"
        )

        if (tempFile.exists()) {
            tempFile.delete()
        }

        try {

            ZipOutputStream(
                FileOutputStream(tempFile)
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
                        file.isFile &&
                                file.length() > 0L &&
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

            // Sécurité minimale avant validation
            if (!tempFile.exists() || tempFile.length() <= 0L) {
                throw IllegalStateException(
                    "Le fichier ZIP généré est vide."
                )
            }

            // Seulement maintenant on crée le fichier final.
            if (!tempFile.renameTo(zipFile)) {

                tempFile.copyTo(
                    target = zipFile,
                    overwrite = false
                )

                tempFile.delete()
            }

            if (!zipFile.exists() || zipFile.length() <= 0L) {
                throw IllegalStateException(
                    "L'export ZIP n'a pas pu être finalisé."
                )
            }

            return zipFile

        } catch (e: Exception) {

            tempFile.delete()

            throw e
        }
    }
    fun stopPlayback() {
        player?.release()
        player = null
    }
}
