package com.ipb.castelobranco.features.gallery.data.manage

import com.ipb.castelobranco.core.domain.error.AppError
import com.ipb.castelobranco.core.testing.tempDirContext
import com.ipb.castelobranco.features.gallery.data.albumDto
import com.ipb.castelobranco.features.gallery.data.api.FakeGalleryApi
import com.ipb.castelobranco.features.gallery.data.apiError
import com.ipb.castelobranco.features.gallery.data.galleryAlbum
import com.ipb.castelobranco.features.gallery.data.galleryPhoto
import com.ipb.castelobranco.features.gallery.data.local.GalleryMediaStore
import com.ipb.castelobranco.features.gallery.data.photoDto
import com.ipb.castelobranco.features.gallery.data.photoUrl
import com.ipb.castelobranco.features.gallery.domain.manage.AlbumDraft
import com.ipb.castelobranco.features.gallery.domain.manage.AlbumEdit
import com.ipb.castelobranco.features.gallery.domain.manage.Field
import com.ipb.castelobranco.features.gallery.domain.manage.PhotoEdit
import com.ipb.castelobranco.features.gallery.domain.manage.isCycle
import com.ipb.castelobranco.features.gallery.domain.manage.isOrderMismatch
import com.ipb.castelobranco.features.gallery.domain.manage.isValidation
import com.ipb.castelobranco.features.gallery.domain.model.GalleryIndex
import com.ipb.castelobranco.features.gallery.domain.model.GalleryLocalChange
import com.ipb.castelobranco.features.gallery.domain.model.GalleryLocalState
import com.ipb.castelobranco.features.gallery.domain.model.GallerySyncResult
import com.ipb.castelobranco.features.gallery.domain.repository.GalleryRepository
import com.ipb.castelobranco.features.gallery.domain.usecase.SyncGalleryUseCase
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.runTest
import kotlinx.serialization.json.JsonNull
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import retrofit2.Response
import java.io.File

class GalleryManageRepositoryImplTest {

    @get:Rule
    val folder = TemporaryFolder()

    private val dispatcher = StandardTestDispatcher()
    private val api = FakeGalleryApi()
    private val applied = mutableListOf<GalleryLocalChange>()
    private val localState = MutableStateFlow(GalleryLocalState.EMPTY)
    private lateinit var gallery: GalleryRepository
    private lateinit var syncGallery: SyncGalleryUseCase
    private lateinit var media: GalleryMediaStore
    private lateinit var repository: GalleryManageRepositoryImpl

    @Before
    fun setup() {
        gallery = mockk {
            every { localState } returns this@GalleryManageRepositoryImplTest.localState
            coEvery { applyLocal(any(), any()) } answers {
                applied += firstArg<GalleryLocalChange>()
                true
            }
        }
        syncGallery = mockk { coEvery { afterWrite() } returns GallerySyncResult.Synced(0) }
        media = GalleryMediaStore(tempDirContext(folder))
        repository = GalleryManageRepositoryImpl(api, gallery, syncGallery, media, dispatcher)
    }

    private fun syncs(times: Int) = coVerify(exactly = times) { syncGallery.afterWrite() }

    // region albums

    @Test
    fun `createAlbum applies the returned album and syncs once`() = runTest(dispatcher) {
        api.respondSuccess("createAlbum", albumDto(9, parentId = 2, name = "Sábado", position = 3))

        val result = repository.createAlbum(AlbumDraft("Sábado", "", null, parentId = 2))

        assertEquals(9L, result.getOrThrow().id)
        val expected = galleryAlbum(9, parentId = 2, name = "Sábado", position = 3)
        assertEquals(GalleryLocalChange.UpsertAlbum(expected), applied.single())
        assertEquals("""{"name":"Sábado","parent_id":2}""", api.writes.single().body.toString())
        syncs(1)
    }

    @Test
    fun `offline - fails with Sem conexao and changes nothing`() = runTest(dispatcher) {
        api.respondWriteIOException("createAlbum")

        val error = repository.createAlbum(AlbumDraft("X", "", null, null)).exceptionOrNull() as AppError

        assertTrue(error is AppError.Network)
        assertEquals(GalleryManageRepositoryImpl.OFFLINE_MESSAGE, error.userMessage)
        assertTrue(applied.isEmpty())
        syncs(0)
    }

