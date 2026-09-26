package com.ipb.castelobranco.features.admin.members.domain.usecase

import com.ipb.castelobranco.features.admin.members.fixedDateProvider
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import java.time.LocalDate

class ComputeMemberAgeUseCaseTest {

    private val useCase = ComputeMemberAgeUseCase(fixedDateProvider) // today = 26/09/2026

    @Test
    fun `age counts full years`() {
        assertEquals("36 anos", useCase.ageLabel(LocalDate.of(1990, 4, 2)))
    }

    @Test
    fun `birthday tomorrow is still one year less`() {
        assertEquals("35 anos", useCase.ageLabel(LocalDate.of(1990, 9, 27)))
    }

    @Test
    fun `one year is singular`() {
        assertEquals("1 ano", useCase.ageLabel(LocalDate.of(2025, 9, 26)))
    }

    @Test
    fun `missing or future birth date has no age`() {
        assertNull(useCase.ageLabel(null))
        assertNull(useCase.ageLabel(LocalDate.of(2030, 1, 1)))
    }

    @Test
    fun `birth year 0001 means the year is unknown and gives no age`() {
        assertNull(useCase.ageLabel(LocalDate.of(1, 4, 2)))
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
