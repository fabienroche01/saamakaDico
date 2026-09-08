package com.saamaka.dico.testeurs

import org.junit.Assert.assertEquals
import org.junit.Test

class TesterWorkflowPerformanceTest {
    @Test
    fun `adjacent lookup remains stable for large navigation context`() {
        val ids = (1..10_000).toList()
        assertEquals(5001, adjacentEntryId(ids, 5000, 1))
        assertEquals(4999, adjacentEntryId(ids, 5000, -1))
    }
}
