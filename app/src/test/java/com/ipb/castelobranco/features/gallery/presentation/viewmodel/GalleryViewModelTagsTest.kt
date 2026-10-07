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
import com.ipb.castelobranco.features.gallery.data.galleryAlbum
import com.ipb.castelobranco.features.gallery.data.galleryPhoto
import com.ipb.castelobranco.features.gallery.domain.manage.FakeGalleryManageRepository
import com.ipb.castelobranco.features.gallery.domain.model.GalleryIndex
import com.ipb.castelobranco.features.gallery.domain.model.GalleryLocalState
import com.ipb.castelobranco.features.gallery.domain.model.GalleryMember
import com.ipb.castelobranco.features.gallery.domain.model.GalleryPhoto
import com.ipb.castelobranco.features.gallery.domain.model.GallerySyncResult
import com.ipb.castelobranco.features.gallery.domain.model.GallerySyncStatus
import com.ipb.castelobranco.features.gallery.domain.repository.GalleryRepository
import com.ipb.castelobranco.features.gallery.domain.usecase.SyncGalleryUseCase
import com.ipb.castelobranco.features.gallery.presentation.state.GalleryDialogState
import com.ipb.castelobranco.features.gallery.presentation.state.GalleryMessage
import com.ipb.castelobranco.features.gallery.presentation.state.PeoplePickerState
import com.ipb.castelobranco.features.gallery.presentation.state.PickerMode
import com.ipb.castelobranco.features.gallery.presentation.state.ViewerSource
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
class GalleryViewModelTagsTest {

    private val dispatcher = StandardTestDispatcher()
    private val localState = MutableStateFlow(GalleryLocalState.EMPTY)
    private val access = FakeAccessRepository()
    private val manage = FakeGalleryManageRepository()
    private lateinit var viewModel: GalleryViewModel

    private val manager = accessOf(Role.MEDIA, Scope.GALLERY to AccessLevel.MANAGE)

    private val ana = GalleryMember(1, "Ana")
    private val joao = GalleryMember(2, "João")
    private val bruno = GalleryMember(3, "Bruno")

    private fun photo(id: Long, albumId: Long, position: Int, vararg people: GalleryMember): GalleryPhoto =
        galleryPhoto(id, albumId = albumId, position = position).copy(members = people.toList())

    /** Album 1 (root): 10 (Ana), 11 (João, Ana), 12 (nobody); album 2 (root, after 1): 20 (Ana). */
    private val photos = listOf(
        photo(10, albumId = 1, position = 0, ana),
        photo(11, albumId = 1, position = 1, joao, ana),
        photo(12, albumId = 1, position = 2),
        photo(20, albumId = 2, position = 0, ana),
    )

