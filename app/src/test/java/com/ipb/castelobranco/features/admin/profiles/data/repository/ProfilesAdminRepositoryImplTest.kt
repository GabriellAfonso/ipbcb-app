package com.ipb.castelobranco.features.admin.profiles.data.repository

import com.ipb.castelobranco.core.domain.access.AccessRequestType
import com.ipb.castelobranco.core.domain.error.AppError
import com.ipb.castelobranco.features.admin.profiles.data.api.ProfilesAdminApi
import com.ipb.castelobranco.features.admin.profiles.data.dto.AccessRequestDto
import com.ipb.castelobranco.features.admin.profiles.data.dto.DenyRequestBody
import com.ipb.castelobranco.features.admin.profiles.data.dto.ManagedAccountDto
import com.ipb.castelobranco.features.admin.profiles.data.dto.ManagedAccountListDto
import com.ipb.castelobranco.features.admin.profiles.domain.model.ProfilesTab
import com.ipb.castelobranco.features.admin.profiles.domain.model.RequestStatus
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import kotlinx.serialization.json.JsonNull
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.ResponseBody.Companion.toResponseBody
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import retrofit2.Response

class ProfilesAdminRepositoryImplTest {

    private class FakeApi : ProfilesAdminApi {
        val lists = mutableMapOf<String, List<ManagedAccountDto>>()
        var accountAnswer: () -> Response<ManagedAccountDto> = { error(500) }
        val patches = mutableListOf<JsonObject>()
        val denials = mutableListOf<Pair<Long, DenyRequestBody>>()

        override suspend fun getAccounts(status: String, ifNoneMatch: String?) =
            Response.success(ManagedAccountListDto(lists[status].orEmpty()))

        override suspend fun getAccount(id: String) = accountAnswer()
        override suspend fun updateAccount(id: String, body: JsonObject): Response<ManagedAccountDto> {
            patches += body
            return accountAnswer()
        }
        override suspend fun revokeAccess(id: String) = accountAnswer()
        override suspend fun approveRequest(requestId: Long) = accountAnswer()
        override suspend fun denyRequest(requestId: Long, body: DenyRequestBody): Response<ManagedAccountDto> {
            denials += requestId to body
            return accountAnswer()
        }
    }

    private val pending = AccessRequestDto(id = 41, type = "access", message = "Sou membro.", status = "pending")
    private val ana = ManagedAccountDto(id = "u-1", name = "Ana", pendingRequests = listOf(pending))
    private val bruno = ManagedAccountDto(id = "u-2", name = "Bruno")
    private val api = FakeApi().apply {
        lists["pending"] = listOf(ana)
        lists["all"] = listOf(ana, bruno)
    }
    private val repository = ProfilesAdminRepositoryImpl(api)

    @Test
    fun `each tab loads its own list`() = runTest {
        repository.refresh(ProfilesTab.PENDING)
        repository.refresh(ProfilesTab.ALL)

        assertEquals(listOf("u-1"), repository.observe(ProfilesTab.PENDING).first()?.map { it.id })
        assertEquals(listOf("u-1", "u-2"), repository.observe(ProfilesTab.ALL).first()?.map { it.id })
        val request = repository.observe(ProfilesTab.PENDING).first()!!.single().pendingRequests.single()
        assertEquals(AccessRequestType.ACCESS, request.type)
        assertEquals(RequestStatus.PENDING, request.status)
    }

    @Test
    fun `approving drops the account from Pendentes and updates it in Todos`() = runTest {
        repository.refresh(ProfilesTab.PENDING)
        repository.refresh(ProfilesTab.ALL)
        api.accountAnswer = {
            Response.success(ana.copy(memberAccess = true, pendingRequests = emptyList()))
        }

        val result = repository.approve(41)

        assertTrue(result.getOrThrow().memberAccess)
        assertEquals(emptyList<String>(), repository.observe(ProfilesTab.PENDING).first()?.map { it.id })
        assertTrue(repository.observe(ProfilesTab.ALL).first()!!.first { it.id == "u-1" }.memberAccess)
    }

    @Test
    fun `deny sends the reason and linking to nothing sends a null member`() = runTest {
        api.accountAnswer = { Response.success(ana) }

        repository.deny(41, "Procure o pastor.")
        repository.linkMember("u-1", null)
        repository.setRequestsBlocked("u-1", true)

        assertEquals(41L to DenyRequestBody("Procure o pastor."), api.denials.single())
        assertEquals(JsonNull, api.patches[0]["member_id"])
        assertEquals(JsonPrimitive(true), api.patches[1]["access_requests_blocked"])
    }

    @Test
    fun `a refusal comes back as an app error with the server message`() = runTest {
        api.accountAnswer = {
            Response.error(
                409,
                """{"error_code":"ACCESS_REQUEST_NOT_PENDING","detail":"Pedido já decidido."}"""
                    .toResponseBody("application/json".toMediaType()),
            )
        }

        val error = repository.approve(41).exceptionOrNull() as AppError.Server

        assertEquals(409, error.code)
        assertEquals("ACCESS_REQUEST_NOT_PENDING", error.errorCode)
        assertEquals("Pedido já decidido.", error.userMessage)
    }

    @Test
    fun `clear forgets every list`() = runTest {
        repository.refresh(ProfilesTab.ALL)

        repository.clear()

        assertNull(repository.observe(ProfilesTab.ALL).first())
    }
}
