package com.ipb.castelobranco.features.gallery.data.download

import com.ipb.castelobranco.core.domain.error.AppError
import com.ipb.castelobranco.features.gallery.data.galleryAlbum
import com.ipb.castelobranco.features.gallery.data.galleryPhoto
import com.ipb.castelobranco.features.gallery.domain.FakeGalleryMediaSettings
import com.ipb.castelobranco.features.gallery.domain.model.GalleryIndex
import com.ipb.castelobranco.features.gallery.domain.model.GalleryLocalState
import com.ipb.castelobranco.features.gallery.domain.model.GalleryPhoto
import com.ipb.castelobranco.features.gallery.domain.model.MediaType
import com.ipb.castelobranco.features.gallery.domain.repository.GalleryRepository
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import io.mockk.slot
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test

class GalleryVideoDownloadJobTest {

    private val repository: GalleryRepository = mockk(relaxed = true)
    private val downloader: GalleryVideoDownloader = mockk()
    private val settings = FakeGalleryMediaSettings(autoDownloadVideos = true)
    private val videos = slot<List<GalleryPhoto>>()
    private val job = GalleryVideoDownloadJob(repository, settings, downloader)
    private var answer: VideoDownloadRun = VideoDownloadRun.Completed(0, 0, 0)

    @Before
    fun setup() {
        val items = listOf(
            galleryPhoto(10, albumId = 1),
            galleryPhoto(11, albumId = 1).copy(mediaType = MediaType.VIDEO),
            galleryPhoto(20, albumId = 2).copy(mediaType = MediaType.VIDEO),
        )
        val index = GalleryIndex(
            listOf(galleryAlbum(1, position = 1), galleryAlbum(2, position = 0)).associateBy { it.id },
            items.associateBy { it.id },
            "c",
        )
        every { repository.localState } returns MutableStateFlow(GalleryLocalState(index, emptyMap(), emptyMap()))
        coEvery { downloader.download(capture(videos), any()) } answers { answer }
    }

    @Test
    fun `switch off - nothing is downloaded`() = runTest {
        settings.autoDownloadVideos.value = false

        assertEquals(VideoWorkDecision.Success, job.run(0, 3))

        coVerify(exactly = 0) { downloader.download(any(), any()) }
    }

    @Test
    fun `switch on - only the videos, in tree order`() = runTest {
        assertEquals(VideoWorkDecision.Success, job.run(0, 3))

        assertEquals(listOf(20L, 11L), videos.captured.map { it.id })
        coVerify(atLeast = 1) { repository.refreshLocalFiles() }
    }

    @Test
    fun `low space ends the work with its own outcome`() = runTest {
        answer = VideoDownloadRun.LowSpace(0, 2)

        assertEquals(VideoWorkDecision.LowSpace, job.run(0, 3))
    }

    @Test
    fun `network failures retry while attempts remain`() = runTest {
        answer = VideoDownloadRun.Completed(1, 2, networkFailures = 1)

        assertEquals(VideoWorkDecision.Retry, job.run(0, 3))
        assertEquals(VideoWorkDecision.Success, job.run(3, 3))
    }

    @Test
    fun `403 fails without retry`() = runTest {
        answer = VideoDownloadRun.Stopped(StopReason.FORBIDDEN, AppError.Auth(403), 0, 2)

        assertEquals(VideoWorkDecision.Fail("Disponível apenas para membros.", 403), job.run(0, 3))
    }
}
