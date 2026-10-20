package com.ipb.castelobranco.features.studies.domain.usecase

import com.ipb.castelobranco.features.studies.DoctrineTestFixtures
import com.ipb.castelobranco.features.studies.domain.model.DoctrineSearchResult
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class SearchDoctrineUseCaseTest {

    private val search = SearchDoctrineUseCase()

    private fun questionNumbers(term: String) =
        search(DoctrineTestFixtures.catechism, term).map { (it as DoctrineSearchResult.Question).number }

    @Test
    fun `ignores accents and case`() {
        assertEquals(listOf(34), questionNumbers("ADOCAO"))
    }

    @Test
    fun `requires every word, in any order`() {
        assertEquals(listOf(33, 34), questionNumbers("graça livre"))
        assertEquals(emptyList<Int>(), questionNumbers("graça homem"))
    }

    @Test
    fun `numeric term puts that question first`() {
        assertEquals(34, questionNumbers("34").first())
    }

    @Test
    fun `blank term or no match returns nothing`() {
        assertTrue(questionNumbers("   ").isEmpty())
        assertTrue(questionNumbers("zzz").isEmpty())
    }

    @Test
    fun `confession matches list items and text after, with chapter and paragraph`() {
        val byListItem = search(DoctrineTestFixtures.confession, "exodo").single() as DoctrineSearchResult.Paragraph
        assertEquals(1, byListItem.chapter)
        assertEquals(2, byListItem.paragraph)

        val byTextAfter = search(DoctrineTestFixtures.confession, "inspiracao")
        assertEquals(1, byTextAfter.size)

        val other = search(DoctrineTestFixtures.confession, "justificados").single() as DoctrineSearchResult.Paragraph
        assertEquals(12, other.chapter)
    }
}
