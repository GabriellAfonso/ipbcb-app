package com.ipb.castelobranco.features.worshiphub.tables.data.api

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class SuggestedSongsExcludeEncoderTest {

    @Test
    fun `encodes one pair per song, repeating the position`() {
        val encoded = SuggestedSongsExcludeEncoder.encode(mapOf(3 to setOf(34, 12), 2 to setOf(7)))

        assertEquals("2:7,3:12,3:34", encoded)
    }

    @Test
    fun `returns null when nothing is excluded`() {
        assertNull(SuggestedSongsExcludeEncoder.encode(emptyMap()))
        assertNull(SuggestedSongsExcludeEncoder.encode(mapOf(3 to emptySet())))
    }
}
