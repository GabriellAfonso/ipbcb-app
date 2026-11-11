package com.ipb.castelobranco.features.bible.domain.model

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class AdjacentChapterTest {

    private val genesis = BibleBook("gn", "Gênesis", List(50) { 10 })
    private val exodus = BibleBook("ex", "Êxodo", List(40) { 10 })
    private val revelation = BibleBook("ap", "Apocalipse", List(22) { 10 })
    private val books = listOf(genesis, exodus, revelation)

    @Test
    fun `next inside a book moves one chapter`() {
        assertEquals(genesis to 2, books.adjacentChapter(genesis, 1, forward = true))
    }

    @Test
    fun `previous inside a book moves one chapter back`() {
        assertEquals(exodus to 9, books.adjacentChapter(exodus, 10, forward = false))
    }

    @Test
    fun `next from a book's last chapter opens chapter 1 of the next book`() {
        assertEquals(exodus to 1, books.adjacentChapter(genesis, 50, forward = true))
    }

    @Test
    fun `previous from chapter 1 opens the last chapter of the previous book`() {
        assertEquals(genesis to 50, books.adjacentChapter(exodus, 1, forward = false))
    }

    @Test
    fun `next from the last chapter of Revelation wraps to Genesis 1`() {
        assertEquals(genesis to 1, books.adjacentChapter(revelation, 22, forward = true))
    }

    @Test
    fun `previous from Genesis 1 wraps to the last chapter of Revelation`() {
        assertEquals(revelation to 22, books.adjacentChapter(genesis, 1, forward = false))
    }

    @Test
    fun `a book missing from the index gives null`() {
        assertNull(books.adjacentChapter(BibleBook("xx", "Nenhum", listOf(1)), 1, forward = true))
    }
}
