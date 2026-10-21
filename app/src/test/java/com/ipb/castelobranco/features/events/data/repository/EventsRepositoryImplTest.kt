package com.ipb.castelobranco.features.events.data.repository

import com.ipb.castelobranco.core.domain.snapshot.Logger
import com.ipb.castelobranco.core.domain.snapshot.NetworkResult
import com.ipb.castelobranco.core.domain.snapshot.RefreshResult
import com.ipb.castelobranco.core.domain.snapshot.SnapshotCache
import com.ipb.castelobranco.core.domain.snapshot.SnapshotFetcher
import com.ipb.castelobranco.core.domain.snapshot.SnapshotState
import com.ipb.castelobranco.features.events.data.dto.EventPublicDto
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.async
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class EventsRepositoryImplTest {

    private class FakeCache : SnapshotCache<List<EventPublicDto>> {
        var stored: List<EventPublicDto>? = null
        var etag: String? = null
        override suspend fun load() = stored
        override suspend fun loadETag() = etag
        override suspend fun save(dto: List<EventPublicDto>, etag: String?) {
            stored = dto
            this.etag = etag
        }
        override suspend fun clear() {
            stored = null
            etag = null
        }
    }

    private class FakeFetcher(
        private val gate: CompletableDeferred<Unit>? = null,
    ) : SnapshotFetcher<List<EventPublicDto>> {
        var calls = 0
        override suspend fun fetch(etag: String?): NetworkResult<List<EventPublicDto>> {
            calls++
            gate?.await()
            return NetworkResult.Success(listOf(DTO), "\"v1\"")
        }
    }

    @Test
    fun `refresh saves the list and emits it`() = runTest {
        val cache = FakeCache()
        val repo = EventsRepositoryImpl(cache, FakeFetcher(), Logger.Noop, StandardTestDispatcher(testScheduler))

        assertEquals(RefreshResult.Updated, repo.refreshEvents())

        assertEquals(listOf(DTO), cache.stored)
        val state = repo.observe().value
        assertEquals(listOf(5), (state as SnapshotState.Data).value.map { it.id })
    }

    @Test
    fun `clear empties disk and memory`() = runTest {
        val cache = FakeCache()
        val repo = EventsRepositoryImpl(cache, FakeFetcher(), Logger.Noop, StandardTestDispatcher(testScheduler))
        repo.refreshEvents()

        repo.clear()

        assertNull(cache.stored)
        assertNull(cache.etag)
        assertTrue(repo.observe().value is SnapshotState.Loading)
    }

    @Test
    fun `a refresh asked while another runs is skipped`() = runTest {
        val gate = CompletableDeferred<Unit>()
        val fetcher = FakeFetcher(gate)
        val repo = EventsRepositoryImpl(FakeCache(), fetcher, Logger.Noop, StandardTestDispatcher(testScheduler))

        val first = async { repo.refreshEvents() }
        testScheduler.runCurrent()
        val second = repo.refreshEvents()
        gate.complete(Unit)

        assertEquals(RefreshResult.NotModified, second)
        assertEquals(RefreshResult.Updated, first.await())
        assertEquals(1, fetcher.calls)
    }

    private companion object {
        val DTO = EventPublicDto(
            id = 5,
            title = "Almoço",
            startDate = "2026-11-14",
            imageUrl = "https://host/x.jpg",
        )
    }
}
