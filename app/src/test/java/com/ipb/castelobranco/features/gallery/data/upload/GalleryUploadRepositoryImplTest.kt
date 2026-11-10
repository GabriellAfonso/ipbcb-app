package com.ipb.castelobranco.features.gallery.data.upload

import com.ipb.castelobranco.core.testing.tempDirContext
import com.ipb.castelobranco.features.gallery.data.local.GalleryMediaStore
import com.ipb.castelobranco.features.gallery.domain.upload.GalleryUploadScheduler
import com.ipb.castelobranco.features.gallery.domain.upload.PickedMedia
import com.ipb.castelobranco.features.gallery.domain.upload.PickedVideoInfo
import com.ipb.castelobranco.features.gallery.domain.upload.UploadEntry
import com.ipb.castelobranco.features.gallery.domain.upload.UploadState
import com.ipb.castelobranco.features.gallery.domain.upload.VideoContainer
import com.ipb.castelobranco.features.gallery.domain.upload.VideoProbeResult
import com.ipb.castelobranco.features.gallery.domain.upload.VideoUploadCheck
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder

class GalleryUploadRepositoryImplTest {

    @get:Rule
    val folder = TemporaryFolder()

    private val dispatcher = StandardTestDispatcher()
    private val copier = mockk<PickedImageCopier>()
    private val scheduler = mockk<GalleryUploadScheduler>(relaxed = true)
    private var probed = VideoProbeResult(
        container = VideoContainer.MP4,
        byteSize = 1024,
        videoMime = "video/avc",
        audioMimes = listOf("audio/mp4a-latm"),
        durationMs = 4_000,
        dateTaken = "2030-01-01",
        hasPoster = true,
    )
    private var picked: PickedVideoInfo? = PickedVideoInfo("video/avc", emptyList(), 60_000, "2026-03-14")
    private lateinit var media: GalleryMediaStore
    private lateinit var queue: GalleryUploadQueueStore
    private lateinit var repository: GalleryUploadRepositoryImpl

    @Before
    fun setup() {
        media = GalleryMediaStore(tempDirContext(folder))
        queue = GalleryUploadQueueStore(GalleryUploadQueueStoreTest.InMemoryQueueSnapshot(), media, dispatcher)
        repository = GalleryUploadRepositoryImpl(
            queue = queue,
            copier = copier,
            preparer = mockk(),
            probe = { _, _ -> probed },
            videoReader = { picked },
            mediaStore = media,
            scheduler = scheduler,
            ioDispatcher = dispatcher,
        )
    }

    private fun cutOnDisk(name: String) = media.saveUpload(name, "x".byteInputStream())

    private val cut = UploadEntry.Cut("trim-a.mp4", "Culto.mp4", "2026-03-14")

    @Test
    fun `a cut is queued without a copy, with the original's date and the output's duration`() = runTest(dispatcher) {
        cutOnDisk("trim-a.mp4")

        repository.enqueue(7, listOf(cut))

        val item = queue.items.value.single()
        assertEquals("trim-a.mp4", item.fileName)
        assertEquals("Culto.mp4", item.displayName)
        assertEquals("2026-03-14", item.dateTaken)
        assertEquals(4_000L, item.durationMs)
        assertEquals(UploadState.Prepared, item.state)
        coVerify(exactly = 0) { copier.copy(any()) }
    }

    @Test
    fun `a cut without an original date stays without one`() = runTest(dispatcher) {
        cutOnDisk("trim-a.mp4")

        repository.enqueue(7, listOf(cut.copy(dateTaken = null)))

        assertNull(queue.items.value.single().dateTaken)
    }

    @Test
    fun `a refused cut is listed with the reason and its file is deleted`() = runTest(dispatcher) {
        val file = cutOnDisk("trim-a.mp4")
        probed = probed.copy(hasPoster = false)

        repository.enqueue(7, listOf(cut))

        val item = queue.items.value.single()
        assertEquals(UploadState.Failed, item.state)
        assertEquals(VideoUploadCheck.NO_POSTER, item.failure)
        assertFalse(file.exists())
    }

    @Test
    fun `entries are queued in order`() = runTest(dispatcher) {
        cutOnDisk("trim-a.mp4")
        coEvery { copier.copy("p") } returns PickedImageCopier.Copy("p-id", "p-id-src.jpg", "p.jpg", "image/jpeg")

        repository.enqueue(7, listOf(UploadEntry.Picked("p"), cut))

        assertEquals(listOf("p.jpg", "Culto.mp4"), queue.items.value.map { it.displayName })
    }

    @Test
    fun `inspect marks an accepted video as trimmable, reading the original's data`() = runTest(dispatcher) {
        coEvery { copier.inspect("v") } returns PickedMedia("v", "video/mp4", 400L)
        coEvery { copier.videoDisplayName("v") } returns "Culto.mp4"

        val trimmable = checkNotNull(repository.inspect(listOf("v")).single().trimmable)

        assertEquals("2026-03-14", trimmable.dateTaken)
        assertEquals(60_000L, trimmable.durationMs)
    }

    @Test
    fun `inspect leaves a refused codec or an unreadable video out of the editor`() = runTest(dispatcher) {
        coEvery { copier.inspect("v") } returns PickedMedia("v", "video/mp4", 400L)

        picked = PickedVideoInfo("video/mpeg2", emptyList(), 60_000, null)
        assertNull(repository.inspect(listOf("v")).single().trimmable)

        picked = null
        assertNull(repository.inspect(listOf("v")).single().trimmable)
    }

    @Test
    fun `inspect does not read a photo as a video`() = runTest(dispatcher) {
        coEvery { copier.inspect("p") } returns PickedMedia("p", "image/jpeg", 1L)
        picked = null

        assertNull(repository.inspect(listOf("p")).single().trimmable)
    }

    @Test
    fun `the sweep deletes only cuts the queue does not know`() = runTest(dispatcher) {
        val queued = cutOnDisk("trim-a.mp4")
        repository.enqueue(7, listOf(cut))
        val orphan = cutOnDisk("trim-orphan.mp4")
        val pickedCopy = cutOnDisk("abc-src.mp4")

        repository.sweepOrphanCuts()

        assertTrue(queued.exists())
        assertTrue(pickedCopy.exists())
        assertFalse(orphan.exists())
    }
}
