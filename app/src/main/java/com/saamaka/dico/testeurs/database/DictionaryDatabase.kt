package com.saamaka.dico.testeurs.database

import android.content.Context
import android.database.sqlite.SQLiteDatabase
import android.util.Log
import com.saamaka.dico.testeurs.model.DictionaryEntry
import com.saamaka.dico.testeurs.filterAndRankByLanguageNormalized
import com.saamaka.dico.testeurs.filterAndRankAcrossLanguages
import com.saamaka.dico.testeurs.normalizeMultilingualSearch
import com.saamaka.dico.testeurs.accentInsensitiveGlob
import com.saamaka.dico.testeurs.requireBackgroundSearch
import android.os.Looper
import com.saamaka.dico.testeurs.assembleAttestedPhrase
import com.saamaka.dico.testeurs.cleanPhraseInput
import com.saamaka.dico.testeurs.composeKnownVouloirPhrase
import com.saamaka.dico.testeurs.resolveAttestedFrenchSubject
import com.saamaka.dico.testeurs.FrenchVerbInflections
import com.saamaka.dico.testeurs.CorrectionProposal
import com.saamaka.dico.testeurs.findAttestedPhraseRule
import com.saamaka.dico.testeurs.normalizeAttestedPhraseKey
import com.saamaka.dico.testeurs.LocalPhraseIndex
import com.saamaka.dico.testeurs.model.PhraseTranslationResult
import com.saamaka.dico.testeurs.model.PhraseTranslationKind
import com.saamaka.dico.testeurs.model.RecognizedPhraseSegment
import com.saamaka.dico.testeurs.model.TranslationReliability
import java.io.FileOutputStream
import java.io.File
import java.text.Normalizer
import java.util.Locale

class DictionaryDatabase(private val context: Context) {

    private val databaseName = "SaamakaDico_v11_25.db"
    private val databasePreferences = context.getSharedPreferences(
        DATABASE_PREFERENCES_NAME,
        Context.MODE_PRIVATE
    )
    @Volatile
    private var cachedPhraseIndex: LocalPhraseIndex? = null
    @Volatile
    private var preparedDatabase: File? = null

    private fun phraseIndex(): LocalPhraseIndex {
        cachedPhraseIndex?.let { return it }
        return synchronized(this) {
            cachedPhraseIndex ?: LocalPhraseIndex(allEntries()).also {
                cachedPhraseIndex = it
            }
        }
    }

    fun preparePhraseTranslationIndex() {
        phraseIndex()
    }

    fun exactLocalEntry(text: String, frenchToSaamaka: Boolean): DictionaryEntry? =
        phraseIndex().exactEntry(text, frenchToSaamaka)

    private fun cleanTranslationForDisplay(text: String): String {
        return text
            .replace(Regex("\\([^)]*\\)"), "")
            .replace(Regex("\\{[^}]*\\}"), "")
            .replace(Regex("\\[[^\\]]*\\]"), "")
            .replace(Regex("\\s+"), " ")
            .trim()
            .split("/")
            .first()
            .trim()

    }

    private fun detectMissingFrenchExpression(
        words: List<String>,
        index: Int
    ): Pair<String, Int>? {

        val remaining = words.drop(index)

        fun matches(vararg parts: String): Boolean {
            if (remaining.size < parts.size) return false

            return parts.indices.all { i ->
                normalizeForSearch(remaining[i]) ==
                        normalizeForSearch(parts[i])
            }
        }

        return when {
            matches("il", "n'y", "a", "pas") ->
                "il n'y a pas" to 4

            matches("il", "n'existe", "pas") ->
                "il n'existe pas" to 3

            matches("il", "y", "a") ->
                "il y a" to 3

            matches("ne", "plus") ->
                "ne plus" to 2

            matches("ne", "pas") ->
                "ne pas" to 2

            else -> null
        }
    }

    @Synchronized
    private fun ensureEmbeddedDatabase(): File {
        preparedDatabase?.let { return it }

        val destination = context.getDatabasePath(databaseName)
        val installedVersion = databasePreferences.getInt(INSTALLED_DB_VERSION_KEY, 0)

        if (!destination.exists()) {
            destination.parentFile?.mkdirs()
            val firstInstallTemporary = File(destination.parentFile, "$databaseName.install")
            firstInstallTemporary.delete()
            try {
                copyAssetTo(databaseName, firstInstallTemporary)
                verifyDictionaryDatabase(firstInstallTemporary)
                check(firstInstallTemporary.renameTo(destination)) {
                    "Unable to install embedded dictionary"
                }
                check(
                    databasePreferences.edit()
                        .putInt(INSTALLED_DB_VERSION_KEY, EMBEDDED_DB_VERSION)
                        .commit()
                ) {
                    "Unable to persist embedded dictionary version"
                }
            } catch (error: Exception) {
                firstInstallTemporary.delete()
                destination.delete()
                Log.e(TAG, "Dictionary DB first installation failed", error)
                throw error
            }
            Log.i(TAG, "Dictionary DB installed for the first time (version $EMBEDDED_DB_VERSION)")
            preparedDatabase = destination
            return destination
        }

        if (installedVersion >= EMBEDDED_DB_VERSION) {
            Log.d(TAG, "Dictionary DB already up to date (version $installedVersion)")
            preparedDatabase = destination
            return destination
        }

        val temporary = File(destination.parentFile, "$databaseName.update")
        val backup = File(destination.parentFile, "$databaseName.backup")
        temporary.delete()
        backup.delete()

        try {
            copyAssetTo(databaseName, temporary)
            verifyDictionaryDatabase(temporary)

            check(destination.renameTo(backup)) {
                "Unable to create dictionary backup"
            }
            check(temporary.renameTo(destination)) {
                "Unable to install updated dictionary"
            }
            verifyDictionaryDatabase(destination)

            check(
                databasePreferences.edit()
                    .putInt(INSTALLED_DB_VERSION_KEY, EMBEDDED_DB_VERSION)
                    .commit()
            ) {
                "Unable to persist updated dictionary version"
            }

            backup.delete()
            temporary.delete()
            cachedPhraseIndex = null
            Log.i(
                TAG,
                "Dictionary DB updated $installedVersion -> $EMBEDDED_DB_VERSION"
            )
        } catch (error: Exception) {
            temporary.delete()
            if (backup.exists()) {
                destination.delete()
                if (!backup.renameTo(destination)) {
                    backup.copyTo(destination, overwrite = true)
                }
            }
            Log.e(TAG, "Dictionary DB update failed; previous DB kept", error)
        } finally {
            temporary.delete()
            if (destination.exists()) {
                backup.delete()
            }
        }

        check(destination.exists()) { "No valid dictionary database is available" }
        verifyDictionaryDatabase(destination)
        preparedDatabase = destination
        return destination
    }

    private fun copyAssetTo(assetName: String, destination: File) {
        context.assets.open(assetName).use { input ->
            FileOutputStream(destination).use { output ->
                input.copyTo(output)
                output.fd.sync()
            }
        }
    }

