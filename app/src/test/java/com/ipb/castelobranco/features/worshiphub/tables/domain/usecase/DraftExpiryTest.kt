package com.ipb.castelobranco.features.worshiphub.tables.domain.usecase

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class DraftExpiryTest {

    private val changedAt = 1_000_000L

    @Test
    fun `less than an hour later the draft is alive`() {
        assertFalse(DraftExpiry.isExpired(changedAt, changedAt + DraftExpiry.DRAFT_TTL_MS - 1))
    }

    @Test
    fun `an hour later the draft is expired`() {
        assertTrue(DraftExpiry.isExpired(changedAt, changedAt + DraftExpiry.DRAFT_TTL_MS))
    }

    @Test
    fun `a clock moved back never expires it`() {
        assertFalse(DraftExpiry.isExpired(changedAt, changedAt - 10 * DraftExpiry.DRAFT_TTL_MS))
    }
}
