package com.ipb.castelobranco.features.hymnal.data.repository

import com.ipb.castelobranco.core.domain.snapshot.Logger
import com.ipb.castelobranco.core.domain.snapshot.NetworkResult
import com.ipb.castelobranco.core.domain.snapshot.RefreshResult
import com.ipb.castelobranco.core.domain.snapshot.SnapshotCache
import com.ipb.castelobranco.core.domain.snapshot.SnapshotFetcher
import com.ipb.castelobranco.core.domain.snapshot.SnapshotState
import com.ipb.castelobranco.features.hymnal.data.dto.HymnDto
import com.ipb.castelobranco.features.hymnal.data.dto.HymnLyricDto
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class HymnalRepositoryImplTest {

    private lateinit var cache: FakeHymnCache
    private lateinit var fetcher: FakeHymnFetcher

    private val fakeHymns = listOf(
        HymnDto(number = "1", title = "Quão Grande És Tu", lyrics = listOf(HymnLyricDto("verse", "Senhor, meu Deus"))),
        HymnDto(number = "2", title = "Grande É o Senhor", lyrics = emptyList()),
    )

    @Before
    fun setup() {
        cache = FakeHymnCache()
        fetcher = FakeHymnFetcher(NetworkResult.NotModified)
    }

    private fun buildRepository() = HymnalRepositoryImpl(cache, fetcher, Logger.Noop)

    // region initial state

    @Test
    fun `observeHymnal initially emits Loading`() = runTest {
        val repo = buildRepository()
        val state = repo.observeHymnal().first()
        assertTrue(state is SnapshotState.Loading)
    }

    // endregion

    // region preload via observe

    @Test
    fun `observeHymnal emits Data after preload with cached hymns`() = runTest {
        cache.stored = fakeHymns
        val repo = buildRepository()

        repo.preload()

        val state = repo.observeHymnal().first()
        assertTrue(state is SnapshotState.Data)
        val hymns = (state as SnapshotState.Data<List<com.ipb.castelobranco.features.hymnal.domain.model.Hymn>>).value
        assertEquals(2, hymns.size)
        assertEquals("1", hymns[0].number)
        assertEquals("Quão Grande És Tu", hymns[0].title)
    }

    @Test
    fun `observeHymnal remains Loading after preload with empty cache`() = runTest {
        cache.stored = null
        val repo = buildRepository()

        repo.preload()

        val state = repo.observeHymnal().first()
        assertTrue(state is SnapshotState.Loading)
    }

    // endregion

    // region refreshHymnal

    @Test
    fun `refreshHymnal returns Updated on successful fetch`() = runTest {
        fetcher.result = NetworkResult.Success(fakeHymns, etag = "v1")
        val repo = buildRepository()

        val result = repo.refreshHymnal()

        assertEquals(RefreshResult.Updated, result)
    }

    @Test
    fun `refreshHymnal updates observeHymnal state to Data on success`() = runTest {
        fetcher.result = NetworkResult.Success(fakeHymns, etag = "v1")
        val repo = buildRepository()

        repo.refreshHymnal()

        val state = repo.observeHymnal().first()
        assertTrue(state is SnapshotState.Data)
    }

    @Test
    fun `refreshHymnal maps DTOs to domain Hymn objects`() = runTest {
        fetcher.result = NetworkResult.Success(fakeHymns, etag = "v1")
        val repo = buildRepository()

        repo.refreshHymnal()

        val state = repo.observeHymnal().first() as SnapshotState.Data<List<com.ipb.castelobranco.features.hymnal.domain.model.Hymn>>
        assertEquals(2, state.value.size)
        assertEquals("Quão Grande És Tu", state.value[0].title)
    }

    @Test
    fun `refreshHymnal returns NotModified when server says not modified`() = runTest {
        fetcher.result = NetworkResult.NotModified
        val repo = buildRepository()

        val result = repo.refreshHymnal()

        assertEquals(RefreshResult.NotModified, result)
    }

    @Test
    fun `refreshHymnal returns CacheUsed on network failure when cache is populated`() = runTest {
        cache.stored = fakeHymns
        fetcher.result = NetworkResult.Failure(RuntimeException("network error"))
        val repo = buildRepository()

        val result = repo.refreshHymnal()

        assertEquals(RefreshResult.CacheUsed, result)
    }

    @Test
    fun `refreshHymnal returns Error on network failure when cache is empty`() = runTest {
        cache.stored = null
        fetcher.result = NetworkResult.Failure(RuntimeException("network error"))
        val repo = buildRepository()

        val result = repo.refreshHymnal()

        assertTrue(result is RefreshResult.Error)
    }

    // endregion

    // region fakes

    private class FakeHymnCache(var stored: List<HymnDto>? = null) : SnapshotCache<List<HymnDto>> {
        override suspend fun load(): List<HymnDto>? = stored
        override suspend fun loadETag(): String? = null
        override suspend fun save(dto: List<HymnDto>, etag: String?) { stored = dto }
        override suspend fun clear() { stored = null }
    }

    private class FakeHymnFetcher(var result: NetworkResult<List<HymnDto>>) : SnapshotFetcher<List<HymnDto>> {
        override suspend fun fetch(etag: String?): NetworkResult<List<HymnDto>> = result
    }

    // endregion
}
