package com.ipb.castelobranco.features.admin.members.presentation.util

import com.ipb.castelobranco.features.admin.members.domain.model.BirthDate
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class MemberFormattingTest {

    @Test
    fun `birth date reads as far as it is known`() {
        assertEquals("12/03/1990", formatBirth(BirthDate(day = 12, month = 3, year = 1990)))
        assertEquals("12/03", formatBirth(BirthDate(day = 12, month = 3)))
        assertEquals("1990", formatBirth(BirthDate(year = 1990)))
        assertNull(formatBirth(BirthDate.NONE))
    }
}
