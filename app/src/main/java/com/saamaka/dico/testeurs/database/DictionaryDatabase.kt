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
            .replace(Regex("\\[[^]]*\\]"), "")
            .replace(Regex("\\s+"), " ")
            .trim()
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
                        else -> entry.french
                    }

                    val normalizedSource =
                        normalizeForSearch(sourceText)

                    val normalizedSaamaka =
                        normalizeForSearch(entry.saamaka)

                    when {
                        normalizedSource == normalizedQuery ||
                                normalizedSaamaka == normalizedQuery -> 0

                        normalizedSource.startsWith(normalizedQuery) ||
                                normalizedSaamaka.startsWith(normalizedQuery) -> 1

                        normalizedSource.contains(normalizedQuery) ||
                                normalizedSaamaka.contains(normalizedQuery) -> 2

                        else -> 3
                    }
                }
            )
            .take(limit)
    }

    fun translateExactPhrase(
        text: String,
        frenchToSaamaka: Boolean
    ): String? {

        val term = text.trim()

        if (term.isBlank()) {
            return null
        }

        val results = if (frenchToSaamaka) {
            search(
                query = term,
                languageCode = "fr",
                limit = 20
            )
        } else {
            search(
                query = term,
                languageCode = "fr",
                limit = 20
            )
        }

        val normalizedTerm = normalizeForSearch(term)

        val exact = results.firstOrNull { entry ->

            val source = if (frenchToSaamaka) {
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

    private fun normalizeFrenchWordForTranslation(word: String): String {
        val clean = word
            .trim()
            .lowercase()
            .trim(',', '.', ';', ':', '!', '?', '\'', '"')

        return when (clean) {
            "vais", "vas", "va", "allons", "allez", "vont",
            "allais", "allait", "allaient",
            "irai", "iras", "ira", "irons", "irez", "iront" -> "aller"

            "suis", "es", "est", "sommes", "êtes", "sont",
            "étais", "était", "étions", "étiez", "étaient",
            "serai", "seras", "sera", "serons", "serez", "seront" -> "être"

            "ai", "as", "a", "avons", "avez", "ont",
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

            else -> clean
        }
    }
    fun translatePhrase(
        text: String,
        frenchToSaamaka: Boolean
    ): String? {

        val cleanText = text.trim()

        if (cleanText.isBlank()) {
            return null
        }

        // 1. Priorité absolue : phrase/expression exacte
        val exact = translateExactPhrase(
            text = cleanText,
            frenchToSaamaka = frenchToSaamaka
        )

        if (!exact.isNullOrBlank()) {
            return "✅ Traduction attestée dans le dictionnaire :\n$exact"
        }

        val textForTranslation = cleanText
            .replace(Regex("[{}()]"), " ")
            .replace(Regex("\\s+"), " ")
            .trim()
        // 2. Sinon traduction approximative
        val words = textForTranslation
            .split(Regex("\\s+"))
            .filter { it.isNotBlank() }
            .map { word ->
                if (frenchToSaamaka) {
                    normalizeFrenchWordForTranslation(word)
                } else {
                    word
                }
            }

        if (words.isEmpty()) {
            return null
        }

        val translatedParts = mutableListOf<String>()

        var index = 0

        while (index < words.size) {

            var foundTranslation: String? = null
            var consumedWords = 0

            // On essaie d'abord les expressions les plus longues
            val maxChunk = minOf(8, words.size - index)

            for (size in maxChunk downTo 1) {

                val part = words
                    .subList(index, index + size)
                    .joinToString(" ")

                val translated = translateExactPhrase(
                    text = part,
                    frenchToSaamaka = frenchToSaamaka
                )

                if (!translated.isNullOrBlank()) {
                    foundTranslation = translated
                    consumedWords = size
                    break
                }
            }

            if (foundTranslation != null) {
                translatedParts += cleanTranslationForDisplay(foundTranslation)
                index += consumedWords
            } else {
                // Mot non trouvé : on le conserve pour ne pas inventer
                val missingWord = words[index]

                translatedParts += "[$missingWord]"
                index++
            }
        }

        val missingParts = translatedParts
            .filter { it.startsWith("[") && it.endsWith("]") }

        val missingInfo = if (missingParts.isNotEmpty()) {
            "\n\n🔎 À rechercher : " +
                    missingParts.joinToString(", ") {
                        it.removePrefix("[").removeSuffix("]")
                    }
        } else {
            ""
        }

        return "⚠️ Traduction approximative — à vérifier :\n" +
                translatedParts.joinToString(" ") +
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