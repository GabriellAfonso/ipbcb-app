package com.ipb.castelobranco.features.worshiphub.tables.domain.usecase

import com.ipb.castelobranco.features.worshiphub.tables.domain.model.DraftRow
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class RepertoireValidationTest {

    private fun row(position: Int, songId: Int?, tone: String) = DraftRow(position, songId, tone, false)

    @Test
    fun `filled rows with keys can be saved, empty rows ignored`() {
        assertTrue(RepertoireValidation.canSave(listOf(row(1, 12, "G"), row(2, null, ""), row(3, 55, " Bbm "))))
    }

    @Test
    fun `no filled row cannot be saved`() {
        assertFalse(RepertoireValidation.canSave(listOf(row(1, null, ""), row(2, null, "G"))))
    }

    @Test
    fun `a filled row without key cannot be saved`() {
        assertFalse(RepertoireValidation.canSave(listOf(row(1, 12, "G"), row(2, 55, "  "))))
    }

    @Test
    fun `a key longer than three characters cannot be saved`() {
        assertFalse(RepertoireValidation.canSave(listOf(row(1, 12, "Gmaj7"))))
    }
}
