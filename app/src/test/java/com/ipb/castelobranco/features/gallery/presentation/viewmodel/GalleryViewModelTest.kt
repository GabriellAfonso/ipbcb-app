package com.ipb.castelobranco.features.gallery.presentation.viewmodel

import androidx.work.WorkInfo
import androidx.work.WorkManager
import androidx.work.workDataOf
import com.ipb.castelobranco.core.data.NetworkConnectivityObserver
import com.ipb.castelobranco.features.gallery.data.work.GalleryDownloadWorker
import com.ipb.castelobranco.features.gallery.domain.model.Album
import com.ipb.castelobranco.features.gallery.domain.repository.GalleryRepository
import com.ipb.castelobranco.features.gallery.domain.usecase.GalleryAutoDownloadUseCase
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import java.io.File

@OptIn(ExperimentalCoroutinesApi::class)
class GalleryViewModelTest {

    private val testDispatcher = StandardTestDispatcher()

    private lateinit var repository: GalleryRepository
    private lateinit var autoDownload: GalleryAutoDownloadUseCase
    private lateinit var connectivityObserver: NetworkConnectivityObserver
    private lateinit var workManager: WorkManager
    private lateinit var viewModel: GalleryViewModel

    @Before
    fun setup() {
        Dispatchers.setMain(testDispatcher)

        repository = mockk()
        autoDownload = mockk(relaxed = true)
        connectivityObserver = mockk()
        workManager = mockk()

        every { repository.albumsFlow } returns MutableStateFlow(emptyList())
        every { repository.thumbnailsFlow } returns MutableStateFlow(emptyMap())
        every { connectivityObserver.isOnWifi } returns flowOf(false)
        every { workManager.getWorkInfosForUniqueWorkFlow(any()) } returns flowOf(emptyList())

        viewModel = GalleryViewModel(repository, autoDownload, connectivityObserver, workManager)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    // region albums / thumbnails exposure

    @Test
    fun `albums exposes repository albumsFlow`() = runTest {
        val albums = listOf(Album(1L, "Conferência 2024"), Album(2L, "Culto Especial"))
        every { repository.albumsFlow } returns MutableStateFlow(albums)
        viewModel = GalleryViewModel(repository, autoDownload, connectivityObserver, workManager)
        advanceUntilIdle()
        assertEquals(albums, viewModel.albums.value)
    }

    @Test
    fun `thumbnails exposes repository thumbnailsFlow`() = runTest {
        val file = mockk<File>()
        val thumbnails = mapOf(1L to file, 2L to null)
        every { repository.thumbnailsFlow } returns MutableStateFlow(thumbnails)
        viewModel = GalleryViewModel(repository, autoDownload, connectivityObserver, workManager)
        advanceUntilIdle()
        assertEquals(thumbnails, viewModel.thumbnails.value)
    }

    // endregion

    // region downloadState

    @Test
    fun `downloadState defaults when no work info is available`() = runTest {
        every { workManager.getWorkInfosForUniqueWorkFlow(any()) } returns flowOf(emptyList())
        viewModel = GalleryViewModel(repository, autoDownload, connectivityObserver, workManager)
        advanceUntilIdle()
        val state = viewModel.downloadState.value
        assertFalse(state.isDownloading)
        assertFalse(state.isPending)
        assertNull(state.error)
    }

    @Test
    fun `downloadState initial value has isResolved false before flow collects`() {
        assertFalse(viewModel.downloadState.value.isResolved)
    }

    @Test
    fun `downloadState maps RUNNING WorkInfo to isDownloading with progress`() = runTest {
        val progress = workDataOf(
            GalleryDownloadWorker.KEY_DOWNLOADED to 3,
            GalleryDownloadWorker.KEY_TOTAL to 10
        )
        val workInfo = mockk<WorkInfo>()
        every { workInfo.state } returns WorkInfo.State.RUNNING
        every { workInfo.progress } returns progress
        every { workManager.getWorkInfosForUniqueWorkFlow(any()) } returns flowOf(listOf(workInfo))
        viewModel = GalleryViewModel(repository, autoDownload, connectivityObserver, workManager)

        val job = launch { viewModel.downloadState.collect { } }
        advanceUntilIdle()
        job.cancel()

        val state = viewModel.downloadState.value
        assertTrue(state.isDownloading)
        assertEquals(3, state.downloaded)
        assertEquals(10, state.total)
        assertTrue(state.isResolved)
    }

    @Test
    fun `downloadState maps ENQUEUED WorkInfo to isPending with isResolved`() = runTest {
        val workInfo = mockk<WorkInfo>()
        every { workInfo.state } returns WorkInfo.State.ENQUEUED
        every { workInfo.runAttemptCount } returns 0
        every { workManager.getWorkInfosForUniqueWorkFlow(any()) } returns flowOf(listOf(workInfo))
        viewModel = GalleryViewModel(repository, autoDownload, connectivityObserver, workManager)

        val job = launch { viewModel.downloadState.collect { } }
        advanceUntilIdle()
        job.cancel()

        val state = viewModel.downloadState.value
        assertTrue(state.isPending)
        assertFalse(state.isDownloading)
        assertTrue(state.isResolved)
    }

    @Test
    fun `downloadState maps ENQUEUED after a failed attempt to isResuming without error`() = runTest {
        val workInfo = mockk<WorkInfo>()
        every { workInfo.state } returns WorkInfo.State.ENQUEUED
        every { workInfo.runAttemptCount } returns 2
        every { workManager.getWorkInfosForUniqueWorkFlow(any()) } returns flowOf(listOf(workInfo))
        viewModel = GalleryViewModel(repository, autoDownload, connectivityObserver, workManager)

        val job = launch { viewModel.downloadState.collect { } }
        advanceUntilIdle()
        job.cancel()

        val state = viewModel.downloadState.value
        assertTrue(state.isResuming)
        assertFalse(state.isPending)
        assertNull(state.error)
        assertTrue(state.isResolved)
    }

    @Test
    fun `downloadState maps FAILED WorkInfo with 401 to error state with errorCode`() = runTest {
        val outputData = workDataOf(
            GalleryDownloadWorker.KEY_ERROR to "Não autorizado",
            GalleryDownloadWorker.KEY_ERROR_CODE to 401
        )
        val workInfo = mockk<WorkInfo>()
        every { workInfo.state } returns WorkInfo.State.FAILED
        every { workInfo.outputData } returns outputData
        every { workManager.getWorkInfosForUniqueWorkFlow(any()) } returns flowOf(listOf(workInfo))
        viewModel = GalleryViewModel(repository, autoDownload, connectivityObserver, workManager)

        val job = launch { viewModel.downloadState.collect { } }
        advanceUntilIdle()
        job.cancel()

        val state = viewModel.downloadState.value
        assertEquals("Não autorizado", state.error)
        assertEquals(401, state.errorCode)
        assertTrue(state.isResolved)
    }

    @Test
    fun `downloadState maps FAILED 403 with albums on the device - errorCode kept, albums still exposed`() = runTest {
        val albums = listOf(Album(1L, "Culto"))
        every { repository.albumsFlow } returns MutableStateFlow(albums)
        val outputData = workDataOf(
            GalleryDownloadWorker.KEY_ERROR to "Disponível apenas para membros.",
            GalleryDownloadWorker.KEY_ERROR_CODE to 403
        )
        val workInfo = mockk<WorkInfo>()
        every { workInfo.state } returns WorkInfo.State.FAILED
        every { workInfo.outputData } returns outputData
        every { workManager.getWorkInfosForUniqueWorkFlow(any()) } returns flowOf(listOf(workInfo))
        viewModel = GalleryViewModel(repository, autoDownload, connectivityObserver, workManager)

        val job = launch { viewModel.downloadState.collect { } }
        advanceUntilIdle()
        job.cancel()

        val state = viewModel.downloadState.value
        assertEquals(403, state.errorCode)
        assertEquals("Disponível apenas para membros.", state.error)
        assertEquals(albums, viewModel.albums.value)
    }

    @Test
    fun `downloadState maps FAILED WorkInfo with errorCode 0 to null errorCode`() = runTest {
        val outputData = workDataOf(
            GalleryDownloadWorker.KEY_ERROR to "Falha genérica",
            GalleryDownloadWorker.KEY_ERROR_CODE to 0
        )
        val workInfo = mockk<WorkInfo>()
        every { workInfo.state } returns WorkInfo.State.FAILED
        every { workInfo.outputData } returns outputData
        every { workManager.getWorkInfosForUniqueWorkFlow(any()) } returns flowOf(listOf(workInfo))
        viewModel = GalleryViewModel(repository, autoDownload, connectivityObserver, workManager)

        val job = launch { viewModel.downloadState.collect { } }
        advanceUntilIdle()
        job.cancel()

        assertNull(viewModel.downloadState.value.errorCode)
    }

    @Test
    fun `downloadState uses default error message when KEY_ERROR is absent in FAILED state`() = runTest {
        val outputData = workDataOf(GalleryDownloadWorker.KEY_ERROR_CODE to 500)
        val workInfo = mockk<WorkInfo>()
        every { workInfo.state } returns WorkInfo.State.FAILED
        every { workInfo.outputData } returns outputData
        every { workManager.getWorkInfosForUniqueWorkFlow(any()) } returns flowOf(listOf(workInfo))
        viewModel = GalleryViewModel(repository, autoDownload, connectivityObserver, workManager)

        val job = launch { viewModel.downloadState.collect { } }
        advanceUntilIdle()
        job.cancel()

        assertEquals("Falha ao baixar galeria", viewModel.downloadState.value.error)
    }

    @Test
    fun `downloadState maps other WorkInfo states to resolved with no flags set`() = runTest {
        val workInfo = mockk<WorkInfo>()
        every { workInfo.state } returns WorkInfo.State.SUCCEEDED
        every { workManager.getWorkInfosForUniqueWorkFlow(any()) } returns flowOf(listOf(workInfo))
        viewModel = GalleryViewModel(repository, autoDownload, connectivityObserver, workManager)

        val job = launch { viewModel.downloadState.collect { } }
        advanceUntilIdle()
        job.cancel()

        val state = viewModel.downloadState.value
        assertFalse(state.isDownloading)
        assertFalse(state.isPending)
        assertNull(state.error)
        assertTrue(state.isResolved)
    }

    // endregion

    // region isOnWifi

    @Test
    fun `isOnWifi is false when connectivity observer emits false`() = runTest {
        every { connectivityObserver.isOnWifi } returns flowOf(false)
        viewModel = GalleryViewModel(repository, autoDownload, connectivityObserver, workManager)
        advanceUntilIdle()
        assertFalse(viewModel.isOnWifi.value)
    }

    @Test
    fun `isOnWifi is true when connectivity observer emits true`() = runTest {
        every { connectivityObserver.isOnWifi } returns flowOf(true)
        viewModel = GalleryViewModel(repository, autoDownload, connectivityObserver, workManager)

        val job = launch { viewModel.isOnWifi.collect { } }
        advanceUntilIdle()
        job.cancel()

        assertTrue(viewModel.isOnWifi.value)
    }

    // endregion

    // region downloadAllPhotos / downloadWithMobileData

    @Test
    fun `downloadAllPhotos delegates to autoDownload enqueueWifiOnly`() = runTest {
        viewModel.downloadAllPhotos()
        verify { autoDownload.enqueueWifiOnly() }
    }

    @Test
    fun `downloadWithMobileData delegates to autoDownload enqueueAnyNetwork`() = runTest {
        viewModel.downloadWithMobileData()
        verify { autoDownload.enqueueAnyNetwork() }
    }

    @Test
    fun `retryDownload replaces the existing work`() = runTest {
        viewModel.retryDownload()
        // REPLACE é o que descarta o WorkInfo com o erro anterior
        verify { autoDownload.enqueueWifiOnly(replaceExisting = true) }
    }

    // endregion

    // region clearGallery

    @Test
    fun `clearGallery delegates to repository clearAllPhotos`() = runTest {
        coEvery { repository.clearAllPhotos() } returns Unit
        viewModel.clearGallery()
        advanceUntilIdle()
        coVerify { repository.clearAllPhotos() }
    }

    // endregion

    // region getLocalPhotos / getPhotoName

    @Test
    fun `getLocalPhotos delegates to repository`() = runTest {
        val photos = listOf(mockk<File>())
        coEvery { repository.getLocalPhotos(1L) } returns photos
        val result = viewModel.getLocalPhotos(1L)
        assertEquals(photos, result)
    }

    @Test
    fun `getPhotoName returns repository value when found`() = runTest {
        coEvery { repository.getPhotoName(1L, 42L) } returns "Batismo João"
        val result = viewModel.getPhotoName(1L, 42L)
        assertEquals("Batismo João", result)
    }

    @Test
    fun `getPhotoName returns Foto when repository returns null`() = runTest {
        coEvery { repository.getPhotoName(1L, 99L) } returns null
        val result = viewModel.getPhotoName(1L, 99L)
        assertEquals("Foto", result)
    }

    // endregion
}
