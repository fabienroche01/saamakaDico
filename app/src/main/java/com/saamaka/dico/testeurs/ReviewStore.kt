package com.saamaka.dico.testeurs

import android.content.Context
import org.json.JSONArray
import org.json.JSONObject

class ReviewStore(context: Context) {

    private val preferences =
        context.getSharedPreferences(
            "saamaka_reviews",
            Context.MODE_PRIVATE
        )

    fun save(action: ReviewAction) {
        val actions = all().toMutableList()

        actions.removeAll { existingAction ->
            existingAction.entryId == action.entryId
        }

        actions.add(
            index = 0,
            element = action
        )

        write(actions)
    }

    fun all(): List<ReviewAction> {
        val raw = preferences.getString(KEY_REVIEWS, "[]").orEmpty()

        val array = runCatching {
            JSONArray(raw)
        }.getOrElse {
            JSONArray()
        }

        return buildList {
            for (index in 0 until array.length()) {
                val item = array.optJSONObject(index) ?: continue

                val actionType = runCatching {
                    ReviewActionType.valueOf(
                        item.optString("action")
                    )
                }.getOrElse {
                    ReviewActionType.REJECTED
                }

                add(
                    ReviewAction(
                        id = item.optLong("id"),
                        entryId = item.optInt("entryId"),
                        sourceLanguage = item.optString("sourceLanguage", "fr"),
                        targetLanguage = item.optString("targetLanguage", "srm"),
                        frenchCurrent =
                            item.optString("frenchCurrent"),
                        saamakaCurrent =
                            item.optString("saamakaCurrent"),
                        action = actionType,
                        frenchProposed =
                            item.optString("frenchProposed")
                                .takeIf { it.isNotBlank() },
                        saamakaProposed =
                            item.optString("saamakaProposed")
                                .takeIf { it.isNotBlank() },
                        comment =
                            item.optString("comment"),
                        reviewer =
                            item.optString("reviewer"),
                        createdAt =
                            item.optLong("createdAt"),
                        categoryCurrent = item.optString("categoryCurrent"),
                        categoryProposed = item.optString("categoryProposed")
                            .takeIf { it.isNotBlank() }
                    )
                )
            }
        }
    }

    fun validatedCount(): Int =
        all().count { it.action == ReviewActionType.VALIDATED }

    fun correctedCount(): Int =
        all().count { it.action == ReviewActionType.CORRECTED }

    fun exportText(sinceMillis: Long = 0L): String {
        // Corrections are exported from CorrectionStore, which applies the shared hasChanges rule.
        val actions = all().filter { it.action != ReviewActionType.CORRECTED && it.createdAt > sinceMillis }

        if (actions.isEmpty()) {
            return "Aucune validation ou correction enregistrée."
        }

        return buildString {
            appendLine("SAAMAKA DICO — REVUES LINGUISTIQUES")
            appendLine("Nombre d'actions : ${actions.size}")
            appendLine()

            actions.forEachIndexed { index, action ->
                appendLine("Action ${index + 1}")
                appendLine("ID mot : ${action.entryId}")
                appendLine("Français : ${action.frenchCurrent}")
                appendLine("Saamaka : ${action.saamakaCurrent}")
                appendLine("Type : ${action.action}")
                appendLine("Correcteur : ${action.reviewer}")

                if (!action.frenchProposed.isNullOrBlank()) {
                    appendLine("Français proposé : ${action.frenchProposed}")
                }

                if (!action.saamakaProposed.isNullOrBlank()) {
                    appendLine("Saamaka proposé : ${action.saamakaProposed}")
                }

                if (!action.categoryProposed.isNullOrBlank()) {
                    appendLine("Catégorie actuelle : ${action.categoryCurrent}")
                    appendLine("Catégorie proposée : ${action.categoryProposed}")
                }

                if (action.comment.isNotBlank()) {
                    appendLine("Commentaire : ${action.comment}")
                }

                appendLine("Date : ${action.createdAt}")
                appendLine("------------------------------")
            }
        }
    }
    private fun write(actions: List<ReviewAction>) {
        val array = JSONArray()

        actions.forEach { action ->
            array.put(
                JSONObject().apply {
                    put("id", action.id)
                    put("entryId", action.entryId)
                    put("sourceLanguage", action.sourceLanguage)
                    put("targetLanguage", action.targetLanguage)
                    put("frenchCurrent", action.frenchCurrent)
                    put("saamakaCurrent", action.saamakaCurrent)
                    put("action", action.action.name)
                    put(
                        "frenchProposed",
                        action.frenchProposed.orEmpty()
                    )
                    put(
                        "saamakaProposed",
                        action.saamakaProposed.orEmpty()
                    )
                    put("categoryCurrent", action.categoryCurrent)
                    put("categoryProposed", action.categoryProposed.orEmpty())
                    put("comment", action.comment)
                    put("reviewer", action.reviewer)
                    put("createdAt", action.createdAt)
                }
            )
        }

        preferences.edit()
            .putString(KEY_REVIEWS, array.toString())
            .apply()
    }

    private companion object {
        const val KEY_REVIEWS = "reviews"
    }
}
