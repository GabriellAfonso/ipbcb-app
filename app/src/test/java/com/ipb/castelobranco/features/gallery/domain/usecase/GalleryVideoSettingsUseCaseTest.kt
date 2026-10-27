package com.ipb.castelobranco.features.gallery.domain.usecase

import com.ipb.castelobranco.features.gallery.domain.FakeGalleryMediaSettings
import com.ipb.castelobranco.features.gallery.domain.download.GalleryVideoDownloadScheduler
import io.mockk.mockk
import io.mockk.verify
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class GalleryVideoSettingsUseCaseTest {

    private val settings = FakeGalleryMediaSettings()
    private val scheduler: GalleryVideoDownloadScheduler = mockk(relaxed = true)
    private val useCase = GalleryVideoSettingsUseCase(settings, scheduler)

    @Test
    fun `turning the switch on queues the videos again, off stops them`() = runTest {
        useCase.setAutoDownload(true)
        assertTrue(settings.autoDownloadVideos.value)
        verify(exactly = 1) { scheduler.enqueue(replaceExisting = true) }

        useCase.setAutoDownload(false)
        assertFalse(settings.autoDownloadVideos.value)
        verify(exactly = 1) { scheduler.cancel() }
    }

    @Test
    fun `Nao obrigado hides the card and leaves the switch off`() = runTest {
        useCase.dismissCard()

        assertTrue(settings.videoCardDismissed.value)
        assertFalse(settings.autoDownloadVideos.value)
        verify(exactly = 0) { scheduler.enqueue(any()) }
    }

    @Test
    fun `Baixar hides the card and turns the switch on`() = runTest {
        useCase.acceptCard()

        assertTrue(settings.videoCardDismissed.value)
        assertTrue(settings.autoDownloadVideos.value)
        verify(exactly = 1) { scheduler.enqueue(replaceExisting = true) }
    }
}
