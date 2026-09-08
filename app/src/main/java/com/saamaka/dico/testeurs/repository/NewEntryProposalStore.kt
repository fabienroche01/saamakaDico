package com.saamaka.dico.testeurs.repository

import android.content.Context
import com.saamaka.dico.testeurs.NewEntryProposal
import org.json.JSONArray
import org.json.JSONObject
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class NewEntryProposalStore(context: Context) {
    private val preferences = context.getSharedPreferences(
        "saamaka_new_entry_proposals",
        Context.MODE_PRIVATE
    )

    fun save(proposal: NewEntryProposal) {
        val proposals = all().filterNot { it.localId == proposal.localId }.toMutableList()
        proposals.add(0, proposal)
        write(proposals)
    }

    fun cancel(localId: String) {
        write(all().filterNot { it.localId == localId })
    }

    fun all(): List<NewEntryProposal> {
        val array = runCatching {
            JSONArray(preferences.getString(KEY_PROPOSALS, "[]").orEmpty())
        }.getOrElse { JSONArray() }
        return buildList {
            for (index in 0 until array.length()) {
                val item = array.optJSONObject(index) ?: continue
                add(
                    NewEntryProposal(
                        localId = item.optString("localId"),
                        saamaka = item.optString("saamaka"),
                        french = item.optString("french"),
                        english = item.optString("english"),
                        dutch = item.optString("dutch"),
                        category = item.optString("category"),
                        comment = item.optString("comment"),
                        testerName = item.optString("testerName"),
                        createdAt = item.optLong("createdAt"),
                        audioFileName = item.optString("audioFileName").takeIf { it.isNotBlank() }
                    )
                )
            }
        }
    }

    fun exportText(sinceMillis: Long = 0L): String {
        val proposals = all().filter { it.createdAt > sinceMillis }
        if (proposals.isEmpty()) return "Aucune nouvelle entrée proposée."
        val formatter = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.ROOT)
        return buildString {
            appendLine("SAAMAKA DICO — NOUVELLES ENTRÉES PROPOSÉES")
            appendLine("Nombre de propositions : ${proposals.size}")
            appendLine()
            proposals.forEachIndexed { index, proposal ->
                appendLine("Proposition ${index + 1}")
                appendLine("Identifiant local : ${proposal.localId}")
                appendLine("Saamaka : ${proposal.saamaka}")
                if (proposal.french.isNotBlank()) appendLine("Français : ${proposal.french}")
                if (proposal.english.isNotBlank()) appendLine("English : ${proposal.english}")
                if (proposal.dutch.isNotBlank()) appendLine("Nederlands : ${proposal.dutch}")
                appendLine("Catégorie : ${proposal.category}")
                if (proposal.comment.isNotBlank()) appendLine("Commentaire : ${proposal.comment}")
                appendLine("Testeur : ${proposal.testerName}")
                appendLine("Date : ${formatter.format(Date(proposal.createdAt))}")
                appendLine("Fichier audio : ${proposal.audioFileName ?: "Aucun"}")
                appendLine("---")
            }
        }
    }

    private fun write(proposals: List<NewEntryProposal>) {
        val array = JSONArray()
        proposals.forEach { proposal ->
            array.put(JSONObject().apply {
                put("localId", proposal.localId)
                put("saamaka", proposal.saamaka)
                put("french", proposal.french)
                put("english", proposal.english)
                put("dutch", proposal.dutch)
                put("category", proposal.category)
                put("comment", proposal.comment)
                put("testerName", proposal.testerName)
                put("createdAt", proposal.createdAt)
                put("audioFileName", proposal.audioFileName.orEmpty())
            })
        }
        preferences.edit().putString(KEY_PROPOSALS, array.toString()).apply()
    }

    private companion object {
        const val KEY_PROPOSALS = "proposals"
    }
}
