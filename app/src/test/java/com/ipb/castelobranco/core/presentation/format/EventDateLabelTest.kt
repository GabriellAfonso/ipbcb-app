package com.ipb.castelobranco.core.presentation.format

import org.junit.Assert.assertEquals
import org.junit.Test
import java.time.LocalDate
import java.time.LocalTime

class EventDateLabelTest {

    private val nov14 = LocalDate.of(2026, 11, 14)

    @Test
    fun `one day without time shows day and short month`() {
        assertEquals("14 nov", EventDateLabel.dateLabel(nov14, null, null))
    }

    @Test
    fun `one day with time appends the time`() {
        assertEquals("14 nov · 19:30", EventDateLabel.dateLabel(nov14, null, LocalTime.of(19, 30)))
    }

    @Test
    fun `end equal to start is a one day event`() {
        assertEquals("14 nov", EventDateLabel.dateLabel(nov14, nov14, null))
        assertEquals("Sábado, 14 de novembro", EventDateLabel.fullDateLabel(nov14, nov14))
    }

    @Test
    fun `range in the same month says the month once`() {
        assertEquals("14 a 16 nov", EventDateLabel.dateLabel(nov14, LocalDate.of(2026, 11, 16), null))
        assertEquals("14 a 16 de novembro", EventDateLabel.fullDateLabel(nov14, LocalDate.of(2026, 11, 16)))
    }

    @Test
    fun `range across months names both months`() {
        val start = LocalDate.of(2026, 11, 30)
        val end = LocalDate.of(2026, 12, 2)
        assertEquals("30 nov a 2 dez", EventDateLabel.dateLabel(start, end, null))
        assertEquals("30 de novembro a 2 de dezembro", EventDateLabel.fullDateLabel(start, end))
    }

    @Test
    fun `range across years carries both years`() {
        val start = LocalDate.of(2026, 12, 30)
        val end = LocalDate.of(2027, 1, 2)
        assertEquals("30 dez 2026 a 2 jan 2027", EventDateLabel.dateLabel(start, end, null))
        assertEquals("30 dez 2026 a 2 jan 2027 · 09:05", EventDateLabel.dateLabel(start, end, LocalTime.of(9, 5)))
    }
}
