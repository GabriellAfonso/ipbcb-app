package com.ipb.castelobranco.features.profile.data.access

import app.cash.turbine.test
import com.ipb.castelobranco.core.domain.access.Access
import com.ipb.castelobranco.core.domain.access.AccessLevel
import com.ipb.castelobranco.core.domain.access.Role
import com.ipb.castelobranco.core.domain.access.Scope
import com.ipb.castelobranco.core.domain.error.AppError
import com.ipb.castelobranco.core.domain.snapshot.Logger
import com.ipb.castelobranco.core.domain.snapshot.NetworkResult
import com.ipb.castelobranco.core.domain.snapshot.SnapshotCache
import com.ipb.castelobranco.core.domain.snapshot.SnapshotFetcher
import com.ipb.castelobranco.features.profile.data.dto.MeProfileDto
import com.ipb.castelobranco.features.profile.data.dto.RoleDto
import com.ipb.castelobranco.features.profile.data.snapshot.ProfileSnapshotRepository
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.async
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.yield
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.util.concurrent.atomic.AtomicInteger

class ProfileAccessRepositoryTest {

    private class FakeCache(var stored: MeProfileDto? = null) : SnapshotCache<MeProfileDto> {
        override suspend fun load(): MeProfileDto? = stored
        override suspend fun loadETag(): String? = null
        override suspend fun save(dto: MeProfileDto, etag: String?) {
            stored = dto
        }
        override suspend fun clear() {
            stored = null
        }
    }

    private class FakeFetcher(
        var result: NetworkResult<MeProfileDto>,
        val gate: CompletableDeferred<Unit>? = null,
    ) : SnapshotFetcher<MeProfileDto> {
        val calls = AtomicInteger(0)
        override suspend fun fetch(etag: String?): NetworkResult<MeProfileDto> {
            calls.incrementAndGet()
            gate?.await()
            return result
        }
    }

    private val leader = MeProfileDto(
        name = "Ana",
        isMember = true,
        roles = listOf(RoleDto("leader", "Liderança")),
        permissions = mapOf("members" to "manage"),
    )

    private fun repository(
        cache: FakeCache,
        fetcher: FakeFetcher,
    ): Pair<ProfileSnapshotRepository, ProfileAccessRepository> {
        val snapshot = ProfileSnapshotRepository(cache, fetcher, Logger.Noop)
        return snapshot to ProfileAccessRepository(snapshot)
    }

    @Test
    fun `access is NONE while the profile is loading`() = runTest {
        val (_, repo) = repository(FakeCache(), FakeFetcher(NetworkResult.NotModified))

        repo.access.test {
            assertEquals(Access.NONE, awaitItem())
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `access follows the loaded profile and goes back to NONE when cleared`() = runTest {
        val cache = FakeCache(stored = leader)
        val (snapshot, repo) = repository(cache, FakeFetcher(NetworkResult.NotModified))

        repo.access.test {
            assertEquals(Access.NONE, awaitItem())

            snapshot.preload()
            val loaded = awaitItem()
            assertTrue(loaded.holds(Role.LEADER))
            assertTrue(loaded.allows(Scope.MEMBERS, AccessLevel.MANAGE))

            snapshot.clearCache()
            assertEquals(Access.NONE, awaitItem())
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `a failed first load leaves access at NONE`() = runTest {
        val fetcher = FakeFetcher(NetworkResult.Failure(AppError.Server(code = 500)))
        val (_, repo) = repository(FakeCache(), fetcher)

        repo.refresh()

        repo.access.test {
            assertEquals(Access.NONE, awaitItem())
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `concurrent refreshes read the profile once`() = runTest {
        val gate = CompletableDeferred<Unit>()
        val fetcher = FakeFetcher(NetworkResult.Success(leader, etag = null), gate)
        val (_, repo) = repository(FakeCache(), fetcher)

        val first = async { repo.refresh() }
        while (fetcher.calls.get() == 0) yield()
        repo.refresh()
        gate.complete(Unit)
        first.await()

        assertEquals(1, fetcher.calls.get())
    }
}
