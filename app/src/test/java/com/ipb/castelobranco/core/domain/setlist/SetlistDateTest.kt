package com.ipb.castelobranco.core.domain.setlist

import org.junit.Assert.assertEquals
import org.junit.Test
import java.time.LocalDate

class SetlistDateTest {

    private val sunday = LocalDate.of(2026, 10, 4)

    @Test
    fun `on a Sunday the setlist is for today`() {
        assertEquals(sunday, setlistDateFor(sunday))
    }

    @Test
    fun `any other day of the week points to the next Sunday`() {
        (1L..6L).forEach { daysBefore ->
            assertEquals(sunday, setlistDateFor(sunday.minusDays(daysBefore)))
        }
    }

    @Test
    fun `Monday after a Sunday points to the following Sunday`() {
        assertEquals(sunday.plusWeeks(1), setlistDateFor(sunday.plusDays(1)))
    }
}
