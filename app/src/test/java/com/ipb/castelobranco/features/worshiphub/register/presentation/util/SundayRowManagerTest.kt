package com.ipb.castelobranco.features.worshiphub.register.presentation.util

import com.ipb.castelobranco.features.worshiphub.register.presentation.state.SundaySongRowState
import com.ipb.castelobranco.core.domain.model.Song
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotSame
import org.junit.Test

class SundayRowManagerTest {

    private fun song(id: Int, title: String, artist: String = "") =
        Song(id = id, title = title, artist = artist, categoryName = "")

    private fun row(position: Int, songQuery: String = "", selectedSongId: Int? = null, tone: String = "") =
        SundaySongRowState(position = position, songQuery = songQuery, selectedSongId = selectedSongId, tone = tone)

    // region selectSong

    @Test
    fun `selectSong updates only the matching row`() {
        val rows = listOf(row(1), row(2), row(3))
        val song = song(id = 10, title = "Hallelujah", artist = "Cohen")

        val result = selectSong(rows, position = 2, song = song)

        assertEquals(3, result.size)
        assertEquals("Hallelujah [Cohen]", result[1].songQuery)
        assertEquals(10, result[1].selectedSongId)
        assertEquals(row(1), result[0])
        assertEquals(row(3), result[2])
    }

    @Test
    fun `selectSong sets songQuery using SongLabelFormatter`() {
        val rows = listOf(row(1))
        val song = song(id = 5, title = "Amazing Grace", artist = "")

        val result = selectSong(rows, position = 1, song = song)

        assertEquals("Amazing Grace", result[0].songQuery)
    }

    @Test
    fun `selectSong with non-existent position leaves all rows unchanged`() {
        val rows = listOf(row(1), row(2))
        val song = song(id = 1, title = "Song")

        val result = selectSong(rows, position = 99, song = song)

        assertEquals(rows, result)
    }

    // endregion

    // region updateTone

    @Test
    fun `updateTone updates only the matching row`() {
        val rows = listOf(row(1), row(2), row(3))

        val result = updateTone(rows, position = 2, tone = "Am")

        assertEquals("Am", result[1].tone)
        assertEquals("", result[0].tone)
        assertEquals("", result[2].tone)
    }

    @Test
    fun `updateTone with non-existent position leaves all rows unchanged`() {
        val rows = listOf(row(1, tone = "C"), row(2, tone = "G"))

        val result = updateTone(rows, position = 99, tone = "D")

        assertEquals(rows, result)
    }

    // endregion

    // region addRow

    @Test
    fun `addRow appends row with position maxPosition plus 1`() {
        val rows = listOf(row(1), row(2), row(3))

        val result = addRow(rows)

        assertEquals(4, result.size)
        assertEquals(4, result.last().position)
    }

    @Test
    fun `addRow on empty list creates row with position 1`() {
        val result = addRow(emptyList())

        assertEquals(1, result.size)
        assertEquals(1, result.first().position)
    }

    @Test
    fun `addRow does not mutate original list`() {
        val rows = listOf(row(1), row(2))

        val result = addRow(rows)

        assertNotSame(rows, result)
        assertEquals(2, rows.size)
    }

    // endregion

    // region removeRow

    @Test
    fun `removeRow with position greater than 4 removes the row`() {
        val rows = listOf(row(1), row(2), row(3), row(4), row(5))

        val result = removeRow(rows, position = 5)

        assertEquals(4, result.size)
        assertEquals(false, result.any { it.position == 5 })
    }

    @Test
    fun `removeRow with position 4 leaves list unchanged`() {
        val rows = listOf(row(1), row(2), row(3), row(4))

        val result = removeRow(rows, position = 4)

        assertEquals(rows, result)
    }

    @Test
    fun `removeRow with position less than 4 leaves list unchanged`() {
        val rows = listOf(row(1), row(2), row(3))

        val result = removeRow(rows, position = 2)

        assertEquals(rows, result)
    }

    @Test
    fun `removeRow with non-existent position leaves list unchanged`() {
        val rows = listOf(row(5), row(6))

        val result = removeRow(rows, position = 99)

        assertEquals(rows, result)
    }

    // endregion
}
