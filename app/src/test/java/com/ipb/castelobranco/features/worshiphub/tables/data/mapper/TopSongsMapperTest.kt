package com.ipb.castelobranco.features.worshiphub.tables.data.mapper

import com.ipb.castelobranco.features.worshiphub.tables.data.dto.TopSongDto
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class TopSongsMapperTest {

    @Test
    fun `toDomain maps title and playCount correctly`() {
        val dto = TopSongDto(title = "Firmes na Fé", playCount = 12)

        val result = dto.toDomain()

        assertEquals("Firmes na Fé", result.title)
        assertEquals(12, result.playCount)
    }

    @Test
    fun `toDomain with zero playCount`() {
        val dto = TopSongDto(title = "Nova Música", playCount = 0)

        val result = dto.toDomain()

        assertEquals(0, result.playCount)
    }

    @Test
    fun `list toDomain maps all items preserving order`() {
        val dtos = listOf(
            TopSongDto(title = "A", playCount = 10),
            TopSongDto(title = "B", playCount = 5),
            TopSongDto(title = "C", playCount = 8),
        )

        val result = dtos.toDomain()

        assertEquals(3, result.size)
        assertEquals(listOf("A", "B", "C"), result.map { it.title })
        assertEquals(listOf(10, 5, 8), result.map { it.playCount })
    }

    @Test
    fun `list toDomain with empty list returns empty`() {
        val result = emptyList<TopSongDto>().toDomain()

        assertTrue(result.isEmpty())
    }
}
