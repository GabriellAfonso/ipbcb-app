package com.ipb.castelobranco.features.gallery.domain.trash

import com.ipb.castelobranco.core.domain.error.AppError
import com.ipb.castelobranco.features.gallery.data.galleryAlbum
import com.ipb.castelobranco.features.gallery.domain.manage.FakeGalleryManageRepository
import com.ipb.castelobranco.features.gallery.domain.model.GalleryIndex
import com.ipb.castelobranco.features.gallery.domain.model.GalleryLocalState
import com.ipb.castelobranco.features.gallery.domain.repository.GalleryRepository
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class RestoreTrashItemUseCaseTest {

    private val manage = FakeGalleryManageRepository()
    private val localState = MutableStateFlow(GalleryLocalState.EMPTY)
    private val gallery = mockk<GalleryRepository> {
        every { localState } returns this@RestoreTrashItemUseCaseTest.localState
    }
    private val restore = RestoreTrashItemUseCase(manage, gallery)

    private val album = TrashKey(TrashKind.ALBUM, 9)

    private fun holdAlbums(vararg ids: Long) {
        localState.value = GalleryLocalState(
            GalleryIndex(ids.associateWith { galleryAlbum(it) }, emptyMap(), "c"),
            emptyMap(),
            emptyMap(),
        )
    }

    private fun badRequest(vararg extras: Pair<String, String>) =
        AppError.Server(code = 400, userMessage = "detalhe", extras = extras.toMap())

    @Test
    fun `success is Restored`() = runTest {
        assertEquals(RestoreResult.Restored, restore(album))
        assertEquals(listOf("restore:ALBUM:9"), manage.calls)
    }

    @Test
    fun `404 is NotInTrash`() = runTest {
        manage.restoreFailures[album] = AppError.Server(code = 404)

        assertEquals(RestoreResult.NotInTrash, restore(album))
    }

    @Test
    fun `400 with trashed_parent_id names the parent`() = runTest {
        val error = badRequest("kind" to "album", "id" to "9", "trashed_parent_id" to "7")
        manage.restoreFailures[album] = error

        assertEquals(RestoreResult.TrashedParent(7, error), restore(album))
    }

    @Test
    fun `name conflict with the album on the device does not sync`() = runTest {
        holdAlbums(3)
        val error = badRequest("album_id" to "9", "name" to "Culto", "conflicting_album_id" to "3")
        manage.restoreFailures[album] = error

        assertEquals(RestoreResult.NameConflict(3, isOnDevice = true, error = error), restore(album))
        assertEquals(0, manage.syncs)
    }

    @Test
    fun `name conflict with the album missing syncs, then reports whether it arrived`() = runTest {
        val error = badRequest("conflicting_album_id" to "3")
        manage.restoreFailures[album] = error

        assertEquals(RestoreResult.NameConflict(3, isOnDevice = false, error = error), restore(album))
        assertEquals(1, manage.syncs)

        holdAlbums(3)
        assertEquals(RestoreResult.NameConflict(3, isOnDevice = true, error = error), restore(album))
    }

    @Test
    fun `offline and other refusals are Failed`() = runTest {
        val offline = AppError.Network(userMessage = "Sem conexão")
        manage.restoreFailures[album] = offline
        assertEquals(RestoreResult.Failed(offline), restore(album))

        val unreadable = badRequest("trashed_parent_id" to "x")
        manage.restoreFailures[album] = unreadable
        assertTrue(restore(album) is RestoreResult.Failed)
    }
}
