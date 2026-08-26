package com.saamaka.dico.testeurs

import com.saamaka.dico.testeurs.model.DictionaryEntry
import java.text.Normalizer
import java.util.Locale

internal enum class MissionFilter(val label: String) {
    ALL("Tous"),
    DOUBTFUL("Douteux"),
    TO_COMPLETE("À compléter"),
    COMPLETED("Terminés")
}

internal enum class MissionVisualStatus(
    val label: String,
    val isDoubtful: Boolean = false,
    val isToComplete: Boolean = false
) {
    DOUBTFUL("⚠️ Douteux", isDoubtful = true),
    TO_COMPLETE("❓ Traduction manquante", isToComplete = true),
    NEW("⬜ Nouveau"),
    ALREADY_VALIDATED("✅ Déjà validé")
}

internal data class MissionEntryClassification(
    val visualStatus: MissionVisualStatus,
    val isCompleted: Boolean
) {
    fun matches(filter: MissionFilter): Boolean = when (filter) {
        MissionFilter.ALL -> true
        MissionFilter.DOUBTFUL -> visualStatus.isDoubtful
        MissionFilter.TO_COMPLETE -> visualStatus.isToComplete
        MissionFilter.COMPLETED -> isCompleted
    }
}

internal fun classifyMissionEntry(
    entry: DictionaryEntry,
    hasLocalValidation: Boolean,
    hasLocalCorrection: Boolean
): MissionEntryClassification {
    val visualStatus = when {
        entry.valide.trim().equals("D", ignoreCase = true) -> MissionVisualStatus.DOUBTFUL
        entry.saamaka.isBlank() -> MissionVisualStatus.TO_COMPLETE
        entry.valide.isBlank() -> MissionVisualStatus.NEW
        else -> MissionVisualStatus.ALREADY_VALIDATED
    }

    return MissionEntryClassification(
        visualStatus = visualStatus,
        isCompleted = hasLocalValidation || hasLocalCorrection
    )
}

internal fun classifyMissionEntries(
    entries: List<DictionaryEntry>,
    completedIds: Set<Int>,
    correctedIds: Set<Int>
): Map<Int, MissionEntryClassification> = entries.associate { entry ->
    entry.id to classifyMissionEntry(
        entry = entry,
        hasLocalValidation = entry.id in completedIds,
        hasLocalCorrection = entry.id in correctedIds
    )
}

internal fun filterMissionEntries(
    entries: List<DictionaryEntry>,
    filter: MissionFilter,
    query: String,
    classifications: Map<Int, MissionEntryClassification>,
    localCorrections: Map<Int, CorrectionProposal> = emptyMap()
): List<DictionaryEntry> {
    val normalizedQuery = normalizeMissionSearch(query)

    return entries.filter { entry ->
        val classification = classifications.getValue(entry.id)
        val correction = localCorrections[entry.id]
        val matchesQuery = normalizedQuery.isBlank() || listOf(
            entry.french,
            entry.saamaka,
            entry.english,
            entry.dutch,
            correction?.frenchProposed.orEmpty(),
            correction?.saamakaProposed.orEmpty()
        ).any { normalizeMissionSearch(it).contains(normalizedQuery) }

        classification.matches(filter) && matchesQuery
    }
}

internal fun normalizeMissionSearch(value: String): String =
    Normalizer.normalize(value.trim(), Normalizer.Form.NFD)
        .replace("\\p{Mn}+".toRegex(), "")
        .lowercase(Locale.ROOT)
