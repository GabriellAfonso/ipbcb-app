package com.ipb.castelobranco.features.admin.members.data.repository

import com.ipb.castelobranco.features.admin.members.apiError
import com.ipb.castelobranco.features.admin.members.data.api.FakeMembersAdminApi
import com.ipb.castelobranco.features.admin.members.data.dto.MemberListDto
import com.ipb.castelobranco.features.admin.members.data.dto.PhotoUrlDto
import com.ipb.castelobranco.features.admin.members.data.photo.FakeMemberPhotoStore
import com.ipb.castelobranco.features.admin.members.notModified
import com.ipb.castelobranco.features.admin.members.ok
import com.ipb.castelobranco.features.admin.members.summaryDto
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/** How the roll keeps the encrypted photo cache true to the server (spec 012, FR-011/FR-012). */
class MembersAdminPhotoCacheTest {

    private val api = FakeMembersAdminApi()
    private val photos = FakeMemberPhotoStore()
    private val repository = MembersAdminRepositoryImpl(api, photos)

    private val roll = MemberListDto(
        listOf(summaryDto(1, "Ana Souza", photoUrl = OLD_URL), summaryDto(3, "Carla Mendes"))
    )

    private suspend fun loadRoll() {
        api.onGetMembers = { ok(roll, etag = "\"v1\"") }
        repository.refreshMembers().getOrThrow()
    }

    @Test
    fun `fresh list keeps only photos still in the roll`() = runTest {
        loadRoll()

        assertEquals(listOf(setOf(OLD_URL)), photos.retained)
    }

    @Test
    fun `unchanged list does not prune`() = runTest {
        loadRoll()
        api.onGetMembers = { notModified() }

        repository.refreshMembers()

        assertEquals(1, photos.retained.size)
    }

    @Test
    fun `replacing a photo drops the old one`() = runTest {
        loadRoll()
        api.onUploadPhoto = { _, _ -> ok(PhotoUrlDto(NEW_URL)) }

        repository.uploadPhoto(1, byteArrayOf(1), "image/jpeg").getOrThrow()

        assertEquals(listOf(OLD_URL), photos.removed)
    }

    @Test
    fun `removing a photo drops it`() = runTest {
        loadRoll()
        api.onRemovePhoto = { ok(Unit) }

        repository.removePhoto(1).getOrThrow()

        assertEquals(listOf(OLD_URL), photos.removed)
    }

    @Test
    fun `deleting a member drops their photo`() = runTest {
        loadRoll()
        api.onDeleteMember = { ok(Unit) }

        repository.deleteMember(1).getOrThrow()

        assertEquals(listOf(OLD_URL), photos.removed)
    }

    @Test
    fun `refused list wipes every photo`() = runTest {
        api.onGetMembers = { apiError(403, "PERMISSION_DENIED", "Apenas líderes.") }

        assertTrue(repository.refreshMembers().isFailure)
        assertEquals(1, photos.wipes)
    }

    @Test
    fun `expired session on a record wipes every photo`() = runTest {
        api.onGetMember = { _, _ -> apiError(401, "NOT_AUTHENTICATED", "Faça login.") }

        assertTrue(repository.getMember(1).isFailure)
        assertEquals(1, photos.wipes)
    }

    @Test
    fun `server error keeps the photos`() = runTest {
        api.onGetMembers = { apiError(500) }

        repository.refreshMembers()

        assertEquals(0, photos.wipes)
    }

    private companion object {
        const val OLD_URL = "https://gabrielafonso.com.br/ipbcb/media/members/old.jpg"
        const val NEW_URL = "https://gabrielafonso.com.br/ipbcb/media/members/new.jpg"
    }
}
