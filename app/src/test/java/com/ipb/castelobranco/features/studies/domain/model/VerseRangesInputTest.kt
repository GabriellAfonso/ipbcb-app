package com.ipb.castelobranco.features.studies.domain.model

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class VerseRangesInputTest {

    @Test
    fun `parses ranges and single verses`() {
        assertEquals(listOf(VerseSpan(1, 3), VerseSpan(5, 5)), VerseRangesInput.parse(" 1-3, 5 "))
    }

    @Test
    fun `blank is the whole chapter`() {
        assertEquals(emptyList<VerseSpan>(), VerseRangesInput.parse(""))
    }

    @Test
    fun `invalid input is rejected`() {
        assertNull(VerseRangesInput.parse("3-1"))
        assertNull(VerseRangesInput.parse("a"))
        assertNull(VerseRangesInput.parse("1-2-3"))
        assertNull(VerseRangesInput.parse("0"))
    }

    @Test
    fun `format is the inverse of parse`() {
        assertEquals("1-3,5", VerseRangesInput.format(listOf(VerseSpan(1, 3), VerseSpan(5, 5))))
    }
}
