package com.ipb.castelobranco.features.gallery.presentation.viewmodel

import androidx.work.WorkInfo
import androidx.work.WorkManager
import com.ipb.castelobranco.core.data.NetworkConnectivityObserver
import com.ipb.castelobranco.core.domain.access.AccessLevel
import com.ipb.castelobranco.core.domain.access.ObserveAccessUseCase
import com.ipb.castelobranco.core.domain.access.Role
import com.ipb.castelobranco.core.domain.access.Scope
import com.ipb.castelobranco.core.domain.error.AppError
import com.ipb.castelobranco.core.testing.FakeAccessRepository
import com.ipb.castelobranco.core.testing.accessOf
import com.ipb.castelobranco.features.gallery.data.coverUrl
import com.ipb.castelobranco.features.gallery.data.galleryAlbum
import com.ipb.castelobranco.features.gallery.data.galleryPhoto
import com.ipb.castelobranco.features.gallery.domain.manage.FakeGalleryManageRepository
import com.ipb.castelobranco.features.gallery.domain.model.GalleryAlbum
import com.ipb.castelobranco.features.gallery.domain.model.GalleryIndex
import com.ipb.castelobranco.features.gallery.domain.model.GalleryLocalState
import com.ipb.castelobranco.features.gallery.domain.model.GalleryPhoto
import com.ipb.castelobranco.features.gallery.domain.model.GallerySyncResult
import com.ipb.castelobranco.features.gallery.domain.model.GallerySyncStatus
import com.ipb.castelobranco.features.gallery.domain.model.TreeTarget
import com.ipb.castelobranco.features.gallery.domain.repository.GalleryRepository
import com.ipb.castelobranco.features.gallery.domain.upload.UploadItem
import com.ipb.castelobranco.features.gallery.domain.upload.UploadState
import com.ipb.castelobranco.features.gallery.domain.usecase.SyncGalleryUseCase
import com.ipb.castelobranco.features.gallery.presentation.components.GalleryGridKeys
import com.ipb.castelobranco.features.gallery.presentation.state.ConfirmAction
import com.ipb.castelobranco.features.gallery.presentation.state.FailedUpload
import com.ipb.castelobranco.features.gallery.presentation.state.FormTarget
import com.ipb.castelobranco.features.gallery.presentation.state.GalleryDialogState
import com.ipb.castelobranco.features.gallery.presentation.state.GalleryMessage
import com.ipb.castelobranco.features.gallery.presentation.state.GalleryPermissions
import io.mockk.coEvery
import io.mockk.every
import io.mockk.mockk
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

@OptIn(ExperimentalCoroutinesApi::class)
class GalleryViewModelManageTest {

    private val dispatcher = StandardTestDispatcher()
    private val localState = MutableStateFlow(GalleryLocalState.EMPTY)
    private val access = FakeAccessRepository()
    private val manage = FakeGalleryManageRepository()
    private val uploads = FakeUploadRepository()
    private lateinit var viewModel: GalleryViewModel

    private val manager = accessOf(Role.MEDIA, Scope.GALLERY to AccessLevel.MANAGE)
    private val owner = accessOf(Role.ADMIN, Scope.GALLERY to AccessLevel.OWNER)

    /**
     * Root 1 "Retiro" (own cover) with sub-album 2 "Sábado" and photos 10, 11, 12;
     * sub-album 2 holds photo 13; root 3 "Culto" (no cover).
     */
    private val albums = listOf(
        galleryAlbum(1, name = "Retiro", coverUrl = coverUrl("r")),
        galleryAlbum(2, parentId = 1, name = "Sábado"),
        galleryAlbum(3, position = 1, name = "Culto"),
    )
    private val photos = listOf(
        galleryPhoto(10, albumId = 1, position = 0, name = "IMG_10.jpg"),
        galleryPhoto(11, albumId = 1, position = 1),
        galleryPhoto(12, albumId = 1, position = 2),
        galleryPhoto(13, albumId = 2),
    )

