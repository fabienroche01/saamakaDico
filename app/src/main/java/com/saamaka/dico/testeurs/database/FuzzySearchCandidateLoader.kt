package com.saamaka.dico.testeurs.database

import android.content.Context
import android.database.sqlite.SQLiteDatabase
import com.saamaka.dico.testeurs.accentInsensitiveGlob
import com.saamaka.dico.testeurs.model.DictionaryEntry
import com.saamaka.dico.testeurs.normalizeMultilingualSearch

internal fun loadFuzzySearchCandidates(
    context: Context,
    databaseName: String,
    normalizedQuery: String,
    languageCode: String?,
    limit: Int = 250
): List<DictionaryEntry> {
    val query = normalizeMultilingualSearch(normalizedQuery)
    val firstToken = query.substringBefore(' ').filter { it.isLetterOrDigit() }
    if (firstToken.length < 4) return emptyList()

    val prefixLength = if (firstToken.length >= 6) 2 else 1
    val prefix = firstToken.take(prefixLength)
    val prefixGlob = accentInsensitiveGlob(prefix) + "*"

    val columns = if (languageCode == null) {
        listOf("saamaka", "francais", "english", "nederlands")
    } else {
        listOf(
            when (languageCode) {
                "en" -> "english"
                "nl" -> "nederlands"
                "srm" -> "saamaka"
                else -> "francais"
            }
        )
    }

    val where = columns.joinToString(" OR ") { "$it GLOB ?" }
    val args = buildList {
        repeat(columns.size) { add(prefixGlob) }
        add(limit.toString())
    }.toTypedArray()

    val file = context.getDatabasePath(databaseName)
    if (!file.exists()) return emptyList()

    val db = SQLiteDatabase.openDatabase(
        file.absolutePath,
        null,
        SQLiteDatabase.OPEN_READONLY
    )

    return try {
        buildList {
            db.rawQuery(
                """
                SELECT id, francais, english, nederlands, saamaka, categorie, valide
                FROM dictionnaire
                WHERE id > 1
                  AND TRIM(COALESCE(saamaka, '')) <> ''
                  AND TRIM(COALESCE(francais, '')) <> ''
                  AND UPPER(TRIM(saamaka)) NOT IN ('#NAME?', '#N/A', 'N/A')
                  AND UPPER(TRIM(francais)) NOT IN ('#NAME?', '#N/A', 'N/A')
                  AND ($where)
                ORDER BY id
                LIMIT ?
                """.trimIndent(),
                args
            ).use { cursor ->
                while (cursor.moveToNext()) {
                    add(
                        DictionaryEntry(
                            id = cursor.getInt(cursor.getColumnIndexOrThrow("id")),
                            french = cursor.getString(cursor.getColumnIndexOrThrow("francais")).orEmpty().trim(),
                            english = cursor.getString(cursor.getColumnIndexOrThrow("english")).orEmpty().trim(),
                            dutch = cursor.getString(cursor.getColumnIndexOrThrow("nederlands")).orEmpty().trim(),
                            saamaka = cursor.getString(cursor.getColumnIndexOrThrow("saamaka")).orEmpty().trim(),
                            categorie = cursor.getString(cursor.getColumnIndexOrThrow("categorie")).orEmpty().trim(),
                            valide = cursor.getString(cursor.getColumnIndexOrThrow("valide")).orEmpty().trim()
                        )
                    )
                }
            }
        }
    } finally {
        db.close()
    }
}
