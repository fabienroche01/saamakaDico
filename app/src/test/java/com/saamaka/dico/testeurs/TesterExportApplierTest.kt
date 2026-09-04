package com.saamaka.dico.testeurs

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class TesterExportApplierTest {
    @Test
    fun `rejects two approved values for the same entry field`() {
        val approval = TesterChangeApproval(
            corrections = listOf(
                ApprovedTesterCorrection(42, "french", "bonjour"),
                ApprovedTesterCorrection(42, "francais", "salut")
            )
        )

        val errors = TesterExportApplier.validateApproval(approval)

        assertEquals(1, errors.size)
        assertTrue(errors.single().contains("entryId=42"))
    }

    @Test
    fun `allows identical approved corrections from multiple sources`() {
        val approval = TesterChangeApproval(
            corrections = listOf(
                ApprovedTesterCorrection(42, "saamaka", "odi"),
                ApprovedTesterCorrection(42, "saamaka", "odi")
            )
        )

        assertTrue(TesterExportApplier.validateApproval(approval).isEmpty())
    }

    @Test
    fun `rejects different new entries sharing one local id`() {
        val approval = TesterChangeApproval(
            newEntries = listOf(
                ApprovedTesterNewEntry("local-1", "odi", "bonjour", "", "", "salutation"),
                ApprovedTesterNewEntry("local-1", "odii", "bonjour", "", "", "salutation")
            )
        )

        val errors = TesterExportApplier.validateApproval(approval)

        assertEquals(listOf("Multiple approved new entries use localId=local-1"), errors)
    }
}
