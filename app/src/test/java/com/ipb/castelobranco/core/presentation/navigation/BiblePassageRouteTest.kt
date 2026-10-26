package com.ipb.castelobranco.core.presentation.navigation

import org.junit.Assert.assertEquals
import org.junit.Test

class BiblePassageRouteTest {

    @Test
    fun `verse ranges round trip`() {
        val encoded = AppRoutes.encodeVerseRanges(listOf(3..5, 7..7))

        assertEquals("3-5,7", encoded)
        assertEquals(setOf(3, 4, 5, 7), AppRoutes.parseVerseRanges(encoded))
    }

    @Test
    fun `malformed parts are dropped`() {
        assertEquals(setOf(2), AppRoutes.parseVerseRanges("2,x,5-3,0,1-2-3,4-"))
    }

    @Test
    fun `missing verses highlight nothing`() {
        assertEquals(emptySet<Int>(), AppRoutes.parseVerseRanges(null))
        assertEquals(emptySet<Int>(), AppRoutes.parseVerseRanges(""))
    }

    @Test
    fun `passage route carries the highlighted ranges`() {
        assertEquals(
            "bible_passage?book=1Tn&chapter=3&verse=16&verses=16-17",
            AppRoutes.biblePassage("1Tn", 3, 16, listOf(16..17)),
        )
    }
}
