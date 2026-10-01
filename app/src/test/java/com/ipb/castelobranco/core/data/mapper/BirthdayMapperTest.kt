package com.ipb.castelobranco.core.data.mapper

import com.ipb.castelobranco.core.data.dto.BirthdayDto
import com.ipb.castelobranco.core.data.dto.BirthdaysResponseDto
import com.ipb.castelobranco.core.domain.model.Gender
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class BirthdayMapperTest {

    @Test
    fun `maps month and orders by month then day`() {
        val dto = BirthdaysResponseDto(
            listOf(
                BirthdayDto(name = "Carlos", gender = null, birthMonth = 7, birthDay = 2),
                BirthdayDto(name = "Bob", gender = "M", birthMonth = 1, birthDay = 20),
                BirthdayDto(name = "Alice", gender = "F", birthMonth = 1, birthDay = 5),
            )
        )

        val result = dto.toDomain()

        assertEquals(listOf("Alice", "Bob", "Carlos"), result.map { it.name })
        assertEquals(listOf(1, 1, 7), result.map { it.month })
        assertEquals(listOf(Gender.FEMALE, Gender.MALE, Gender.UNKNOWN), result.map { it.gender })
    }

    @Test
    fun `drops entries with blank names`() {
        val dto = BirthdaysResponseDto(listOf(BirthdayDto(name = " ", birthMonth = 3, birthDay = 1)))

        assertTrue(dto.toDomain().isEmpty())
    }
}
