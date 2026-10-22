package com.ipb.castelobranco.features.worshiphub.register.data.mapper

import com.ipb.castelobranco.features.worshiphub.register.domain.model.SundayPlayPushItem
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class RegisterSundayPlaysRequestMapperTest {

    private fun play(songId: Int, position: Int, tone: String) =
        SundayPlayPushItem(songId = songId, position = position, tone = tone)

    @Test
    fun `buildRegisterRequest maps date correctly`() {
        val result = buildRegisterRequest(date = "2025-06-15", plays = emptyList())

        assertEquals("2025-06-15", result.date)
    }

    @Test
    fun `buildRegisterRequest maps plays to DTOs correctly`() {
        val plays = listOf(
            play(songId = 1, position = 1, tone = "C"),
            play(songId = 2, position = 2, tone = "G"),
        )

        val result = buildRegisterRequest(date = "2025-06-15", plays = plays)

        assertEquals(2, result.plays.size)
        assertEquals(1, result.plays[0].songId)
        assertEquals(1, result.plays[0].position)
        assertEquals("C", result.plays[0].tone)
        assertEquals(2, result.plays[1].songId)
        assertEquals(2, result.plays[1].position)
        assertEquals("G", result.plays[1].tone)
    }

    @Test
    fun `buildRegisterRequest with empty plays produces empty list`() {
        val result = buildRegisterRequest(date = "2025-06-15", plays = emptyList())

        assertTrue(result.plays.isEmpty())
    }
}
