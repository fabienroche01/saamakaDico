package com.saamaka.dico.testeurs.repository

import android.content.Context
import com.saamaka.dico.testeurs.DeletionProposal
import org.json.JSONArray
import org.json.JSONObject
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class DeletionProposalStore(context: Context) {
    private val preferences = context.getSharedPreferences(
        "saamaka_deletion_proposals",
        Context.MODE_PRIVATE
    )

    fun save(proposal: DeletionProposal) {
        val proposals = all().filterNot { it.entryId == proposal.entryId }.toMutableList()
        proposals.add(0, proposal)
        write(proposals)
    }

    fun proposalFor(entryId: Int): DeletionProposal? =
        all().firstOrNull { it.entryId == entryId }

    fun cancel(entryId: Int) {
        write(all().filterNot { it.entryId == entryId })
    }

    fun all(): List<DeletionProposal> {
        val raw = preferences.getString(KEY_PROPOSALS, "[]").orEmpty()
        val array = runCatching { JSONArray(raw) }.getOrElse { JSONArray() }
        return buildList {
            for (index in 0 until array.length()) {
                val item = array.optJSONObject(index) ?: continue
                add(
                    DeletionProposal(
                        entryId = item.optInt("entryId"),
                        saamaka = item.optString("saamaka"),
                        french = item.optString("french"),
                        english = item.optString("english"),
                        dutch = item.optString("dutch"),
                        reason = item.optString("reason"),
                        comment = item.optString("comment"),
                        testerName = item.optString("testerName"),
                        createdAt = item.optLong("createdAt")
                    )
                )
            }
        }
    }

    fun exportText(sinceMillis: Long = 0L): String {
        val proposals = all().filter { it.createdAt > sinceMillis }
        if (proposals.isEmpty()) return "Aucune proposition de suppression enregistrée."
        val formatter = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.ROOT)
        return buildString {
            appendLine("SAAMAKA DICO — PROPOSITIONS DE SUPPRESSION")
            appendLine("Nombre de propositions : ${proposals.size}")
            appendLine()
            proposals.forEachIndexed { index, proposal ->
                appendLine("Proposition ${index + 1}")
                appendLine("ID de l'entrée : ${proposal.entryId}")
                appendLine("Saamaka : ${proposal.saamaka}")
                if (proposal.french.isNotBlank()) appendLine("Français : ${proposal.french}")
                if (proposal.english.isNotBlank()) appendLine("English : ${proposal.english}")
                if (proposal.dutch.isNotBlank()) appendLine("Nederlands : ${proposal.dutch}")
                appendLine("Motif : ${proposal.reason}")
                if (proposal.comment.isNotBlank()) appendLine("Commentaire : ${proposal.comment}")
                appendLine("Testeur : ${proposal.testerName.ifBlank { "Non renseigné" }}")
                appendLine("Date : ${formatter.format(Date(proposal.createdAt))}")
                appendLine("---")
            }
        }
    }

    private fun write(proposals: List<DeletionProposal>) {
        val array = JSONArray()
        proposals.forEach { proposal ->
            array.put(JSONObject().apply {
                put("entryId", proposal.entryId)
                put("saamaka", proposal.saamaka)
                put("french", proposal.french)
                put("english", proposal.english)
                put("dutch", proposal.dutch)
                put("reason", proposal.reason)
                put("comment", proposal.comment)
                put("testerName", proposal.testerName)
                put("createdAt", proposal.createdAt)
            })
        }
        preferences.edit().putString(KEY_PROPOSALS, array.toString()).apply()
    }

    private companion object {
        const val KEY_PROPOSALS = "proposals"
    }
}
