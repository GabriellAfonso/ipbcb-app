package com.ipb.castelobranco.features.settings.presentation.viewmodel

import app.cash.turbine.test
import com.ipb.castelobranco.core.data.NetworkConnectivityObserver
import com.ipb.castelobranco.core.domain.gallery.GalleryMissing
import com.ipb.castelobranco.core.domain.gallery.GalleryStoragePort
import com.ipb.castelobranco.core.domain.gallery.GalleryUsage
import com.ipb.castelobranco.core.domain.gallery.GalleryVideoSummary
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class GallerySettingsViewModelTest {

    private val dispatcher = StandardTestDispatcher()
    private val auto = MutableStateFlow(false)
    private val videos = MutableStateFlow(GalleryVideoSummary(total = 12, downloaded = 0, totalBytes = 480L * MB))
    private val wifi = MutableStateFlow(true)
    private lateinit var storage: GalleryStoragePort
    private lateinit var viewModel: GallerySettingsViewModel

    @Before
    fun setup() {
        Dispatchers.setMain(dispatcher)
        storage = mockk(relaxed = true)
        every { storage.autoDownloadVideos } returns auto
        every { storage.videos } returns videos
        coEvery { storage.usage() } returns GalleryUsage(320 * MB, 0)
        coEvery { storage.missing() } returns GalleryMissing(photos = 0, videos = 12, isPaused = false)
        val connectivity = mockk<NetworkConnectivityObserver> { every { isOnWifi } returns wifi }
        viewModel = GallerySettingsViewModel(storage, connectivity)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun `the switch describes what is available, then the progress`() = runTest(dispatcher) {
        viewModel.uiState.test {
            advanceUntilIdle()
            assertEquals("Somente no WiFi · 12 vídeos (480 MB)", expectMostRecentItem().videosDescription)

            auto.value = true
            videos.value = videos.value.copy(downloaded = 8)
            advanceUntilIdle()
            assertEquals("8 de 12 baixados", expectMostRecentItem().videosDescription)
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `turning the switch on goes to the gallery`() = runTest(dispatcher) {
        viewModel.setAutoDownloadVideos(true)
        advanceUntilIdle()

        coVerify { storage.setAutoDownloadVideos(true) }
    }

    @Test
    fun `deleting only videos sends the checked types and says so`() = runTest(dispatcher) {
        viewModel.uiState.test {
            advanceUntilIdle()
            viewModel.openDelete()
            viewModel.toggle(0)
            advanceUntilIdle()
            viewModel.messages.test {
                viewModel.confirm()
                advanceUntilIdle()
                assertEquals("Vídeos apagados", awaitItem())
            }
            assertNull(expectMostRecentItem().dialog)
            cancelAndIgnoreRemainingEvents()
        }
        coVerify(exactly = 1) { storage.delete(photos = false, videos = true) }
    }

    @Test
    fun `download offers videos unchecked and checking them downloads videos`() = runTest(dispatcher) {
        viewModel.uiState.test {
            viewModel.openDownload()
            advanceUntilIdle()
            val dialog = expectMostRecentItem().dialog
            assertTrue(dialog is GalleryStorageDialog.Download)
            viewModel.toggle(1)
            viewModel.confirm()
            advanceUntilIdle()
            cancelAndIgnoreRemainingEvents()
        }
        coVerify(exactly = 1) { storage.download(photos = false, videos = true, useMobileData = false) }
    }

    @Test
    fun `photos off WiFi ask first, and downloading now goes over mobile data`() = runTest(dispatcher) {
        wifi.value = false
        coEvery { storage.missing() } returns GalleryMissing(photos = 5, videos = 0, isPaused = false)
        viewModel.uiState.test {
            viewModel.openDownload()
            advanceUntilIdle()
            viewModel.confirm()
            advanceUntilIdle()
            val dialog = expectMostRecentItem().dialog
            assertTrue(dialog is GalleryStorageDialog.Download && dialog.askingMobileData)
            viewModel.answerMobileData(downloadNow = true)
            advanceUntilIdle()
            cancelAndIgnoreRemainingEvents()
        }
        coVerify(exactly = 1) { storage.download(photos = true, videos = false, useMobileData = true) }
    }

    @Test
    fun `photos off WiFi may wait for WiFi`() = runTest(dispatcher) {
        wifi.value = false
        coEvery { storage.missing() } returns GalleryMissing(photos = 5, videos = 0, isPaused = false)
        viewModel.openDownload()
        advanceUntilIdle()
        viewModel.confirm()
        viewModel.answerMobileData(downloadNow = false)
        advanceUntilIdle()
        coVerify(exactly = 1) { storage.download(photos = true, videos = false, useMobileData = false) }
    }

    private companion object {
        const val MB = 1024L * 1024
    }
}
