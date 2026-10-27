package com.ipb.castelobranco.features.gallery.data.download

import com.ipb.castelobranco.core.testing.tempDirContext
import com.ipb.castelobranco.features.gallery.data.api.FakeGalleryApi
import com.ipb.castelobranco.features.gallery.data.galleryPhoto
import com.ipb.castelobranco.features.gallery.data.local.GalleryMediaStore
import com.ipb.castelobranco.features.gallery.domain.model.GalleryPhoto
import com.ipb.castelobranco.features.gallery.domain.model.MediaType
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder

class GalleryVideoDownloaderTest {

    @get:Rule
    val folder = TemporaryFolder()

    private val api = FakeGalleryApi()
    private lateinit var storage: GalleryMediaStore
    private var freeBytes = 10L * GalleryVideoDownloader.MIN_FREE_BYTES
    private lateinit var downloader: GalleryVideoDownloader
    private val bytes = "0123456789abcdef".toByteArray()

    @Before
    fun setup() {
        storage = GalleryMediaStore(tempDirContext(folder))
        downloader = GalleryVideoDownloader(api, storage) { freeBytes }
    }

    private fun video(id: Long, size: Long? = bytes.size.toLong()): GalleryPhoto =
        galleryPhoto(id, url = url(id)).copy(mediaType = MediaType.VIDEO, durationMs = 1000, fileSize = size)

    private fun url(id: Long) = "https://example.com/ipbcb/media/gallery/1/$id.mp4"

    @Test
    fun `a whole download becomes the video and is counted`() = runTest {
        api.respondRanged(url(1), bytes)
        var done = 0

        val run = downloader.download(listOf(video(1)), onVideoDone = { done++ })

        assertEquals(VideoDownloadRun.Completed(1, 1, networkFailures = 0), run)
        assertEquals(String(bytes), storage.videoFiles().getValue(1).readText())
        assertEquals(1, done)
        assertEquals(listOf<String?>(null), api.ranges)
    }

    @Test
    fun `a cut download keeps the partial, never counts it, and resumes from where it stopped`() = runTest {
        api.respondCut(url(1), bytes.copyOfRange(0, 6))

        val first = downloader.download(listOf(video(1)))

        assertEquals(VideoDownloadRun.Completed(0, 1, networkFailures = 1), first)
        assertTrue(storage.videoFiles().isEmpty())
        assertEquals(6L, storage.partialVideo(1, "mp4").length())

        api.respondRanged(url(1), bytes)
        val second = downloader.download(listOf(video(1)))

        assertEquals(1, second.downloaded)
        assertEquals("bytes=6-", api.ranges.last())
        assertEquals(String(bytes), storage.videoFiles().getValue(1).readText())
    }

    @Test
    fun `a server that ignores Range sends everything again and the file starts over`() = runTest {
        storage.partialVideo(1, "mp4").writeText("012")
        api.respondRanged(url(1), bytes, ignoreRange = true)

        downloader.download(listOf(video(1)))

        assertEquals(String(bytes), storage.videoFiles().getValue(1).readText())
    }

    @Test
    fun `416 with the partial already whole completes it`() = runTest {
        storage.partialVideo(1, "mp4").writeBytes(bytes)
        api.respondRanged(url(1), bytes)

        val run = downloader.download(listOf(video(1)))

        assertEquals(1, run.downloaded)
        assertTrue(storage.videoFile(1, "mp4").exists())
    }

    @Test
    fun `a video already on disk is counted without a request`() = runTest {
        storage.videoFile(1, "mp4").apply { parentFile?.mkdirs() }.writeBytes(bytes)

        val run = downloader.download(listOf(video(1)))

        assertEquals(1, run.downloaded)
        assertTrue(api.requestedUrls.isEmpty())
    }

    @Test
    fun `under 1 GB free after the next video - stops before requesting it`() = runTest {
        freeBytes = GalleryVideoDownloader.MIN_FREE_BYTES + 5

        val run = downloader.download(listOf(video(1)))

        assertEquals(VideoDownloadRun.LowSpace(0, 1), run)
        assertTrue(api.requestedUrls.isEmpty())
    }

    @Test
    fun `an unknown size counts as the upload limit for the space check`() = runTest {
        freeBytes = GalleryVideoDownloader.MIN_FREE_BYTES + GalleryVideoDownloader.UNKNOWN_SIZE_BYTES - 1

        val run = downloader.download(listOf(video(1, size = null)))

        assertEquals(VideoDownloadRun.LowSpace(0, 1), run)
    }

    @Test
    fun `403 stops the run`() = runTest {
        api.respondError(url(1), 403)

        val run = downloader.download(listOf(video(1), video(2)))

        assertTrue(run is VideoDownloadRun.Stopped && run.reason == StopReason.FORBIDDEN)
        assertEquals(listOf(url(1)), api.requestedUrls)
        assertFalse(storage.videoFile(1, "mp4").exists())
    }
}
