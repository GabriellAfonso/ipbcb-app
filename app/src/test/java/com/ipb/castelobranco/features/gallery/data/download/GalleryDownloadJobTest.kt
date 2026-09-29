package com.ipb.castelobranco.features.gallery.data.download

import com.ipb.castelobranco.core.domain.error.AppError
import com.ipb.castelobranco.features.gallery.data.galleryAlbum
import com.ipb.castelobranco.features.gallery.data.galleryPhoto
import com.ipb.castelobranco.features.gallery.domain.model.GalleryIndex
import com.ipb.castelobranco.features.gallery.domain.model.GalleryLocalState
import com.ipb.castelobranco.features.gallery.domain.model.GalleryPhoto
import com.ipb.castelobranco.features.gallery.domain.model.GallerySyncResult
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

class GalleryDownloadJobTest {

    private lateinit var repository: GalleryRepository
    private lateinit var downloader: GalleryPhotoDownloader
    private lateinit var job: GalleryDownloadJob
    private val state = MutableStateFlow(GalleryLocalState.EMPTY)
    private val photos = slot<List<GalleryPhoto>>()

    private val index = GalleryIndex(
        albums = listOf(galleryAlbum(1, position = 1), galleryAlbum(2, position = 0)).associateBy { it.id },
        photos = listOf(galleryPhoto(10, albumId = 1), galleryPhoto(20, albumId = 2)).associateBy { it.id },
        cursor = "c1",
    )

    @Before
    fun setup() {
        repository = mockk(relaxed = true)
        downloader = mockk()
        every { repository.localState } returns state
        coEvery { downloader.download(capture(photos), any(), any()) } answers {
            GalleryDownloadRun.Completed(photos.captured.size, photos.captured.size, 0, 0)
        }
        job = GalleryDownloadJob(repository, downloader)
    }

    private suspend fun run(attempt: Int = 0) = job.run(attempt, MAX_RETRIES, onProgress = { _, _ -> })

    @Test
    fun `downloads the index photos in tree order`() = runTest {
        state.value = GalleryLocalState(index, emptyMap(), emptyMap())

        assertEquals(WorkDecision.Success, run())

        assertEquals(listOf(20L, 10L), photos.captured.map { it.id })
        coVerify(exactly = 0) { repository.sync() }
        coVerify(atLeast = 1) { repository.refreshLocalFiles() }
    }

    @Test
    fun `no index - syncs first, then downloads`() = runTest {
        coEvery { repository.sync() } answers {
            state.value = GalleryLocalState(index, emptyMap(), emptyMap())
            GallerySyncResult.Synced(2)
        }

        assertEquals(WorkDecision.Success, run())

        assertEquals(2, photos.captured.size)
    }

    @Test
    fun `no index and the sync is refused - fails with the code`() = runTest {
        coEvery { repository.sync() } returnsMany listOf(
            GallerySyncResult.Failed(AppError.Auth(401)),
            GallerySyncResult.Failed(AppError.Auth(403)),
        )

        assertEquals(401, (run() as WorkDecision.Fail).code)
        assertEquals(403, (run() as WorkDecision.Fail).code)
    }

    @Test
    fun `no index and the sync fails on the network - retries up to the limit`() = runTest {
        coEvery { repository.sync() } returns GallerySyncResult.Failed(AppError.Network())

        assertEquals(WorkDecision.Retry, run(attempt = 0))
        assertEquals(WorkDecision.Fail("Falha ao baixar galeria", 0), run(attempt = MAX_RETRIES))
    }

    private companion object {
        const val MAX_RETRIES = 3
    }
}
