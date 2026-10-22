package com.ipb.castelobranco.features.worshiphub.tables.data.mapper

import com.ipb.castelobranco.features.worshiphub.tables.data.dto.SongsBySundayDto
import com.ipb.castelobranco.features.worshiphub.tables.data.dto.SundaySongDto
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class SongsBySundayMapperTest {

    @Test
    fun `toDomain maps date and songs correctly`() {
        val dto = SongsBySundayDto(
            date = "04/05/2025",
            songs = listOf(
                SundaySongDto(position = 1, title = "Grandioso és Tu", artist = "Stuart K. Hine", tone = "G"),
                SundaySongDto(position = 2, title = "Castelo Forte", artist = "Martinho Lutero", tone = "D"),
            )
        )

        val result = dto.toDomain()

        assertEquals("04/05/2025", result.date)
        assertEquals(2, result.songs.size)

        assertEquals(1, result.songs[0].position)
        assertEquals("Grandioso és Tu", result.songs[0].title)
        assertEquals("Stuart K. Hine", result.songs[0].artist)
        assertEquals("G", result.songs[0].tone)

        assertEquals(2, result.songs[1].position)
        assertEquals("Castelo Forte", result.songs[1].title)
        assertEquals("D", result.songs[1].tone)
    }

    @Test
    fun `toDomain with empty songs list`() {
        val dto = SongsBySundayDto(date = "11/05/2025", songs = emptyList())

        val result = dto.toDomain()

        assertEquals("11/05/2025", result.date)
        assertTrue(result.songs.isEmpty())
    }

    @Test
    fun `list toDomain maps multiple sundays preserving order`() {
        val dtos = listOf(
            SongsBySundayDto(date = "04/05/2025", songs = listOf(SundaySongDto(1, "A", "X", "G"))),
            SongsBySundayDto(date = "11/05/2025", songs = listOf(SundaySongDto(1, "B", "Y", "D"))),
        )

        val result = dtos.toDomain()

        assertEquals(2, result.size)
        assertEquals("04/05/2025", result[0].date)
        assertEquals("11/05/2025", result[1].date)
    }

    @Test
    fun `list toDomain with empty list returns empty`() {
        val result = emptyList<SongsBySundayDto>().toDomain()

        assertTrue(result.isEmpty())
    }
}