    @Before
    fun setup() {
        Dispatchers.setMain(dispatcher)
        val repository = mockk<GalleryRepository>(relaxed = true) {
            every { localState } returns this@GalleryViewModelTagsTest.localState
            every { syncStatus } returns MutableStateFlow(GallerySyncStatus(hasAnswered = true))
        }
        val syncGallery = mockk<SyncGalleryUseCase> {
            coEvery { this@mockk.invoke() } returns GallerySyncResult.Synced(0)
        }
        viewModel = GalleryViewModel(
            repository = repository,
            syncGallery = syncGallery,
            autoDownload = mockk(relaxed = true),
            manage = manageUseCases(manage, FakeUploadRepository(), repository),
            observeAccess = ObserveAccessUseCase(access),
            connectivityObserver = mockk<NetworkConnectivityObserver> { every { isOnWifi } returns flowOf(true) },
            workManager = mockk<WorkManager> {
                every { getWorkInfosForUniqueWorkFlow(any()) } returns MutableStateFlow<List<WorkInfo>>(emptyList())
            },
            previewLoader = mockk(relaxed = true),
            defaultDispatcher = dispatcher,
        )
        publish(photos)
        manage.taggableResult = Result.success(listOf(ana, bruno, joao))
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    private fun publish(photos: List<GalleryPhoto>) {
        val albums = listOf(galleryAlbum(1), galleryAlbum(2, position = 1))
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

    private fun TestScope.picker(): PeoplePickerState? {
        advanceUntilIdle()
        return (viewModel.dialog.value as? GalleryDialogState.PeoplePicker)?.picker
    }

    private fun TestScope.message(): GalleryMessage? {
        advanceUntilIdle()
        return viewModel.message.value
    }

    private fun TestScope.asManager() {
        access.state.value = manager
        advanceUntilIdle()
    }

    // region root entries

    @Test
    fun `people entry shows once the gallery is on the device`() = runTest(dispatcher) {
        val root = observe(viewModel.rootState)
        assertTrue(root().showPeople)
    }

    @Test
    fun `no people entry while organizing`() = runTest(dispatcher) {
        asManager()
        val root = observe(viewModel.rootState)

        viewModel.startOrganize(null)

        assertFalse(root().showPeople)
    }

    // endregion

    // region viewer details

    @Test
    fun `viewer photos carry the people in the photo's order, the album and the date`() = runTest(dispatcher) {
        publish(photos + photo(13, albumId = 1, position = 3).copy(description = "Culto", dateTaken = "2026-03-14"))
        val viewer = observe(viewModel.viewerState(1, 11))

        val byId = viewer().photos.associateBy { it.id }
        assertEquals(listOf("João", "Ana"), byId.getValue(11).people)
        assertEquals(1L, byId.getValue(11).albumId)
        assertNull(byId.getValue(11).description)
        assertEquals("Culto", byId.getValue(13).description)
        assertEquals("14/03/2026", byId.getValue(13).dateTaken)
    }

    // endregion

    // region tag one photo

    @Test
    fun `the picker lists the photo's people first, checked, then everyone else`() = runTest(dispatcher) {
        asManager()

        viewModel.openTagPhoto(11)

        val picker = picker()!!
        assertEquals(PickerMode.Photo(11), picker.mode)
        assertEquals(listOf("João", "Ana", "Bruno"), picker.people.map { it.name })
        assertEquals(setOf(1L, 2L), picker.checked)
        assertFalse(picker.isLoading)
    }

    @Test
    fun `without manage the picker does not open`() = runTest(dispatcher) {
        viewModel.openTagPhoto(11)

        assertNull(picker())
    }

    @Test
    fun `saving sends the full set and says so`() = runTest(dispatcher) {
        asManager()
        viewModel.openTagPhoto(11)
        picker()

        viewModel.togglePickerPerson(3)
        viewModel.savePicker()
        advanceUntilIdle()

        assertEquals(listOf("setMembers:11:[1, 2, 3]"), manage.calls)
        assertNull(picker())
        assertEquals(TagTexts.SAVED, message()?.text)
    }

    @Test
    fun `saving an unchanged set sends nothing and closes`() = runTest(dispatcher) {
        asManager()
        viewModel.openTagPhoto(11)
        picker()

        viewModel.togglePickerPerson(3)
        viewModel.togglePickerPerson(3)
        viewModel.savePicker()
        advanceUntilIdle()

        assertTrue(manage.calls.isEmpty())
        assertNull(picker())
        assertNull(message())
    }

    @Test
    fun `a 404 keeps the picker open, reloads it and shows the gone-meanwhile text`() = runTest(dispatcher) {
        asManager()
        viewModel.openTagPhoto(11)
        picker()
        manage.tagFailures += AppError.Server(code = 404)

        viewModel.togglePickerPerson(3)
        viewModel.savePicker()
        advanceUntilIdle()

        val picker = picker()!!
        assertFalse(picker.isSaving)
        assertEquals(2, manage.taggableReads)
        assertEquals(TagTexts.NOT_FOUND, message()?.text)
    }

    @Test
    fun `a failed list read shows the error, and retry reads again`() = runTest(dispatcher) {
        asManager()
        manage.taggableResult = Result.failure(AppError.Network(userMessage = "Sem conexão"))

        viewModel.openTagPhoto(11)
        assertEquals("Sem conexão", picker()!!.error)
        assertFalse(picker()!!.canConfirm)

        manage.taggableResult = Result.success(listOf(ana))
        viewModel.retryPickerLoad()
        assertNull(picker()!!.error)
        assertEquals(2, manage.taggableReads)
    }

    @Test
    fun `losing manage closes the picker`() = runTest(dispatcher) {
        asManager()
        viewModel.openTagPhoto(11)
        picker()

        access.state.value = accessOf(null)

        assertNull(picker())
    }

    // endregion

    // region tag many photos

    @Test
    fun `remove lists only the people of the selected photos, without the network`() = runTest(dispatcher) {
        asManager()
        val album = observe(viewModel.albumState(1))
        viewModel.onPhotoLongPress(1, 10)
        viewModel.togglePhotoSelection(1, 12)
        assertTrue(album().canRemovePeople)

        viewModel.openBulkTag(1, remove = true)

        val picker = picker()!!
        assertEquals(PickerMode.Remove(1, listOf(10L, 12L)), picker.mode)
        assertEquals(listOf("Ana"), picker.people.map { it.name })
        assertTrue(picker.checked.isEmpty())
        assertFalse(picker.canConfirm)
        assertEquals(0, manage.taggableReads)
    }

    @Test
    fun `remove is not offered when nobody is tagged in the selection`() = runTest(dispatcher) {
        asManager()
        val album = observe(viewModel.albumState(1))
        viewModel.onPhotoLongPress(1, 12)
        assertFalse(album().canRemovePeople)

        viewModel.openBulkTag(1, remove = true)

        assertNull(picker())
    }

    @Test
    fun `bulk remove sends the removals and ends the selection`() = runTest(dispatcher) {
        asManager()
        val album = observe(viewModel.albumState(1))
        viewModel.onPhotoLongPress(1, 10)
        viewModel.togglePhotoSelection(1, 11)
        viewModel.openBulkTag(1, remove = true)
        picker()

        viewModel.togglePickerPerson(1)
        viewModel.savePicker()
        advanceUntilIdle()

        assertEquals(Triple(listOf(10L, 11L), emptyList<Long>(), listOf(1L)), manage.memberChanges.single())
        assertNull(picker())
        assertTrue(album().selection.isEmpty())
        assertEquals("Marcações atualizadas em 2 fotos", message()?.text)
    }

    @Test
    fun `bulk add starts with nobody checked and sends the additions`() = runTest(dispatcher) {
        asManager()
        observe(viewModel.albumState(1))
        viewModel.onPhotoLongPress(1, 12)
        viewModel.openBulkTag(1, remove = false)
        assertTrue(picker()!!.checked.isEmpty())
        assertEquals(1, manage.taggableReads)

        viewModel.togglePickerPerson(3)
        viewModel.savePicker()
        advanceUntilIdle()

        assertEquals(Triple(listOf(12L), listOf(3L), emptyList<Long>()), manage.memberChanges.single())
        assertEquals("Marcações atualizadas em 1 foto", message()?.text)
    }

    @Test
    fun `bulk 404 keeps the selection and the picker, which reloads`() = runTest(dispatcher) {
        asManager()
        val album = observe(viewModel.albumState(1))
        viewModel.onPhotoLongPress(1, 10)
        viewModel.openBulkTag(1, remove = false)
        picker()
        manage.tagFailures += AppError.Server(code = 404)

        viewModel.togglePickerPerson(3)
        viewModel.savePicker()
        advanceUntilIdle()

        assertEquals(TagTexts.NOT_FOUND, message()?.text)
        assertEquals(2, manage.taggableReads)
        assertFalse(picker()!!.isSaving)
        assertEquals(setOf(10L), album().selection)
    }

    // endregion

    // region viewer over a filter result

    @Test
    fun `a viewer opened from a filter pages through the result, not the album`() = runTest(dispatcher) {
        val viewer = observe(viewModel.viewerState(ViewerSource.People(setOf(1)), 11))

        assertEquals(listOf(10L, 11L, 20L), viewer().photos.map { it.id })
        assertEquals(1, viewer().currentIndex)

        val both = observe(viewModel.viewerState(ViewerSource.People(setOf(1, 2)), 11))
        assertEquals(listOf(11L), both().photos.map { it.id })
    }

    @Test
    fun `a photo untagged elsewhere leaves the result like a removed photo`() = runTest(dispatcher) {
        val viewer = observe(viewModel.viewerState(ViewerSource.People(setOf(1)), 11))
        viewer()

        publish(listOf(photos[0], photo(11, albumId = 1, position = 1, joao), photos[2], photos[3]))

        assertEquals(listOf(10L, 20L), viewer().photos.map { it.id })
        assertEquals(20L, viewer().photos[viewer().currentIndex].id)
        assertEquals(GalleryMessage.PhotoLeftResult, message())
    }

    @Test
    fun `a photo deleted from the result is reported as removed`() = runTest(dispatcher) {
        val viewer = observe(viewModel.viewerState(ViewerSource.People(setOf(1)), 20))
        viewer()

        publish(photos.filterNot { it.id == 20L })

        assertEquals(11L, viewer().photos[viewer().currentIndex].id)
        assertEquals(GalleryMessage.PhotoRemoved, message())
    }

    @Test
    fun `the last photo leaving the result closes the viewer`() = runTest(dispatcher) {
        val viewer = observe(viewModel.viewerState(ViewerSource.People(setOf(2)), 11))
        viewer()

        publish(photos.filterNot { it.id == 11L })

        assertTrue(viewer().isClosed)
    }

    @Test
    fun `untagging from the result's own viewer says the save, not that it left`() = runTest(dispatcher) {
        asManager()
        manage.onSuccess = { call ->
            if (call == "setMembers") publish(listOf(photos[0], photo(11, albumId = 1, position = 1, ana), photos[2]))
        }
        val viewer = observe(viewModel.viewerState(ViewerSource.People(setOf(2)), 11))
        viewer()
        viewModel.openTagPhoto(11)
        picker()

        viewModel.togglePickerPerson(2)
        viewModel.savePicker()
        advanceUntilIdle()

        assertTrue(viewer().isClosed)
        assertEquals(TagTexts.SAVED, message()?.text)
    }

    // endregion
}
