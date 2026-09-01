package com.saamaka.dico.testeurs.repository

import android.content.Context
import com.saamaka.dico.testeurs.CorrectionProposal
import org.json.JSONArray
import org.json.JSONObject

class CorrectionStore(context: Context) {
    private val preferences = context.getSharedPreferences(
        "saamaka_corrections",
        Context.MODE_PRIVATE
    )

    fun save(proposal: CorrectionProposal) {
        val all = all().toMutableList()
        all.add(0, proposal)
        write(all)
    }

    fun all(): List<CorrectionProposal> {
        val raw = preferences.getString(KEY_CORRECTIONS, "[]") ?: "[]"
        val array = runCatching { JSONArray(raw) }.getOrElse { JSONArray() }

        return buildList {
            for (index in 0 until array.length()) {
                val item = array.optJSONObject(index) ?: continue
                add(
                    CorrectionProposal(
                        id = item.optLong("id"),
                        entryId = item.optInt("entryId"),
                        frenchCurrent = item.optString("frenchCurrent"),
                        saamakaCurrent = item.optString("saamakaCurrent"),
                        frenchProposed = item.optString("frenchProposed"),
                        saamakaProposed = item.optString("saamakaProposed"),
                        comment = item.optString("comment"),
                        testerName = item.optString("testerName"),
                        createdAt = item.optLong("createdAt")
                    )
                )
            }
        }
    }

    fun latestForEntry(entryId: Int): CorrectionProposal? {
        return all().firstOrNull { it.entryId == entryId }
    }

    fun latestByEntry(): Map<Int, CorrectionProposal> = buildMap {
        all().forEach { proposal -> putIfAbsent(proposal.entryId, proposal) }
    }

    fun clear() {
        preferences.edit().remove(KEY_CORRECTIONS).apply()
    }

    fun testerName(): String =
        preferences.getString(KEY_TESTER_NAME, "").orEmpty()

    fun setTesterName(name: String) {
        val newName = name.trim()

        if (newName.isBlank()) {
            return
        }

        val existingName = preferences
            .getString(KEY_TESTER_NAME, "")
            ?.trim()
            .orEmpty()

        // V13 : l'identité du testeur est enregistrée une seule fois.
        // Un nom déjà présent ne peut pas être remplacé silencieusement.
        if (existingName.isNotBlank()) {
            return
        }

        preferences.edit()
            .putString(KEY_TESTER_NAME, newName)
            .apply()
    }

    fun exportText(): String {
        val proposals = all()
        if (proposals.isEmpty()) return "Aucune correction enregistrée."

        return buildString {
            appendLine("SAAMAKA DICO — CORRECTIONS TESTEURS")
            appendLine()

            proposals.forEachIndexed { index, proposal ->
                appendLine("Correction ${index + 1}")
                appendLine("Testeur : ${proposal.testerName.ifBlank { "Non renseigné" }}")
                appendLine("ID du mot : ${proposal.entryId}")
                appendLine("Français actuel : ${proposal.frenchCurrent}")
                appendLine("Saamaka actuel : ${proposal.saamakaCurrent}")
                appendLine("Français proposé : ${proposal.frenchProposed}")
                appendLine("Saamaka proposé : ${proposal.saamakaProposed}")
                appendLine("Commentaire : ${proposal.comment}")
                appendLine("---")
            }
        }
    }

    private fun write(proposals: List<CorrectionProposal>) {
        val array = JSONArray()

        proposals.forEach { proposal ->
            array.put(
                JSONObject().apply {
                    put("id", proposal.id)
                    put("entryId", proposal.entryId)
                    put("frenchCurrent", proposal.frenchCurrent)
                    put("saamakaCurrent", proposal.saamakaCurrent)
                    put("frenchProposed", proposal.frenchProposed)
                    put("saamakaProposed", proposal.saamakaProposed)
                    put("comment", proposal.comment)
                    put("testerName", proposal.testerName)
                    put("createdAt", proposal.createdAt)
                }
            )
        }

        preferences.edit()
            .putString(KEY_CORRECTIONS, array.toString())
            .apply()
    }

    private companion object {
        const val KEY_CORRECTIONS = "corrections"
        const val KEY_TESTER_NAME = "tester_name"
    }
}
