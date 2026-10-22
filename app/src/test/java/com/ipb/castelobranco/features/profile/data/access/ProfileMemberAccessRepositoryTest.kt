package com.ipb.castelobranco.features.profile.data.access

import com.ipb.castelobranco.core.domain.access.Access
import com.ipb.castelobranco.core.domain.access.AccessRepository
import com.ipb.castelobranco.core.domain.access.AccessRequestType
import com.ipb.castelobranco.core.domain.access.AccessStatus
import com.ipb.castelobranco.core.domain.error.AppError
import com.ipb.castelobranco.core.domain.snapshot.Logger
import com.ipb.castelobranco.core.domain.snapshot.NetworkResult
import com.ipb.castelobranco.core.domain.snapshot.SnapshotCache
import com.ipb.castelobranco.core.domain.snapshot.SnapshotFetcher
import com.ipb.castelobranco.features.profile.data.api.FakeProfileApi
import com.ipb.castelobranco.features.profile.data.dto.CreateAccessRequestBody
import com.ipb.castelobranco.features.profile.data.dto.MeProfileDto
import com.ipb.castelobranco.features.profile.data.snapshot.ProfileSnapshotRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.emptyFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.ResponseBody.Companion.toResponseBody
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import retrofit2.Response

class ProfileMemberAccessRepositoryTest {

    private class FakeCache(var stored: MeProfileDto?) : SnapshotCache<MeProfileDto> {
        override suspend fun load(): MeProfileDto? = stored
        override suspend fun loadETag(): String? = null
        override suspend fun save(dto: MeProfileDto, etag: String?) {
            stored = dto
        }
        override suspend fun clear() {
            stored = null
        }
    }

    private class FakeFetcher : SnapshotFetcher<MeProfileDto> {
        override suspend fun fetch(etag: String?): NetworkResult<MeProfileDto> = NetworkResult.NotModified
    }

    private class CountingAccessRepository : AccessRepository {
        var refreshes = 0
        override val access: Flow<Access> = emptyFlow()
        override suspend fun refresh() {
            refreshes++
        }
    }

    private val api = FakeProfileApi()
    private val access = CountingAccessRepository()

    private fun snapshot(dto: MeProfileDto?) = ProfileSnapshotRepository(FakeCache(dto), FakeFetcher(), Logger.Noop)

    private fun repository(dto: MeProfileDto?) = ProfileMemberAccessRepository(snapshot(dto), api, access)

    @Test
    fun `member access comes from the cached profile`() = runTest {
        val snapshot = snapshot(MeProfileDto(name = "Ana", memberAccess = false, accessStatus = "pending"))
        val repo = ProfileMemberAccessRepository(snapshot, api, access)

        snapshot.preload()
        val value = repo.memberAccess.first { it.status != AccessStatus.UNKNOWN }

        assertEquals(AccessStatus.PENDING, value.status)
    }

    @Test
    fun `a request is sent and the profile is read again`() = runTest {
        api.accessRequestScript = { Response.success(201, Unit) }

        val result = repository(null).request(AccessRequestType.ACCESS, "Sou membro.")

        assertTrue(result.isSuccess)
        assertEquals(listOf(CreateAccessRequestBody(type = "access", message = "Sou membro.")), api.accessRequestBodies)
        assertEquals(1, access.refreshes)
    }

    @Test
    fun `a refusal comes back as an app error and still refreshes the profile`() = runTest {
        api.accessRequestScript = {
            Response.error(
                429,
                """{"error_code":"ACCESS_REQUEST_RATE_LIMITED","detail":"Tente amanhã.","retry_after_seconds":60}"""
                    .toResponseBody("application/json".toMediaType()),
            )
        }

        val result = repository(null).request(AccessRequestType.DATA_CORRECTION, "Data errada.")

        val error = result.exceptionOrNull() as AppError.Server
        assertEquals("Tente amanhã.", error.userMessage)
        assertEquals(1, access.refreshes)
    }
}
