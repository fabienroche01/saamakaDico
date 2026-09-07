package com.saamaka.dico.testeurs

import com.saamaka.dico.testeurs.model.DictionaryEntry

internal fun nextTesterEntry(
    allEntries: List<DictionaryEntry>,
    currentId: Int?,
    validatedIds: Set<Int>,
    deletionProposalIds: Set<Int>
): DictionaryEntry? {
    if (allEntries.isEmpty()) return null

    fun isAvailable(entry: DictionaryEntry): Boolean =
        entry.id !in validatedIds &&
            entry.id !in deletionProposalIds &&
            entry.french.isNotBlank() &&
            entry.saamaka.isNotBlank() &&
            !entry.french.equals("#NAME?", ignoreCase = true) &&
            !entry.saamaka.equals("#NAME?", ignoreCase = true)

    val currentIndex = allEntries.indexOfFirst { it.id == currentId }
    val startIndex = if (currentIndex >= 0) currentIndex else -1

    return (1..allEntries.size)
        .asSequence()
        .map { offset -> allEntries[(startIndex + offset) % allEntries.size] }
        .firstOrNull(::isAvailable)
}
