package com.ipb.castelobranco.features.worshiphub.lyrics.data.mapper

import com.ipb.castelobranco.features.worshiphub.lyrics.data.dto.LyricsDto
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class LyricsMapperTest {

    private fun dto(
        id: Int = 1,
        songId: Int = 10,
        content: String = "Verso 1\nRefrão",
        updatedAt: String = "2025-05-01T00:00:00Z"
    ) = LyricsDto(id = id, songId = songId, content = content, updatedAt = updatedAt)

    @Test
    fun `toDomain maps all fields correctly`() {
        val input = dto(id = 5, songId = 42, content = "Grande é o Senhor\nE mui digno de louvor")

        val result = input.toDomain()

        assertEquals(5, result.id)
        assertEquals(42, result.songId)
        assertEquals("Grande é o Senhor\nE mui digno de louvor", result.content)
    }

    @Test
    fun `toDomain drops updatedAt from dto`() {
        val input = dto(updatedAt = "2025-01-01T12:00:00Z")

        val result = input.toDomain()

        // Lyrics domain model has no updatedAt field — only id, songId, content
        assertEquals(1, result.id)
        assertEquals(10, result.songId)
    }

    @Test
    fun `toDomain preserves multiline content`() {
        val multiline = "Linha 1\nLinha 2\nLinha 3\n\nLinha após espaço"
        val input = dto(content = multiline)

        val result = input.toDomain()

        assertEquals(multiline, result.content)
    }

    @Test
    fun `list toDomain maps all items preserving order`() {
        val dtos = listOf(
            dto(id = 1, songId = 10),
            dto(id = 2, songId = 20),
            dto(id = 3, songId = 30),
        )

        val result = dtos.toDomain()

        assertEquals(3, result.size)
        assertEquals(listOf(1, 2, 3), result.map { it.id })
        assertEquals(listOf(10, 20, 30), result.map { it.songId })
    }

    @Test
    fun `list toDomain with empty list returns empty`() {
        val result = emptyList<LyricsDto>().toDomain()

        assertTrue(result.isEmpty())
    }
}
