package com.ipb.castelobranco.features.bible.presentation.components

import com.ipb.castelobranco.features.bible.domain.model.BibleHeading
import org.junit.Assert.assertEquals
import org.junit.Test

class VerseItemIndexTest {

    // Mt 5 (ARA) in miniature: two headings open verse 1, one opens verse 3.
    private val headings = listOf(
        BibleHeading(verse = 1, level = 1, title = "O sermão do monte"),
        BibleHeading(verse = 1, level = 2, title = "As bem-aventuranças"),
        BibleHeading(verse = 3, level = 2, title = "Os discípulos, o sal da terra"),
    )

    @Test
    fun `without headings the verse sits right after the chapter title`() {
        assertEquals(1, verseItemIndex(verse = 1, headings = emptyList()))
        assertEquals(5, verseItemIndex(verse = 5, headings = emptyList()))
    }

    @Test
    fun `a verse opened by headings scrolls to its first heading`() {
        assertEquals(1, verseItemIndex(verse = 1, headings = headings))
        // header, 2 headings, v1, v2 → the heading of v3 is item 5.
        assertEquals(5, verseItemIndex(verse = 3, headings = headings))
    }

    @Test
    fun `a verse without its own heading counts the headings before it`() {
        // header, 2 headings, v1 → v2 is item 4.
        assertEquals(4, verseItemIndex(verse = 2, headings = headings))
    }

    @Test
    fun `verse zero or below falls back to the first verse`() {
        assertEquals(1, verseItemIndex(verse = 0, headings = headings))
    }
}
