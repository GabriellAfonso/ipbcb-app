package com.ipb.castelobranco.features.worshiphub.tables.data.mapper

import com.ipb.castelobranco.features.worshiphub.tables.data.dto.SuggestedSongDto
import com.ipb.castelobranco.features.worshiphub.tables.data.dto.SuggestedSongInnerDto
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class SuggestedSongsMapperTest {

    private fun dto(id: Int, songId: Int, title: String, artist: String, date: String, tone: String, position: Int) =
        SuggestedSongDto(
            id = id,
            song = SuggestedSongInnerDto(id = songId, title = title, artist = artist),
            date = date,
            tone = tone,
            position = position
        )

    @Test
    fun `toDomain maps all fields including nested song`() {
        val input = dto(
            id = 10, songId = 42, title = "Firmes na Fé", artist = "Paulo César",
            date = "04/05/2025", tone = "A", position = 1
        )

        val result = input.toDomain()

        assertEquals(10, result.id)
        assertEquals(42, result.songId)
        assertEquals("Firmes na Fé", result.title)
        assertEquals("Paulo César", result.artist)
        assertEquals("04/05/2025", result.date)
        assertEquals("A", result.tone)
        assertEquals(1, result.position)
    }

    @Test
    fun `list toDomain sorts by position ascending`() {
        val dtos = listOf(
            dto(1, 10, "C", "X", "04/05/2025", "G", position = 3),
            dto(2, 20, "A", "Y", "04/05/2025", "D", position = 1),
            dto(3, 30, "B", "Z", "04/05/2025", "A", position = 2),
        )

        val result = dtos.toDomain()

        assertEquals(listOf(1, 2, 3), result.map { it.position })
        assertEquals(listOf("A", "B", "C"), result.map { it.title })
    }

    @Test
    fun `list toDomain with already sorted input preserves order`() {
        val dtos = listOf(
            dto(1, 10, "A", "X", "04/05/2025", "G", position = 1),
            dto(2, 20, "B", "Y", "04/05/2025", "D", position = 2),
        )

        val result = dtos.toDomain()

        assertEquals(listOf(1, 2), result.map { it.position })
    }

    @Test
    fun `list toDomain with empty list returns empty`() {
        val result = emptyList<SuggestedSongDto>().toDomain()

        assertTrue(result.isEmpty())
    }

    @Test
    fun `list toDomain with single item returns single item`() {
        val dtos = listOf(
            dto(1, 10, "Única", "Artista", "04/05/2025", "E", position = 1)
        )

        val result = dtos.toDomain()

        assertEquals(1, result.size)
        assertEquals("Única", result[0].title)
    }
}