    private fun verifyDictionaryDatabase(file: File) {
        val database = SQLiteDatabase.openDatabase(
            file.absolutePath,
            null,
            SQLiteDatabase.OPEN_READONLY
        )
        try {
            database.rawQuery(
                "SELECT COUNT(*) FROM sqlite_master WHERE type = 'table' AND name = 'dictionnaire'",
                null
            ).use { cursor ->
                check(cursor.moveToFirst() && cursor.getInt(0) == 1) {
                    "Embedded dictionary is missing the dictionnaire table"
                }
            }
        } finally {
            database.close()
        }
    }

    private fun openDatabase(): SQLiteDatabase {
        val destination = ensureEmbeddedDatabase()
        return SQLiteDatabase.openDatabase(
            destination.absolutePath,
            null,
            SQLiteDatabase.OPEN_READONLY
        )
    }

    fun countByValidation(value: String?): Int {
        val db = openDatabase()

        val sql: String
        val args: Array<String>?

        if (value == null) {
            sql = """
                SELECT COUNT(*)
                FROM dictionnaire
                WHERE id > 1
                  AND TRIM(COALESCE(francais, '')) <> ''
                  AND TRIM(COALESCE(saamaka, '')) <> ''
                  AND (valide IS NULL OR TRIM(valide) = '')
            """.trimIndent()

            args = null
        } else {
            sql = """
                SELECT COUNT(*)
                FROM dictionnaire
                WHERE id > 1
                  AND TRIM(COALESCE(francais, '')) <> ''
                  AND TRIM(COALESCE(saamaka, '')) <> ''
                  AND UPPER(TRIM(valide)) = ?
            """.trimIndent()

            args = arrayOf(value.uppercase())
        }

        return try {
            db.rawQuery(sql, args).use { cursor ->
                if (cursor.moveToFirst()) {
                    cursor.getInt(0)
                } else {
                    0
                }
            }
        } finally {
            db.close()
        }
    }

    fun countEntries(): Int {
        val db = openDatabase()

        return try {
            db.rawQuery(
                """
                SELECT COUNT(*)
                FROM dictionnaire
                WHERE id > 1
                  AND TRIM(COALESCE(saamaka, '')) <> ''
                  AND TRIM(COALESCE(francais, '')) <> ''
                  AND UPPER(TRIM(saamaka)) NOT IN ('#NAME?', '#N/A', 'N/A')
                  AND UPPER(TRIM(francais)) NOT IN ('#NAME?', '#N/A', 'N/A')
                """.trimIndent(),
                null
            ).use { cursor ->
                if (cursor.moveToFirst()) {
                    cursor.getInt(0)
                } else {
                    0
                }
            }
        } finally {
            db.close()
        }
    }

    fun search(
        query: String,
        languageCode: String? = "fr",
        limit: Int = 100
    ): List<DictionaryEntry> {

        requireBackgroundSearch(Looper.myLooper() == Looper.getMainLooper())
        val normalizedQuery = normalizeMultilingualSearch(query)

        if (normalizedQuery.isBlank()) {
            return emptyList()
        }

        val columns = if (languageCode == null) {
            listOf("saamaka", "francais", "english", "nederlands")
        } else listOf(
            when (languageCode) {
                "en" -> "english"
                "nl" -> "nederlands"
                "srm" -> "saamaka"
                else -> "francais"
            }
        )
        val languageCodes = if (languageCode == null) listOf("srm", "fr", "en", "nl") else listOf(languageCode)
        val glob = accentInsensitiveGlob(normalizedQuery)
        val wordGlobs = normalizedQuery.split(' ').filter(String::isNotBlank).map(::accentInsensitiveGlob)
        val exactClause = columns.joinToString(" OR ") { "$it GLOB ?" }
        val prefixClause = columns.joinToString(" OR ") { "$it GLOB ?" }
        val allWordsClause = columns.joinToString(" OR ") { column ->
            wordGlobs.joinToString(" AND ", prefix = "(", postfix = ")") { "$column GLOB ?" }
        }
        val containsClause = if (wordGlobs.size <= 1) {
            columns.joinToString(" OR ") { "$it GLOB ?" }
        } else {
            columns.joinToString(" OR ") { column ->
                wordGlobs.joinToString(" OR ", prefix = "(", postfix = ")") { "$column GLOB ?" }
            }
        }
        val candidateLimit = (limit * 4).coerceAtLeast(limit)
        val args = buildList {
            if (wordGlobs.size <= 1) {
                repeat(columns.size) { add("*$glob*") }
            } else {
                repeat(columns.size) { wordGlobs.forEach { add("*$it*") } }
            }
            repeat(columns.size) { add(glob) }
            repeat(columns.size) { add("$glob*") }
            repeat(columns.size) { wordGlobs.forEach { add("*$it*") } }
            add(candidateLimit.toString())
        }.toTypedArray()

        val db = openDatabase()
        val candidates = mutableListOf<DictionaryEntry>()

        try {
            db.rawQuery(
                """
            SELECT id, francais, english, nederlands, saamaka, categorie, valide
                FROM dictionnaire
            WHERE id > 1
              AND TRIM(COALESCE(saamaka, '')) <> ''
              AND UPPER(TRIM(saamaka)) NOT IN ('#NAME?', '#N/A', 'N/A')
              AND ($containsClause)
            ORDER BY CASE
                WHEN $exactClause THEN 0
                WHEN $prefixClause THEN 1
                WHEN $allWordsClause THEN 2
                ELSE 3
            END, id
            LIMIT ?
""".trimIndent(),
                args
            ).use { cursor ->

                while (cursor.moveToNext()) {
                    candidates += DictionaryEntry(
                        id = cursor.getInt(
                            cursor.getColumnIndexOrThrow("id")
                        ),
                        french = cursor.getString(
                            cursor.getColumnIndexOrThrow("francais")
                        ).orEmpty().trim(),
                        english = cursor.getString(
                            cursor.getColumnIndexOrThrow("english")
                        ).orEmpty().trim(),
                        dutch = cursor.getString(
                            cursor.getColumnIndexOrThrow("nederlands")
                        ).orEmpty().trim(),
                        saamaka = cursor.getString(
                            cursor.getColumnIndexOrThrow("saamaka")
                        ).orEmpty().trim(),

                        categorie = cursor.getString(
                            cursor.getColumnIndexOrThrow("categorie")
                        ).orEmpty().trim(),

                        valide = cursor.getString(
                            cursor.getColumnIndexOrThrow("valide")
                        ).orEmpty().trim()
                    )
                }
            }

        } finally {
            db.close()
        }

        return if (languageCode == null) {
            filterAndRankAcrossLanguages(candidates, normalizedQuery, languageCodes, limit)
        } else {
            filterAndRankByLanguageNormalized(candidates, normalizedQuery, languageCode, limit)
        }
    }

