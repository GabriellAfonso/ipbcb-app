package com.ipb.castelobranco.features.gallery.domain.usecase

import com.ipb.castelobranco.features.gallery.domain.FakeGalleryMediaSettings
import com.ipb.castelobranco.features.gallery.domain.download.GalleryDownloadScheduler
import com.ipb.castelobranco.features.gallery.domain.download.GalleryVideoDownloadScheduler
import com.ipb.castelobranco.features.gallery.domain.repository.GalleryPreviewCache
import com.ipb.castelobranco.features.gallery.domain.repository.GalleryRepository
import io.mockk.coVerify
import io.mockk.coVerifyOrder
import io.mockk.mockk
import io.mockk.verify
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class DeleteGalleryUseCaseTest {

    private val repository: GalleryRepository = mockk(relaxed = true)
    private val downloadScheduler: GalleryDownloadScheduler = mockk(relaxed = true)
    private val videoScheduler: GalleryVideoDownloadScheduler = mockk(relaxed = true)
    private val settings = FakeGalleryMediaSettings(autoDownloadVideos = true)
    private val previewCache: GalleryPreviewCache = mockk(relaxed = true)
    private val useCase = DeleteGalleryUseCase(repository, downloadScheduler, videoScheduler, settings, previewCache)

    @Test
    fun `both types - cancels the downloads and pauses before clearing the gallery and the previews`() = runTest {
        useCase()

        coVerifyOrder {
            downloadScheduler.cancel()
            repository.setSyncPaused(true)
            repository.clear()
            previewCache.clear()
        }
        verify { videoScheduler.cancel() }
        assertFalse(settings.autoDownloadVideos.value)
    }

    @Test
    fun `never syncs or enqueues a download`() = runTest {
        useCase()

        coVerify(exactly = 0) { repository.sync() }
        verify(exactly = 0) { downloadScheduler.enqueueWifiOnly(any()) }
        verify(exactly = 0) { downloadScheduler.enqueueAnyNetwork() }
        verify(exactly = 0) { videoScheduler.enqueue(any()) }
    }

    @Test
    fun `only videos - deletes the videos and turns the switch off, photos and sync untouched`() = runTest {
        useCase(photos = false, videos = true)

        coVerify { repository.clearVideos() }
        verify { videoScheduler.cancel() }
        assertFalse(settings.autoDownloadVideos.value)
        coVerify(exactly = 0) { repository.setSyncPaused(any()) }
        coVerify(exactly = 0) { repository.clear() }
        coVerify(exactly = 0) { repository.clearPhotos() }
        verify(exactly = 0) { downloadScheduler.cancel() }
    }

    @Test
    fun `only photos - deletes the originals and pauses photo downloads, videos and sync kept`() = runTest {
        useCase(photos = true, videos = false)

        coVerifyOrder {
            downloadScheduler.cancel()
            repository.clearPhotos()
            previewCache.clear()
        }
        assertTrue(settings.photoDownloadsPaused.value)
        assertTrue(settings.autoDownloadVideos.value)
        coVerify(exactly = 0) { repository.setSyncPaused(any()) }
        coVerify(exactly = 0) { repository.clearVideos() }
        verify(exactly = 0) { videoScheduler.cancel() }
    }
}
