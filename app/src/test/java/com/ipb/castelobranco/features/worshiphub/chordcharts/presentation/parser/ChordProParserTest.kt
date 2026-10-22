package com.ipb.castelobranco.features.worshiphub.chordcharts.presentation.parser

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class ChordProParserTest {

    // region empty content

    @Test
    fun `content with only directives and no lines produces blocks without crashing`() {
        val result = ChordProParser.parse("{verse: A}\n{verse: B}")

        assertEquals(2, result.size)
    }

    // endregion

    // region directives

    @Test
    fun `verse directive with name creates block with title`() {
        val result = ChordProParser.parse("{verse: Estrofe 1}")

        assertEquals(1, result.size)
        assertEquals("Estrofe 1", result[0].title)
        assertFalse(result[0].isIntro)
    }

    @Test
    fun `verse directive without name creates block with null title`() {
        val result = ChordProParser.parse("{verse}\nlinha")

        assertEquals(1, result.size)
        assertNull(result[0].title)
        assertFalse(result[0].isIntro)
    }

    @Test
    fun `chorus directive creates block with null title`() {
        val result = ChordProParser.parse("{chorus}\nlinha")

        assertEquals(1, result.size)
        assertNull(result[0].title)
        assertFalse(result[0].isIntro)
    }

    @Test
    fun `intro directive creates block with isIntro true and null title`() {
        val result = ChordProParser.parse("{intro}")

        assertEquals(1, result.size)
        assertTrue(result[0].isIntro)
        assertNull(result[0].title)
    }

    @Test
    fun `directive is case insensitive - INTRO uppercase`() {
        val result = ChordProParser.parse("{INTRO}")

        assertEquals(1, result.size)
        assertTrue(result[0].isIntro)
    }

    @Test
    fun `directive is case insensitive - Verse with capital`() {
        val result = ChordProParser.parse("{Verse: Nome}")

        assertEquals(1, result.size)
        assertEquals("Nome", result[0].title)
    }

    // endregion

    // region line parsing

    @Test
    fun `plain text line produces Lyrics token`() {
        val result = ChordProParser.parse("{verse}\nAleluia")

        val tokens = result[0].lines[0].tokens
        assertEquals(1, tokens.size)
        assertEquals(LineToken.Lyrics("Aleluia"), tokens[0])
    }

    @Test
    fun `empty line produces ChordLine with empty tokens`() {
        val result = ChordProParser.parse("{verse}\n")

        val tokens = result[0].lines[0].tokens
        assertTrue(tokens.isEmpty())
    }

    @Test
    fun `line with only chords produces alternating Chord and Lyrics space`() {
        val result = ChordProParser.parse("{verse}\n[C][G][Am]")

        val tokens = result[0].lines[0].tokens
        val chords = tokens.filterIsInstance<LineToken.Chord>()
        val lyrics = tokens.filterIsInstance<LineToken.Lyrics>()

        assertTrue(chords.isNotEmpty())
        lyrics.forEach { assertEquals(" ", it.value) }
    }

    @Test
    fun `inline chord before word produces Chord then Lyrics`() {
        val result = ChordProParser.parse("{verse}\n[C]palavra")

        val tokens = result[0].lines[0].tokens
        assertTrue(tokens[0] is LineToken.Chord)
        assertEquals("C", (tokens[0] as LineToken.Chord).value)
        assertTrue(tokens[1] is LineToken.Lyrics)
        assertEquals("palavra", (tokens[1] as LineToken.Lyrics).value)
    }

    @Test
    fun `multiple chords at same position are merged`() {
        val result = ChordProParser.parse("{verse}\n[C][Am]palavra")

        val tokens = result[0].lines[0].tokens
        val chord = tokens.filterIsInstance<LineToken.Chord>().first()
        assertEquals("C Am", chord.value)
    }

    @Test
    fun `chord after end of text appended with Lyrics space`() {
        val result = ChordProParser.parse("{verse}\npalavra[G]")

        val tokens = result[0].lines[0].tokens
        val lastChord = tokens.filterIsInstance<LineToken.Chord>().last()
        val afterChord = tokens[tokens.indexOf(lastChord) + 1]
        assertEquals("G", lastChord.value)
        assertEquals(LineToken.Lyrics(" "), afterChord)
    }

    // endregion

    // region block grouping

    @Test
    fun `lines before any directive are grouped in initial block`() {
        val content = "linha1\nlinha2\n{verse}\nlinha3"

        val result = ChordProParser.parse(content)

        assertEquals(2, result.size)
        assertEquals(2, result[0].lines.size)
        assertNull(result[0].title)
    }

    @Test
    fun `directive mid-content flushes previous block`() {
        val content = "{verse: A}\nlinha1\n{verse: B}\nlinha2"

        val result = ChordProParser.parse(content)

        assertEquals(2, result.size)
        assertEquals("A", result[0].title)
        assertEquals("B", result[1].title)
        assertEquals(1, result[0].lines.size)
        assertEquals(1, result[1].lines.size)
    }

    @Test
    fun `multiple blocks are parsed correctly`() {
        val content = """
            {verse: Estrofe}
            linha estrofe
            {chorus}
            linha refrão
        """.trimIndent()

        val result = ChordProParser.parse(content)

        assertEquals(2, result.size)
        assertEquals("Estrofe", result[0].title)
        assertNull(result[1].title)
    }

    // endregion
}