    private fun translateExactAlternatives(
        text: String,
        frenchToSaamaka: Boolean
    ): List<String> {

        return phraseIndex().exactAlternatives(text, frenchToSaamaka)
    }

    fun translateExactPhrase(
        text: String,
        frenchToSaamaka: Boolean
    ): String? {

        return phraseIndex().exactTranslation(text, frenchToSaamaka)
    }

    private fun prepareFrenchTextForTranslation(text: String): String {
        return text
            .lowercase()
            .replace('’', '\'')
            .replace(Regex("\\bj'"), "je ")
            .replace(Regex("\\bm'"), "me ")
            .replace(Regex("\\bt'"), "te ")
            .replace(Regex("\\bs'"), "se ")
            .replace(Regex("\\bc'"), "ce ")
            .replace(Regex("\\bn'"), "ne ")
            .replace(Regex("\\bd'"), "de ")
            .replace(Regex("\\bl'"), "le ")
            .replace(Regex("\\bqu'"), "que ")
            .replace(Regex("\\s+"), " ")
            .trim()
    }

    private fun frenchNegativeSubjectToSaamaka(
        subject: String
    ): String? {

        return when (normalizeForSearch(subject)) {

            // 1SG
            "je" -> "ma"

            // 2SG
            "tu" -> "ja"

            // 3SG
            "il", "elle" -> "an"

            // 1PL
            "nous" -> "wa"

            // 2PL et 3PL :
            // non ajoutés car la forme négative
            // n'est pas donnée dans notre source.
            else -> null
        }
    }

    private fun translateConfirmedNegativePattern(
        text: String
    ): String? {

        val prepared = prepareFrenchTextForTranslation(text)

        val words = prepared
            .split(Regex("\\s+"))
            .filter { it.isNotBlank() }

        if (words.size < 4) {
            return null
        }

        val negativeSubject =
            frenchNegativeSubjectToSaamaka(words[0])
                ?: return null

        // Structure :
        // je ne dors pas
        // tu ne manges pas
        // il ne vient pas
        if (normalizeForSearch(words[1]) != "ne") {
            return null
        }

        val pasIndex = words.indexOfFirst { word ->
            normalizeForSearch(word) == "pas"
        }

        if (pasIndex != 3) {
            return null
        }

        val frenchVerb = words[2]

        val normalizedVerb =
            normalizeFrenchWordForTranslation(frenchVerb)

        // Cas confirmé de ɗɛ
        if (normalizedVerb == "être") {

            return "$negativeSubject ɗɛ"
        }

        val alternatives =
            translateExactAlternatives(
                text = normalizedVerb,
                frenchToSaamaka = true
            )

        // Une seule traduction attestée :
        // on peut construire la négation.
        if (alternatives.size == 1) {

            return "$negativeSubject ${
                cleanTranslationForDisplay(
                    alternatives.first()
                )
            }"
        }

