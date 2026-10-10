package com.ipb.castelobranco.features.worshiphub.shared.domain

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class ContentSearchTest {

    private val lyrics = ContentSearch.index(
        """
        Grande é o Senhor e mui digno de louvor
        Na cidade do nosso Deus

        Seu santo monte, alegria de toda terra
        """.trimIndent(),
    )

    @Test
    fun `finds the line holding the query, ignoring accents and case`() {
        assertEquals("Grande é o Senhor e mui digno de louvor", lyrics.findSnippet("GRANDE E O SENHOR"))
    }

    @Test
    fun `matches words in any order and across lines`() {
        assertEquals("Na cidade do nosso Deus", lyrics.findSnippet("deus cidade"))
        assertEquals("Seu santo monte, alegria de toda terra", lyrics.findSnippet("senhor terra alegria"))
    }

    @Test
    fun `returns null when any word is missing`() {
        assertNull(lyrics.findSnippet("grande oceano"))
    }

    @Test
    fun `returns null for a blank query`() {
        assertNull(lyrics.findSnippet("   "))
    }

    @Test
    fun `ignores chordpro chords and directives`() {
        val chart = ContentSearch.index(
            """
            {start_of_chorus: Coro}
            [D]Gran[A]de é o [G]Senhor
            {end_of_chorus}
            """.trimIndent(),
        )

        assertEquals("Grande é o Senhor", chart.findSnippet("grande senhor"))
        assertNull(chart.findSnippet("chorus"))
    }

    @Test
    fun `matchesTitle needs the whole query in the title`() {
        assertTrue(matchesTitle("Oceanos da Graça", "graca"))
        assertFalse(matchesTitle("Oceanos da Graça", "graca oceanos"))
    }
}
