package com.saamaka.dico.testeurs

import com.saamaka.dico.testeurs.model.DictionaryEntry

data class DictionaryQualityAudit(
    val totalEntries: Int,
    val exactDuplicateGroups: Int,
    val missingFrench: Int,
    val missingSaamaka: Int,
    val longExpressions: Int,
    val categoryVariantGroups: Int,
    val affectedEntryIds: Set<Int> = emptySet()
) {
    val missingFields: Int get() = missingFrench + missingSaamaka
    val issueCount: Int
        get() = exactDuplicateGroups + missingFields + longExpressions + categoryVariantGroups
    val affectedEntryCount: Int get() = affectedEntryIds.size
}

data class DictionaryQualityDetails(
    val exactDuplicateEntryIds: Set<Int>,
    val missingFrenchEntryIds: Set<Int>,
    val missingSaamakaEntryIds: Set<Int>,
    val longExpressionEntryIds: Set<Int>,
    val categoryVariantEntryIds: Set<Int>
) {
    val affectedEntryIds: Set<Int>
        get() = exactDuplicateEntryIds + missingFrenchEntryIds + missingSaamakaEntryIds +
            longExpressionEntryIds + categoryVariantEntryIds
}

private fun normalizeQualityText(value: String): String =
    value.trim().lowercase().replace(Regex("\\s+"), " ")

fun inspectDictionaryQuality(entries: List<DictionaryEntry>): DictionaryQualityDetails {
    val duplicateGroups = entries
        .asSequence()
        .filter { it.saamaka.isNotBlank() && it.french.isNotBlank() }
        .groupBy {
            normalizeQualityText(it.saamaka) + "\u0000" + normalizeQualityText(it.french)
        }
        .values
        .filter { it.size > 1 }
        .toList()

    val categoryVariantGroups = entries
        .asSequence()
        .filter { it.categorie.isNotBlank() }
        .groupBy { normalizeQualityText(it.categorie) }
        .values
        .filter { group -> group.map { it.categorie.trim() }.distinct().size > 1 }
        .toList()

    return DictionaryQualityDetails(
        exactDuplicateEntryIds = duplicateGroups.flatten().map { it.id }.toSet(),
        missingFrenchEntryIds = entries.filter { it.french.isBlank() }.map { it.id }.toSet(),
        missingSaamakaEntryIds = entries.filter { it.saamaka.isBlank() }.map { it.id }.toSet(),
        longExpressionEntryIds = entries.filter { entry ->
            val frenchWords = entry.french.trim().split(Regex("\\s+")).count { it.isNotBlank() }
            val saamakaWords = entry.saamaka.trim().split(Regex("\\s+")).count { it.isNotBlank() }
            entry.french.length > 100 || entry.saamaka.length > 100 ||
                frenchWords > 14 || saamakaWords > 14
        }.map { it.id }.toSet(),
        categoryVariantEntryIds = categoryVariantGroups.flatten().map { it.id }.toSet()
    )
}

fun auditDictionaryEntries(entries: List<DictionaryEntry>): DictionaryQualityAudit {
    val details = inspectDictionaryQuality(entries)

    val duplicateGroups = entries
        .asSequence()
        .filter { it.saamaka.isNotBlank() && it.french.isNotBlank() }
        .groupBy {
            normalizeQualityText(it.saamaka) + "\u0000" + normalizeQualityText(it.french)
        }
        .values
        .count { it.size > 1 }

    val categoryVariantGroups = entries
        .asSequence()
        .map { it.categorie.trim() }
        .filter { it.isNotBlank() }
        .groupBy(::normalizeQualityText)
        .values
        .count { variants -> variants.map { it.trim() }.distinct().size > 1 }

    return DictionaryQualityAudit(
        totalEntries = entries.size,
        exactDuplicateGroups = duplicateGroups,
        missingFrench = details.missingFrenchEntryIds.size,
        missingSaamaka = details.missingSaamakaEntryIds.size,
        longExpressions = details.longExpressionEntryIds.size,
        categoryVariantGroups = categoryVariantGroups,
        affectedEntryIds = details.affectedEntryIds
    )
}
