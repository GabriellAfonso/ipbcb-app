package com.ipb.castelobranco.features.gallery.presentation.state

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class TrimTimeTest {

    @Test
    fun `reads seconds, minutes and hours`() {
        assertEquals(83_000L, TrimTime.parse("83"))
        assertEquals(83_000L, TrimTime.parse("1:23"))
        assertEquals(3_723_000L, TrimTime.parse("1:02:03"))
        assertEquals(5_000L, TrimTime.parse(" 0:05 "))
    }

    @Test
    fun `reads a decimal with comma or point`() {
        assertEquals(83_500L, TrimTime.parse("83,5"))
        assertEquals(83_250L, TrimTime.parse("1:23.25"))
    }

    @Test
    fun `refuses what is not a time`() {
        listOf("", "abc", "1:", ":30", "1:75", "1:60:00", "-5", "1:2:3:4", "1,5:00").forEach {
            assertNull(it, TrimTime.parse(it))
        }
    }

    @Test
    fun `formats with tenths only when there are any`() {
        assertEquals("1:23", TrimTime.format(83_000))
        assertEquals("1:23,5", TrimTime.format(83_540))
    }
}
