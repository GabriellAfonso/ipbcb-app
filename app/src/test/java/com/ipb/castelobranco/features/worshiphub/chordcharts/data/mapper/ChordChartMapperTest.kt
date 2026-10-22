package com.ipb.castelobranco.features.worshiphub.chordcharts.data.mapper

import com.ipb.castelobranco.features.worshiphub.chordcharts.data.dto.ChordChartDto
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class ChordChartMapperTest {

    private fun dto(
        id: Int = 1,
        songId: Int = 10,
        content: String = "{title: Test}",
        tone: String = "G",
        instrument: String = "Violão",
        updatedAt: String = "2025-05-01T00:00:00Z"
    ) = ChordChartDto(id = id, songId = songId, content = content, tone = tone, instrument = instrument, updatedAt = updatedAt)

    @Test
    fun `toDomain maps all fields correctly`() {
        val input = dto(id = 5, songId = 42, content = "{title: Firmes}", tone = "A", instrument = "Teclado")

        val result = input.toDomain()

        assertEquals(5, result.id)
        assertEquals(42, result.songId)
        assertEquals("{title: Firmes}", result.content)
        assertEquals("A", result.tone)
        assertEquals("Teclado", result.instrument)
    }

    @Test
    fun `toDomain drops updatedAt from dto`() {
        val input = dto(updatedAt = "2025-01-01T12:00:00Z")

        val result = input.toDomain()

        // ChordChart domain model has no updatedAt field — only id, songId, content, tone, instrument
        assertEquals(1, result.id)
        assertEquals(10, result.songId)
    }

    @Test
    fun `list toDomain maps all items preserving order`() {
        val dtos = listOf(
            dto(id = 1, tone = "G"),
            dto(id = 2, tone = "D"),
            dto(id = 3, tone = "A"),
        )

        val result = dtos.toDomain()

        assertEquals(3, result.size)
        assertEquals(listOf(1, 2, 3), result.map { it.id })
        assertEquals(listOf("G", "D", "A"), result.map { it.tone })
    }

    @Test
    fun `list toDomain with empty list returns empty`() {
        val result = emptyList<ChordChartDto>().toDomain()

        assertTrue(result.isEmpty())
    }
}
