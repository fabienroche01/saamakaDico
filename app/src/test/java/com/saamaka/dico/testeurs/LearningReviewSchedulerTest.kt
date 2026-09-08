package com.saamaka.dico.testeurs

import com.saamaka.dico.testeurs.model.DictionaryEntry
import org.junit.Assert.assertEquals
import org.junit.Test

class LearningReviewSchedulerTest {
    private fun entry(id: Int) = DictionaryEntry(id, "s$id", "f$id", "", "", "", "")

    @Test fun reviewAgainReturnsTomorrow() {
        assertEquals(86_400_000L, nextLearningReviewAt(0L, mastered = false))
    }

    @Test fun masteredReturnsInThreeDays() {
        assertEquals(259_200_000L, nextLearningReviewAt(0L, mastered = true))
    }

    @Test fun dueReviewHasPriorityOverUnseen() {
        val entries = listOf(entry(1), entry(2), entry(3))
        val selected = selectNextLearningReviewEntry(
            entries, reviewIds = setOf(2), knownIds = emptySet(),
            dueAtById = mapOf(2 to 10L), currentId = 1, now = 20L
        )
        assertEquals(2, selected?.id)
    }

    @Test fun scheduleRoundTrips() {
        val original = mapOf(1 to 100L, 2 to 200L)
        assertEquals(original, parseLearningDueSchedule(serializeLearningDueSchedule(original)))
    }
}
