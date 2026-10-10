package com.ipb.castelobranco.features.worshiphub.songs.domain.validation

import com.ipb.castelobranco.features.worshiphub.songs.domain.model.SongEditFields
import com.ipb.castelobranco.features.worshiphub.songs.domain.model.SongFieldErrors
import org.junit.Assert.assertEquals
import org.junit.Test

class SongEditValidatorTest {

    private val valid = SongEditFields("Oceans", "Hillsong", "https://youtu.be/abc")

    @Test
    fun `valid fields have no errors`() {
        assertEquals(SongFieldErrors(), SongEditValidator.validate(valid))
    }

    @Test
    fun `empty link is valid, it clears the link`() {
        assertEquals(SongFieldErrors(), SongEditValidator.validate(valid.copy(youtubeLink = "  ")))
    }

    @Test
    fun `blank title and artist are required`() {
        val errors = SongEditValidator.validate(valid.copy(title = "   ", artist = ""))

        assertEquals(SongEditValidator.TITLE_REQUIRED, errors.title)
        assertEquals(SongEditValidator.ARTIST_REQUIRED, errors.artist)
    }

    @Test
    fun `text longer than the limit after trimming is rejected`() {
        val atLimit = "a".repeat(SongEditValidator.MAX_TEXT_LENGTH)

        assertEquals(null, SongEditValidator.validate(valid.copy(title = " $atLimit ")).title)
        assertEquals(
            SongEditValidator.TEXT_TOO_LONG,
            SongEditValidator.validate(valid.copy(artist = atLimit + "a")).artist,
        )
    }

    @Test
    fun `link must be an http or https url`() {
        listOf("youtube.com/watch", "ftp://youtube.com/x", "https://", "not a link").forEach { link ->
            assertEquals(link, SongEditValidator.LINK_INVALID, SongEditValidator.validate(valid.copy(youtubeLink = link)).youtubeLink)
        }
        assertEquals(null, SongEditValidator.validate(valid.copy(youtubeLink = "HTTP://youtube.com/x")).youtubeLink)
    }

    @Test
    fun `link longer than 200 characters is rejected`() {
        val link = "https://youtube.com/" + "a".repeat(SongEditValidator.MAX_LINK_LENGTH)

        assertEquals(SongEditValidator.LINK_TOO_LONG, SongEditValidator.validate(valid.copy(youtubeLink = link)).youtubeLink)
    }
}
