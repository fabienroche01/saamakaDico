package com.saamaka.dico.testeurs

import com.saamaka.dico.testeurs.model.DictionaryEntry

internal fun trustedLearningEntries(
    entries: List<DictionaryEntry>,
    correctedEntryIds: Set<Int>,
    deletionProposalEntryIds: Set<Int>
): List<DictionaryEntry> = entries.filter { entry ->
    entry.valide.trim().equals("O", ignoreCase = true) &&
        entry.id !in correctedEntryIds &&
        entry.id !in deletionProposalEntryIds &&
        entry.saamaka.isNotBlank() &&
        entry.french.isNotBlank() &&
        entry.categorie.isNotBlank()
}
