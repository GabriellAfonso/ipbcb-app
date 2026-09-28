package com.ipb.castelobranco.features.admin.members.domain.usecase

import com.ipb.castelobranco.features.admin.members.domain.model.BirthDate
import com.ipb.castelobranco.features.admin.members.fixedDateProvider
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import java.time.LocalDate

class ComputeMemberAgeUseCaseTest {

    private val useCase = ComputeMemberAgeUseCase(fixedDateProvider) // today = 26/09/2026

    @Test
    fun `age counts full years`() {
        assertEquals("36 anos", useCase.ageLabel(BirthDate(2, 4, 1990)))
    }

    @Test
    fun `birthday tomorrow is still one year less`() {
        assertEquals("35 anos", useCase.ageLabel(BirthDate(27, 9, 1990)))
    }

    @Test
    fun `one year is singular`() {
        assertEquals("1 ano", useCase.ageLabel(BirthDate(26, 9, 2025)))
    }

    @Test
    fun `missing or future birth date has no age`() {
        assertNull(useCase.ageLabel(BirthDate.NONE))
        assertNull(useCase.ageLabel(BirthDate(1, 1, 2030)))
    }

    @Test
    fun `day and month only give no age`() {
        assertNull(useCase.ageLabel(BirthDate(day = 2, month = 4)))
    }

    @Test
    fun `year only counts by year`() {
        assertEquals("36 anos", useCase.ageLabel(BirthDate(year = 1990)))
        assertEquals("1 ano", useCase.ageLabel(BirthDate(year = 2025)))
    }

    @Test
    fun `time since baptism`() {
        assertEquals("há 21 anos", useCase.sinceLabel(LocalDate.of(2005, 6, 12)))
        assertEquals("há 1 ano", useCase.sinceLabel(LocalDate.of(2025, 1, 1)))
        assertEquals("este ano", useCase.sinceLabel(LocalDate.of(2026, 2, 1)))
    }

    @Test
    fun `missing or future baptism has no time since`() {
        assertNull(useCase.sinceLabel(null))
        assertNull(useCase.sinceLabel(LocalDate.of(2027, 1, 1)))
    }
}
