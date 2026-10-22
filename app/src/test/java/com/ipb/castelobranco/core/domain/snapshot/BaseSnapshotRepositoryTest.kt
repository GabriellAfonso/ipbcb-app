package com.ipb.castelobranco.core.domain.snapshot

import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import com.ipb.castelobranco.core.domain.error.AppError

@OptIn(ExperimentalCoroutinesApi::class)
class BaseSnapshotRepositoryTest {

    // region Fakes

    private class FakeCache(
        private var stored: String? = null,
        private var storedEtag: String? = null
    ) : SnapshotCache<String> {
        var lastSavedDto: String? = null
        var lastSavedEtag: String? = null

        override suspend fun load(): String? = stored
        override suspend fun loadETag(): String? = storedEtag
        override suspend fun save(dto: String, etag: String?) {
            stored = dto
            storedEtag = etag
            lastSavedDto = dto
            lastSavedEtag = etag
        }
        override suspend fun clear() {
            stored = null
            storedEtag = null
        }
    }

    private class FakeFetcher(
        private val result: NetworkResult<String>
    ) : SnapshotFetcher<String> {
        override suspend fun fetch(etag: String?): NetworkResult<String> = result
    }

    private class ThrowingFetcher(private val error: Throwable) : SnapshotFetcher<String> {
        override suspend fun fetch(etag: String?): NetworkResult<String> = throw error
    }

    private fun buildRepository(
        cache: SnapshotCache<String>,
        fetcher: SnapshotFetcher<String>
    ) = object : BaseSnapshotRepository<String, String>(
        cache = cache,
        fetcher = fetcher,
        mapper = { it.uppercase() },
        tag = "Test"
    ) {}

    // endregion

    // region loadCurrentState

    @Test
    fun `loadCurrentState reads the disk cache when memory is still Loading`() = runTest {
        val repo = buildRepository(FakeCache(stored = "hello"), FakeFetcher(NetworkResult.NotModified))

        assertEquals(SnapshotState.Data("HELLO"), repo.loadCurrentState())
    }

    @Test
    fun `loadCurrentState stays Loading when there is no cache`() = runTest {
        val repo = buildRepository(FakeCache(stored = null), FakeFetcher(NetworkResult.NotModified))

        assertTrue(repo.loadCurrentState() is SnapshotState.Loading)
    }

    @Test
    fun `loadCurrentState keeps data already in memory`() = runTest {
        val cache = FakeCache(stored = "old")
        val repo = buildRepository(cache, FakeFetcher(NetworkResult.NotModified))
        repo.preload()
        cache.save("new", null)

        assertEquals(SnapshotState.Data("OLD"), repo.loadCurrentState())
    }

    // endregion

    // region preload

    @Test
    fun `preload with empty cache keeps state as Loading`() = runTest {
        val cache = FakeCache(stored = null)
        val repo = buildRepository(cache, FakeFetcher(NetworkResult.NotModified))

        repo.preload()

        assertTrue(repo.getCurrentState() is SnapshotState.Loading)
    }

    @Test
    fun `preload with cached data transitions state to Data with mapped value`() = runTest {
        val cache = FakeCache(stored = "hello")
        val repo = buildRepository(cache, FakeFetcher(NetworkResult.NotModified))

        repo.preload()

        assertEquals(SnapshotState.Data("HELLO"), repo.getCurrentState())
    }

    // endregion

    // region refresh — Success

    @Test
    fun `refresh on Success returns Updated`() = runTest {
        val cache = FakeCache()
        val repo = buildRepository(cache, FakeFetcher(NetworkResult.Success("data", etag = "v1")))

        val result = repo.refresh()

        assertEquals(RefreshResult.Updated, result)
    }

    @Test
    fun `refresh on Success updates state to Data with mapped value`() = runTest {
        val cache = FakeCache()
        val repo = buildRepository(cache, FakeFetcher(NetworkResult.Success("data", etag = "v1")))

        repo.refresh()

        assertEquals(SnapshotState.Data("DATA"), repo.getCurrentState())
    }

    @Test
    fun `refresh on Success saves dto and etag to cache`() = runTest {
        val cache = FakeCache()
        val repo = buildRepository(cache, FakeFetcher(NetworkResult.Success("data", etag = "v1")))

        repo.refresh()

        assertEquals("data", cache.lastSavedDto)
        assertEquals("v1", cache.lastSavedEtag)
    }

    @Test
    fun `refresh on Success with null etag saves dto with null etag`() = runTest {
        val cache = FakeCache()
        val repo = buildRepository(cache, FakeFetcher(NetworkResult.Success("data", etag = null)))

        repo.refresh()

        assertEquals("data", cache.lastSavedDto)
        assertNull(cache.lastSavedEtag)
    }

    // endregion

    // region refresh — NotModified

    @Test
    fun `refresh on NotModified when state is Loading loads cache and returns NotModified`() = runTest {
        val cache = FakeCache(stored = "cached")
        val repo = buildRepository(cache, FakeFetcher(NetworkResult.NotModified))

        // state starts as Loading
        assertTrue(repo.getCurrentState() is SnapshotState.Loading)

        val result = repo.refresh()

        assertEquals(RefreshResult.NotModified, result)
        assertEquals(SnapshotState.Data("CACHED"), repo.getCurrentState())
    }