    @Test
    fun `duplicate name - validation error, nothing applied, no sync`() = runTest(dispatcher) {
        val duplicate = apiError("VALIDATION_ERROR", "Já existe um álbum com esse nome aqui.")
        api.respondWriteError("patchAlbum", 400, duplicate)

        val error = repository.editAlbum(AlbumEdit(3, name = Field.Set("Sábado"))).exceptionOrNull() as AppError

        assertTrue(error.isValidation())
        assertEquals("Já existe um álbum com esse nome aqui.", error.userMessage)
        assertTrue(applied.isEmpty())
        syncs(0)
    }

    @Test
    fun `403 - nothing applied`() = runTest(dispatcher) {
        api.respondWriteError("createAlbum", 403, apiError("PERMISSION_DENIED", "Sem permissão."))

        val error = repository.createAlbum(AlbumDraft("X", "", null, null)).exceptionOrNull()

        assertEquals(403, (error as AppError.Auth).code)
        assertTrue(applied.isEmpty())
    }

    @Test
    fun `move to root sends an explicit null parent`() = runTest(dispatcher) {
        api.respondSuccess("patchAlbum", albumDto(3))

        repository.editAlbum(AlbumEdit(3, parentId = Field.Set(null)))

        assertEquals(JsonNull, api.writes.single().body!!["parent_id"])
    }

    @Test
    fun `cycle and not found refusals sync, so the screen shows the tree as it is now`() = runTest(dispatcher) {
        api.respondWriteError(
            "patchAlbum",
            400,
            apiError(
                "VALIDATION_ERROR",
                "Não é possível mover.",
                "album_id" to "3",
                "parent_id" to "9",
                "chain" to "[9,3]",
            ),
        )
        api.respondWriteError("patchAlbum", 404, apiError("NOT_FOUND", "x"))

        val cycle = repository.editAlbum(AlbumEdit(3, parentId = Field.Set(9))).exceptionOrNull() as AppError
        repository.editAlbum(AlbumEdit(3, parentId = Field.Set(9)))

        assertTrue(cycle.isCycle())
        assertTrue(applied.isEmpty())
        syncs(2)
    }

    @Test
    fun `deleteAlbum - 204 and 404 both remove the tree locally`() = runTest(dispatcher) {
        api.respondNoContent("deleteAlbum")
        api.respondWriteError("deleteAlbum", 404, apiError("NOT_FOUND", "x"))

        assertTrue(repository.deleteAlbum(3).isSuccess)
        assertTrue(repository.deleteAlbum(4).isSuccess)

        assertEquals(
            listOf(GalleryLocalChange.RemoveAlbumTree(3), GalleryLocalChange.RemoveAlbumTree(4)),
            applied,
        )
    }

    @Test
    fun `reorders apply the sent ids`() = runTest(dispatcher) {
        api.respondNoContent("orderAlbums")
        api.respondNoContent("orderPhotos")

        repository.reorderAlbums(null, listOf(5, 1))
        repository.reorderPhotos(7, listOf(12, 10))

        assertEquals(
            listOf(
                GalleryLocalChange.ReorderAlbums(null, listOf(5, 1)),
                GalleryLocalChange.ReorderPhotos(7, listOf(12, 10)),
            ),
            applied,
        )
        assertEquals("""{"parent_id":null,"ids":[5,1]}""", api.writes.first().body.toString())
    }

    @Test
    fun `order mismatch - recognized and synced, nothing applied`() = runTest(dispatcher) {
        api.respondWriteError(
            "orderPhotos",
            400,
            apiError("VALIDATION_ERROR", "Order must list every sibling exactly once.", "missing" to "[4]"),
        )

        val error = repository.reorderPhotos(7, listOf(1)).exceptionOrNull() as AppError

        assertTrue(error.isOrderMismatch())
        assertTrue(applied.isEmpty())
        syncs(1)
    }

    // endregion

    // region covers

    @Test
    fun `setCover sends one image and applies the returned album, deleting a temp file`() = runTest(dispatcher) {
        val image = folder.newFile("cover.jpg").apply { writeText("img") }
        api.respondSuccess("putCover", albumDto(3, coverUrl = "https://x/c.jpg"))

        repository.setCover(3, image, deleteAfter = true)

        assertEquals("cover.jpg", api.writes.single().fileName)
        assertEquals("https://x/c.jpg", (applied.single() as GalleryLocalChange.UpsertAlbum).album.coverUrl)
        assertFalse(image.exists())
    }

