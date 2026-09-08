package com.saamaka.dico.testeurs

internal enum class TesterEntryStatus {
    UNTOUCHED,
    VALIDATED,
    CORRECTED,
    DELETION_PROPOSED,
    TO_REVIEW
}

internal data class TesterProgressStats(
    val treatedEntryIds: Set<Int>,
    val validationCount: Int,
    val correctionCount: Int,
    val deletionCount: Int,
    val newEntryCount: Int
) {
    val treatedWordCount: Int get() = treatedEntryIds.size
}

internal fun testerEntryStatus(
    databaseValidated: Boolean,
    locallyValidated: Boolean,
    corrected: Boolean,
    deletionProposed: Boolean,
    toReview: Boolean
): TesterEntryStatus = when {
    deletionProposed -> TesterEntryStatus.DELETION_PROPOSED
    corrected -> TesterEntryStatus.CORRECTED
    databaseValidated || locallyValidated -> TesterEntryStatus.VALIDATED
    toReview -> TesterEntryStatus.TO_REVIEW
    else -> TesterEntryStatus.UNTOUCHED
}

internal fun adjacentEntryId(
    entryIds: List<Int>,
    currentId: Int,
    offset: Int
): Int? {
    if (offset == 0) return currentId.takeIf { it in entryIds }
    val currentIndex = entryIds.indexOf(currentId)
    if (currentIndex < 0) return null
    return entryIds.getOrNull(currentIndex + offset)
}

internal fun testerProgressStats(
    validatedIds: Set<Int>,
    correctedIds: Set<Int>,
    deletionIds: Set<Int>,
    newEntryCount: Int
): TesterProgressStats = TesterProgressStats(
    treatedEntryIds = validatedIds + correctedIds + deletionIds,
    validationCount = validatedIds.size,
    correctionCount = correctedIds.size,
    deletionCount = deletionIds.size,
    newEntryCount = newEntryCount.coerceAtLeast(0)
)

internal fun classificationNeedsReview(classification: MissionEntryClassification): Boolean =
    classification.visualStatus == MissionVisualStatus.DOUBTFUL ||
        classification.visualStatus == MissionVisualStatus.TO_COMPLETE
