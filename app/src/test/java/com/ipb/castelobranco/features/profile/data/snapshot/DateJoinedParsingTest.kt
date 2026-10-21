package com.ipb.castelobranco.features.profile.data.snapshot

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import java.time.LocalDate

class DateJoinedParsingTest {

    @Test
    fun `parses an ISO date`() {
        assertEquals(LocalDate.of(2025, 3, 14), "2025-03-14".toLocalDateOrNull())
    }

    @Test
    fun `an absent or malformed date is unknown`() {
        assertNull(null.toLocalDateOrNull())
        assertNull("14/03/2025".toLocalDateOrNull())
    }
}
