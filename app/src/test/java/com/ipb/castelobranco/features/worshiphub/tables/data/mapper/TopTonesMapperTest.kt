package com.ipb.castelobranco.features.worshiphub.tables.data.mapper

import com.ipb.castelobranco.features.worshiphub.tables.data.dto.TopToneDto
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class TopTonesMapperTest {

    @Test
    fun `toDomain maps tone and count correctly`() {
        val dto = TopToneDto(tone = "G", count = 25)

        val result = dto.toDomain()

        assertEquals("G", result.tone)
        assertEquals(25, result.count)
    }

    @Test
    fun `toDomain with zero count`() {
        val dto = TopToneDto(tone = "C#m", count = 0)

        val result = dto.toDomain()

        assertEquals("C#m", result.tone)
        assertEquals(0, result.count)
    }

    @Test
    fun `list toDomain maps all items preserving order`() {
        val dtos = listOf(
            TopToneDto(tone = "G", count = 25),
            TopToneDto(tone = "D", count = 18),
            TopToneDto(tone = "A", count = 12),
        )

        val result = dtos.toDomain()

        assertEquals(3, result.size)
        assertEquals(listOf("G", "D", "A"), result.map { it.tone })
        assertEquals(listOf(25, 18, 12), result.map { it.count })
    }

    @Test
    fun `list toDomain with empty list returns empty`() {
        val result = emptyList<TopToneDto>().toDomain()

        assertTrue(result.isEmpty())
    }
}
