package com.ipb.castelobranco.features.gallery.presentation.viewmodel

import androidx.work.WorkInfo
import androidx.work.WorkManager
import com.ipb.castelobranco.core.data.NetworkConnectivityObserver
import com.ipb.castelobranco.core.domain.access.ObserveAccessUseCase
import com.ipb.castelobranco.core.domain.access.AccessLevel
import com.ipb.castelobranco.core.domain.access.Role
import com.ipb.castelobranco.core.domain.access.Scope
import com.ipb.castelobranco.core.testing.accessOf
import com.ipb.castelobranco.core.testing.FakeAccessRepository
import com.ipb.castelobranco.features.gallery.data.galleryAlbum
import com.ipb.castelobranco.features.gallery.data.galleryPhoto
import com.ipb.castelobranco.features.gallery.domain.FakeGalleryMediaSettings
import com.ipb.castelobranco.features.gallery.domain.manage.FakeGalleryManageRepository
import com.ipb.castelobranco.features.gallery.domain.model.GalleryAlbum
import com.ipb.castelobranco.features.gallery.domain.model.GalleryIndex
import com.ipb.castelobranco.features.gallery.domain.model.GalleryLocalState
import com.ipb.castelobranco.features.gallery.domain.model.GalleryPhoto
import com.ipb.castelobranco.features.gallery.domain.model.GallerySyncResult
import com.ipb.castelobranco.features.gallery.domain.model.GallerySyncStatus
import com.ipb.castelobranco.features.gallery.domain.model.MediaType
import com.ipb.castelobranco.features.gallery.domain.repository.GalleryRepository
import com.ipb.castelobranco.features.gallery.domain.upload.PickedMedia
import com.ipb.castelobranco.features.gallery.domain.usecase.SyncGalleryUseCase
import com.ipb.castelobranco.features.gallery.presentation.state.GalleryDialogState
import com.ipb.castelobranco.features.gallery.presentation.state.GalleryMessage
import com.ipb.castelobranco.features.gallery.presentation.state.PhotoImage
import com.ipb.castelobranco.features.gallery.presentation.state.ViewerSource
import io.mockk.coEvery
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
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
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import java.io.File

@OptIn(ExperimentalCoroutinesApi::class)
class GalleryViewModelVideoTest {

    private val dispatcher = StandardTestDispatcher()
    private val localState = MutableStateFlow(GalleryLocalState.EMPTY)
    private val onWifi = MutableStateFlow(true)
    private val settings = FakeGalleryMediaSettings()
    private val uploads = FakeUploadRepository()
    private val access = FakeAccessRepository()
    private lateinit var viewModel: GalleryViewModel

    private val albums = listOf(galleryAlbum(1, position = 0), galleryAlbum(2, position = 1))
    private val photo = galleryPhoto(10, albumId = 1, position = 0)
    private val video = galleryPhoto(11, albumId = 1, position = 1)
        .copy(mediaType = MediaType.VIDEO, durationMs = 84_000, fileSize = 40L * MB, thumbnailUrl = "poster")
    private val onlyVideo = galleryPhoto(20, albumId = 2).copy(mediaType = MediaType.VIDEO, fileSize = 10L * MB)

