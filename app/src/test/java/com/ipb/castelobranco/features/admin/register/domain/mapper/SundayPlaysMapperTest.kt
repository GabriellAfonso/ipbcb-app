package com.ipb.castelobranco.features.admin.register.domain.mapper

import com.ipb.castelobranco.features.admin.register.presentation.state.SundaySongRowState
import com.ipb.castelobranco.core.domain.model.Song
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDate

class SundayPlaysMapperTest {

    private fun song(id: Int) = Song(id = id, title = "Song $id", artist = "", categoryName = "")

    private fun row(
        position: Int,
        songQuery: String = "",
        selectedSongId: Int? = null,
        tone: String = ""
    ) = SundaySongRowState(position = position, songQuery = songQuery, selectedSongId = selectedSongId, tone = tone)

    // region toSundayPlayItems

    @Test
    fun `complete row with existing song produces item`() {
        val songs = listOf(song(1))
        val rows = listOf(row(1, selectedSongId = 1, tone = "C"))

        val result = toSundayPlayItems(rows, songs)

        assertEquals(1, result.size)
        assertEquals(1, result[0].songId)
        assertEquals(1, result[0].position)
        assertEquals("C", result[0].tone)
    }

    @Test
    fun `empty row is ignored`() {
        val rows = listOf(row(1))

        val result = toSundayPlayItems(rows, emptyList())

        assertTrue(result.isEmpty())
    }

    @Test
    fun `row with selectedSongId not in availableSongs is ignored`() {
        val songs = listOf(song(1))
        val rows = listOf(row(1, selectedSongId = 99, tone = "C"))

        val result = toSundayPlayItems(rows, songs)

        assertTrue(result.isEmpty())
    }

    @Test
    fun `row with only songQuery and no selectedSongId is ignored`() {
        val songs = listOf(song(1))
        val rows = listOf(row(1, songQuery = "Some Song", tone = "C"))

        val result = toSundayPlayItems(rows, songs)

        assertTrue(result.isEmpty())
    }

    @Test
    fun `tone with surrounding spaces is trimmed in result`() {
        val songs = listOf(song(1))
        val rows = listOf(row(1, selectedSongId = 1, tone = "  Am  "))

        val result = toSundayPlayItems(rows, songs)

        assertEquals("Am", result[0].tone)
    }

    @Test
    fun `multiple valid rows produce all items`() {
        val songs = listOf(song(1), song(2), song(3))
        val rows = listOf(
            row(1, selectedSongId = 1, tone = "C"),
            row(2, selectedSongId = 2, tone = "G"),
            row(3, selectedSongId = 3, tone = "Am"),
        )

        val result = toSundayPlayItems(rows, songs)

        assertEquals(3, result.size)
    }

    @Test
    fun `empty availableSongs filters all rows`() {
        val rows = listOf(
            row(1, selectedSongId = 1, tone = "C"),
            row(2, selectedSongId = 2, tone = "G"),
        )

        val result = toSundayPlayItems(rows, emptyList())

        assertTrue(result.isEmpty())
    }

    // endregion

    // region dateIso

    @Test
    fun `dateIso formats LocalDate as YYYY-MM-DD`() {
        val date = LocalDate.of(2025, 6, 15)

        assertEquals("2025-06-15", dateIso(date))
    }

    @Test
    fun `dateIso with null returns empty string`() {
        assertEquals("", dateIso(null))
    }

    @Test
    fun `dateIso formats year-end date correctly`() {
        assertEquals("2025-12-31", dateIso(LocalDate.of(2025, 12, 31)))
    }

    @Test
    fun `dateIso formats new year date correctly`() {
        assertEquals("2026-01-01", dateIso(LocalDate.of(2026, 1, 1)))
    }

    // endregion
}
