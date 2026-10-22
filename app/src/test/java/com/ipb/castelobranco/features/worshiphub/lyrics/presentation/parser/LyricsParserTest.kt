package com.ipb.castelobranco.features.worshiphub.lyrics.presentation.parser

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class LyricsParserTest {

    // region empty / blank input

    @Test
    fun `empty string returns empty list`() {
        val result = LyricsParser.parse("")
        assertTrue(result.isEmpty())
    }

    @Test
    fun `only blank lines returns empty list`() {
        val result = LyricsParser.parse("\n\n\n")
        assertTrue(result.isEmpty())
    }

    // endregion

    // region single stanza

    @Test
    fun `single stanza without separators returns one stanza with correct lines`() {
        val result = LyricsParser.parse("Line one\nLine two\nLine three")

        assertEquals(1, result.size)
        assertEquals(listOf("Line one", "Line two", "Line three"), result[0].lines)
    }

    @Test
    fun `last stanza without trailing blank line is still included`() {
        val result = LyricsParser.parse("Stanza one\n\nStanza two")

        assertEquals(2, result.size)
        assertEquals(listOf("Stanza two"), result[1].lines)
    }

    // endregion

    // region multiple stanzas

    @Test
    fun `two stanzas separated by blank line returns two stanzas`() {
        val content = "Line A1\nLine A2\n\nLine B1\nLine B2"
        val result = LyricsParser.parse(content)

        assertEquals(2, result.size)
        assertEquals(listOf("Line A1", "Line A2"), result[0].lines)
        assertEquals(listOf("Line B1", "Line B2"), result[1].lines)
    }

    @Test
    fun `multiple consecutive blank lines are treated as single separator`() {
        val content = "Stanza one\n\n\n\nStanza two"
        val result = LyricsParser.parse(content)

        assertEquals(2, result.size)
        assertEquals(listOf("Stanza one"), result[0].lines)
        assertEquals(listOf("Stanza two"), result[1].lines)
    }

    // endregion

    // region trimming

    @Test
    fun `lines with surrounding spaces are trimmed`() {
        val result = LyricsParser.parse("  Line one  \n   Line two   ")

        assertEquals(1, result.size)
        assertEquals(listOf("Line one", "Line two"), result[0].lines)
    }

    @Test
    fun `line with only spaces is treated as blank and does not enter stanza`() {
        val content = "Line one\n   \nLine two"
        val result = LyricsParser.parse(content)

        assertEquals(2, result.size)
        assertEquals(listOf("Line one"), result[0].lines)
        assertEquals(listOf("Line two"), result[1].lines)
    }

    // endregion
}
