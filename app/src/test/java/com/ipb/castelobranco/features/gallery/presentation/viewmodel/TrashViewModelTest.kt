package com.ipb.castelobranco.features.gallery.presentation.viewmodel

import app.cash.turbine.test
import com.ipb.castelobranco.core.domain.access.AccessLevel
import com.ipb.castelobranco.core.domain.access.ObserveAccessUseCase
import com.ipb.castelobranco.core.domain.access.Role
import com.ipb.castelobranco.core.domain.access.Scope
import com.ipb.castelobranco.core.domain.error.AppError
import com.ipb.castelobranco.core.testing.FakeAccessRepository
import com.ipb.castelobranco.core.testing.accessOf
import com.ipb.castelobranco.features.gallery.data.galleryAlbum
import com.ipb.castelobranco.features.gallery.domain.manage.FakeGalleryManageRepository
import com.ipb.castelobranco.features.gallery.domain.model.GalleryIndex
import com.ipb.castelobranco.features.gallery.domain.model.GalleryLocalState
import com.ipb.castelobranco.features.gallery.domain.repository.GalleryRepository
import com.ipb.castelobranco.features.gallery.domain.trash.LoadTrashUseCase
import com.ipb.castelobranco.features.gallery.domain.trash.RestoreTrashItemUseCase
import com.ipb.castelobranco.features.gallery.domain.trash.TrashEntry
import com.ipb.castelobranco.features.gallery.domain.trash.TrashKey
import com.ipb.castelobranco.features.gallery.domain.trash.TrashKind
import com.ipb.castelobranco.features.gallery.presentation.state.TrashEvent
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.CompletableDeferred
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
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class TrashViewModelTest {

    private val dispatcher = StandardTestDispatcher()
    private val manage = FakeGalleryManageRepository()
    private val access = FakeAccessRepository(accessOf(Role.ADMIN, Scope.GALLERY to AccessLevel.OWNER))
    private val localState = MutableStateFlow(GalleryLocalState.EMPTY)

    private val album = entry(TrashKind.ALBUM, 7)
    private val photo = entry(TrashKind.PHOTO, 301)

    @Before
    fun setup() {
        Dispatchers.setMain(dispatcher)
        manage.trashResult = Result.success(listOf(album, photo))
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    private fun viewModel(): TrashViewModel {
        val gallery = mockk<GalleryRepository> { every { localState } returns this@TrashViewModelTest.localState }
        return TrashViewModel(
            loadTrash = LoadTrashUseCase(manage),
            restoreItem = RestoreTrashItemUseCase(manage, gallery),
            observeAccess = ObserveAccessUseCase(access),
            previewLoader = mockk(relaxed = true),
        )
    }

    private fun badRequest(vararg extras: Pair<String, String>) =
        AppError.Server(code = 400, userMessage = "detalhe do servidor", extras = extras.toMap())

    // region list

    @Test
    fun `loads on open in the server order`() = runTest(dispatcher) {
        val viewModel = viewModel()
        assertTrue(viewModel.state.value.isLoading)

        advanceUntilIdle()

        val state = viewModel.state.value
        assertFalse(state.isLoading)
        assertEquals(listOf(album.key, photo.key), state.rows.map { it.key })
        assertFalse(state.isEmpty)
    }

    @Test
    fun `empty trash`() = runTest(dispatcher) {
        manage.trashResult = Result.success(emptyList())
        val viewModel = viewModel()
        advanceUntilIdle()

        assertTrue(viewModel.state.value.isEmpty)
    }

    @Test
    fun `offline read shows the error, retry loads again`() = runTest(dispatcher) {
        manage.trashResult = Result.failure(AppError.Network())
        val viewModel = viewModel()
        advanceUntilIdle()
        assertEquals(TrashTexts.OFFLINE, viewModel.state.value.error)
        assertFalse(viewModel.state.value.isEmpty)

        manage.trashResult = Result.success(listOf(photo))
        viewModel.retry()
        advanceUntilIdle()

        assertNull(viewModel.state.value.error)
        assertEquals(listOf(photo.key), viewModel.state.value.rows.map { it.key })
    }

    @Test
    fun `a failed refresh keeps the rows and says why`() = runTest(dispatcher) {
        val viewModel = viewModel()
        advanceUntilIdle()
        manage.trashResult = Result.failure(AppError.Network())

        viewModel.events.test {
            viewModel.refresh()
            advanceUntilIdle()
            assertEquals(TrashEvent.Message(TrashTexts.OFFLINE), awaitItem())
        }
        assertEquals(2, viewModel.state.value.rows.size)
        assertNull(viewModel.state.value.error)
    }

    @Test
    fun `losing owner closes the screen`() = runTest(dispatcher) {
        val viewModel = viewModel()
        advanceUntilIdle()

        viewModel.events.test {
            access.state.value = accessOf(Role.MEDIA, Scope.GALLERY to AccessLevel.MANAGE)
            advanceUntilIdle()
            assertEquals(TrashEvent.Close(TrashTexts.ACCESS_LOST), awaitItem())
        }
    }

    // endregion

    // region restore

    @Test
    fun `restore success removes the row and says so`() = runTest(dispatcher) {
        val viewModel = viewModel()
        advanceUntilIdle()

        viewModel.events.test {
            viewModel.restore(photo.key)
            advanceUntilIdle()
            assertEquals(TrashEvent.Message("Foto restaurada"), awaitItem())
        }
        assertEquals(listOf(album.key), viewModel.state.value.rows.map { it.key })
        assertNull(viewModel.state.value.restoring)
        assertEquals("restore:PHOTO:301", manage.calls.single())
    }

    @Test
    fun `one restore at a time, no refresh meanwhile`() = runTest(dispatcher) {
        val viewModel = viewModel()
        advanceUntilIdle()
        val reads = manage.trashReads
        val gate = CompletableDeferred<Unit>()
        manage.restoreGate = gate

        viewModel.restore(album.key)
        advanceUntilIdle()
        viewModel.restore(photo.key)
        viewModel.refresh()
        advanceUntilIdle()

        assertEquals(album.key, viewModel.state.value.restoring)
        assertFalse(viewModel.state.value.canRefresh)
        assertEquals(listOf("restore:ALBUM:7"), manage.calls)
        assertEquals(reads, manage.trashReads)

        gate.complete(Unit)
        advanceUntilIdle()
        assertNull(viewModel.state.value.restoring)
    }

    @Test
    fun `404 reloads the list and says the item left the trash`() = runTest(dispatcher) {
        val viewModel = viewModel()
        advanceUntilIdle()
        manage.restoreFailures[album.key] = AppError.Server(code = 404)
        manage.trashResult = Result.success(listOf(photo))

        viewModel.events.test {
            viewModel.restore(album.key)
            advanceUntilIdle()
            assertEquals(TrashEvent.Message(TrashTexts.NOT_IN_TRASH), awaitItem())
        }
        assertEquals(2, manage.trashReads)
        assertEquals(listOf(photo.key), viewModel.state.value.rows.map { it.key })
    }

    @Test
    fun `other errors keep the list unchanged`() = runTest(dispatcher) {
        val viewModel = viewModel()
        advanceUntilIdle()
        manage.restoreFailures[photo.key] = AppError.Network(userMessage = "Sem conexão")

        viewModel.events.test {
            viewModel.restore(photo.key)
            advanceUntilIdle()
            assertEquals(TrashEvent.Message("Sem conexão"), awaitItem())
        }
        assertEquals(2, viewModel.state.value.rows.size)
        assertEquals(1, manage.trashReads)
    }

    // endregion

    // region refusals

    @Test
    fun `trashed parent in the list is highlighted`() = runTest(dispatcher) {
        val sub = entry(TrashKind.ALBUM, 9)
        manage.trashResult = Result.success(listOf(sub, album))
        val viewModel = viewModel()
        advanceUntilIdle()
        manage.restoreFailures[sub.key] = badRequest("kind" to "album", "id" to "9", "trashed_parent_id" to "7")

        viewModel.events.test {
            viewModel.restore(sub.key)
            advanceUntilIdle()
            assertEquals(TrashEvent.Message("detalhe do servidor"), awaitItem())
        }
        assertEquals(TrashKey(TrashKind.ALBUM, 7), viewModel.state.value.highlighted)
    }

    @Test
    fun `trashed parent not in the list shows only the detail`() = runTest(dispatcher) {
        val viewModel = viewModel()
        advanceUntilIdle()
        manage.restoreFailures[photo.key] = badRequest("kind" to "photo", "id" to "301", "trashed_parent_id" to "50")

        viewModel.events.test {
            viewModel.restore(photo.key)
            advanceUntilIdle()
            assertEquals(TrashEvent.Message("detalhe do servidor"), awaitItem())
        }
        assertNull(viewModel.state.value.highlighted)
    }

    @Test
    fun `name conflict exposes the conflicting album to open`() = runTest(dispatcher) {
        val index = GalleryIndex(mapOf(3L to galleryAlbum(3)), emptyMap(), "c")
        localState.value = GalleryLocalState(index, emptyMap(), emptyMap())
        val viewModel = viewModel()
        advanceUntilIdle()
        manage.restoreFailures[album.key] =
            badRequest("album_id" to "7", "name" to "Culto", "conflicting_album_id" to "3")

        viewModel.events.test {
            viewModel.restore(album.key)
            advanceUntilIdle()
            assertEquals(TrashEvent.Message("detalhe do servidor", openAlbumId = 3), awaitItem())
        }
        assertEquals(2, viewModel.state.value.rows.size)
    }

    @Test
    fun `name conflict with an album the device does not hold offers no button`() = runTest(dispatcher) {
        val viewModel = viewModel()
        advanceUntilIdle()
        manage.restoreFailures[album.key] = badRequest("conflicting_album_id" to "3")

        viewModel.events.test {
            viewModel.restore(album.key)
            advanceUntilIdle()
            assertEquals(TrashEvent.Message("detalhe do servidor", openAlbumId = null), awaitItem())
        }
        assertEquals(1, manage.syncs)
    }

    // endregion

    private companion object {
        fun entry(kind: TrashKind, id: Long) = TrashEntry(
            kind = kind,
            id = id,
            name = "item $id",
            deletedAt = "2026-09-29T14:03:11Z",
            deletedBy = null,
            uploadedBy = null,
            purgeOn = "2026-10-29",
            subAlbumCount = 0,
            photoCount = 0,
            thumbnailUrl = null,
        )
    }
}
