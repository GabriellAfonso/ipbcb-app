package com.ipb.castelobranco.features.gallery.domain.usecase

import com.ipb.castelobranco.core.domain.error.AppError
import com.ipb.castelobranco.features.gallery.domain.FakeGalleryMediaSettings
import com.ipb.castelobranco.features.gallery.domain.download.GalleryDownloadScheduler
import com.ipb.castelobranco.features.gallery.domain.download.GalleryVideoDownloadScheduler
import com.ipb.castelobranco.features.gallery.domain.model.GallerySyncResult
import com.ipb.castelobranco.features.gallery.domain.repository.GalleryRepository
import io.mockk.coEvery
import io.mockk.mockk
import io.mockk.verify
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test

class SyncGalleryUseCaseTest {

    private lateinit var repository: GalleryRepository
    private lateinit var scheduler: GalleryDownloadScheduler
    private lateinit var videoScheduler: GalleryVideoDownloadScheduler
    private lateinit var settings: FakeGalleryMediaSettings
    private lateinit var useCase: SyncGalleryUseCase

    @Before
    fun setup() {
        repository = mockk()
        scheduler = mockk(relaxed = true)
        videoScheduler = mockk(relaxed = true)
        settings = FakeGalleryMediaSettings()
        useCase = SyncGalleryUseCase(repository, scheduler, videoScheduler, settings)
    }

    @Test
    fun `missing originals after a sync are queued on WiFi keeping a running download`() = runTest {
        coEvery { repository.sync() } returns GallerySyncResult.Synced(missingOriginals = 3)

        assertEquals(GallerySyncResult.Synced(3), useCase())

        verify(exactly = 1) { scheduler.enqueueWifiOnly(replaceExisting = false) }
    }

    @Test
    fun `nothing missing - nothing queued`() = runTest {
        coEvery { repository.sync() } returns GallerySyncResult.Synced(missingOriginals = 0)

        useCase()

        verify(exactly = 0) { scheduler.enqueueWifiOnly(any()) }
        verify(exactly = 0) { videoScheduler.enqueue(any()) }
    }

    @Test
    fun `failed or skipped sync - nothing queued`() = runTest {
        coEvery { repository.sync() } returnsMany listOf(
            GallerySyncResult.Failed(AppError.Auth(403)),
            GallerySyncResult.Skipped,
        )

        useCase()
        useCase()

        verify(exactly = 0) { scheduler.enqueueWifiOnly(any()) }
    }

    @Test
    fun `missing videos with the switch off - never queued`() = runTest {
        coEvery { repository.sync() } returns GallerySyncResult.Synced(missingOriginals = 0, missingVideos = 2)

        useCase()

        verify(exactly = 0) { videoScheduler.enqueue(any()) }
    }

    @Test
    fun `missing videos with the switch on - queued keeping a running video download`() = runTest {
        settings.autoDownloadVideos.value = true
        coEvery { repository.sync() } returns GallerySyncResult.Synced(missingOriginals = 0, missingVideos = 2)

        useCase()

        verify(exactly = 1) { videoScheduler.enqueue(replaceExisting = false) }
    }

    @Test
    fun `photo downloads paused - missing photos are not queued`() = runTest {
        settings.photoDownloadsPaused.value = true
        coEvery { repository.sync() } returns GallerySyncResult.Synced(missingOriginals = 4)

        useCase()

        verify(exactly = 0) { scheduler.enqueueWifiOnly(any()) }
    }
}
