package com.ipb.castelobranco.features.gallery.domain.usecase

import com.ipb.castelobranco.features.gallery.domain.download.GalleryDownloadScheduler
import com.ipb.castelobranco.features.gallery.domain.model.GallerySyncResult
import com.ipb.castelobranco.features.gallery.domain.repository.GalleryPreviewCache
import com.ipb.castelobranco.features.gallery.domain.repository.GalleryRepository
import com.ipb.castelobranco.features.gallery.domain.sync.GallerySyncScheduler
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.coVerifyOrder
import io.mockk.mockk
import io.mockk.verify
import kotlinx.coroutines.test.runTest
import org.junit.Before
import org.junit.Test

class GalleryAutoDownloadUseCaseTest {

    private lateinit var downloadScheduler: GalleryDownloadScheduler
    private lateinit var syncScheduler: GallerySyncScheduler
    private lateinit var syncGallery: SyncGalleryUseCase
    private lateinit var repository: GalleryRepository
    private lateinit var previewCache: GalleryPreviewCache
    private lateinit var useCase: GalleryAutoDownloadUseCase

    @Before
    fun setup() {
        downloadScheduler = mockk(relaxed = true)
        syncScheduler = mockk(relaxed = true)
        syncGallery = mockk()
        repository = mockk(relaxed = true)
        previewCache = mockk(relaxed = true)
        coEvery { syncGallery() } returns GallerySyncResult.Synced(0)
        useCase = GalleryAutoDownloadUseCase(downloadScheduler, syncScheduler, syncGallery, repository, previewCache)
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
    fun `enqueueAnyNetwork delegates to the scheduler`() {
        useCase.enqueueAnyNetwork()

        verify(exactly = 1) { downloadScheduler.enqueueAnyNetwork() }
    }

    @Test
    fun `clearOnLogout cancels both works, then clears the gallery and the preview cache`() = runTest {
        useCase.clearOnLogout()

        coVerifyOrder {
            downloadScheduler.cancel()
            syncScheduler.cancel()
            repository.clear()
            previewCache.clear()
        }
    }
}
