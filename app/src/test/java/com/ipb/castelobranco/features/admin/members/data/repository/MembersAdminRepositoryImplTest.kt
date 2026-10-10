package com.ipb.castelobranco.features.admin.members.data.repository

import com.ipb.castelobranco.features.admin.members.data.photo.FakeMemberPhotoStore
import com.ipb.castelobranco.core.domain.error.AppError
import com.ipb.castelobranco.features.admin.members.apiError
import com.ipb.castelobranco.features.admin.members.data.api.FakeMembersAdminApi
import com.ipb.castelobranco.features.admin.members.data.dto.MemberListDto
import com.ipb.castelobranco.features.admin.members.data.dto.PhotoUrlDto
import com.ipb.castelobranco.features.admin.members.domain.model.MemberChanges
import com.ipb.castelobranco.features.admin.members.domain.model.MemberField
import com.ipb.castelobranco.features.admin.members.notModified
import com.ipb.castelobranco.features.admin.members.ok
import com.ipb.castelobranco.features.admin.members.recordDto
import com.ipb.castelobranco.features.admin.members.summaryDto
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.IOException

class MembersAdminRepositoryImplTest {

    private val api = FakeMembersAdminApi()
    private val repository = MembersAdminRepositoryImpl(api, FakeMemberPhotoStore())

    private val roll = MemberListDto(
        listOf(summaryDto(1, "Ana Souza"), summaryDto(3, "Carla Mendes", isActive = false))
    )

    private suspend fun loadRoll() {
        api.onGetMembers = { ok(roll, etag = "\"v1\"") }
        repository.refreshMembers().getOrThrow()
    }

    private suspend fun names() = repository.observeMembers().first()!!.map { it.name }

    @Test
    fun `list is null until loaded, then holds every member valid or not`() = runTest {
        assertNull(repository.observeMembers().first())

        loadRoll()

        assertEquals(listOf("Ana Souza", "Carla Mendes"), names())
    }

    @Test
    fun `second refresh revalidates with the etag and keeps the list on 304`() = runTest {
        loadRoll()
        api.onGetMembers = { notModified() }

        val result = repository.refreshMembers()

        assertTrue(result.isSuccess)
        assertEquals(listOf(null, "\"v1\""), api.membersIfNoneMatch)
        assertEquals(2, names().size)
    }

    @Test
    fun `create inserts the member in name order`() = runTest {
        loadRoll()
        api.onCreateMember = { ok(recordDto(id = 2, name = "Bruno Carvalho")) }

        repository.createMember(MemberChanges(mapOf(MemberField.NAME to "Bruno Carvalho"))).getOrThrow()

        assertEquals(listOf("Ana Souza", "Bruno Carvalho", "Carla Mendes"), names())
    }

    @Test
    fun `update replaces the summary in the list`() = runTest {
        loadRoll()
        api.onUpdateMember = { id, _ -> ok(recordDto(id = id, name = "Ana S. Lima", isActive = false)) }

        repository.updateMember(1, MemberChanges(mapOf(MemberField.IS_VALID to false))).getOrThrow()

        val ana = repository.observeMembers().first()!!.first { it.id == 1 }
        assertEquals("Ana S. Lima", ana.name)
        assertEquals(false, ana.isValid)
    }

    @Test
    fun `photo upload and removal update the photo in the list`() = runTest {
        loadRoll()
        api.onUploadPhoto = { _, _ -> ok(PhotoUrlDto("https://host/media/members/x.jpg")) }
        api.onRemovePhoto = { ok(Unit) }

        val url = repository.uploadPhoto(1, byteArrayOf(1, 2), "image/jpeg").getOrThrow()
        assertEquals("https://host/media/members/x.jpg", url)
        assertEquals(url, repository.observeMembers().first()!!.first { it.id == 1 }.photoUrl)

        repository.removePhoto(1).getOrThrow()
        assertNull(repository.observeMembers().first()!!.first { it.id == 1 }.photoUrl)
    }

    @Test
    fun `delete removes the member from the list`() = runTest {
        loadRoll()
        api.onDeleteMember = { ok(Unit) }

        repository.deleteMember(1).getOrThrow()

        assertEquals(listOf("Carla Mendes"), names())
    }

    @Test
    fun `record is revalidated with its etag`() = runTest {
        api.onGetMember = { id, _ -> ok(recordDto(id = id), etag = "\"r1\"") }
        repository.getMember(12).getOrThrow()
        api.onGetMember = { _, _ -> notModified() }

        val again = repository.getMember(12).getOrThrow()

        assertEquals("Ana Souza", again.name)
        assertEquals(listOf(null, "\"r1\""), api.memberIfNoneMatch)
    }

    @Test
    fun `404 on a member says it no longer exists and drops it from the list`() = runTest {
        loadRoll()
        api.onGetMember = { _, _ -> apiError(404, "NOT_FOUND", "Not found.") }

        val error = repository.getMember(1).exceptionOrNull()

        assertTrue(error is AppError.Server)
        assertEquals(404, (error as AppError.Server).code)
        assertEquals("Este membro não existe mais", error.userMessage)
        assertEquals(listOf("Carla Mendes"), names())
    }

    @Test
    fun `403 surfaces as an auth error with the server detail`() = runTest {
        api.onGetMembers = { apiError(403, "PERMISSION_DENIED", "Apenas líderes.") }

        val error = repository.refreshMembers().exceptionOrNull()

        assertTrue(error is AppError.Auth)
        assertEquals(403, (error as AppError.Auth).code)
        assertEquals("Apenas líderes.", error.userMessage)
    }

    @Test
    fun `connection failure becomes a network error`() = runTest {
        api.onGetMembers = { throw IOException("offline") }

        assertTrue(repository.refreshMembers().exceptionOrNull() is AppError.Network)
    }

    @Test
    fun `clear forgets the list and the etags`() = runTest {
        loadRoll()

        repository.clear()

        assertNull(repository.observeMembers().first())
        api.onGetMembers = { ok(roll) }
        repository.refreshMembers()
        assertEquals(null, api.membersIfNoneMatch.last())
    }
}
