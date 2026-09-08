package com.saamaka.dico.testeurs

import com.saamaka.dico.testeurs.model.DictionaryEntry

data class DictionaryQualityAudit(
    val totalEntries: Int,
    val exactDuplicateGroups: Int,
    val missingFrench: Int,
    val missingSaamaka: Int,
    val longExpressions: Int,
    val categoryVariantGroups: Int
) {
    val missingFields: Int get() = missingFrench + missingSaamaka
    val issueCount: Int
        get() = exactDuplicateGroups + missingFields + longExpressions + categoryVariantGroups
}

private fun normalizeQualityText(value: String): String =
    value.trim().lowercase().replace(Regex("\\s+"), " ")

fun auditDictionaryEntries(entries: List<DictionaryEntry>): DictionaryQualityAudit {
    val duplicateGroups = entries
        .asSequence()
        .filter { it.saamaka.isNotBlank() && it.french.isNotBlank() }
        .groupBy {
            normalizeQualityText(it.saamaka) + "\u0000" + normalizeQualityText(it.french)
        }
        .values
        .count { it.size > 1 }

    val missingFrench = entries.count { it.french.isBlank() }
    val missingSaamaka = entries.count { it.saamaka.isBlank() }
    val longExpressions = entries.count { entry ->
        val frenchWords = entry.french.trim().split(Regex("\\s+")).count { it.isNotBlank() }
        val saamakaWords = entry.saamaka.trim().split(Regex("\\s+")).count { it.isNotBlank() }
        entry.french.length > 100 || entry.saamaka.length > 100 ||
            frenchWords > 14 || saamakaWords > 14
    }

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
        missingFrench = missingFrench,
        missingSaamaka = missingSaamaka,
        longExpressions = longExpressions,
        categoryVariantGroups = categoryVariantGroups
    )
}