    @Test
    fun `refresh on NotModified when state is Loading and cache is empty keeps Loading`() = runTest {
        val cache = FakeCache(stored = null)
        val repo = buildRepository(cache, FakeFetcher(NetworkResult.NotModified))

        val result = repo.refresh()

        assertEquals(RefreshResult.NotModified, result)
        assertTrue(repo.getCurrentState() is SnapshotState.Loading)
    }

    @Test
    fun `refresh on NotModified when state is already Data does not overwrite state`() = runTest {
        val cache = FakeCache(stored = "cached")
        val repo = buildRepository(cache, FakeFetcher(NetworkResult.NotModified))

        // Pre-populate state via preload
        repo.preload()
        assertEquals(SnapshotState.Data("CACHED"), repo.getCurrentState())

        // Refresh with NotModified should not change state
        val result = repo.refresh()

        assertEquals(RefreshResult.NotModified, result)
        assertEquals(SnapshotState.Data("CACHED"), repo.getCurrentState())
    }

    // endregion

    // region clearCache

    @Test
    fun `clearCache resets state to Loading`() = runTest {
        val cache = FakeCache(stored = "data")
        val repo = buildRepository(cache, FakeFetcher(NetworkResult.NotModified))
        repo.preload()
        assertEquals(SnapshotState.Data("DATA"), repo.getCurrentState())

        repo.clearCache()

        assertTrue(repo.getCurrentState() is SnapshotState.Loading)
    }

    @Test
    fun `clearCache removes stored data from cache`() = runTest {
        val cache = FakeCache(stored = "data")
        val repo = buildRepository(cache, FakeFetcher(NetworkResult.NotModified))

        repo.clearCache()

        assertNull(cache.load())
    }

    // endregion

    // region refresh — Failure

    @Test
    fun `refresh on Failure with cache available returns CacheUsed and updates state`() = runTest {
        val cache = FakeCache(stored = "fallback")
        val error = RuntimeException("network error")
        val repo = buildRepository(cache, FakeFetcher(NetworkResult.Failure(error)))

        val result = repo.refresh()

        assertEquals(RefreshResult.CacheUsed, result)
        assertEquals(SnapshotState.Data("FALLBACK"), repo.getCurrentState())
    }

    @Test
    fun `refresh on Failure with empty cache returns Error with throwable`() = runTest {
        val cache = FakeCache(stored = null)
        val error = RuntimeException("network error")
        val repo = buildRepository(cache, FakeFetcher(NetworkResult.Failure(error)))

        val result = repo.refresh()

        assertTrue(result is RefreshResult.Error)
        assertEquals(error, (result as RefreshResult.Error).throwable.cause)
    }

    // endregion

    // region refresh — AppError.Auth

    @Test
    fun `refresh on AppError Auth ignores cache and emits Error state`() = runTest {
        val cache = FakeCache(stored = "fallback")
        val exception = AppError.Auth(code = 401, message = "Não autorizado")
        val repo = buildRepository(cache, FakeFetcher(NetworkResult.Failure(exception)))

        val result = repo.refresh()

        assertTrue(result is RefreshResult.Error)
        assertEquals(exception, (result as RefreshResult.Error).throwable)
        assertTrue(repo.getCurrentState() is SnapshotState.Error)
    }

    @Test
    fun `refresh on AppError Auth clears cache even when it has data`() = runTest {
        val cache = FakeCache(stored = "fallback")
        val exception = AppError.Auth(code = 403, message = "Proibido")
        val repo = buildRepository(cache, FakeFetcher(NetworkResult.Failure(exception)))

        repo.refresh()

        assertNull(cache.load())
    }

    @Test
    fun `refresh on AppError Auth preserves exception in Error state`() = runTest {
        val cache = FakeCache(stored = null)
        val exception = AppError.Auth(code = 401, message = "Token inválido")
        val repo = buildRepository(cache, FakeFetcher(NetworkResult.Failure(exception)))

        repo.refresh()

        val state = repo.getCurrentState()
        assertTrue(state is SnapshotState.Error)
        assertEquals(exception, (state as SnapshotState.Error).error)
    }

    // endregion

    // region refresh — unexpected exception

    @Test
    fun `refresh catches unexpected exception from fetcher and returns Error`() = runTest {
        val cache = FakeCache()
        val error = IllegalStateException("unexpected")
        val repo = buildRepository(cache, ThrowingFetcher(error))

        val result = repo.refresh()

        assertTrue(result is RefreshResult.Error)
        assertEquals(error, (result as RefreshResult.Error).throwable)
    }

    // endregion

    // region getCurrentState / observe

    @Test
    fun `getCurrentState reflects latest state after operations`() = runTest {
        val cache = FakeCache(stored = "item")
        val repo = buildRepository(cache, FakeFetcher(NetworkResult.NotModified))

        assertTrue(repo.getCurrentState() is SnapshotState.Loading)

        repo.preload()
        assertEquals(SnapshotState.Data("ITEM"), repo.getCurrentState())
    }

    @Test
    fun `observe emits current state immediately`() = runTest {
        val cache = FakeCache(stored = "item")
        val repo = buildRepository(cache, FakeFetcher(NetworkResult.NotModified))

        repo.preload()

        val observed = repo.observe().value
        assertEquals(SnapshotState.Data("ITEM"), observed)
    }

    // endregion
}
