package com.ipb.castelobranco.features.admin.members.domain.usecase

import com.ipb.castelobranco.features.admin.members.data.photo.FakeMemberPhotoStore
import com.ipb.castelobranco.core.domain.error.AppError
import com.ipb.castelobranco.features.admin.members.data.api.FakeMembersAdminApi
import com.ipb.castelobranco.features.admin.members.data.repository.MembersAdminRepositoryImpl
import com.ipb.castelobranco.features.admin.members.domain.model.MemberDraft
import com.ipb.castelobranco.features.admin.members.domain.model.toDraft
import com.ipb.castelobranco.features.admin.members.fieldError
import com.ipb.castelobranco.features.admin.members.ok
import com.ipb.castelobranco.features.admin.members.record
import com.ipb.castelobranco.features.admin.members.recordDto
import kotlinx.coroutines.test.runTest
import kotlinx.serialization.json.jsonPrimitive
import org.junit.Assert.assertEquals
import org.junit.Assert.assertSame
import org.junit.Assert.assertTrue
import org.junit.Test

class SaveMemberUseCaseTest {

    private val api = FakeMembersAdminApi()
    private val save = SaveMemberUseCase(MembersAdminRepositoryImpl(api, FakeMemberPhotoStore()))

    @Test
    fun `new member is created with the filled fields`() = runTest {
        api.onCreateMember = { ok(recordDto(id = 40, name = "Bruno Carvalho")) }

        val created = save(MemberDraft(name = "Bruno Carvalho"), original = null, originalDraft = null)

        assertEquals(40, created.getOrThrow().id)
        assertEquals(setOf("name"), api.createdBodies.single().keys)
    }

    @Test
    fun `edit sends only what changed`() = runTest {
        val original = record()
        api.onUpdateMember = { id, _ -> ok(recordDto(id = id)) }

        save(original.toDraft().copy(statusId = 3), original, original.toDraft()).getOrThrow()

        val (id, body) = api.updatedBodies.single()
        assertEquals(12, id)
        assertEquals(setOf("status_id"), body.keys)
        assertEquals("3", body["status_id"]!!.jsonPrimitive.content)
    }

    @Test
    fun `edit with nothing changed sends nothing`() = runTest {
        val original = record()

        val result = save(original.toDraft(), original, original.toDraft())

        assertSame(original, result.getOrThrow())
        assertEquals(0, api.calls)
    }

    @Test
    fun `server field errors come back on the result`() = runTest {
        api.onCreateMember = { fieldError("status_id", "Situação inexistente.") }

        val error = save(MemberDraft(name = "X", statusId = 99), null, null).exceptionOrNull()

        assertTrue(error is AppError.Server)
        assertEquals(listOf("Situação inexistente."), (error as AppError.Server).fieldErrors!!["status_id"])
    }
}