        // Plusieurs sens ou aucun :
        // on ne choisit pas à la place du locuteur.
        return null
    }

    private fun frenchSubjectToSaamaka(
        subject: String
    ): String? = resolveAttestedFrenchSubject(subject)

    private fun isFrenchFutureSimpleForm(
        word: String
    ): Boolean {

        val clean = word
            .trim()
            .lowercase()
            .trim(',', '.', ';', ':', '!', '?', '\'', '"')

        return clean.endsWith("rai") ||
                clean.endsWith("ras") ||
                clean.endsWith("ra") ||
                clean.endsWith("rons") ||
                clean.endsWith("rez") ||
                clean.endsWith("ront")
    }

    private fun translateConfirmedInaccompliPattern(
        text: String
    ): String? {

        val prepared = prepareFrenchTextForTranslation(text)

        val words = prepared
            .split(Regex("\\s+"))
            .filter { it.isNotBlank() }

        if (words.size != 2) {
            return null
        }

        val subject =
            frenchSubjectToSaamaka(words[0])
                ?: return null

        // On traite d'abord les phrases simples :
        // je marche
        // tu dors
        // il mange
        val frenchVerb = words[1]

        val infinitive =
            normalizeFrenchWordForTranslation(
                frenchVerb
            )

        // Si aucune normalisation n'a eu lieu,
        // on peut quand même essayer la forme telle quelle
        val alternatives =
            translateExactAlternatives(
                text = infinitive,
                frenchToSaamaka = true
            )

        if (alternatives.size != 1) {
            return null
        }

        val verb =
            cleanTranslationForDisplay(
                alternatives.first()
            )

        return "$subject ta $verb"
    }

    private fun translateConfirmedFuturePattern(
        text: String
    ): String? {

        val prepared = prepareFrenchTextForTranslation(text)

        val words = prepared
            .split(Regex("\\s+"))
            .filter { it.isNotBlank() }

        // Pour V14.7 on commence volontairement
        // avec : sujet + verbe au futur
        if (words.size != 2) {
            return null
        }

        val subject = frenchSubjectToSaamaka(
            words[0]
        ) ?: return null

        val frenchFutureVerb = words[1]

        if (!isFrenchFutureSimpleForm(frenchFutureVerb)) {
            return null
        }

        val infinitive =
            normalizeFrenchWordForTranslation(
                frenchFutureVerb
            )

        // Cas spécial déjà attesté :
        // être -> ɗɛ
        if (infinitive == "être") {
            return "$subject o ɗɛ"
        }

        // Si la normalisation n'a même pas réussi
        // à retrouver l'infinitif, on ne devine rien.
        if (
            normalizeForSearch(infinitive) ==
            normalizeForSearch(frenchFutureVerb)
        ) {
            return null
        }

        val alternatives =
            translateExactAlternatives(
                text = infinitive,
                frenchToSaamaka = true
            )

        // Une seule traduction attestée :
        // sujet + o + verbe
        if (alternatives.size == 1) {

            val verb =
                cleanTranslationForDisplay(
                    alternatives.first()
                )

            return "$subject o $verb"
        }

        // 0 ou plusieurs traductions :
        // pas de choix automatique
        return null
    }

    private fun translateConfirmedNearFuturePattern(
        text: String
    ): String? {

        val prepared = prepareFrenchTextForTranslation(text)

        val words = prepared
            .split(Regex("\\s+"))
            .filter { it.isNotBlank() }

        // Exemple : je vais dormir
        if (words.size != 3) {
            return null
        }

        val subject =
            frenchSubjectToSaamaka(words[0])
                ?: return null

        val allerForm =
            normalizeFrenchWordForTranslation(words[1])

        // Le deuxième mot doit réellement être une forme de "aller"
        if (
            normalizeForSearch(allerForm) !=
            normalizeForSearch("aller")
        ) {
            return null
        }

        val infinitive =
            normalizeFrenchWordForTranslation(words[2])

        // Être : cas attesté
        if (
            normalizeForSearch(infinitive) ==
            normalizeForSearch("être")
        ) {
            return "$subject o ɗɛ"
        }

        val alternatives =
            translateExactAlternatives(
                text = infinitive,
                frenchToSaamaka = true
            )

        // On ne choisit automatiquement
        // que s'il existe une seule traduction
        if (alternatives.size != 1) {
            return null
        }

        val verb =
            cleanTranslationForDisplay(
                alternatives.first()
            )

        return "$subject o $verb"
    }

    private fun normalizeFrenchWordForTranslation(word: String): String {
        val clean = word
            .trim()
            .lowercase()
            .trim(',', '.', ';', ':', '!', '?', '\'', '"')

        return when (clean) {
            "j'", "j’" -> "je"
            else -> FrenchVerbInflections.lemma(clean) ?: clean
        }
        }

    private fun saamakaNegativeSubjectToFrench(
        subject: String
    ): String? {

        return when (normalizeForSearch(subject)) {
            "ma" -> "je"
            "ja" -> "tu"
            "an" -> "il/elle"
            "wa" -> "nous"

            // Pas de forme ajoutée pour 2PL / 3PL :
            // elles n'étaient pas indiquées dans la source fournie.
            else -> null
        }
    }

    private fun conjugateFrenchPresent(
        subject: String,
        infinitive: String
    ): String? {

        val verb = normalizeForSearch(infinitive)

        return when (verb) {

            "etre" -> when (subject) {
                "je" -> "suis"
                "tu" -> "es"
                "il/elle" -> "est"
                "nous" -> "sommes"
                else -> null
            }

            "avoir" -> when (subject) {
                "je" -> "ai"
                "tu" -> "as"
                "il/elle" -> "a"
                "nous" -> "avons"
                else -> null
            }

            "aller" -> when (subject) {
                "je" -> "vais"
                "tu" -> "vas"
                "il/elle" -> "va"
                "nous" -> "allons"
                else -> null
            }

            "venir" -> when (subject) {
                "je" -> "viens"
                "tu" -> "viens"
                "il/elle" -> "vient"
                "nous" -> "venons"
                else -> null
            }

            "dormir" -> when (subject) {
                "je" -> "dors"
                "tu" -> "dors"
                "il/elle" -> "dort"
                "nous" -> "dormons"
                else -> null
            }

            "prendre" -> when (subject) {
                "je" -> "prends"
                "tu" -> "prends"
                "il/elle" -> "prend"
                "nous" -> "prenons"
                else -> null
            }

            "faire" -> when (subject) {
                "je" -> "fais"
                "tu" -> "fais"
                "il/elle" -> "fait"
                "nous" -> "faisons"
                else -> null
            }

            "vouloir" -> when (subject) {
                "je" -> "veux"
                "tu" -> "veux"
                "il/elle" -> "veut"
                "nous" -> "voulons"
                else -> null
            }

            "pouvoir" -> when (subject) {
                "je" -> "peux"
                "tu" -> "peux"
                "il/elle" -> "peut"
                "nous" -> "pouvons"
                else -> null
            }

            else -> {

                // Verbes réguliers en -er
                if (verb.endsWith("er") && verb.length > 2) {

                    val stem = infinitive.dropLast(2)

                    when (subject) {
                        "je" -> stem + "e"
                        "tu" -> stem + "es"
                        "il/elle" -> stem + "e"
                        "nous" -> stem + "ons"
                        else -> null
                    }

                } else {
                    null
                }
            }
        }
    }


    private fun saamakaSubjectToFrench(
        subject: String
    ): String? {

        return when (normalizeForSearch(subject)) {
            "mi" -> "je"
            "i" -> "tu"
            "a" -> "il/elle"
            "u" -> "nous"
            "unu" -> "vous"
            "de" -> "ils/elles"
            else -> null
        }
    }

    private fun translateConfirmedSaamakaGrammarToFrench(
        text: String
    ): String? {

        val cleanText = text
            .trim()
            .replace(Regex("\\s+"), " ")

        val words = cleanText
            .split(Regex("\\s+"))
            .filter { it.isNotBlank() }

        if (words.size < 3) {
            return null
        }

        // -------------------------------------------------
        // 1. TROUVER LE MARQUEUR GRAMMATICAL o / ta
        // -------------------------------------------------

        val markerIndex = words.indexOfFirst { word ->
            val normalized = normalizeForSearch(word)
            normalized == "o" || normalized == "ta"
        }

        if (markerIndex <= 0 || markerIndex >= words.lastIndex) {
            return null
        }

        val marker =
            normalizeForSearch(words[markerIndex])

        // -------------------------------------------------
        // 2. SUJET
        // -------------------------------------------------

        val saamakaSubject =
            words.subList(0, markerIndex)
                .joinToString(" ")

        val frenchSubject =
            if (markerIndex == 1) {

                saamakaSubjectToFrench(words[0])

            } else {

                confirmedSaamakaNominalToFrench(
                    saamakaSubject
                ) ?: translateExactPhrase(
                    text = saamakaSubject,
                    frenchToSaamaka = false
                )

            } ?: return null

        // -------------------------------------------------
        // 3. VERBE
        // -------------------------------------------------

        val saamakaVerb =
            words[markerIndex + 1]

        val frenchAlternatives =
            translateExactAlternatives(
                text = saamakaVerb,
                frenchToSaamaka = false
            )

        val frenchVerb =
            confirmedSaamakaVerbToFrench(
                saamakaVerb
            ) ?: run {

                val frenchAlternatives =
                    translateExactAlternatives(
                        text = saamakaVerb,
                        frenchToSaamaka = false
                    )

                frenchAlternatives.firstOrNull { candidate ->

                    val cleanCandidate =
                        normalizeForSearch(candidate)

                    cleanCandidate.endsWith("er") ||
                            cleanCandidate.endsWith("ir") ||
                            cleanCandidate.endsWith("re")

                }

            } ?: return null



        // -------------------------------------------------
        // 4. COMPLÉMENT ÉVENTUEL
        // -------------------------------------------------

        val saamakaObject =
            if (markerIndex + 2 < words.size) {
                words.subList(
                    markerIndex + 2,
                    words.size
                ).joinToString(" ")
            } else {
                ""
            }

val frenchObject =
    if (saamakaObject.isNotBlank()) {

        confirmedSaamakaNominalToFrench(
            saamakaObject
        ) ?: translateExactPhrase(
            text = saamakaObject,
            frenchToSaamaka = false
        ) ?: saamakaObject

    } else {
        ""
    }

        // -------------------------------------------------
        // 5. CONSTRUCTION FRANÇAISE
        // -------------------------------------------------

        return when (marker) {

            // FUTUR
            "o" -> {

                val future =
                    conjugateFrenchFuture(
                        subject = frenchSubject,
                        infinitive = frenchVerb
                    )

                if (!future.isNullOrBlank()) {

                    if (frenchObject.isNotBlank()) {
                        "$future $frenchObject"
                    } else {
                        future
                    }

                } else {

                    // Sujet nominal :
                    // on ne tente pas de conjugaison automatique complexe
                    if (frenchObject.isNotBlank()) {
                        "$frenchSubject va $frenchVerb $frenchObject"
                    } else {
                        "$frenchSubject va $frenchVerb"
                    }
                }
            }

            // INACCOMPLI
            "ta" -> {

                // Sujet pronominal :
                // on peut essayer de conjuguer
                val present =
                    conjugateFrenchPresent(
                        subject = frenchSubject,
                        infinitive = frenchVerb
                    )

                if (!present.isNullOrBlank()) {

                    val base =
                        "$frenchSubject $present"

                    if (frenchObject.isNotBlank()) {
                        "$base $frenchObject"
                    } else {
                        base
                    }

                } else {

                    // Sujet nominal :
                    // on garde une formulation sûre
                    val base =
                        "$frenchSubject est en train de $frenchVerb"

                    if (frenchObject.isNotBlank()) {
                        "$base $frenchObject"
                    } else {
                        base
                    }
                }
            }

            else -> null
        }
    }

    private fun translateConfirmedNegativeInaccompliToFrench(
        text: String
    ): String? {

        val words = text
            .trim()
            .replace(Regex("\\s+"), " ")
            .split(Regex("\\s+"))
            .filter { it.isNotBlank() }

        if (words.size < 3) {
            return null
        }

        // -----------------------------------------
        // CAS 1 : pronom négatif + ta + verbe
        // ex. ja ta wooko
        // -----------------------------------------

        if (
            normalizeForSearch(words[1]) == "ta"
        ) {

            val subject =
                saamakaNegativeSubjectToFrench(
                    words[0]
                )

            if (subject != null) {

                val saamakaVerb = words[2]

                val frenchVerb =
                    confirmedSaamakaVerbToFrench(
                        saamakaVerb
                    ) ?: return null

                val conjugated =
                    conjugateFrenchPresent(
                        subject = subject,
                        infinitive = frenchVerb
                    ) ?: return null

                return when (subject) {
                    "je" -> "je ne $conjugated pas"
                    "tu" -> "tu ne $conjugated pas"
                    "il/elle" -> "il/elle ne $conjugated pas"
                    "nous" -> "nous ne $conjugated pas"
                    else -> null
                }
            }
        }

        // -----------------------------------------
        // CAS 2 : sujet nominal + an + ta + verbe
        // ex. Tjuba an ta kai
        // -----------------------------------------

        val anIndex =
            words.indexOfFirst {
                normalizeForSearch(it) == "an"
            }

        if (
            anIndex > 0 &&
            anIndex + 2 < words.size &&
            normalizeForSearch(words[anIndex + 1]) == "ta"
        ) {

            val saamakaSubject =
                words.subList(0, anIndex)
                    .joinToString(" ")

            val saamakaVerb =
                words[anIndex + 2]

            // Cas explicitement attesté dans la source
            if (
                normalizeForSearch(saamakaSubject) == "tjuba" &&
                normalizeForSearch(saamakaVerb) == "kai"
            ) {
                return "il ne pleut pas"
            }

            // Pour les autres sujets nominaux :
            // on ne généralise pas encore sans preuve supplémentaire.
            return null
        }

        return null
    }

    private fun confirmedSaamakaNominalToFrench(
        text: String
    ): String? {

        return when (normalizeForSearch(text)) {

            "di mii" -> "l'enfant"
            "di mujee" -> "la femme"
            "di njanjan" -> "la nourriture"
            "di baketi" -> "l'assiette"
            "di kondee" -> "le village"

            else -> null
        }
    }

    private fun confirmedSaamakaVerbToFrench(
        text: String
    ): String? {

        return when (normalizeForSearch(text)) {

            "waka" -> "marcher"
            "sindo" -> "s'asseoir"
            "booko" -> "casser"
            "njan" -> "manger"
            "wooko" -> "travailler"
            "baka" -> "laver"
            "duumi" -> "dormir"
            "dɛ", "de" -> "être"

            else -> null
        }
    }

    private fun conjugateFrenchFuture(
        subject: String,
        infinitive: String
    ): String? {

        val verb =
            normalizeForSearch(infinitive)

        if (verb == "etre") {
            return when (subject) {
                "je" -> "je serai"
                "tu" -> "tu seras"
                "il/elle" -> "il/elle sera"
                "nous" -> "nous serons"
                "vous" -> "vous serez"
                "ils/elles" -> "ils/elles seront"
                else -> null
            }
        }

        if (verb == "dormir") {
            return when (subject) {
                "je" -> "je dormirai"
                "tu" -> "tu dormiras"
                "il/elle" -> "il/elle dormira"
                "nous" -> "nous dormirons"
                "vous" -> "vous dormirez"
                "ils/elles" -> "ils/elles dormiront"
                else -> null
            }
        }

        if (verb == "aimer") {
            return when (subject) {
                "je" -> "j'aimerai"
                "tu" -> "tu aimeras"
                "il/elle" -> "il/elle aimera"
                "nous" -> "nous aimerons"
                "vous" -> "vous aimerez"
                "ils/elles" -> "ils/elles aimeront"
                else -> null
            }
        }

        return null
    }

    fun translatePhrase(
        text: String,
        frenchToSaamaka: Boolean,
        localCorrections: List<CorrectionProposal> = emptyList()
    ): PhraseTranslationResult? {

        val cleanText = text
            .trim()
            .replace(Regex("\\s+"), " ")

        if (cleanText.isBlank()) {
            return null
        }

        fun complete(
            translation: String,
            kind: PhraseTranslationKind = PhraseTranslationKind.PROPOSAL
        ) = PhraseTranslationResult(
            translation = translation,
            recognizedSegments = listOf(
                RecognizedPhraseSegment(cleanText, translation)
            ),
            untranslatedSegments = emptyList(),
            isComplete = true,
            reliability = TranslationReliability.HIGH,
            kind = kind
        )

        // La phrase complète attestée a toujours priorité sur les règles
        // grammaticales et sur l'assemblage de segments.
        val exact = translateExactPhrase(
            text = cleanPhraseInput(cleanText),
            frenchToSaamaka = frenchToSaamaka
        )
        if (!exact.isNullOrBlank()) {
            return complete(cleanTranslationForDisplay(exact))
        }

        val attestedRule = findAttestedPhraseRule(
            text = cleanText,
            frenchToSaamaka = frenchToSaamaka
        )
        if (!attestedRule.isNullOrBlank()) {
            return complete(attestedRule, PhraseTranslationKind.VALIDATED_RULE)
        }

        if (frenchToSaamaka) {
            composeKnownVouloirPhrase(
                text = cleanText,
                resolveWord = phraseIndex().frenchFallbackResolver::resolve
            )?.let { return it }
        }

        fun correctionTranslation(segment: String): String? {
            val normalizedSegment = normalizeAttestedPhraseKey(segment)
            return localCorrections.mapNotNull { correction ->
                val source = if (frenchToSaamaka) {
                    correction.frenchProposed.ifBlank { correction.frenchCurrent }
                } else {
                    correction.saamakaProposed.ifBlank { correction.saamakaCurrent }
                }
                val translation = if (frenchToSaamaka) {
                    correction.saamakaProposed.ifBlank { correction.saamakaCurrent }
                } else {
                    correction.frenchProposed.ifBlank { correction.frenchCurrent }
                }
                translation.trim().takeIf {
                    source.isNotBlank() &&
                        it.isNotBlank() &&
                        normalizeAttestedPhraseKey(source) == normalizedSegment
                }
            }.distinctBy(::normalizeAttestedPhraseKey).singleOrNull()
        }

        if (frenchToSaamaka) {

            val confirmedNegative =
                translateConfirmedNegativePattern(
                    cleanText
                )

            if (!confirmedNegative.isNullOrBlank()) {

                return complete(confirmedNegative)
            }
        }

        if (frenchToSaamaka) {

            val normalizedSentence =
                normalizeForSearch(
                    prepareFrenchTextForTranslation(
                        cleanText
                    )
                )

            if (
                normalizedSentence == "c est" ||
                normalizedSentence == "ce est"
            ) {
                return complete("✅ Construction grammaticale attestée :\nɗa")
            }

            if (
                normalizedSentence == "ce n est pas" ||
                normalizedSentence == "ce ne est pas" ||
                normalizedSentence == "c est pas" ||
                normalizedSentence == "ce est pas"
            ) {
                return complete("✅ Construction grammaticale attestée :\nna")
            }
        }

        // -------------------------------------------------
        // 1. PRIORITÉ ABSOLUE : PHRASE EXACTE ATTESTÉE
        // -------------------------------------------------
        if (frenchToSaamaka) {

            val confirmedFuture =
                translateConfirmedFuturePattern(
                    cleanText
                )

            if (!confirmedFuture.isNullOrBlank()) {

                return complete("✅ Futur grammatical attesté :\n$confirmedFuture")
            }
        }

        if (frenchToSaamaka) {

            val confirmedNearFuture =
                translateConfirmedNearFuturePattern(
                    cleanText
                )

            if (!confirmedNearFuture.isNullOrBlank()) {

                return complete("✅ Futur grammatical attesté :\n$confirmedNearFuture")
            }
        }

        if (frenchToSaamaka) {

            val confirmedInaccompli =
                translateConfirmedInaccompliPattern(
                    cleanText
                )

            if (!confirmedInaccompli.isNullOrBlank()) {

                return complete(confirmedInaccompli, PhraseTranslationKind.GRAMMATICAL)
            }
        }
        if (!frenchToSaamaka) {

            val negativeInaccompli =
                translateConfirmedNegativeInaccompliToFrench(
                    cleanText
                )

            if (!negativeInaccompli.isNullOrBlank()) {
                return complete(negativeInaccompli)
            }
        }

        if (!frenchToSaamaka) {

            val negativeFrench =
                translateConfirmedSaamakaNegativeToFrench(
                    cleanText
                )

            if (!negativeFrench.isNullOrBlank()) {
                return complete(negativeFrench)
            }
        }
// ----------------------------------------------
// SAAMAKA -> FRANÇAIS : GRAMMAIRE o / ta
// ----------------------------------------------
        if (!frenchToSaamaka) {

            val grammaticalFrench =
                translateConfirmedSaamakaGrammarToFrench(
                    cleanText
                )

            if (!grammaticalFrench.isNullOrBlank()) {

                return complete(grammaticalFrench)
            }
        }


// ----------------------------------------------
// TRADUCTION EXACTE DU DICTIONNAIRE
// ----------------------------------------------

        val localCorrectionPairs = mutableSetOf<Pair<String, String>>()
        val frenchFallbackResolver = if (frenchToSaamaka) {
            phraseIndex().frenchFallbackResolver
        } else {
            null
        }

        return assembleAttestedPhrase(
            text = cleanText,
            highReliabilityMatch = { segment ->
                segment.detail == null && (
                    normalizeAttestedPhraseKey(segment.source) to
                        normalizeAttestedPhraseKey(segment.translation)
                    ) !in localCorrectionPairs
            }
        ) { segment ->
            findAttestedPhraseRule(segment, frenchToSaamaka)?.let {
                return@assembleAttestedPhrase RecognizedPhraseSegment(segment, it)
            }

            correctionTranslation(segment)?.let {
                localCorrectionPairs += normalizeAttestedPhraseKey(segment) to
                    normalizeAttestedPhraseKey(it)
                return@assembleAttestedPhrase RecognizedPhraseSegment(
                    source = segment,
                    translation = it,
                    detail = "$segment : correction locale utilisée"
                )
            }

            if (!frenchToSaamaka || segment.contains(' ')) {
                return@assembleAttestedPhrase translateExactPhrase(
                    text = segment,
                    frenchToSaamaka = frenchToSaamaka
                )?.let {
                    RecognizedPhraseSegment(
                        source = segment,
                        translation = cleanTranslationForDisplay(it)
                    )
                }
            }

            frenchFallbackResolver?.resolve(segment)?.let { resolution ->
                RecognizedPhraseSegment(
                    source = segment,
                    translation = cleanTranslationForDisplay(resolution.saamaka),
                    detail = resolution.detail.takeUnless {
                        resolution.kind == com.saamaka.dico.testeurs.FrenchResolutionKind.EXACT
                    },
                    matchedSource = resolution.matchedFrench,
                    alternatives = resolution.alternatives
                )
            }
        }


        // -------------------------------------------------
        // 2. NETTOYAGE LÉGER
        // -------------------------------------------------

        val textForTranslation =
            if (frenchToSaamaka) {
                prepareFrenchTextForTranslation(cleanText)
                    .replace(Regex("[{}()]"), " ")
                    .replace(Regex("\\s+"), " ")
                    .trim()
            } else {
                cleanText
                    .replace(Regex("[{}()]"), " ")
                    .replace(Regex("\\s+"), " ")
                    .trim()
            }

        val originalWords = textForTranslation
            .split(Regex("\\s+"))
            .filter { it.isNotBlank() }

        if (originalWords.isEmpty()) {
            return null
        }

        // Version normalisée utilisée seulement comme solution de repli.
        // On conserve toujours la forme originale pour rechercher
        // d'abord les expressions réellement attestées.
        val normalizedWords = originalWords.map { word ->
            if (frenchToSaamaka) {
                normalizeFrenchWordForTranslation(word)
            } else {
                word
            }
        }

        val translatedParts = mutableListOf<String>()
        val missingWords = mutableListOf<String>()

        var index = 0

        // -------------------------------------------------
        // 3. RECHERCHE DES EXPRESSIONS LES PLUS LONGUES
        // -------------------------------------------------

        while (index < originalWords.size) {

            if (frenchToSaamaka) {

                val missingExpression =
                    detectMissingFrenchExpression(
                        originalWords,
                        index
                    )

                if (missingExpression != null) {

                    val expression = missingExpression.first
                    val consumed = missingExpression.second

                    val exactExpression =
                        translateExactPhrase(
                            text = expression,
                            frenchToSaamaka = true
                        )

                    if (!exactExpression.isNullOrBlank()) {
                        translatedParts +=
                            cleanTranslationForDisplay(
                                exactExpression
                            )
                    } else {
                        translatedParts += "[$expression]"
                        missingWords += expression
                    }

                    index += consumed
                    continue
                }
            }
            var foundTranslation: String? = null
            var consumedWords = 0

            val maxChunk = minOf(
                8,
                originalWords.size - index
            )

            for (size in maxChunk downTo 1) {

                // A. Forme réellement écrite par l'utilisateur
                val originalPart = originalWords
                    .subList(index, index + size)
                    .joinToString(" ")

                var translated = translateExactPhrase(
                    text = originalPart,
                    frenchToSaamaka = frenchToSaamaka
                )

                // B. Si rien n'est trouvé en FR :
                // essai avec la forme française normalisée
                // B. Si rien n'est trouvé en FR :
// essai avec la forme française normalisée
                if (
                    translated.isNullOrBlank() &&
                    frenchToSaamaka
                ) {

                    val normalizedPart = normalizedWords
                        .subList(index, index + size)
                        .joinToString(" ")

                    if (
                        !normalizedPart.equals(
                            originalPart,
                            ignoreCase = true
                        )
                    ) {

                        val alternatives = translateExactAlternatives(
                            text = normalizedPart,
                            frenchToSaamaka = true
                        )

                        when {
                            alternatives.size == 1 -> {
                                translated = alternatives.first()
                            }

                            alternatives.size > 1 -> {
                                translatedParts +=
                                    "[$normalizedPart : plusieurs traductions]"

                                missingWords +=
                                    "$normalizedPart → " +
                                            alternatives.joinToString(" / ")

                                index += size
                                consumedWords = -1
                                break
                            }
                        }
                    }
                }
                if (!translated.isNullOrBlank()) {

                    foundTranslation =
                        cleanTranslationForDisplay(translated)

                    consumedWords = size
                    break
                }
            }

            // -------------------------------------------------
            // 4. TRADUCTION TROUVÉE OU MOT INCONNU
            // -------------------------------------------------
            if (consumedWords == -1) {
                continue
            }

            if (foundTranslation != null) {

                translatedParts += foundTranslation
                index += consumedWords

            } else {

                val originalWord = originalWords[index]

                val displayWord =
                    if (frenchToSaamaka) {
                        normalizedWords[index]
                    } else {
                        originalWord
                    }

                translatedParts += "[$displayWord]"

                val normalizedMissing = displayWord.lowercase()

                if (
                    normalizedMissing !in setOf(
                        "à",
                        "a",
                        "le",
                        "la",
                        "les",
                        "un",
                        "une",
                        "de",
                        "du",
                        "des",
                        "au",
                        "aux"
                    )
                ) {
                    missingWords += displayWord
                }

                index++
            }
        }

        // -------------------------------------------------
        // 5. CONSTRUCTION DU RÉSULTAT
        // -------------------------------------------------

        val translatedText =
            cleanTranslationForDisplay(
                translatedParts.joinToString(" ")
            )

        val missingInfo =
            if (missingWords.isNotEmpty()) {

                val uniqueMissing = missingWords
                    .distinctBy { it.lowercase() }

                "\n\n🔎 À rechercher : " +
                        uniqueMissing.joinToString(", ")

            } else {
                ""
            }

        return PhraseTranslationResult(
            translation = translatedParts.joinToString(" ").ifBlank { "[à vérifier]" },
            recognizedSegments = translatedParts.map {
                RecognizedPhraseSegment(source = "", translation = it)
            },
            untranslatedSegments = missingWords,
            isComplete = false,
            reliability = TranslationReliability.LOW
        )
    }

    private fun translateConfirmedSaamakaNegativeToFrench(
        text: String
    ): String? {

        val words = text
            .trim()
            .replace(Regex("\\s+"), " ")
            .split(Regex("\\s+"))
            .filter { it.isNotBlank() }

        if (words.size != 2) {
            return null
        }

        val subject =
            saamakaNegativeSubjectToFrench(words[0])
                ?: return null

        val saamakaVerb = words[1]

        val infinitive =
            when (normalizeForSearch(saamakaVerb)) {

                "dɛ", "de" -> "être"
                "duumi" -> "dormir"

                else -> {

                    val alternatives =
                        translateExactAlternatives(
                            text = saamakaVerb,
                            frenchToSaamaka = false
                        )

                    alternatives.firstOrNull { candidate ->

                        val normalized =
                            normalizeForSearch(candidate)

                        normalized.endsWith("er") ||
                                normalized.endsWith("ir") ||
                                normalized.endsWith("re")

                    } ?: return null
                }
            }

        val conjugated =
            conjugateFrenchPresent(
                subject = subject,
                infinitive = infinitive
            ) ?: return null

        return when (subject) {
            "je" -> "je ne $conjugated pas"
            "tu" -> "tu ne $conjugated pas"
            "il/elle" -> "il/elle ne $conjugated pas"
            "nous" -> "nous ne $conjugated pas"
            else -> null
        }
    }


    fun findByIds(ids: Set<Int>): List<DictionaryEntry> {
        if (ids.isEmpty()) {
            return emptyList()
        }

        val placeholders = ids.joinToString(",") { "?" }
        val db = openDatabase()
        val results = mutableListOf<DictionaryEntry>()

        try {
            db.rawQuery(
                """
                SELECT id, francais, english, nederlands, saamaka, categorie, valide
                FROM dictionnaire
                WHERE id IN ($placeholders)
                  AND TRIM(COALESCE(saamaka, '')) <> ''
                  AND TRIM(COALESCE(francais, '')) <> ''
                  AND UPPER(TRIM(saamaka)) NOT IN ('#NAME?', '#N/A', 'N/A')
                  AND UPPER(TRIM(francais)) NOT IN ('#NAME?', '#N/A', 'N/A')
                ORDER BY francais COLLATE NOCASE
                """.trimIndent(),
                ids.map { it.toString() }.toTypedArray()
            ).use { cursor ->

                while (cursor.moveToNext()) {
                    results += DictionaryEntry(
                        id = cursor.getInt(
                            cursor.getColumnIndexOrThrow("id")
                        ),
                        french = cursor.getString(
                            cursor.getColumnIndexOrThrow("francais")
                        ).orEmpty().trim(),
                        english = cursor.getString(
                            cursor.getColumnIndexOrThrow("english")
                        ).orEmpty().trim(),
                        dutch = cursor.getString(
                            cursor.getColumnIndexOrThrow("nederlands")
                        ).orEmpty().trim(),
                        saamaka = cursor.getString(
                            cursor.getColumnIndexOrThrow("saamaka")
                        ).orEmpty().trim(),

                        categorie = cursor.getString(
                            cursor.getColumnIndexOrThrow("categorie")
                        ).orEmpty().trim(),

                        valide = cursor.getString(
                            cursor.getColumnIndexOrThrow("valide")
                        ).orEmpty().trim()
                    )
                }
            }
        } finally {
            db.close()
        }

        return results
    }

    fun findByOrderedIds(ids: List<Int>): List<DictionaryEntry> {
        if (ids.isEmpty()) {
            return emptyList()
        }

        val entriesById = findByIds(ids.toSet()).associateBy { it.id }

        return ids.mapNotNull {
            entriesById[it]
        }
    }

    fun allEntries(): List<DictionaryEntry> {
        val db = openDatabase()
        val results = mutableListOf<DictionaryEntry>()

        try {
            db.rawQuery(
                """
                SELECT id, francais, english, nederlands, saamaka, categorie, valide
                FROM dictionnaire
                WHERE id > 1
                  AND TRIM(COALESCE(saamaka, '')) <> ''
                  AND TRIM(COALESCE(francais, '')) <> ''
                  AND UPPER(TRIM(saamaka)) NOT IN ('#NAME?', '#N/A', 'N/A')
                  AND UPPER(TRIM(francais)) NOT IN ('#NAME?', '#N/A', 'N/A')
                ORDER BY francais COLLATE NOCASE
                """.trimIndent(),
                null
            ).use { cursor ->

                while (cursor.moveToNext()) {
                    results += DictionaryEntry(
                        id = cursor.getInt(
                            cursor.getColumnIndexOrThrow("id")
                        ),
                        french = cursor.getString(
                            cursor.getColumnIndexOrThrow("francais")
                        ).orEmpty().trim(),
                        english = cursor.getString(
                            cursor.getColumnIndexOrThrow("english")
                        ).orEmpty().trim(),
                        dutch = cursor.getString(
                            cursor.getColumnIndexOrThrow("nederlands")
                        ).orEmpty().trim(),
                        saamaka = cursor.getString(
                            cursor.getColumnIndexOrThrow("saamaka")
                        ).orEmpty().trim(),

                        categorie = cursor.getString(
                            cursor.getColumnIndexOrThrow("categorie")
                        ).orEmpty().trim(),

                        valide = cursor.getString(
                            cursor.getColumnIndexOrThrow("valide")
                        ).orEmpty().trim()
                    )
                }
            }
        } finally {
            db.close()
        }

        return results.distinctBy {
            normalizeForSearch(it.french) +
                    "|" +
                    normalizeForSearch(it.saamaka)
        }
    }



    fun categories(): List<String> {
        val db = openDatabase()
        val results = mutableListOf<String>()

        try {
            db.rawQuery(
                """
            SELECT DISTINCT categorie
            FROM dictionnaire
            WHERE TRIM(COALESCE(categorie, '')) <> ''
            ORDER BY categorie COLLATE NOCASE
            """.trimIndent(),
                null
            ).use { cursor ->
                while (cursor.moveToNext()) {
                    results += cursor.getString(0).orEmpty().trim()
                }
            }
        } finally {
            db.close()
        }

        return results
    }
    fun missionByCategory(
        category: String,
        limit: Int = 50
    ): List<DictionaryEntry> {

        val db = openDatabase()

        fun loadEntries(condition: String, max: Int): List<DictionaryEntry> {
            val results = mutableListOf<DictionaryEntry>()

            db.rawQuery(
                """
            SELECT id, francais, english, nederlands, saamaka, categorie, valide
            FROM dictionnaire
            WHERE TRIM(COALESCE(francais, '')) <> ''
              AND categorie = ? COLLATE NOCASE
              AND $condition
              AND UPPER(TRIM(francais)) NOT IN ('#NAME?', '#N/A', 'N/A')
              AND UPPER(TRIM(COALESCE(saamaka, ''))) NOT IN ('#NAME?', '#N/A', 'N/A')
            ORDER BY RANDOM()
            LIMIT ?
            """.trimIndent(),
                arrayOf(category, max.toString())
            ).use { cursor ->

                while (cursor.moveToNext()) {
                    results += DictionaryEntry(
                        id = cursor.getInt(
                            cursor.getColumnIndexOrThrow("id")
                        ),
                        french = cursor.getString(
                            cursor.getColumnIndexOrThrow("francais")
                        ).orEmpty().trim(),
                        english = cursor.getString(
                            cursor.getColumnIndexOrThrow("english")
                        ).orEmpty().trim(),
                        dutch = cursor.getString(
                            cursor.getColumnIndexOrThrow("nederlands")
                        ).orEmpty().trim(),
                        saamaka = cursor.getString(
                            cursor.getColumnIndexOrThrow("saamaka")
                        ).orEmpty().trim(),
                        categorie = cursor.getString(
                            cursor.getColumnIndexOrThrow("categorie")
                        ).orEmpty().trim(),
                        valide = cursor.getString(
                            cursor.getColumnIndexOrThrow("valide")
                        ).orEmpty().trim()
                    )
                }
            }

            return results
        }

        try {
            val half = limit / 2

            // 50 % douteux
            val doubtful = loadEntries(
                "UPPER(TRIM(COALESCE(valide, ''))) = 'D'",
                half
            )

            // 50 % nouveaux / non validés
            // 50 % nouveaux = traduction Saamaka manquante
            val newEntries = loadEntries(
                "TRIM(COALESCE(saamaka, '')) = ''",
                limit - doubtful.size
            )

            val mission = (doubtful + newEntries).toMutableList()

            // Si la catégorie n'a pas assez de D ou de nouveaux,
            // on complète avec d'autres entrées de cette catégorie.
            if (mission.size < limit) {
                val missing = limit - mission.size
                val existingIds = mission.map { it.id }.toSet()

                val extra = loadEntries(
                    "1 = 1",
                    limit
                ).filter {
                    it.id !in existingIds
                }.take(missing)

                mission += extra
            }

            return mission.shuffled()

        } finally {
            db.close()
        }
    }

    private fun searchRank(
        entry: DictionaryEntry,
        normalizedQuery: String
    ): Int {

        val french = normalizeForSearch(entry.french)
        val saamaka = normalizeForSearch(entry.saamaka)

        return when {
            french == normalizedQuery ||
                    saamaka == normalizedQuery -> 0

            french.startsWith(normalizedQuery) ||
                    saamaka.startsWith(normalizedQuery) -> 1

            french.contains(normalizedQuery) ||
                    saamaka.contains(normalizedQuery) -> 2

            else -> 3
        }
    }

    private fun normalizeForSearch(value: String): String =
        Normalizer.normalize(
            value.trim(),
            Normalizer.Form.NFD
        )
            .replace("\\p{Mn}+".toRegex(), "")
            .lowercase(Locale.ROOT)

    companion object {
        const val EMBEDDED_DB_VERSION = 2
        private const val DATABASE_PREFERENCES_NAME = "embedded_dictionary"
        private const val INSTALLED_DB_VERSION_KEY = "installed_db_version"
        private const val TAG = "DictionaryDatabase"
    }
}