    @Before
    fun setup() {
        Dispatchers.setMain(dispatcher)
        val repository = mockk<GalleryRepository>(relaxed = true) {
            every { localState } returns this@GalleryViewModelVideoTest.localState
            every { syncStatus } returns MutableStateFlow(GallerySyncStatus(hasAnswered = true))
        }
        val syncGallery = mockk<SyncGalleryUseCase> {
            coEvery { this@mockk.invoke() } returns GallerySyncResult.Synced(0)
        }
        viewModel = GalleryViewModel(
            repository = repository,
            syncGallery = syncGallery,
            autoDownload = mockk(relaxed = true),
            manage = manageUseCases(FakeGalleryManageRepository(), uploads, repository),
            videoSettings = videoSettingsUseCase(settings),
            storage = storageUseCase(repository),
            observeAccess = ObserveAccessUseCase(access),
            connectivityObserver = mockk<NetworkConnectivityObserver> { every { isOnWifi } returns onWifi },
            workManager = mockk<WorkManager> {
                every { getWorkInfosForUniqueWorkFlow(any()) } returns MutableStateFlow<List<WorkInfo>>(emptyList())
            },
            previewLoader = mockk(relaxed = true),
            defaultDispatcher = dispatcher,
        )
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    private fun publish(
        items: List<GalleryPhoto> = listOf(photo, video, onlyVideo),
        albums: List<GalleryAlbum> = this.albums,
        videos: Map<Long, File> = emptyMap(),
    ) {
        localState.value = GalleryLocalState(
            GalleryIndex(albums.associateBy { it.id }, items.associateBy { it.id }, "c"),
            originals = emptyMap(),
            covers = emptyMap(),
            videos = videos,
        )
    }

    private fun <T> TestScope.observe(flow: StateFlow<T>): () -> T {
        backgroundScope.launch { flow.collect {} }
        return { advanceUntilIdle(); flow.value }
    }

    @Test
    fun `undownloaded videos and the albums left with only them are hidden`() = runTest(dispatcher) {
        publish()
        val root = observe(viewModel.rootState)
        val album = observe(viewModel.albumState(1))

        assertEquals(listOf(1L), root().albums.map { it.id })
        assertEquals(listOf(10L), album().photos.map { it.id })
    }

    @Test
    fun `a downloaded video shows a frame of the local file, a play badge and its duration`() = runTest(dispatcher) {
        val file = File("11.mp4")
        publish(videos = mapOf(11L to file))
        val album = observe(viewModel.albumState(1))

        val tile = album().photos.single { it.id == 11L }
        assertEquals(PhotoImage.VideoFrame(file), tile.image)
        assertTrue(tile.isVideo)
        assertEquals("1:24", tile.duration)
    }

    @Test
    fun `the viewer pages only through visible items and carries the video file`() = runTest(dispatcher) {
        val file = File("11.mp4")
        publish(videos = mapOf(11L to file))
        val viewer = observe(viewModel.viewerState(ViewerSource.Album(1), 11))

        assertEquals(listOf(10L, 11L), viewer().photos.map { it.id })
        assertEquals(file, viewer().photos[1].file)
        assertEquals("video/mp4", viewer().photos[1].mimeType)
    }

    @Test
    fun `Organizar lists hidden videos and albums dimmed, so the saved order has every id`() = runTest(dispatcher) {
        access.state.value = accessOf(Role.ADMIN, Scope.GALLERY to AccessLevel.OWNER)
        publish()
        val root = observe(viewModel.rootState)
        val album = observe(viewModel.albumState(1))
        album()

        viewModel.startOrganize(null)
        assertEquals(listOf(1L, 2L), root().albums.map { it.id })
        assertFalse(root().albums.single { it.id == 2L }.onDevice)
        viewModel.cancelOrganize()

        viewModel.startOrganize(1)
        val tiles = album().photos
        assertEquals(listOf(10L, 11L), tiles.map { it.id })
        assertFalse(tiles.single { it.id == 11L }.onDevice)
        assertEquals(PhotoImage.Preview("poster"), tiles.single { it.id == 11L }.image)
    }

    @Test
    fun `the move picker lists hidden albums marked as not on the device`() = runTest(dispatcher) {
        access.state.value = accessOf(Role.ADMIN, Scope.GALLERY to AccessLevel.OWNER)
        publish()
        observe(viewModel.rootState)()

        viewModel.openMovePhotos(1, photoId = 10)
        advanceUntilIdle()

        val targets = (viewModel.dialog.value as GalleryDialogState.MovePicker).picker.targets
        assertFalse(targets.single { it.albumId == 2L }.onDevice)
        assertTrue(targets.single { it.albumId == 1L }.onDevice)
    }

    @Test
    fun `the card shows the missing videos and their size while the switch is off`() = runTest(dispatcher) {
        publish()
        val root = observe(viewModel.rootState)

        val card = root().videoCard
        assertNotNull(card)
        card!!
        assertEquals(2, card.count)
        assertEquals("Há 2 vídeos na galeria (50 MB)", card.title)
    }

    @Test
    fun `either card button hides it forever and Nao obrigado points to settings`() = runTest(dispatcher) {
        publish()
        val root = observe(viewModel.rootState)

        viewModel.dismissVideoCard()

        assertNull(root().videoCard)
        assertTrue(settings.videoCardDismissed.value)
        assertEquals(GalleryMessage.Text(VideoTexts.CARD_DISMISSED), viewModel.message.value)
        assertFalse(settings.autoDownloadVideos.value)
    }

    @Test
    fun `Baixar on the card turns the switch on and, off WiFi, says videos wait for it`() = runTest(dispatcher) {
        onWifi.value = false
        publish()
        val root = observe(viewModel.rootState)
        root()

        viewModel.acceptVideoCard()

        assertNull(root().videoCard)
        assertTrue(settings.autoDownloadVideos.value)
        assertEquals(GalleryMessage.Text(VideoTexts.WAITING_WIFI), viewModel.message.value)
    }

    @Test
    fun `videos start muted and the sound choice lasts while the gallery is open`() {
        assertTrue(viewModel.videoMuted.value)

        viewModel.setVideoMuted(false)

        assertFalse(viewModel.videoMuted.value)
    }

    @Test
    fun `picking videos on mobile data asks first, Esperar WiFi holds them`() = runTest(dispatcher) {
        onWifi.value = false
        publish()
        observe(viewModel.rootState)()
        uploads.picked["v"] = PickedMedia("v", "video/mp4", 70L * MB)
        uploads.picked["big"] = PickedMedia("big", "video/mp4", 150L * MB)

        viewModel.addPhotos(1, listOf("p", "v", "big"))
        advanceUntilIdle()

        val ask = viewModel.dialog.value as GalleryDialogState.UploadOnMobileData
        assertEquals(1, ask.videoCount)
        assertEquals(70L * MB, ask.videoBytes)
        assertTrue(uploads.enqueued.isEmpty())

        viewModel.confirmUploadOnMobileData(waitForWifi = true)
        advanceUntilIdle()

        assertEquals(listOf(1L to listOf("p", "v", "big")), uploads.enqueued)
        assertEquals(listOf(true), uploads.wifiOnlyBatches)
    }

    @Test
    fun `photos only, or on WiFi, are queued without asking`() = runTest(dispatcher) {
        publish()
        observe(viewModel.rootState)()
        uploads.picked["v"] = PickedMedia("v", "video/mp4", 70L * MB)

        viewModel.addPhotos(1, listOf("v"))
        advanceUntilIdle()

        assertNull(viewModel.dialog.value)
        assertEquals(listOf(false), uploads.wifiOnlyBatches)
    }

    private companion object {
        const val MB = 1024L * 1024
    }
}
