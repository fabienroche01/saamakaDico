package com.saamaka.dico.testeurs

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class TesterWorkflowStateTest {
    @Test
    fun `deletion outranks correction and validation`() {
        assertEquals(
            TesterEntryStatus.DELETION_PROPOSED,
            testerEntryStatus(true, true, true, true, true)
        )
    }

    @Test
    fun `correction outranks validation`() {
        assertEquals(
            TesterEntryStatus.CORRECTED,
            testerEntryStatus(true, true, true, false, false)
        )
    }

    @Test
    fun `local validation is validated`() {
        assertEquals(
            TesterEntryStatus.VALIDATED,
            testerEntryStatus(false, true, false, false, false)
        )
    }

    @Test
    fun `review state is kept for untouched doubtful entry`() {
        assertEquals(
            TesterEntryStatus.TO_REVIEW,
            testerEntryStatus(false, false, false, false, true)
        )
    }

    @Test
    fun `adjacent navigation never wraps`() {
        val ids = listOf(10, 20, 30)
        assertEquals(10, adjacentEntryId(ids, 20, -1))
        assertEquals(30, adjacentEntryId(ids, 20, 1))
        assertNull(adjacentEntryId(ids, 10, -1))
        assertNull(adjacentEntryId(ids, 30, 1))
    }

    @Test
    fun `treated words use unique dictionary ids`() {
        val stats = testerProgressStats(
            validatedIds = setOf(1, 2),
            correctedIds = setOf(2, 3),
            deletionIds = setOf(3, 4),
            newEntryCount = 2
        )
        assertEquals(4, stats.treatedWordCount)
        assertEquals(2, stats.validationCount)
        assertEquals(2, stats.correctionCount)
        assertEquals(2, stats.deletionCount)
        assertEquals(2, stats.newEntryCount)
    }
}
