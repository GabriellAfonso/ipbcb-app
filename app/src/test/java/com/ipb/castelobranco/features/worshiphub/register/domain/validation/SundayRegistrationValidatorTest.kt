package com.ipb.castelobranco.features.worshiphub.register.domain.validation

import com.ipb.castelobranco.features.worshiphub.register.presentation.state.SundaySongRowState
import com.ipb.castelobranco.core.domain.model.Song
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class SundayRegistrationValidatorTest {

    private val emptySongs = emptyList<Song>()

    private fun row(
        position: Int,
        songQuery: String = "",
        selectedSongId: Int? = null,
        tone: String = ""
    ) = SundaySongRowState(position = position, songQuery = songQuery, selectedSongId = selectedSongId, tone = tone)

    // region complete valid row

    @Test
    fun `complete row with songQuery and tone is valid`() {
        val rows = listOf(row(1, songQuery = "Hallelujah", tone = "C"))

        val result = SundayRegistrationValidator.validateSundayRows(rows, emptySongs)

        assertTrue(result.hasAtLeastOneCompleteValidRow)
        assertTrue(result.errorsByPosition.isEmpty())
        assertTrue(result.incompletePositions.isEmpty())
    }

    @Test
    fun `complete row with selectedSongId and tone is valid`() {
        val rows = listOf(row(1, selectedSongId = 42, tone = "G"))

        val result = SundayRegistrationValidator.validateSundayRows(rows, emptySongs)

        assertTrue(result.hasAtLeastOneCompleteValidRow)
        assertTrue(result.errorsByPosition.isEmpty())
    }

    // endregion

    // region empty row

    @Test
    fun `fully empty row is ignored`() {
        val rows = listOf(row(1))

        val result = SundayRegistrationValidator.validateSundayRows(rows, emptySongs)

        assertFalse(result.hasAtLeastOneCompleteValidRow)
        assertTrue(result.errorsByPosition.isEmpty())
        assertTrue(result.incompletePositions.isEmpty())
    }

    // endregion

    // region incomplete rows

    @Test
    fun `row with songQuery but no tone generates error`() {
        val rows = listOf(row(1, songQuery = "Hallelujah"))

        val result = SundayRegistrationValidator.validateSundayRows(rows, emptySongs)

        assertTrue(result.incompletePositions.contains(1))
        assertTrue(result.errorsByPosition.containsKey(1))
        assertFalse(result.hasAtLeastOneCompleteValidRow)
    }

    @Test
    fun `row with tone but no song input generates error`() {
        val rows = listOf(row(1, tone = "C"))

        val result = SundayRegistrationValidator.validateSundayRows(rows, emptySongs)

        assertTrue(result.incompletePositions.contains(1))
        assertTrue(result.errorsByPosition.containsKey(1))
        assertFalse(result.hasAtLeastOneCompleteValidRow)
    }

    @Test
    fun `tone with only spaces is treated as empty`() {
        val rows = listOf(row(1, songQuery = "Hallelujah", tone = "   "))

        val result = SundayRegistrationValidator.validateSundayRows(rows, emptySongs)

        assertTrue(result.incompletePositions.contains(1))
        assertTrue(result.errorsByPosition.containsKey(1))
    }

    // endregion

    // region selectedSongId as song input

    @Test
    fun `selectedSongId not null with empty songQuery counts as song input`() {
        val rows = listOf(row(1, selectedSongId = 10, tone = "D"))

        val result = SundayRegistrationValidator.validateSundayRows(rows, emptySongs)

        assertTrue(result.hasAtLeastOneCompleteValidRow)
        assertTrue(result.errorsByPosition.isEmpty())
    }

    @Test
    fun `selectedSongId not null without tone generates error`() {
        val rows = listOf(row(1, selectedSongId = 10))

        val result = SundayRegistrationValidator.validateSundayRows(rows, emptySongs)

        assertTrue(result.incompletePositions.contains(1))
        assertFalse(result.hasAtLeastOneCompleteValidRow)
    }

    // endregion

    // region mixed rows

    @Test
    fun `only incomplete rows produce errors and no valid complete row`() {
        val rows = listOf(
            row(1, songQuery = "Song A"),
            row(2, tone = "E"),
        )

        val result = SundayRegistrationValidator.validateSundayRows(rows, emptySongs)

        assertEquals(2, result.errorsByPosition.size)
        assertTrue(result.incompletePositions.containsAll(setOf(1, 2)))
        assertFalse(result.hasAtLeastOneCompleteValidRow)
    }

    @Test
    fun `mix of valid complete and incomplete rows — errors only on incomplete`() {
        val rows = listOf(
            row(1, songQuery = "Song A", tone = "C"),
            row(2, songQuery = "Song B"),
            row(3),
        )

        val result = SundayRegistrationValidator.validateSundayRows(rows, emptySongs)

        assertTrue(result.hasAtLeastOneCompleteValidRow)
        assertEquals(1, result.errorsByPosition.size)
        assertTrue(result.incompletePositions.contains(2))
        assertFalse(result.incompletePositions.contains(1))
        assertFalse(result.incompletePositions.contains(3))
    }

    // endregion

    // region edge cases

    @Test
    fun `empty list returns clean result`() {
        val result = SundayRegistrationValidator.validateSundayRows(emptyList(), emptySongs)

        assertFalse(result.hasAtLeastOneCompleteValidRow)
        assertTrue(result.errorsByPosition.isEmpty())
        assertTrue(result.incompletePositions.isEmpty())
    }

    // endregion
}
