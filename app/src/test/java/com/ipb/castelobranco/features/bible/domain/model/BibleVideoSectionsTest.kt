package com.ipb.castelobranco.features.bible.domain.model

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class BibleVideoSectionsTest {

    private fun video(id: Long, vararg places: Pair<BiblePlace, Int>) = BibleVideo(
        id = id,
        kind = BibleVideoKind.VIDEO,
        url = "https://youtu.be/$id",
        title = "Vídeo $id",
        thumbnailUrl = null,
        places = places.map { (place, position) -> BibleVideoPlace(place, position) },
    )

    private val rm5 = BiblePlace.of("rm", 5)
    private val rm = BiblePlace.of("rm", null)

    @Test
    fun `chapter and book sections follow each place's order`() {
        val videos = listOf(
            video(1, rm5 to 1),
            video(2, rm5 to 0),
            video(3, rm to 1),
            video(4, rm to 0),
        )

        val sections = videos.sectionsFor("rm", 5)

        assertEquals(listOf(2L, 1L), sections.chapter.map { it.id })
        assertEquals(listOf(4L, 3L), sections.book.map { it.id })
    }

    @Test
    fun `a video in both the chapter and the book shows only in the chapter`() {
        val videos = listOf(video(1, rm5 to 0, rm to 0), video(2, rm to 1))

        val sections = videos.sectionsFor("RM", 5)

        assertEquals(listOf(1L), sections.chapter.map { it.id })
        assertEquals(listOf(2L), sections.book.map { it.id })
    }

    @Test
    fun `another chapter of the same book only gets the book section`() {
        val videos = listOf(video(1, rm5 to 0), video(2, rm to 0))

        val sections = videos.sectionsFor("rm", 6)

        assertTrue(sections.chapter.isEmpty())
        assertEquals(listOf(2L), sections.book.map { it.id })
    }

    @Test
    fun `nothing in the chapter nor the book is empty`() {
        assertTrue(listOf(video(1, rm5 to 0)).sectionsFor("gn", 1).isEmpty)
    }

    @Test
    fun `place labels use the book name`() {
        val books = listOf(BibleBook("gn", "Gênesis", listOf(31)))

        assertEquals("Gênesis 1", BiblePlace.of("gn", 1).label(books))
        assertEquals("Gênesis (livro inteiro)", BiblePlace.of("GN", null).label(books))
    }
}
