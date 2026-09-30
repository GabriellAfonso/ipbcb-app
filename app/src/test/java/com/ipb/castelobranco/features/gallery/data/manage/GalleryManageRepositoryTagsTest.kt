package com.ipb.castelobranco.features.gallery.data.manage

import com.ipb.castelobranco.core.domain.error.AppError
import com.ipb.castelobranco.core.testing.tempDirContext
import com.ipb.castelobranco.features.gallery.data.api.FakeGalleryApi
import com.ipb.castelobranco.features.gallery.data.apiError
import com.ipb.castelobranco.features.gallery.data.dto.GalleryPhotoMemberDto
import com.ipb.castelobranco.features.gallery.data.dto.toDomain
import com.ipb.castelobranco.features.gallery.data.local.GalleryMediaStore
import com.ipb.castelobranco.features.gallery.data.photoDto
import com.ipb.castelobranco.features.gallery.domain.manage.isNotFound
import com.ipb.castelobranco.features.gallery.domain.model.GalleryIndex
import com.ipb.castelobranco.features.gallery.domain.model.GalleryLocalChange
import com.ipb.castelobranco.features.gallery.domain.model.GalleryLocalState
import com.ipb.castelobranco.features.gallery.domain.model.GalleryMember
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
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder

class GalleryManageRepositoryTagsTest {

    @get:Rule
    val folder = TemporaryFolder()

    private val dispatcher = StandardTestDispatcher()
    private val api = FakeGalleryApi()
    private val localState = MutableStateFlow(GalleryLocalState.EMPTY)
    private lateinit var gallery: GalleryRepository
    private lateinit var syncGallery: SyncGalleryUseCase
    private lateinit var repository: GalleryManageRepositoryImpl

    /** The index the local changes are applied to, as the real repository does under its lock. */
    private var index = GalleryIndex(
        albums = emptyMap(),
        photos = listOf(photoDto(301), photoDto(302)).associate { it.id to it.toDomain() },
        cursor = "c9",
    )

    private val maria = GalleryPhotoMemberDto(12, "Maria Souza")
    private val joao = GalleryPhotoMemberDto(40, "João Lima")

    @Before
    fun setup() {
        gallery = mockk {
            every { localState } returns this@GalleryManageRepositoryTagsTest.localState
            coEvery { applyLocal(any(), any()) } answers {
                index = index.apply(firstArg<GalleryLocalChange>())
                true
            }
        }
        syncGallery = mockk { coEvery { afterWrite() } returns GallerySyncResult.Synced(0) }
        repository = GalleryManageRepositoryImpl(
            api,
            gallery,
            syncGallery,
            GalleryMediaStore(tempDirContext(folder)),
            dispatcher,
        )
    }

    @Test
    fun `taggable members are read and mapped, nothing applied`() = runTest(dispatcher) {
        api.respondSuccess("getTaggableMembers", listOf(joao, maria))

        val members = repository.taggableMembers().getOrThrow()

        assertEquals(listOf(GalleryMember(40, "João Lima"), GalleryMember(12, "Maria Souza")), members)
        coVerify(exactly = 0) { gallery.applyLocal(any(), any()) }
        coVerify(exactly = 0) { syncGallery.afterWrite() }
    }

    @Test
    fun `set members sends the full set, applies the answer without moving the cursor and syncs`() =
        runTest(dispatcher) {
            api.respondSuccess("putPhotoMembers", photoDto(301).copy(members = listOf(joao, maria)))

            val photo = repository.setPhotoMembers(301, listOf(12, 40)).getOrThrow()

            assertEquals(listOf(40L, 12L), photo.members.map { it.id })
            assertEquals("""{"member_ids":[12,40]}""", api.writes.single().body.toString())
            assertEquals(listOf(40L, 12L), index.photos.getValue(301).members.map { it.id })
            assertEquals("c9", index.cursor)
            coVerify(exactly = 1) { syncGallery.afterWrite() }
        }

    @Test
    fun `change members applies every returned photo and leaves the sync to the caller`() = runTest(dispatcher) {
        api.respondSuccess(
            "changePhotoMembers",
            listOf(photoDto(301).copy(members = listOf(maria)), photoDto(302).copy(members = listOf(maria))),
        )

        val photos = repository.changePhotoMembers(listOf(301, 302), listOf(12), emptyList()).getOrThrow()

        assertEquals(2, photos.size)
        assertEquals(
            """{"photo_ids":[301,302],"add_member_ids":[12],"remove_member_ids":[]}""",
            api.writes.single().body.toString(),
        )
        assertEquals(listOf(12L), index.photos.getValue(302).members.map { it.id })
        assertEquals("c9", index.cursor)
        coVerify(exactly = 0) { syncGallery.afterWrite() }
    }

    @Test
    fun `404 with missing ids - nothing applied, a sync runs`() = runTest(dispatcher) {
        val body = apiError(
            "NOT_FOUND",
            "Algumas fotos ou pessoas não existem.",
            "missing_photo_ids" to "[302]",
            "missing_member_ids" to "[]",
        )
        api.respondWriteError("changePhotoMembers", 404, body)

        val error = repository.changePhotoMembers(listOf(301, 302), listOf(12), emptyList())
            .exceptionOrNull() as AppError

        assertTrue(error.isNotFound())
        assertTrue(index.photos.getValue(301).members.isEmpty())
        coVerify(exactly = 1) { syncGallery.afterWrite() }
    }
}
