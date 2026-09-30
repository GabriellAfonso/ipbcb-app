package com.ipb.castelobranco.features.gallery.domain.manage

import com.ipb.castelobranco.core.domain.error.AppError
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class GalleryManageUseCasesTest {

    private val repository = FakeGalleryManageRepository()

    // region names

    @Test
    fun `names are trimmed and limited to 100 characters`() {
        assertEquals(NameCheck.Valid("Retiro"), GalleryNames.validate("  Retiro  "))
        assertEquals(NameCheck.Empty, GalleryNames.validate(""))
        assertEquals(NameCheck.Empty, GalleryNames.validate("   "))
        assertEquals(NameCheck.Valid("a".repeat(100)), GalleryNames.validate("a".repeat(100)))
        assertEquals(NameCheck.TooLong, GalleryNames.validate("a".repeat(101)))
    }

    @Test
    fun `create and edit send trimmed values`() = runTest {
        CreateAlbumUseCase(repository)(AlbumDraft("  Retiro ", " ", null, null))

        assertEquals(listOf("create:Retiro"), repository.calls)
    }

    // endregion

    // region reorder

    private val reorder = ReorderUseCase(repository)

    private fun draft(albums: List<Long>, photos: List<Long>, albumId: Long? = 7) = OrderDraft(
        albumId = albumId,
        albumIds = albums,
        photoIds = photos,
        originalAlbumIds = listOf(1, 2),
        originalPhotoIds = listOf(10, 11),
    )

    @Test
    fun `nothing changed - no request`() = runTest {
        assertEquals(ReorderResult.Unchanged, reorder(draft(listOf(1, 2), listOf(10, 11))))
        assertTrue(repository.calls.isEmpty())
    }

    @Test
    fun `only the photos changed - one request, for the photos`() = runTest {
        assertEquals(ReorderResult.Saved, reorder(draft(listOf(1, 2), listOf(11, 10))))
        assertEquals(listOf("orderPhotos:7:[11, 10]"), repository.calls)
    }

    @Test
    fun `both changed - albums first, then photos`() = runTest {
        reorder(draft(listOf(2, 1), listOf(11, 10)))

        assertEquals(listOf("orderAlbums:7:[2, 1]", "orderPhotos:7:[11, 10]"), repository.calls)
    }

    @Test
    fun `the root orders its albums with a null parent`() = runTest {
        reorder(draft(listOf(2, 1), emptyList(), albumId = null).copy(originalPhotoIds = emptyList()))

        assertEquals(listOf("orderAlbums:null:[2, 1]"), repository.calls)
    }

    @Test
    fun `a mismatch is order changed, another failure is failed`() = runTest {
        repository.orderFailure = AppError.Server(code = 400, extras = mapOf("missing" to "[4]"))
        assertEquals(ReorderResult.OrderChanged, reorder(draft(listOf(2, 1), listOf(10, 11))))

        val boom = AppError.Server(code = 500)
        repository.orderFailure = boom
        assertEquals(ReorderResult.Failed(boom), reorder(draft(listOf(2, 1), listOf(10, 11))))
    }

    // endregion

    // region batches

    @Test
    fun `delete photos - every photo attempted in order, failures kept, one sync`() = runTest {
        val boom = AppError.Server(code = 500)
        repository.failures[11] = boom

        val result = DeletePhotosUseCase(repository)(listOf(10, 11, 12))

        assertEquals(
            listOf("deletePhoto:10:false", "deletePhoto:11:false", "deletePhoto:12:false"),
            repository.calls,
        )
        assertEquals(BatchResult(succeeded = 2, failures = listOf(boom)), result)
        assertEquals(1, repository.syncs)
    }

    @Test
    fun `a lost level stops the batch and reports the rest with it`() = runTest {
        val forbidden = AppError.Auth(code = 403, userMessage = "Sem permissão.")
        repository.failures[11] = forbidden

        val result = MovePhotosUseCase(repository)(listOf(10, 11, 12, 13), targetAlbumId = 5)

        assertEquals(listOf("editPhoto:10:false", "editPhoto:11:false"), repository.calls)
        assertEquals(1, result.succeeded)
        assertEquals(listOf(forbidden, forbidden, forbidden), result.failures)
        assertEquals(4, result.total)
    }

    @Test
    fun `a batch where nothing succeeded does not sync`() = runTest {
        repository.failures[10] = AppError.Server(code = 500)

        MovePhotosUseCase(repository)(listOf(10), targetAlbumId = 5)

        assertEquals(0, repository.syncs)
    }

    // endregion
}
