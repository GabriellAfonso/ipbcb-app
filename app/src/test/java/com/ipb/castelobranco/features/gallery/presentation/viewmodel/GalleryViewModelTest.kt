package com.ipb.castelobranco.features.gallery.presentation.viewmodel

import androidx.work.WorkInfo
import androidx.work.WorkManager
import androidx.work.workDataOf
import com.ipb.castelobranco.core.data.NetworkConnectivityObserver
import com.ipb.castelobranco.core.domain.access.ObserveAccessUseCase
import com.ipb.castelobranco.core.domain.error.AppError
import com.ipb.castelobranco.core.testing.FakeAccessRepository
import com.ipb.castelobranco.features.gallery.data.coverUrl
import com.ipb.castelobranco.features.gallery.data.galleryAlbum
import com.ipb.castelobranco.features.gallery.data.galleryPhoto
import com.ipb.castelobranco.features.gallery.data.work.GalleryDownloadWorker
import com.ipb.castelobranco.features.gallery.domain.manage.FakeGalleryManageRepository
import com.ipb.castelobranco.features.gallery.domain.model.GalleryAlbum
import com.ipb.castelobranco.features.gallery.domain.model.GalleryIndex
import com.ipb.castelobranco.features.gallery.domain.model.GalleryLocalState
import com.ipb.castelobranco.features.gallery.domain.model.GalleryPhoto
import com.ipb.castelobranco.features.gallery.domain.model.GallerySyncResult
import com.ipb.castelobranco.features.gallery.domain.model.GallerySyncStatus
import com.ipb.castelobranco.features.gallery.domain.repository.GalleryRepository
import com.ipb.castelobranco.features.gallery.domain.usecase.GalleryAutoDownloadUseCase
import com.ipb.castelobranco.features.gallery.domain.usecase.SyncGalleryUseCase
import com.ipb.castelobranco.features.gallery.presentation.state.GalleryMessage
import com.ipb.castelobranco.features.gallery.presentation.state.PhotoImage
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.TestScope
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

    private val dispatcher = StandardTestDispatcher()

    private val localState = MutableStateFlow(GalleryLocalState.EMPTY)
    private val syncStatus = MutableStateFlow(GallerySyncStatus())
    private val workInfos = MutableStateFlow<List<WorkInfo>>(emptyList())

    private lateinit var repository: GalleryRepository
    private lateinit var syncGallery: SyncGalleryUseCase
    private lateinit var autoDownload: GalleryAutoDownloadUseCase
    private lateinit var viewModel: GalleryViewModel

    @Before
    fun setup() {
        Dispatchers.setMain(dispatcher)
        repository = mockk(relaxed = true)
        every { repository.localState } returns localState
        every { repository.syncStatus } returns syncStatus
        syncGallery = mockk()
        coEvery { syncGallery() } returns GallerySyncResult.Synced(0)
        autoDownload = mockk(relaxed = true)
        val connectivity = mockk<NetworkConnectivityObserver> { every { isOnWifi } returns flowOf(true) }
        val workManager = mockk<WorkManager> {
            every { getWorkInfosForUniqueWorkFlow(any()) } returns workInfos
        }
        viewModel = GalleryViewModel(
            repository = repository,
            syncGallery = syncGallery,
            autoDownload = autoDownload,
            manage = manageUseCases(FakeGalleryManageRepository(), FakeUploadRepository(), repository),
            observeAccess = ObserveAccessUseCase(FakeAccessRepository()),
            connectivityObserver = connectivity,
            workManager = workManager,
            previewLoader = mockk(relaxed = true),
            defaultDispatcher = dispatcher,
        )
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    private fun publish(
        albums: List<GalleryAlbum>,
        photos: List<GalleryPhoto> = emptyList(),
        originals: Map<Long, File> = emptyMap(),
        covers: Map<String, File> = emptyMap(),
    ) {
        localState.value = GalleryLocalState(
            GalleryIndex(albums.associateBy { it.id }, photos.associateBy { it.id }, "c"),
            originals,
            covers,
        )
    }

    /** Keeps [flow] collected, as a screen would, and returns its latest value on demand. */
    private fun <T> TestScope.observe(flow: StateFlow<T>): () -> T {
        backgroundScope.launch { flow.collect {} }
        return { advanceUntilIdle(); flow.value }
    }

    // region root

    @Test
    fun `opening the gallery syncs`() = runTest(dispatcher) {
        advanceUntilIdle()

        coVerify(exactly = 1) { syncGallery() }
    }

    @Test
    fun `root is loading until the first sync answers`() = runTest(dispatcher) {
        val root = observe(viewModel.rootState)

        assertTrue(root().isLoading)

        syncStatus.value = GallerySyncStatus(hasAnswered = true, lastError = AppError.Network())
        assertFalse(root().isLoading)
        assertEquals(GalleryUiMapper.NO_CONNECTION_MESSAGE, root().syncError)
    }

    @Test
    fun `root shows root albums ordered, with cover file or none`() = runTest(dispatcher) {
        val cover = File("cover.jpg")
        publish(
            albums = listOf(
                galleryAlbum(1, position = 1, coverUrl = coverUrl("a")),
                galleryAlbum(2, position = 0),
                galleryAlbum(3, parentId = 1),
            ),
            covers = mapOf(coverUrl("a") to cover),
        )
        val root = observe(viewModel.rootState)

        assertEquals(listOf(2L, 1L), root().albums.map { it.id })
        assertNull(root().albums[0].cover)
        assertEquals(cover, root().albums[1].cover)
        assertFalse(root().isLoading)
    }

    @Test
    fun `403 with a local copy - notice above the grid, no error state`() = runTest(dispatcher) {
        publish(listOf(galleryAlbum(1)))
        syncStatus.value = GallerySyncStatus(hasAnswered = true, lastError = AppError.Auth(403))
        val root = observe(viewModel.rootState)

        assertTrue(root().showMembersOnlyNotice)
        assertNull(root().syncError)
    }

    @Test
    fun `403 with no local copy - error with code 403`() = runTest(dispatcher) {
        syncStatus.value = GallerySyncStatus(hasAnswered = true, lastError = AppError.Auth(403))
        val root = observe(viewModel.rootState)

        assertEquals(403, root().syncErrorCode)
        assertEquals(GalleryUiMapper.NO_ACCESS_MESSAGE, root().syncError)
    }

    @Test
    fun `download work progress is mapped to the banner state`() = runTest(dispatcher) {
        val info = mockk<WorkInfo> {
            every { state } returns WorkInfo.State.RUNNING
            every { progress } returns workDataOf(
                GalleryDownloadWorker.KEY_DOWNLOADED to 3,
                GalleryDownloadWorker.KEY_TOTAL to 10,
            )
        }
        workInfos.value = listOf(info)
        val root = observe(viewModel.rootState)

        assertTrue(root().download.isDownloading)
        assertEquals(3, root().download.downloaded)
        assertEquals(10, root().download.total)
    }

    @Test
    fun `download actions delegate to the use case`() {
        viewModel.downloadWithMobileData()
        viewModel.retryDownload()

        verify { autoDownload.enqueueAnyNetwork() }
        verify { autoDownload.enqueueWifiOnly(replaceExisting = true) }
    }

    // endregion

    // region album

    @Test
    fun `album shows title, parent subtitle, date, description, sub-albums then photos in order`() =
        runTest(dispatcher) {
            publish(
                albums = listOf(
                    galleryAlbum(1, name = "Eventos"),
                    galleryAlbum(2, parentId = 1, name = "Retiro", eventDate = "2026-03-14", description = "Serra"),
                    galleryAlbum(4, parentId = 2, position = 1),
                    galleryAlbum(3, parentId = 2, position = 0),
                ),
                photos = listOf(
                    galleryPhoto(21, albumId = 2, position = 1),
                    galleryPhoto(20, albumId = 2, position = 0),
                ),
            )
            val album = observe(viewModel.albumState(2))

            with(album()) {
                assertEquals("Retiro", title)
                assertEquals("Eventos", subtitle)
                assertEquals("14/03/2026", eventDate)
                assertEquals("Serra", description)
                assertEquals(listOf(3L, 4L), subAlbums.map { it.id })
                assertEquals(listOf(20L, 21L), photos.map { it.id })
                assertFalse(isEmpty)
            }
        }

    @Test
    fun `root album has no subtitle and an empty album says so`() = runTest(dispatcher) {
        publish(listOf(galleryAlbum(1)))
        val album = observe(viewModel.albumState(1))

        assertNull(album().subtitle)
        assertTrue(album().isEmpty)
    }

    @Test
    fun `photo image - original, else preview, else none`() = runTest(dispatcher) {
        val file = File("10.jpg")
        publish(
            albums = listOf(galleryAlbum(1)),
            photos = listOf(
                galleryPhoto(10, position = 0),
                galleryPhoto(11, position = 1, thumbnailUrl = "https://t/11.jpg"),
                galleryPhoto(12, position = 2),
            ),
            originals = mapOf(10L to file),
        )
        val album = observe(viewModel.albumState(1))

        assertEquals(
            listOf(PhotoImage.Original(file), PhotoImage.Preview("https://t/11.jpg"), PhotoImage.None),
            album().photos.map { it.image },
        )
    }

    @Test
    fun `album deleted while open - removed flag and message`() = runTest(dispatcher) {
        publish(listOf(galleryAlbum(1), galleryAlbum(2, parentId = 1)))
        val album = observe(viewModel.albumState(2))
        assertFalse(album().isRemoved)

        publish(listOf(galleryAlbum(1)))

        assertTrue(album().isRemoved)
        assertEquals(GalleryMessage.AlbumRemoved, viewModel.message.value)
        viewModel.consumeMessage()
        assertNull(viewModel.message.value)
    }

    // endregion

    // region viewer

    private val albumPhotos = listOf(
        galleryPhoto(10, position = 0, name = "a.jpg"),
        galleryPhoto(11, position = 1, name = "b.png"),
        galleryPhoto(12, position = 2, name = "c.jpg"),
    )

    @Test
    fun `viewer opens on the tapped photo with names without extension`() = runTest(dispatcher) {
        publish(listOf(galleryAlbum(1)), albumPhotos, originals = mapOf(11L to File("11.png")))
        val viewer = observe(viewModel.viewerState(1, 11))

        assertEquals(1, viewer().currentIndex)
        assertEquals(listOf("a", "b", "c"), viewer().photos.map { it.title })
        assertTrue(viewer().photos[1].canSaveOrShare)
        assertFalse(viewer().photos[0].canSaveOrShare)
    }

    @Test
    fun `photo on screen deleted - next photo and removed message`() = runTest(dispatcher) {
        publish(listOf(galleryAlbum(1)), albumPhotos)
        val viewer = observe(viewModel.viewerState(1, 11))
        viewer()

        publish(listOf(galleryAlbum(1)), albumPhotos.filter { it.id != 11L })

        assertEquals(12L, viewer().photos[viewer().currentIndex].id)
        assertEquals(GalleryMessage.PhotoRemoved, viewModel.message.value)
    }

    @Test
    fun `last photo deleted - previous photo`() = runTest(dispatcher) {
        publish(listOf(galleryAlbum(1)), albumPhotos)
        val viewer = observe(viewModel.viewerState(1, 12))
        viewer()

        publish(listOf(galleryAlbum(1)), albumPhotos.filter { it.id != 12L })

        assertEquals(11L, viewer().photos[viewer().currentIndex].id)
    }

    @Test
    fun `follows the page the user moved to`() = runTest(dispatcher) {
        publish(listOf(galleryAlbum(1)), albumPhotos)
        val viewer = observe(viewModel.viewerState(1, 10))
        viewer()
        viewModel.onPageChanged(albumId = 1, openedPhotoId = 10, currentPhotoId = 11)

        publish(listOf(galleryAlbum(1)), albumPhotos.filter { it.id != 11L })

        assertEquals(12L, viewer().photos[viewer().currentIndex].id)
    }

    @Test
    fun `only photo deleted - viewer closes`() = runTest(dispatcher) {
        publish(listOf(galleryAlbum(1)), listOf(galleryPhoto(10)))
        val viewer = observe(viewModel.viewerState(1, 10))
        viewer()

        publish(listOf(galleryAlbum(1)))

        assertTrue(viewer().isClosed)
    }

    @Test
    fun `photo moved to another album - moved message`() = runTest(dispatcher) {
        publish(listOf(galleryAlbum(1), galleryAlbum(2)), albumPhotos)
        val viewer = observe(viewModel.viewerState(1, 11))
        viewer()

        val moved = albumPhotos.map { if (it.id == 11L) it.copy(albumId = 2) else it }
        publish(listOf(galleryAlbum(1), galleryAlbum(2)), moved)

        assertEquals(12L, viewer().photos[viewer().currentIndex].id)
        assertEquals(GalleryMessage.PhotoMoved, viewModel.message.value)
    }

    // endregion
}
