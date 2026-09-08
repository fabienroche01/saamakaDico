package com.saamaka.dico.testeurs

import com.saamaka.dico.testeurs.model.DictionaryEntry

private const val DAY_MS = 24L * 60L * 60L * 1000L

fun nextLearningReviewAt(now: Long, mastered: Boolean): Long =
    now + if (mastered) 3L * DAY_MS else DAY_MS

fun selectNextLearningReviewEntry(
    entries: List<DictionaryEntry>,
    reviewIds: Set<Int>,
    knownIds: Set<Int>,
    dueAtById: Map<Int, Long>,
    currentId: Int?,
    now: Long
): DictionaryEntry? {
    val candidates = entries.filter { it.id != currentId }
    if (candidates.isEmpty()) return null

    val dueReview = candidates
        .filter { it.id in reviewIds && (dueAtById[it.id] ?: 0L) <= now }
        .minByOrNull { dueAtById[it.id] ?: 0L }
    if (dueReview != null) return dueReview

    val unseen = candidates.firstOrNull { it.id !in reviewIds && it.id !in knownIds }
    if (unseen != null) return unseen

    val dueKnown = candidates
        .filter { it.id in knownIds && (dueAtById[it.id] ?: Long.MAX_VALUE) <= now }
        .minByOrNull { dueAtById[it.id] ?: Long.MAX_VALUE }
    if (dueKnown != null) return dueKnown

    return candidates
        .filter { it.id in reviewIds }
        .minByOrNull { dueAtById[it.id] ?: Long.MAX_VALUE }
        ?: candidates.firstOrNull()
}

fun parseLearningDueSchedule(values: Set<String>): Map<Int, Long> =
    values.mapNotNull { raw ->
        val split = raw.split(':', limit = 2)
        val id = split.getOrNull(0)?.toIntOrNull() ?: return@mapNotNull null
        val dueAt = split.getOrNull(1)?.toLongOrNull() ?: return@mapNotNull null
        id to dueAt
    }.toMap()

fun serializeLearningDueSchedule(values: Map<Int, Long>): Set<String> =
    values.mapTo(linkedSetOf()) { (id, dueAt) -> "$id:$dueAt" }
