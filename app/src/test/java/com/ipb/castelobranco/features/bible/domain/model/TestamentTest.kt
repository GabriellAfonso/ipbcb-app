package com.ipb.castelobranco.features.bible.domain.model

import org.junit.Assert.assertEquals
import org.junit.Test

class TestamentTest {

    private fun books(count: Int) = (1..count).map { BibleBook(abbrev = "b$it", name = "Livro $it", verseCounts = listOf(1)) }

    @Test
    fun `the full index splits into 39 old and 27 new testament books`() {
        val index = books(66)

        val sections = index.byTestament()

        assertEquals(listOf(Testament.OLD, Testament.NEW), sections.map { it.first })
        assertEquals(index.take(39), sections[0].second)
        assertEquals(index.drop(39), sections[1].second)
    }

    @Test
    fun `an index without new testament books has only the old testament`() {
        val sections = books(10).byTestament()

        assertEquals(listOf(Testament.OLD), sections.map { it.first })
    }

    @Test
    fun `an empty index has no sections`() {
        assertEquals(emptyList<Pair<Testament, List<BibleBook>>>(), emptyList<BibleBook>().byTestament())
    }
}
