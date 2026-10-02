package com.ipb.castelobranco.features.profile.data.access

import app.cash.turbine.test
import com.ipb.castelobranco.core.domain.snapshot.Logger
import com.ipb.castelobranco.core.domain.snapshot.NetworkResult
import com.ipb.castelobranco.core.domain.snapshot.SnapshotCache
import com.ipb.castelobranco.core.domain.snapshot.SnapshotFetcher
import com.ipb.castelobranco.core.domain.worship.WorshipAccess
import com.ipb.castelobranco.features.profile.data.dto.MeProfileDto
import com.ipb.castelobranco.features.profile.data.snapshot.ProfileSnapshotRepository
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Test

class ProfileWorshipAccessRepositoryTest {

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

    private class FakeFetcher(var result: NetworkResult<MeProfileDto>) : SnapshotFetcher<MeProfileDto> {
        override suspend fun fetch(etag: String?): NetworkResult<MeProfileDto> = result
    }

    private val leader = MeProfileDto(name = "Ana", isMember = true, isWorshipMember = true, canSaveSetlist = true)

    @Test
    fun `flags follow the profile - none while loading, the flags, none again when cleared`() = runTest {
        val snapshot = ProfileSnapshotRepository(FakeCache(leader), FakeFetcher(NetworkResult.NotModified), Logger.Noop)
        val repository = ProfileWorshipAccessRepository(snapshot)

        repository.worshipAccess.test {
            assertEquals(WorshipAccess.NONE, awaitItem())

            snapshot.preload()
            assertEquals(WorshipAccess(isWorshipMember = true, canSaveSetlist = true), awaitItem())

            snapshot.clearCache()
            assertEquals(WorshipAccess.NONE, awaitItem())
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `a re-read profile that left the ministry emits the new flags`() = runTest {
        val fetcher = FakeFetcher(NetworkResult.NotModified)
        val snapshot = ProfileSnapshotRepository(FakeCache(leader), fetcher, Logger.Noop)
        val repository = ProfileWorshipAccessRepository(snapshot)

        repository.worshipAccess.test {
            assertEquals(WorshipAccess.NONE, awaitItem())
            snapshot.preload()
            awaitItem()

            fetcher.result = NetworkResult.Success(
                leader.copy(isWorshipMember = false, canSaveSetlist = false), etag = null,
            )
            snapshot.refresh()
            assertEquals(WorshipAccess.NONE, awaitItem())
            cancelAndIgnoreRemainingEvents()
        }
    }
}
