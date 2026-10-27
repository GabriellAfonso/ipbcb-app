package com.ipb.castelobranco.features.gallery.domain.usecase

import com.ipb.castelobranco.features.gallery.domain.FakeGalleryMediaSettings
import com.ipb.castelobranco.features.gallery.domain.download.GalleryDownloadScheduler
import com.ipb.castelobranco.features.gallery.domain.download.GalleryVideoDownloadScheduler
import com.ipb.castelobranco.features.gallery.domain.model.GallerySyncResult
import com.ipb.castelobranco.features.gallery.domain.repository.GalleryPreviewCache
import com.ipb.castelobranco.features.gallery.domain.repository.GalleryRepository
import com.ipb.castelobranco.features.gallery.domain.sync.GallerySyncScheduler
import com.ipb.castelobranco.features.gallery.domain.upload.GalleryUploadRepository
import com.ipb.castelobranco.features.gallery.domain.upload.GalleryUploadScheduler
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.coVerifyOrder
import io.mockk.mockk
import io.mockk.verify
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class GalleryAutoDownloadUseCaseTest {

    private lateinit var downloadScheduler: GalleryDownloadScheduler
    private lateinit var videoScheduler: GalleryVideoDownloadScheduler
    private lateinit var settings: FakeGalleryMediaSettings
    private lateinit var syncScheduler: GallerySyncScheduler
    private lateinit var syncGallery: SyncGalleryUseCase
    private lateinit var repository: GalleryRepository
    private lateinit var previewCache: GalleryPreviewCache
    private lateinit var uploadScheduler: GalleryUploadScheduler
    private lateinit var uploads: GalleryUploadRepository
    private lateinit var useCase: GalleryAutoDownloadUseCase

    @Before
    fun setup() {
        downloadScheduler = mockk(relaxed = true)
        videoScheduler = mockk(relaxed = true)
        settings = FakeGalleryMediaSettings()
        syncScheduler = mockk(relaxed = true)
        syncGallery = mockk()
        repository = mockk(relaxed = true)
        previewCache = mockk(relaxed = true)
        uploadScheduler = mockk(relaxed = true)
        uploads = mockk(relaxed = true)
        coEvery { syncGallery() } returns GallerySyncResult.Synced(0)
        useCase = GalleryAutoDownloadUseCase(
            downloadScheduler,
            videoScheduler,
            settings,
            syncScheduler,
            syncGallery,
            repository,
            previewCache,
            uploadScheduler,
            uploads,
        )
    }

    @Test
    fun `onAppForeground schedules the periodic sync and syncs now`() = runTest {
        useCase.onAppForeground()

        verify(exactly = 1) { syncScheduler.schedulePeriodic() }
        coVerify(exactly = 1) { syncGallery() }
    }

    @Test
    fun `onLoginSuccess replaces the download work, schedules the periodic sync and syncs`() = runTest {
        useCase.onLoginSuccess()

        verify(exactly = 1) { downloadScheduler.enqueueWifiOnly(replaceExisting = true) }
        verify(exactly = 1) { syncScheduler.schedulePeriodic() }
        coVerify(exactly = 1) { syncGallery() }
    }

    @Test
    fun `downloadAfterDelete lifts the pause, then syncs`() = runTest {
        useCase.downloadAfterDelete()

        coVerifyOrder {
            repository.setSyncPaused(false)
            syncGallery()
        }
    }

    @Test
    fun `downloadAfterDelete with only photos keeps videos off and photos unpaused`() = runTest {
        settings.photoDownloadsPaused.value = true

        useCase.downloadAfterDelete(photos = true, videos = false)

        assertFalse(settings.photoDownloadsPaused.value)
        assertFalse(settings.autoDownloadVideos.value)
        verify(exactly = 1) { downloadScheduler.enqueueWifiOnly(replaceExisting = true) }
        verify(exactly = 0) { videoScheduler.enqueue(any()) }
    }

    @Test
    fun `downloadAfterDelete with videos turns the switch on and queues them, photos stay paused`() = runTest {
        useCase.downloadAfterDelete(photos = false, videos = true)

        assertTrue(settings.autoDownloadVideos.value)
        assertTrue(settings.photoDownloadsPaused.value)
        verify(exactly = 1) { videoScheduler.enqueue(replaceExisting = true) }
        verify(exactly = 0) { downloadScheduler.enqueueWifiOnly(any()) }
    }

    @Test
    fun `downloadAfterDelete with mobile data sends the photos on any network`() = runTest {
        useCase.downloadAfterDelete(photos = true, videos = false, useMobileData = true)

        verify(exactly = 1) { downloadScheduler.enqueueAnyNetwork() }
    }

    @Test
    fun `clearOnLogout cancels the video downloads and keeps the video switch`() = runTest {
        settings.autoDownloadVideos.value = true

        useCase.clearOnLogout()

        verify(exactly = 1) { videoScheduler.cancel() }
        assertTrue(settings.autoDownloadVideos.value)
    }

    @Test
    fun `clearOnLogout keeps the pause of a deleted gallery`() = runTest {
        useCase.clearOnLogout()

        coVerify(exactly = 0) { repository.setSyncPaused(any()) }
    }

    @Test
    fun `enqueueAnyNetwork delegates to the scheduler`() {
        useCase.enqueueAnyNetwork()

        verify(exactly = 1) { downloadScheduler.enqueueAnyNetwork() }
    }

    @Test
    fun `clearOnLogout stops the upload queue first, then the other works, the gallery and the previews`() = runTest {
        useCase.clearOnLogout()

        coVerifyOrder {
            uploadScheduler.cancel()
            uploads.clear()
            downloadScheduler.cancel()
            syncScheduler.cancel()
            repository.clear()
            previewCache.clear()
        }
    }
}
