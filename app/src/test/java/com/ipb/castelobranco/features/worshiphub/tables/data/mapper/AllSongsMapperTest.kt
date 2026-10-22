package com.ipb.castelobranco.features.worshiphub.tables.data.mapper

import com.ipb.castelobranco.features.worshiphub.tables.data.dto.AllSongDto
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class AllSongsMapperTest {

    @Test
    fun `toDomain maps all fields correctly`() {
        val dto = AllSongDto(id = 1, title = "Grande é o Senhor", artist = "Adhemar de Campos", categoryName = "Louvor")

        val result = dto.toDomain()

        assertEquals(1, result.id)
        assertEquals("Grande é o Senhor", result.title)
        assertEquals("Adhemar de Campos", result.artist)
        assertEquals("Louvor", result.categoryName)
    }

    @Test
    fun `toDomain uses default empty string for categoryName`() {
        val dto = AllSongDto(id = 2, title = "Título", artist = "Artista")

        val result = dto.toDomain()

        assertEquals("", result.categoryName)
    }

    @Test
    fun `list toDomain maps all items preserving order`() {
        val dtos = listOf(
            AllSongDto(id = 1, title = "A", artist = "X", categoryName = "Cat1"),
            AllSongDto(id = 2, title = "B", artist = "Y", categoryName = "Cat2"),
            AllSongDto(id = 3, title = "C", artist = "Z", categoryName = "Cat3"),
        )

        val result = dtos.toDomain()

        assertEquals(3, result.size)
        assertEquals(listOf(1, 2, 3), result.map { it.id })
        assertEquals(listOf("A", "B", "C"), result.map { it.title })
    }

    @Test
    fun `list toDomain with empty list returns empty`() {
        val result = emptyList<AllSongDto>().toDomain()

        assertTrue(result.isEmpty())
    }
}
