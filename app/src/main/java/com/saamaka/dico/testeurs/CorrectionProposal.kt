package com.saamaka.dico.testeurs

data class CorrectionProposal(
    val id: Long,
    val entryId: Int,
    val frenchCurrent: String,
    val saamakaCurrent: String,
    val frenchProposed: String,
    val saamakaProposed: String,
    val comment: String,
    val testerName: String,
    val createdAt: Long,
    val categoryCurrent: String = "",
    val categoryProposed: String = ""
)

internal fun proposedCorrectionValue(current: String, edited: String): String =
    edited.trim().takeUnless { it == current }.orEmpty()

fun CorrectionProposal.hasChanges(): Boolean =
    hasProposedChange(frenchCurrent, frenchProposed) ||
        hasProposedChange(saamakaCurrent, saamakaProposed) ||
        hasProposedChange(categoryCurrent, categoryProposed)

private fun hasProposedChange(current: String, proposed: String): Boolean =
    proposed.trim().isNotEmpty() && proposed.trim() != current.trim()

internal fun correctionExportText(proposals: List<CorrectionProposal>): String {
    val changed = proposals.filter(CorrectionProposal::hasChanges)
    if (changed.isEmpty()) return "Aucune correction enregistrée."

    return buildString {
        appendLine("SAAMAKA DICO — CORRECTIONS TESTEURS")
        appendLine()
        changed.forEachIndexed { index, proposal ->
            appendLine("Correction ${index + 1}")
            appendLine("Testeur : ${proposal.testerName.ifBlank { "Non renseigné" }}")
            appendLine("ID du mot : ${proposal.entryId}")
            appendLine("Français actuel : ${proposal.frenchCurrent}")
            appendLine("Saamaka actuel : ${proposal.saamakaCurrent}")
            proposal.frenchProposed.takeIf { it.isNotBlank() }?.let {
                appendLine("Français proposé : $it")
            }
            proposal.saamakaProposed.takeIf { it.isNotBlank() }?.let {
                appendLine("Saamaka proposé : $it")
            }
            proposal.categoryProposed.takeIf { it.isNotBlank() }?.let {
                appendLine("Catégorie actuelle : ${proposal.categoryCurrent}")
                appendLine("Catégorie proposée : $it")
            }
            appendLine("Commentaire : ${proposal.comment}")
            appendLine("---")
        }
    }
}
