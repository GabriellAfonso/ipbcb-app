package com.ipb.castelobranco.features.bible.domain.model

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class BibleProgressTest {

    private val server = BibleProgress(chapters = mapOf("gn" to setOf(1, 2)), videos = setOf(7L))

    @Test
    fun `pending changes are applied on top of the server state`() {
        val merged = server.applying(
            listOf(
                ProgressChange(ProgressTarget.chapter("gn", 3), done = true, changedAt = "t"),
                ProgressChange(ProgressTarget.chapter("gn", 1), done = false, changedAt = "t"),
                ProgressChange(ProgressTarget.Video(7L), done = false, changedAt = "t"),
                ProgressChange(ProgressTarget.Video(9L), done = true, changedAt = "t"),
            )
        )

        assertEquals(setOf(2, 3), merged.chapters["gn"])
        assertEquals(setOf(9L), merged.videos)
    }

    @Test
    fun `a book is read only when every chapter is`() {
        assertTrue(server.isBookRead("GN", chapterCount = 2))
        assertFalse(server.isBookRead("gn", chapterCount = 3))
        assertEquals(2, server.readCount("gn"))
        assertEquals(0, server.readCount("ex"))
    }

    @Test
    fun `unmarking the last chapter of a book drops the book`() {
        val merged = BibleProgress(mapOf("ex" to setOf(1)), emptySet())
            .applying(listOf(ProgressChange(ProgressTarget.chapter("ex", 1), done = false, changedAt = "t")))

        assertTrue(merged.chapters.isEmpty())
    }
}
