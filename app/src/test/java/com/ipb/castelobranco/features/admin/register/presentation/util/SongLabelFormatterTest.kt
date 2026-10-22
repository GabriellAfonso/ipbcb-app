package com.ipb.castelobranco.features.admin.register.presentation.util

import com.ipb.castelobranco.core.domain.model.Song
import org.junit.Assert.assertEquals
import org.junit.Test

class SongLabelFormatterTest {

    private fun song(title: String, artist: String) = Song(id = 1, title = title, artist = artist, categoryName = "")

    @Test
    fun `non-blank artist returns title with artist in brackets`() {
        val result = SongLabelFormatter.format(song("Hallelujah", "Leonard Cohen"))

        assertEquals("Hallelujah [Leonard Cohen]", result)
    }

    @Test
    fun `empty artist returns title only`() {
        val result = SongLabelFormatter.format(song("Hallelujah", ""))

        assertEquals("Hallelujah", result)
    }

    @Test
    fun `artist with only spaces returns title only`() {
        val result = SongLabelFormatter.format(song("Hallelujah", "   "))

        assertEquals("Hallelujah", result)
    }
}
