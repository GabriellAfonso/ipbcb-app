package com.ipb.castelobranco.features.gallery.domain.upload

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class UploadBatchTest {

    private fun photo(id: String) = PickedMedia(id, "image/jpeg", 1L)

    private fun video(id: String, mb: Long = 40) = PickedMedia(
        id,
        "video/mp4",
        mb * MB,
        TrimmableVideo("$id.mp4", 60_000, "2026-03-14", "video/avc"),
    )

    private fun cut(file: String, mb: Long) =
        VideoDecision.Cut(UploadEntry.Cut(file, "$file.mp4", null), mb * MB)

    @Test
    fun `photos and untrimmable videos start decided`() {
        val refused = PickedMedia("mkv", "video/x-matroska", 10L * MB)

        val batch = UploadBatch.of(1, listOf(photo("p"), refused))

        assertTrue(batch.isComplete)
    }

    @Test
    fun `the position counts only the videos the editor goes through`() {
        val batch = UploadBatch.of(1, listOf(photo("p"), video("a"), photo("q"), video("b")))

        assertEquals(1, batch.position)
        assertEquals(2, batch.editableCount)
        assertEquals(2, batch.decided(batch.currentIndex, VideoDecision.Whole).position)
    }

    @Test
    fun `entries keep the picked order, with cuts in place`() {
        val batch = UploadBatch.of(1, listOf(video("a"), photo("p"), video("b")))
        val decided = batch.decided(0, cut("trim-a", 5)).allWhole()

        assertTrue(decided.isComplete)
        assertEquals(
            listOf(UploadEntry.Cut("trim-a", "trim-a.mp4", null), UploadEntry.Picked("p"), UploadEntry.Picked("b")),
            decided.entries,
        )
        assertEquals(listOf("trim-a"), decided.cutFileNames)
    }

    @Test
    fun `the mobile-data load counts real sizes and skips a too-big whole video`() {
        val batch = UploadBatch.of(1, listOf(video("big", 400), video("whole", 30), video("toobig", 150), photo("p")))
        val decided = batch.decided(0, cut("trim", 40)).allWhole()

        assertEquals(MobileDataLoad(2, 70L * MB), decided.mobileDataLoad)
    }

    @Test
    fun `a batch with an undecided video is not complete`() {
        assertFalse(UploadBatch.of(1, listOf(video("a"))).isComplete)
    }

    private companion object {
        const val MB = 1024L * 1024
    }
}
