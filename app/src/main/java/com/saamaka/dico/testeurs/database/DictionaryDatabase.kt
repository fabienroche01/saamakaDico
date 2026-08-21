package com.saamaka.dico.testeurs.database

import android.content.Context
import android.database.sqlite.SQLiteDatabase
import com.saamaka.dico.testeurs.model.DictionaryEntry
import java.io.FileOutputStream
import java.text.Normalizer
import java.util.Locale

class DictionaryDatabase(private val context: Context) {

    private val databaseName = "SaamakaDico_v11_25.db"

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

    private fun openDatabase(): SQLiteDatabase {
        val destination = context.getDatabasePath(databaseName)

        if (!destination.exists()) {
            destination.parentFile?.mkdirs()

            context.assets.open(databaseName).use { input ->
                FileOutputStream(destination).use { output ->
                    input.copyTo(output)
                }
            }
        }

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
        languageCode: String = "fr",
        limit: Int = 100
    ): List<DictionaryEntry> {

        val term = query.trim()

        if (term.isBlank()) {
            return emptyList()
        }

        val sourceColumn = when (languageCode) {
            "en" -> "english"
            "nl" -> "nederlands"
            "srm" -> "saamaka"
            else -> "francais"
        }

        val db = openDatabase()
        val candidates = mutableListOf<DictionaryEntry>()

        val contains = "%$term%"
        val starts = "$term%"

        try {
            db.rawQuery(
                """
            SELECT id, francais, english, nederlands, saamaka, categorie, valide
                FROM dictionnaire
            WHERE id > 1
              AND TRIM(COALESCE(saamaka, '')) <> ''
              AND TRIM(COALESCE($sourceColumn, '')) <> ''
              AND UPPER(TRIM(saamaka)) NOT IN ('#NAME?', '#N/A', 'N/A')
              AND UPPER(TRIM($sourceColumn)) NOT IN ('#NAME?', '#N/A', 'N/A')
              AND (
                  saamaka LIKE ? COLLATE NOCASE
                  OR $sourceColumn LIKE ? COLLATE NOCASE
              )
            ORDER BY
  CASE
      WHEN saamaka = ? COLLATE NOCASE
        OR $sourceColumn = ? COLLATE NOCASE THEN 0
      WHEN saamaka LIKE ? COLLATE NOCASE
        OR $sourceColumn LIKE ? COLLATE NOCASE THEN 1
      ELSE 2
  END,
  $sourceColumn COLLATE NOCASE
LIMIT ?
""".trimIndent(),
                arrayOf(
                    contains,
                    contains,
                    term,
                    term,
                    starts,
                    starts,
                    limit.toString()
                )
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

        val normalizedQuery = normalizeForSearch(term)

        return candidates
            .distinctBy { entry ->
                val sourceText = when (languageCode) {
                    "en" -> entry.english
                    "nl" -> entry.dutch
                    "srm" -> entry.saamaka
                    else -> entry.french

                }

                normalizeForSearch(sourceText) +
                        "|" +
                        normalizeForSearch(entry.saamaka)
            }
            .sortedWith(
                compareBy<DictionaryEntry> { entry ->
                    val sourceText = when (languageCode) {
                        "en" -> entry.english
                        "nl" -> entry.dutch
                        "srm" -> entry.saamaka
                        else -> entry.french
                    }

                    val normalizedSource =
                        normalizeForSearch(sourceText)

                    val normalizedSaamaka =
                        normalizeForSearch(entry.saamaka)

                    when {

                        // 1. Entrée exactement identique
                        normalizedSource == normalizedQuery ||
                                normalizedSaamaka == normalizedQuery -> 0

                        // 2. Le terme existe comme mot séparé
                        normalizedSource
                            .split(Regex("\\s+"))
                            .contains(normalizedQuery) ||
                                normalizedSaamaka
                                    .split(Regex("\\s+"))
                                    .contains(normalizedQuery) -> 1

                        // 3. L'expression commence par le terme
                        normalizedSource.startsWith("$normalizedQuery ") ||
                                normalizedSaamaka.startsWith("$normalizedQuery ") -> 2

                        // 4. Correspondance partielle classique
                        normalizedSource.contains(normalizedQuery) ||
                                normalizedSaamaka.contains(normalizedQuery) -> 3

                        else -> 4
                    }
                }
            )
            .take(limit)
    }

    private fun translateExactAlternatives(
        text: String,
        frenchToSaamaka: Boolean
    ): List<String> {

        val term = text.trim()

        if (term.isBlank()) {
            return emptyList()
        }

        val languageCode =
            if (frenchToSaamaka) {
                "fr"
            } else {
                "srm"
            }

        val normalizedTerm = normalizeForSearch(term)

        return search(
            query = term,
            languageCode = languageCode,
            limit = 100
        )
            .filter { entry ->

                val source =
                    if (frenchToSaamaka) {
                        normalizeForSearch(entry.french)
                    } else {
                        normalizeForSearch(entry.saamaka)
                    }

                source == normalizedTerm
            }
            .sortedWith(
                compareByDescending<DictionaryEntry> {
                    it.valide.equals(
                        "O",
                        ignoreCase = true
                    )
                }
                    .thenBy {
                        it.valide.equals(
                            "D",
                            ignoreCase = true
                        )
                    }
            )
            .mapNotNull { entry ->

                val translation =
                    if (frenchToSaamaka) {
                        entry.saamaka
                    } else {
                        entry.french
                    }

                translation
                    .trim()
                    .takeIf { it.isNotBlank() }
            }
            .distinctBy {
                normalizeForSearch(it)
            }
    }

    fun translateExactPhrase(
        text: String,
        frenchToSaamaka: Boolean
    ): String? {

        val term = text.trim()

        if (term.isBlank()) {
            return null
        }

        val languageCode =
            if (frenchToSaamaka) {
                "fr"
            } else {
                "srm"
            }

        val results = search(
            query = term,
            languageCode = languageCode,
            limit = 100
        )

        val normalizedTerm = normalizeForSearch(term)

        val exact = results
            .sortedWith(
                compareByDescending<DictionaryEntry> {
                    it.valide.equals("O", ignoreCase = true)
                }
                    .thenBy {
                        it.valide.equals("D", ignoreCase = true)
                    }
                    .thenBy {
                        if (frenchToSaamaka) {
                            it.french.contains(" ")
                        } else {
                            it.saamaka.contains(" ")
                        }
                    }
            )
            .firstOrNull { entry ->

                val source =
                    if (frenchToSaamaka) {
                        normalizeForSearch(entry.french)
                    } else {
                        normalizeForSearch(entry.saamaka)
                    }

                source == normalizedTerm
            }

        return exact?.let { entry ->
            if (frenchToSaamaka) {
                entry.saamaka
            } else {
                entry.french
            }
        }
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
    private fun normalizeFrenchWordForTranslation(word: String): String {
        val clean = word
            .trim()
            .lowercase()
            .trim(',', '.', ';', ':', '!', '?', '\'', '"')

        return when (clean) {
            "j'", "j’" -> "je"
            "vais", "vas", "va", "allons", "allez", "vont",
            "allais", "allait", "allaient",
            "irai", "iras", "ira", "irons", "irez", "iront" -> "aller"

            "suis", "es", "est", "sommes", "êtes", "sont",
            "étais", "était", "étions", "étiez", "étaient",
            "serai", "seras", "sera", "serons", "serez", "seront" -> "être"

            "ai", "as", "avons", "avez", "ont",
            "avais", "avait", "avions", "aviez", "avaient",
            "aurai", "auras", "aura", "aurons", "aurez", "auront" -> "avoir"

            "fais", "fait", "faisons", "faites", "font",
            "faisais", "faisait", "faisaient",
            "ferai", "feras", "fera", "ferons", "ferez", "feront" -> "faire"

            "veux", "veut", "voulons", "voulez", "veulent",
            "voulais", "voulait", "voulaient",
            "voudrais", "voudrait" -> "vouloir"

            "peux", "peut", "pouvons", "pouvez", "peuvent",
            "pouvais", "pouvait", "pouvaient",
            "pourrai", "pourras", "pourra", "pourront" -> "pouvoir"

            "viens", "vient", "venons", "venez", "viennent",
            "venais", "venait", "venaient",
            "viendrai", "viendras", "viendra", "viendront" -> "venir"

            "prends", "prend", "prenons", "prenez", "prennent",
            "prenais", "prenait", "prenaient",
            "prendrai", "prendras", "prendra", "prendront" -> "prendre"

            "dors", "dort", "dormons", "dormez", "dorment",
            "dormais", "dormait", "dormaient",
            "dormirai", "dormiras", "dormira", "dormiront" -> "dormir"

            "mange", "manges", "mangeons", "mangez", "mangent",
            "mangeais", "mangeait", "mangeaient",
            "mangerai", "mangeras", "mangera", "mangeront" -> "manger"

            "aime", "aimes", "aimons", "aimez", "aiment",
            "aimais", "aimait", "aimions", "aimiez", "aimaient",
            "aimerai", "aimeras", "aimera", "aimerons", "aimerez", "aimeront" -> "aimer"

            else -> clean
            }
        }

    fun translatePhrase(
        text: String,
        frenchToSaamaka: Boolean
    ): String? {

        val cleanText = text
            .trim()
            .replace(Regex("\\s+"), " ")

        if (cleanText.isBlank()) {
            return null
        }

        if (frenchToSaamaka) {

            val confirmedNegative =
                translateConfirmedNegativePattern(
                    cleanText
                )

            if (!confirmedNegative.isNullOrBlank()) {

                return "✅ Construction grammaticale attestée :\n" +
                        confirmedNegative
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
                return "✅ Construction grammaticale attestée :\nɗa"
            }

            if (
                normalizedSentence == "ce n est pas" ||
                normalizedSentence == "ce ne est pas" ||
                normalizedSentence == "c est pas" ||
                normalizedSentence == "ce est pas"
            ) {
                return "✅ Construction grammaticale attestée :\nna"
            }
        }

        // -------------------------------------------------
        // 1. PRIORITÉ ABSOLUE : PHRASE EXACTE ATTESTÉE
        // -------------------------------------------------

        val exact = translateExactPhrase(
            text = cleanText,
            frenchToSaamaka = frenchToSaamaka
        )

        if (!exact.isNullOrBlank()) {
            return "✅ Traduction attestée dans le dictionnaire :\n" +
                    cleanTranslationForDisplay(exact)
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

        return "⚠️ Traduction approximative — à vérifier :\n" +
                translatedText +
                missingInfo
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
}