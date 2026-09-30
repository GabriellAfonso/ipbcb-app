package com.ipb.castelobranco.features.profile.data.access

import app.cash.turbine.test
import com.ipb.castelobranco.core.domain.snapshot.Logger
import com.ipb.castelobranco.core.domain.snapshot.NetworkResult
import com.ipb.castelobranco.core.domain.snapshot.SnapshotCache
import com.ipb.castelobranco.core.domain.snapshot.SnapshotFetcher
import com.ipb.castelobranco.features.profile.data.dto.MeProfileDto
import com.ipb.castelobranco.features.profile.data.snapshot.ProfileSnapshotRepository
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class ProfileCurrentMemberRepositoryTest {

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

    private val linked = MeProfileDto(name = "Maria", isMember = true, memberId = 12)

    @Test
    fun `member id follows the profile - null while loading, the link, null again when cleared`() = runTest {
        val snapshot = ProfileSnapshotRepository(FakeCache(linked), FakeFetcher(NetworkResult.NotModified), Logger.Noop)
        val repository = ProfileCurrentMemberRepository(snapshot)

        repository.memberId.test {
            assertNull(awaitItem())

            snapshot.preload()
            assertEquals(12L, awaitItem())

            snapshot.clearCache()
            assertNull(awaitItem())
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `a re-read profile that lost its link emits null`() = runTest {
        val fetcher = FakeFetcher(NetworkResult.NotModified)
        val snapshot = ProfileSnapshotRepository(FakeCache(linked), fetcher, Logger.Noop)
        val repository = ProfileCurrentMemberRepository(snapshot)

        repository.memberId.test {
            assertNull(awaitItem())
            snapshot.preload()
            assertEquals(12L, awaitItem())

            fetcher.result = NetworkResult.Success(linked.copy(memberId = null), etag = null)
            snapshot.refresh()
            assertNull(awaitItem())
            cancelAndIgnoreRemainingEvents()
        }
    }
}
