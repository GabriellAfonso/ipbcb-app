package com.ipb.castelobranco.features.bible.data.repository

import com.ipb.castelobranco.core.data.local.SnapshotStorage
import com.ipb.castelobranco.core.domain.snapshot.SnapshotCache
import com.ipb.castelobranco.features.bible.data.api.BibleProgressApi
import com.ipb.castelobranco.features.bible.data.dto.BibleProgressDto
import com.ipb.castelobranco.features.bible.data.dto.ProgressSyncRequestDto
import com.ipb.castelobranco.features.bible.data.dto.ProgressSyncResponseDto
import com.ipb.castelobranco.features.bible.data.local.BibleProgressQueueStore
import com.ipb.castelobranco.features.bible.domain.model.ProgressTarget
import com.ipb.castelobranco.features.bible.domain.repository.ProgressSyncOutcome
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.runTest
import kotlinx.serialization.json.Json
import okhttp3.ResponseBody.Companion.toResponseBody
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import retrofit2.Response

class BibleProgressRepositoryImplTest {

    private val json = Json { ignoreUnknownKeys = true; explicitNulls = false }
    private val dispatcher = StandardTestDispatcher()
    private val api = FakeApi()
    private val cache = FakeCache()
    private val queue = BibleProgressQueueStore(FakeStorage(), json)
    private var scheduled = 0
    private var loggedIn = true

    private val repository = BibleProgressRepositoryImpl(
        api = api,
        cache = cache,
        queue = queue,
        scheduler = { scheduled++ },
        session = { loggedIn },
        clock = { 1_761_570_000_000L },
        ioDispatcher = dispatcher,
    )

    @Test
    fun `a mark shows at once on top of the server state and schedules a send`() = runTest(dispatcher) {
        cache.value = BibleProgressDto(chapters = mapOf("gn" to listOf(1)))

        repository.mark(listOf(ProgressTarget.chapter("gn", 2)), done = true)

        val progress = repository.progress.first()
        assertEquals(setOf(1, 2), progress.chapters["gn"])
        assertEquals(1, scheduled)
        // ISO-8601 with an offset: the server orders changes by the time of the action.
        assertTrue(queue.readAll().single().changedAt.matches(Regex(""".*T.*([+-]\d\d:\d\d|Z)$""")))
    }

    @Test
    fun `a delivered chunk leaves the queue, accepted or rejected`() = runTest(dispatcher) {
        repository.mark(listOf(ProgressTarget.chapter("gn", 1), ProgressTarget.Video(9L)), done = true)
        api.syncResponse = Response.success(ProgressSyncResponseDto(accepted = listOf(0)))

        val outcome = repository.syncOnce()

        assertEquals(ProgressSyncOutcome.Delivered, outcome)
        assertEquals(0, repository.queuedCount())
        assertEquals(listOf("chapter", "video"), api.lastRequest?.changes?.map { it.type })
    }

    @Test
    fun `a server error keeps everything queued`() = runTest(dispatcher) {
        repository.mark(listOf(ProgressTarget.chapter("gn", 1)), done = true)
        api.syncResponse = Response.error(503, "".toResponseBody())

        val outcome = repository.syncOnce()

        assertTrue(outcome is ProgressSyncOutcome.Deferred)
        assertEquals(1, repository.queuedCount())
    }

    @Test
    fun `a chunk refused as a whole is discarded`() = runTest(dispatcher) {
        repository.mark(listOf(ProgressTarget.chapter("gn", 1)), done = true)
        api.syncResponse = Response.error(400, """{"error_code":"VALIDATION_ERROR"}""".toResponseBody())

        assertEquals(ProgressSyncOutcome.Discarded, repository.syncOnce())
        assertEquals(0, repository.queuedCount())
    }

    @Test
    fun `signed out neither fetches nor sends`() = runTest(dispatcher) {
        repository.mark(listOf(ProgressTarget.chapter("gn", 1)), done = true)
        loggedIn = false

        repository.refresh()
        val outcome = repository.syncOnce()

        assertEquals(0, api.getCalls)
        assertTrue(outcome is ProgressSyncOutcome.Deferred)
    }

    @Test
    fun `refresh stores the account's state`() = runTest(dispatcher) {
        api.progressResponse =
            Response.success(BibleProgressDto(chapters = mapOf("RM" to listOf(5)), videos = listOf(3)))

        repository.refresh()

        val progress = repository.progress.first()
        assertTrue(progress.isRead("rm", 5))
        assertTrue(progress.isWatched(3L))
        assertEquals(listOf(5), cache.value?.chapters?.get("RM"))
    }

    @Test
    fun `logout tries to send, then forgets queue and state`() = runTest(dispatcher) {
        cache.value = BibleProgressDto(chapters = mapOf("gn" to listOf(1)))
        repository.mark(listOf(ProgressTarget.chapter("gn", 2)), done = true)
        api.syncResponse = Response.error(503, "".toResponseBody())

        repository.flushAndClear()

        assertEquals(1, api.syncCalls)
        assertEquals(0, repository.queuedCount())
        assertEquals(null, cache.value)
        assertTrue(repository.progress.first().chapters.isEmpty())
    }

    private class FakeApi : BibleProgressApi {
        var progressResponse: Response<BibleProgressDto> = Response.success(BibleProgressDto())
        var syncResponse: Response<ProgressSyncResponseDto> = Response.success(ProgressSyncResponseDto())
        var lastRequest: ProgressSyncRequestDto? = null
        var getCalls = 0
        var syncCalls = 0

        override suspend fun getProgress(): Response<BibleProgressDto> {
            getCalls++
            return progressResponse
        }

        override suspend fun sync(body: ProgressSyncRequestDto): Response<ProgressSyncResponseDto> {
            syncCalls++
            lastRequest = body
            return syncResponse
        }
    }

    private class FakeCache : SnapshotCache<BibleProgressDto> {
        var value: BibleProgressDto? = null
        override suspend fun load(): BibleProgressDto? = value
        override suspend fun save(dto: BibleProgressDto, etag: String?) { value = dto }
        override suspend fun loadETag(): String? = null
        override suspend fun clear() { value = null }
    }

    private class FakeStorage : SnapshotStorage {
        val jsons = mutableMapOf<String, String>()
        override suspend fun save(key: String, json: String) { jsons[key] = json }
        override suspend fun loadOrNull(key: String): String? = jsons[key]
        override suspend fun clear(key: String) { jsons.remove(key) }
        override suspend fun clearAll() = jsons.clear()
        override suspend fun sizeBytes(key: String): Long = 0
        override suspend fun loadETagOrNull(key: String): String? = null
        override suspend fun saveETag(key: String, etag: String) = Unit
        override fun getAbsolutePathForDebug(key: String): String = key
    }
}
