package com.ipb.castelobranco.features.events.presentation.state

import com.ipb.castelobranco.features.events.testing.event
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.Instant
import java.time.LocalDate
import java.time.LocalTime

class EventCalendarEntryTest {

    private val day = LocalDate.of(2026, 11, 14)

    private fun millis(instant: String) = Instant.parse(instant).toEpochMilli()

    @Test
    fun `a one-day event with a time starts at Brasilia time and leaves the end open`() {
        val entry = event(1, start = day, time = LocalTime.of(19, 30)).toCalendarEntry()

        assertEquals(millis("2026-11-14T22:30:00Z"), entry.beginMillis)
        assertNull(entry.endMillis)
        assertFalse(entry.isAllDay)
    }

    @Test
    fun `an event without a time is all day, at UTC midnights with an exclusive end`() {
        val entry = event(1, start = day, end = day.plusDays(2)).toCalendarEntry()

        assertEquals(millis("2026-11-14T00:00:00Z"), entry.beginMillis)
        assertEquals(millis("2026-11-17T00:00:00Z"), entry.endMillis)
        assertTrue(entry.isAllDay)
    }

    @Test
    fun `a longer event with a time runs until the end of its last day`() {
        val entry = event(1, start = day, end = day.plusDays(2), time = LocalTime.of(19, 30)).toCalendarEntry()

        assertEquals(millis("2026-11-14T22:30:00Z"), entry.beginMillis)
        assertEquals(millis("2026-11-17T02:59:00Z"), entry.endMillis)
        assertFalse(entry.isAllDay)
    }

    @Test
    fun `an end date equal to the start is a one-day event`() {
        val entry = event(1, start = day, end = day, time = LocalTime.of(8, 0)).toCalendarEntry()

        assertNull(entry.endMillis)
    }

    @Test
    fun `the text fields go along`() {
        val entry = event(1, start = day, title = "Almoço", location = "Salão", description = "Traga um prato")
            .toCalendarEntry()

        assertEquals("Almoço", entry.title)
        assertEquals("Salão", entry.location)
        assertEquals("Traga um prato", entry.description)
    }
}
