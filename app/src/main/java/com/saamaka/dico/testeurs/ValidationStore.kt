package com.saamaka.dico.testeurs

import android.content.Context

class ValidationStore(context: Context) {
    private val preferences = context.getSharedPreferences(
        "saamaka_validations",
        Context.MODE_PRIVATE
    )

    fun validate(entryId: Int) {
        preferences.edit().putBoolean("valid_$entryId", true).apply()
    }

    fun unvalidate(entryId: Int) {
        preferences.edit().remove("valid_$entryId").apply()
    }

    fun isValidated(entryId: Int): Boolean =
        preferences.getBoolean("valid_$entryId", false)

    fun ids(): Set<Int> =
        preferences.all
            .filter { (key, value) ->
                key.startsWith("valid_") && value == true
            }
            .mapNotNull { (key, _) ->
                key.removePrefix("valid_").toIntOrNull()
            }
            .toSet()

    fun count(): Int = ids().size

    fun exportText(): String {
        val values = ids().sorted()
        if (values.isEmpty()) return "Aucun mot validé."

        return buildString {
            appendLine("SAAMAKA DICO — VALIDATIONS TESTEURS")
            appendLine("Nombre de mots validés : ${values.size}")
            appendLine()
            appendLine("Identifiants validés :")
            appendLine(values.joinToString(","))
        }
    }

    fun clear() {
        preferences.edit().clear().apply()
    }
}