    @Before
    fun setup() {
        Dispatchers.setMain(dispatcher)
        val repository = mockk<GalleryRepository>(relaxed = true) {
            every { localState } returns this@GalleryViewModelManageTest.localState
            every { syncStatus } returns MutableStateFlow(GallerySyncStatus(hasAnswered = true))
        }
        val syncGallery = mockk<SyncGalleryUseCase> {
            coEvery { this@mockk.invoke() } returns GallerySyncResult.Synced(0)
        }
        viewModel = GalleryViewModel(
            repository = repository,
            syncGallery = syncGallery,
            autoDownload = mockk(relaxed = true),
            manage = manageUseCases(manage, uploads),
            observeAccess = ObserveAccessUseCase(access),
            connectivityObserver = mockk<NetworkConnectivityObserver> { every { isOnWifi } returns flowOf(true) },
            workManager = mockk<WorkManager> {
                every { getWorkInfosForUniqueWorkFlow(any()) } returns MutableStateFlow<List<WorkInfo>>(emptyList())
            },
            previewLoader = mockk(relaxed = true),
            defaultDispatcher = dispatcher,
        )
        publish(albums, photos)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    private fun publish(albums: List<GalleryAlbum>, photos: List<GalleryPhoto>) {
        localState.value = GalleryLocalState(
            GalleryIndex(albums.associateBy { it.id }, photos.associateBy { it.id }, "c"),
            emptyMap(),
            emptyMap(),
        )
    }

    private fun <T> TestScope.observe(flow: StateFlow<T>): () -> T {
        backgroundScope.launch { flow.collect {} }
        return { advanceUntilIdle(); flow.value }
    }

    private fun TestScope.dialog(): GalleryDialogState? {
        advanceUntilIdle()
        return viewModel.dialog.value
    }

    private fun TestScope.message(): GalleryMessage? {
        advanceUntilIdle()
        return viewModel.message.value
    }

    // region access

    @Test
    fun `no level - every screen has no permission`() = runTest(dispatcher) {
        val root = observe(viewModel.rootState)
        val album = observe(viewModel.albumState(1))
        val viewer = observe(viewModel.viewerState(1, 10))

        assertEquals(GalleryPermissions.NONE, root().permissions)
        assertEquals(GalleryPermissions.NONE, album().permissions)
        assertEquals(GalleryPermissions.NONE, viewer().permissions)
        assertFalse(album().canRemoveCover)
    }

    @Test
    fun `manage may change but not delete, owner may do both`() = runTest(dispatcher) {
        val album = observe(viewModel.albumState(1))

        access.state.value = manager
        advanceUntilIdle()
        assertEquals(GalleryPermissions(canManage = true, canDelete = false), album().permissions)
        assertFalse(album().canRemoveCover)

        access.state.value = owner
        advanceUntilIdle()
        assertEquals(GalleryPermissions(canManage = true, canDelete = true), album().permissions)
        assertTrue(album().canRemoveCover)
    }

    @Test
    fun `remove cover only for an album whose cover is its own`() = runTest(dispatcher) {
        access.state.value = owner
        advanceUntilIdle()
        val sub = observe(viewModel.albumState(2))

        assertFalse(sub().canRemoveCover)
    }

    @Test
    fun `a lost level hides the controls without reopening the screen`() = runTest(dispatcher) {
        access.state.value = owner
        advanceUntilIdle()
        val viewer = observe(viewModel.viewerState(1, 10))
        assertTrue(viewer().permissions.canDelete)

        access.state.value = accessOf(null)

        assertEquals(GalleryPermissions.NONE, viewer().permissions)
    }

    @Test
    fun `a 403 on a write shows the server message and changes nothing`() = runTest(dispatcher) {
        access.state.value = owner
        advanceUntilIdle()
        manage.failures[1] = AppError.Auth(code = 403, userMessage = "Sem permissão na galeria.")

        viewModel.askDeleteAlbum(1)
        viewModel.confirm()

        assertEquals(GalleryMessage.Text("Sem permissão na galeria."), message())
        assertNull(dialog())
        assertTrue(1L in localState.value.index!!.albums)
    }

    // endregion

    // region forms

    @Test
    fun `new album - blank name refused locally, a valid one created and the form closed`() = runTest(dispatcher) {
        viewModel.openCreateAlbum(1)
        assertEquals(FormTarget.NewAlbum(1), (dialog() as GalleryDialogState.Form).form.target)

        viewModel.saveForm()
        assertEquals(GalleryManageTexts.NAME_EMPTY, (dialog() as GalleryDialogState.Form).form.nameError)
        assertTrue(manage.calls.isEmpty())

        viewModel.onFormNameChange("  Domingo ")
        viewModel.saveForm()

        advanceUntilIdle()
        assertEquals(listOf("create:Domingo"), manage.calls)
        assertNull(dialog())
        assertEquals(GalleryMessage.AlbumCreated, message())
    }

    @Test
    fun `a name over 100 characters is refused locally`() = runTest(dispatcher) {
        viewModel.openCreateAlbum(null)
        viewModel.onFormNameChange("a".repeat(101))
        viewModel.saveForm()

        assertEquals(GalleryManageTexts.NAME_TOO_LONG, (dialog() as GalleryDialogState.Form).form.nameError)
    }

    @Test
    fun `a duplicate name keeps the form open with the server message under the name`() = runTest(dispatcher) {
        manage.failures[2] = AppError.Server(code = 400, errorCode = "VALIDATION_ERROR", userMessage = "Já existe.")

        viewModel.openEditAlbum(2)
        viewModel.saveForm()

        val form = (dialog() as GalleryDialogState.Form).form
        assertEquals("Já existe.", form.nameError)
        assertFalse(form.isSaving)
    }

    @Test
    fun `a second save while saving is ignored`() = runTest(dispatcher) {
        viewModel.openEditAlbum(2)
        viewModel.saveForm()
        viewModel.saveForm()
        advanceUntilIdle()

        advanceUntilIdle()
        assertEquals(listOf("editAlbum:2"), manage.calls)
    }

    @Test
    fun `photo form edits the name without its extension and saves it with it`() = runTest(dispatcher) {
        viewModel.openEditPhoto(10)
        assertEquals("IMG_10", (dialog() as GalleryDialogState.Form).form.name)

        viewModel.onFormNameChange("Batismo")
        viewModel.saveForm()

        advanceUntilIdle()
        assertEquals(listOf("editPhoto:10:true"), manage.calls)
        assertEquals(GalleryMessage.Saved, message())
    }

    // endregion

    // region delete

    @Test
    fun `delete album confirmation counts the whole subtree`() = runTest(dispatcher) {
        viewModel.askDeleteAlbum(1)

        val confirm = (dialog() as GalleryDialogState.Confirm).confirm
        assertEquals("Apagar 'Retiro' com 1 subálbum e 4 fotos? Fica 30 dias na lixeira.", confirm.text)
        assertEquals(ConfirmAction.DeleteAlbum(1), confirm.action)
    }

    @Test
    fun `deleting the open album says it went to the trash, not that it was removed`() = runTest(dispatcher) {
        val album = observe(viewModel.albumState(2))
        album()
        manage.onSuccess = { publish(albums.filterNot { it.id == 2L }, photos.filterNot { it.albumId == 2L }) }

        viewModel.askDeleteAlbum(2)
        viewModel.confirm()

        assertTrue(album().isRemoved)
        assertEquals(GalleryMessage.AlbumTrashed, message())
    }

    @Test
    fun `deleting from the viewer moves to the next photo with its own message`() = runTest(dispatcher) {
        val viewer = observe(viewModel.viewerState(1, 10))
        viewer()
        manage.onSuccess = { publish(albums, photos.filterNot { it.id == 10L }) }

        viewModel.askDeletePhotos(1, photoId = 10)
        assertEquals(GalleryManageTexts.deletePhotos(1), (dialog() as GalleryDialogState.Confirm).confirm.text)
        viewModel.confirm()

        assertEquals(listOf(11L, 12L), viewer().photos.map { it.id })
        assertEquals(GalleryMessage.PhotoTrashed, message())
    }

    @Test
    fun `batch delete reports the partial failure`() = runTest(dispatcher) {
        access.state.value = owner
        advanceUntilIdle()
        val album = observe(viewModel.albumState(1))
        viewModel.onPhotoLongPress(1, 10)
        viewModel.togglePhotoSelection(1, 11)
        assertEquals(setOf(10L, 11L), album().selection)
        manage.failures[11] = AppError.Server(code = 500, userMessage = "Erro no servidor.")

        viewModel.askDeletePhotos(1)
        assertEquals("Apagar 2 fotos? Ficam 30 dias na lixeira.", (dialog() as GalleryDialogState.Confirm).confirm.text)
        viewModel.confirm()

        assertEquals(GalleryMessage.Text("1 de 2 fotos apagadas. 1: Erro no servidor."), message())
        assertTrue(album().selection.isEmpty())
    }

    // endregion

    // region selection

    @Test
    fun `long press needs manage, taps toggle, the last untoggle ends selection`() = runTest(dispatcher) {
        val album = observe(viewModel.albumState(1))

        viewModel.onPhotoLongPress(1, 10)
        assertFalse(album().isSelecting)

        access.state.value = manager
        advanceUntilIdle()
        advanceUntilIdle()
        viewModel.onPhotoLongPress(1, 10)
        viewModel.togglePhotoSelection(1, 12)
        assertEquals(setOf(10L, 12L), album().selection)

        viewModel.togglePhotoSelection(1, 10)
        viewModel.togglePhotoSelection(1, 12)
        assertFalse(album().isSelecting)
    }

    @Test
    fun `a selected photo removed by a sync leaves the selection`() = runTest(dispatcher) {
        access.state.value = manager
        advanceUntilIdle()
        val album = observe(viewModel.albumState(1))
        viewModel.onPhotoLongPress(1, 10)
        viewModel.togglePhotoSelection(1, 11)

        publish(albums, photos.filterNot { it.id == 11L })

        assertEquals(setOf(10L), album().selection)
    }

    // endregion

    // region move

    @Test
    fun `album picker hides the album and its descendants`() = runTest(dispatcher) {
        viewModel.openMoveAlbum(1)

        val targets = (dialog() as GalleryDialogState.MovePicker).picker.targets
        assertEquals(listOf(null, 3L), targets.map { it.albumId })
        assertFalse(targets.first().selectable)
    }

    @Test
    fun `moving from the viewer says where the photo went`() = runTest(dispatcher) {
        val viewer = observe(viewModel.viewerState(1, 10))
        viewer()
        manage.onSuccess = { publish(albums, photos.map { if (it.id == 10L) it.copy(albumId = 3) else it }) }

        viewModel.openMovePhotos(1, photoId = 10)
        viewModel.chooseMoveTarget(TreeTarget(3, "Culto", 0, selectable = true))

        assertEquals(GalleryMessage.PhotoMovedTo("Culto"), message())
        assertEquals(listOf(11L, 12L), viewer().photos.map { it.id })
    }

    @Test
    fun `a batch move reports how many moved`() = runTest(dispatcher) {
        access.state.value = manager
        advanceUntilIdle()
        observe(viewModel.albumState(1))()
        viewModel.onPhotoLongPress(1, 10)
        viewModel.togglePhotoSelection(1, 12)

        viewModel.openMovePhotos(1)
        viewModel.chooseMoveTarget(TreeTarget(3, "Culto", 0, selectable = true))

        advanceUntilIdle()
        assertEquals(listOf("editPhoto:10:false", "editPhoto:12:false"), manage.calls)
        assertEquals(GalleryMessage.Text("2 fotos movidas para 'Culto'"), message())
    }

    // endregion

    // region organize

    @Test
    fun `organize closes selection, ignores cross-group moves, cancel restores order`() = runTest(dispatcher) {
        access.state.value = manager
        advanceUntilIdle()
        val album = observe(viewModel.albumState(1))
        viewModel.onPhotoLongPress(1, 10)

        viewModel.startOrganize(1)
        assertFalse(album().isSelecting)
        assertTrue(album().isOrganizing)

        viewModel.moveOrganizeItem(GalleryGridKeys.photo(12), GalleryGridKeys.photo(10))
        viewModel.moveOrganizeItem(GalleryGridKeys.photo(11), GalleryGridKeys.album(2))
        assertEquals(listOf(12L, 10L, 11L), album().photos.map { it.id })

        viewModel.cancelOrganize()
        assertFalse(album().isOrganizing)
        assertEquals(listOf(10L, 11L, 12L), album().photos.map { it.id })
    }

    @Test
    fun `saving sends only the photos order and closes the mode`() = runTest(dispatcher) {
        val album = observe(viewModel.albumState(1))
        viewModel.startOrganize(1)
        viewModel.moveOrganizeItem(GalleryGridKeys.photo(12), GalleryGridKeys.photo(10))

        viewModel.saveOrganize()

        advanceUntilIdle()
        assertEquals(listOf("orderPhotos:1:[12, 10, 11]"), manage.calls)
        assertFalse(album().isOrganizing)
        assertEquals(GalleryMessage.Saved, message())
    }

    @Test
    fun `a mismatch keeps the mode open with the refreshed list and says so`() = runTest(dispatcher) {
        val album = observe(viewModel.albumState(1))
        viewModel.startOrganize(1)
        viewModel.moveOrganizeItem(GalleryGridKeys.photo(12), GalleryGridKeys.photo(10))
        manage.orderFailure = AppError.Server(code = 400, extras = mapOf("unexpected" to "[14]"))

        viewModel.saveOrganize()
        assertEquals(GalleryMessage.OrderChanged, message())
        assertTrue(album().isOrganizing)

        publish(albums, photos + galleryPhoto(14, albumId = 1, position = 3))

        assertEquals(listOf(10L, 11L, 12L, 14L), album().photos.map { it.id })
        assertTrue(album().isOrganizing)
    }

    @Test
    fun `root organize reorders root albums`() = runTest(dispatcher) {
        val root = observe(viewModel.rootState)
        viewModel.startOrganize(null)
        viewModel.moveOrganizeItem(GalleryGridKeys.album(3), GalleryGridKeys.album(1))

        assertEquals(listOf(3L, 1L), root().albums.map { it.id })
        viewModel.saveOrganize()
        advanceUntilIdle()
        assertEquals(listOf("orderAlbums:null:[3, 1]"), manage.calls)
    }

    // endregion

    // region covers and uploads

    @Test
    fun `remove cover asks first, then says it is done`() = runTest(dispatcher) {
        viewModel.askRemoveCover(1)
        assertEquals("Remover a capa de 'Retiro'?", (dialog() as GalleryDialogState.Confirm).confirm.text)

        viewModel.confirm()

        advanceUntilIdle()
        assertEquals(listOf("removeCover:1"), manage.calls)
        assertEquals(GalleryMessage.CoverRemoved, message())
    }

    @Test
    fun `use as cover sends the photo's original`() = runTest(dispatcher) {
        viewModel.useAsCover(1, 11)

        advanceUntilIdle()
        assertEquals(listOf("original:11", "setCover:1"), manage.calls)
        assertEquals(GalleryMessage.CoverUpdated, message())
    }

    @Test
    fun `album shows its own pending and failed uploads, which can be dismissed`() = runTest(dispatcher) {
        val album = observe(viewModel.albumState(1))
        uploads.items.value = listOf(
            UploadItem("a", 1, "a.jpg", "a.jpg", UploadState.Waiting, enqueuedAt = 1),
            UploadItem("b", 1, "b.jpg", "b.jpg", UploadState.Failed, "O álbum foi apagado", 2),
            UploadItem("c", 3, "c.jpg", "c.jpg", UploadState.Waiting, enqueuedAt = 3),
        )

        assertEquals(1, album().uploads.pending)
        assertEquals(listOf(FailedUpload("b", "b.jpg", "O álbum foi apagado")), album().uploads.failed)

        viewModel.dismissUpload("b")
        assertTrue(album().uploads.failed.isEmpty())
        assertEquals(listOf("b"), uploads.dismissed)
    }

    @Test
    fun `adding photos queues the picked images for the album`() = runTest(dispatcher) {
        viewModel.addPhotos(1, listOf("content://a", "content://b"))
        advanceUntilIdle()

        assertEquals(listOf(1L to listOf("content://a", "content://b")), uploads.enqueued)
    }

    // endregion
}