    @Test
    fun `a refused cover changes nothing`() = runTest(dispatcher) {
        val image = folder.newFile("cover.jpg")
        api.respondWriteError("putCover", 400, apiError("VALIDATION_ERROR", "Formato não suportado."))

        assertTrue(repository.setCover(3, image, deleteAfter = false).isFailure)
        assertTrue(applied.isEmpty())
        assertTrue(image.exists())
    }

    @Test
    fun `removeCover applies nothing and syncs for the resolved cover`() = runTest(dispatcher) {
        api.respondNoContent("deleteCover")

        assertTrue(repository.removeCover(3).isSuccess)
        assertTrue(applied.isEmpty())
        syncs(1)
    }

    @Test
    fun `originalForCover uses the local original, else downloads it to a temp file`() = runTest(dispatcher) {
        val local = File(folder.root, "10.jpg").apply { writeText("o10") }
        localState.value = GalleryLocalState(
            GalleryIndex(
                albums = mapOf(1L to galleryAlbum(1)),
                photos = listOf(galleryPhoto(10), galleryPhoto(11)).associateBy { it.id },
                cursor = "c",
            ),
            originals = mapOf(10L to local),
            covers = emptyMap(),
        )

        val onDevice = repository.originalForCover(10).getOrThrow()
        val downloaded = repository.originalForCover(11).getOrThrow()

        assertEquals(local, onDevice.file)
        assertFalse(onDevice.isTemp)
        assertTrue(downloaded.isTemp)
        assertEquals(listOf(photoUrl(11)), api.requestedUrls)
        assertEquals(String(FakeGalleryApi.DEFAULT_BYTES), downloaded.file.readText())
    }

    @Test
    fun `originalForCover offline fails with Sem conexao`() = runTest(dispatcher) {
        localState.value = GalleryLocalState(
            GalleryIndex(mapOf(1L to galleryAlbum(1)), mapOf(11L to galleryPhoto(11)), "c"),
            emptyMap(),
            emptyMap(),
        )
        api.respondIOException(photoUrl(11))

        val error = repository.originalForCover(11).exceptionOrNull() as AppError

        assertEquals(GalleryManageRepositoryImpl.OFFLINE_MESSAGE, error.userMessage)
    }

    // endregion

    // region photos

    @Test
    fun `editPhoto sends only the changed keys and applies the photo`() = runTest(dispatcher) {
        api.respondSuccess("patchPhoto", photoDto(10, name = "Batismo.jpg"))

        repository.editPhoto(PhotoEdit(10, name = Field.Set("Batismo.jpg"), dateTaken = Field.Set(null)))

        assertEquals("""{"name":"Batismo.jpg","date_taken":null}""", api.writes.single().body.toString())
        assertEquals("Batismo.jpg", (applied.single() as GalleryLocalChange.UpsertPhoto).photo.name)
        syncs(1)
    }

    @Test
    fun `editPhoto in a batch does not sync, a 404 still does`() = runTest(dispatcher) {
        api.respondSuccess("patchPhoto", photoDto(10))
        api.respondWriteError("patchPhoto", 404, apiError("NOT_FOUND", "x"))

        repository.editPhoto(PhotoEdit(10, albumId = Field.Set(2)), syncAfter = false)
        syncs(0)
        repository.editPhoto(PhotoEdit(11, albumId = Field.Set(2)), syncAfter = false)
        syncs(1)
    }

    @Test
    fun `deletePhoto - 204 and 404 both remove it locally`() = runTest(dispatcher) {
        api.respondNoContent("deletePhoto")
        api.respondWriteError("deletePhoto", 404, apiError("NOT_FOUND", "x"))

        assertTrue(repository.deletePhoto(10).isSuccess)
        assertTrue(repository.deletePhoto(11, syncAfter = false).isSuccess)

        assertEquals(
            listOf(GalleryLocalChange.RemovePhotos(setOf(10)), GalleryLocalChange.RemovePhotos(setOf(11))),
            applied,
        )
    }

    @Test
    fun `an empty 2xx body is a failure, not a crash`() = runTest(dispatcher) {
        api.respond<Any>("createAlbum") { Response.success(null) }

        assertTrue(repository.createAlbum(AlbumDraft("X", "", null, null)).isFailure)
        assertTrue(applied.isEmpty())
    }

    // endregion
}
