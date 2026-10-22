package com.ipb.castelobranco.features.gallery.domain.usecase

import com.ipb.castelobranco.features.gallery.domain.download.GalleryDownloadScheduler
import com.ipb.castelobranco.features.gallery.domain.repository.GalleryPreviewCache
import com.ipb.castelobranco.features.gallery.domain.repository.GalleryRepository
import io.mockk.coVerify
import io.mockk.coVerifyOrder
import io.mockk.mockk
import io.mockk.verify
import kotlinx.coroutines.test.runTest
import org.junit.Test

class DeleteGalleryUseCaseTest {

    private val repository: GalleryRepository = mockk(relaxed = true)
    private val downloadScheduler: GalleryDownloadScheduler = mockk(relaxed = true)
    private val previewCache: GalleryPreviewCache = mockk(relaxed = true)
    private val useCase = DeleteGalleryUseCase(repository, downloadScheduler, previewCache)

    @Test
    fun `invoke cancels the download and pauses before clearing the gallery and the previews`() = runTest {
        useCase()

        coVerifyOrder {
            downloadScheduler.cancel()
            repository.setSyncPaused(true)
            repository.clear()
            previewCache.clear()
        }
    }

    @Test
    fun `invoke never syncs or enqueues a download`() = runTest {
        useCase()

        coVerify(exactly = 0) { repository.sync() }
        verify(exactly = 0) { downloadScheduler.enqueueWifiOnly(any()) }
        verify(exactly = 0) { downloadScheduler.enqueueAnyNetwork() }
    }
}
