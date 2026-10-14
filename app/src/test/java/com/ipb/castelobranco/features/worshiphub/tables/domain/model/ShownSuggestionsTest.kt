package com.ipb.castelobranco.features.worshiphub.tables.domain.model

import org.junit.Assert.assertEquals
import org.junit.Test

class ShownSuggestionsTest {

    @Test
    fun `record adds new songs to the position`() {
        val shown = ShownSuggestions().record(3, 12).record(3, 34)

        assertEquals(mapOf(3 to setOf(12, 34)), shown.excludedFor(setOf(3)))
    }

    @Test
    fun `repeated song resets only its position`() {
        val shown = ShownSuggestions()
            .record(2, 7)
            .record(3, 12)
            .record(3, 34)
            .record(3, 12)

        assertEquals(mapOf(2 to setOf(7), 3 to setOf(12)), shown.excludedFor(setOf(2, 3)))
    }

    @Test
    fun `excludedFor leaves out positions that are not free`() {
        val shown = ShownSuggestions().record(1, 5).record(3, 12)

        assertEquals(mapOf(3 to setOf(12)), shown.excludedFor(setOf(3)))
    }

    @Test
    fun `everywhere songs are added to every free position`() {
        val shown = ShownSuggestions().record(3, 12)

        assertEquals(
            mapOf(2 to setOf(50), 3 to setOf(12, 50)),
            shown.excludedFor(setOf(2, 3), everywhere = setOf(50)),
        )
    }

    @Test
    fun `fix drops the position list`() {
        val shown = ShownSuggestions().record(3, 12).fix(3)

        assertEquals(emptyMap<Int, Set<Int>>(), shown.excludedFor(setOf(3)))
    }

    @Test
    fun `unfix starts over with only the current song`() {
        val shown = ShownSuggestions().record(3, 12).fix(3).unfix(3, currentSongId = 40)

        assertEquals(mapOf(3 to setOf(40)), shown.excludedFor(setOf(3)))
    }

    @Test
    fun `unfix without a song starts empty`() {
        val shown = ShownSuggestions().unfix(3, currentSongId = null)

        assertEquals(emptyMap<Int, Set<Int>>(), shown.excludedFor(setOf(3)))
    }
}
