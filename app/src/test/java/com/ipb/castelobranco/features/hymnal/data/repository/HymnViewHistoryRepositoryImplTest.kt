package com.ipb.castelobranco.features.hymnal.data.repository

import com.ipb.castelobranco.core.data.local.SnapshotStorage
import com.ipb.castelobranco.core.domain.auth.AuthStatusProvider
import com.ipb.castelobranco.core.domain.error.AppError
import com.ipb.castelobranco.features.hymnal.data.api.HymnalHistoryApi
import com.ipb.castelobranco.features.hymnal.data.dto.HistorySettingsDto
import com.ipb.castelobranco.features.hymnal.data.dto.IngestRequestDto
import com.ipb.castelobranco.features.hymnal.data.dto.IngestResponseDto
import com.ipb.castelobranco.features.hymnal.data.dto.RejectedEventDto
import com.ipb.castelobranco.features.hymnal.data.local.HymnViewQueueStore
import com.ipb.castelobranco.features.hymnal.data.local.HymnViewSettingsStore
import com.ipb.castelobranco.features.hymnal.data.local.QueuedHymnViewEvent
import com.ipb.castelobranco.features.hymnal.domain.model.SyncOutcome
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import androidx.datastore.preferences.core.Preferences
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.Job
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runTest
import kotlinx.serialization.json.Json
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.ResponseBody.Companion.toResponseBody
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import retrofit2.Response
import java.io.File
import java.io.IOException

@OptIn(ExperimentalCoroutinesApi::class)
class HymnViewHistoryRepositoryImplTest {

    private val testScope = TestScope(UnconfinedTestDispatcher() + Job())

    private lateinit var tempFile: File
    private lateinit var dataStore: DataStore<Preferences>
    private lateinit var storage: FakeStorage
    private lateinit var queueStore: HymnViewQueueStore
    private lateinit var settingsStore: HymnViewSettingsStore
    private lateinit var authedApi: FakeApi
    private lateinit var authLessApi: FakeApi

    private val json = Json {
        ignoreUnknownKeys = true
        encodeDefaults = true
        isLenient = true
        explicitNulls = false
    }

    @Before
    fun setUp() {
        tempFile = File.createTempFile("history_settings_test", ".preferences_pb")
            .also { it.deleteOnExit() }
        dataStore = PreferenceDataStoreFactory.create(scope = testScope, produceFile = { tempFile })
        storage = FakeStorage()
        queueStore = HymnViewQueueStore(storage, json)
        settingsStore = HymnViewSettingsStore(dataStore)
        authedApi = FakeApi()
        authLessApi = FakeApi()
    }

    @After
    fun tearDown() {
        tempFile.delete()
    }

    private fun repository(hasValidToken: Boolean = false) = HymnViewHistoryRepositoryImpl(
        authedApi = authedApi,
        authLessApi = authLessApi,
        queueStore = queueStore,
        settingsStore = settingsStore,
        authStatusProvider = AuthStatusProvider { hasValidToken },
    )

    // region reconciliation

    @Test
    fun `removes accepted rejected and unmentioned ids alike`() = testScope.runTest {
        listOf("a", "b", "c").forEach { queueStore.append(event(it)) }
        authLessApi.ingestResponses += Response.success(
            IngestResponseDto(
                accepted = listOf("a"),
                rejected = listOf(RejectedEventDto("b", "unknown_hymn")),
                // "c" appears in neither list — the service silently dropped it.
            )
        )

        val outcome = repository().syncOnce()

        assertTrue(outcome is SyncOutcome.Delivered)
        assertTrue("queue must drain completely", queueStore.readAll().isEmpty())
    }

    @Test
    fun `leaves events that were never submitted`() = testScope.runTest {
        settingsStore.write(settings(maxBatch = 3))
        listOf("a", "b", "c", "d").forEach { queueStore.append(event(it)) }
        authLessApi.ingestResponses += Response.success(
            IngestResponseDto(accepted = listOf("a", "b", "c"))
        )

        repository().syncOnce()

        assertEquals(listOf("d"), queueStore.readAll().map { it.clientEventId })
    }

    @Test
    fun `chunks the upload at the configured batch size`() = testScope.runTest {
        settingsStore.write(settings(maxBatch = 50))
        repeat(120) { queueStore.append(event("id-$it")) }
        repeat(3) { authLessApi.ingestResponses += Response.success(IngestResponseDto()) }

        val repo = repository()
        repo.syncOnce()
        repo.syncOnce()
        repo.syncOnce()

        assertEquals(listOf(50, 50, 20), authLessApi.requests.map { it.events.size })
        assertTrue(queueStore.readAll().isEmpty())
    }

    @Test
    fun `empty queue issues no request`() = testScope.runTest {
        val outcome = repository().syncOnce()

        assertTrue(outcome is SyncOutcome.Delivered)
        assertTrue(authedApi.requests.isEmpty())
        assertTrue(authLessApi.requests.isEmpty())
    }

    // endregion

    // region failure handling

    @Test
    fun `429 keeps everything queued and defers`() = testScope.runTest {
        queueStore.append(event("a"))
        authLessApi.ingestResponses += errorResponse(429)

        val outcome = repository().syncOnce()

        assertTrue(outcome is SyncOutcome.Deferred)
        assertEquals(1, queueStore.count())
    }

    @Test
    fun `server error keeps everything queued and defers`() = testScope.runTest {
        queueStore.append(event("a"))
        authLessApi.ingestResponses += errorResponse(500)

        val outcome = repository().syncOnce()

        assertTrue(outcome is SyncOutcome.Deferred)
        assertEquals(1, queueStore.count())
    }

    @Test
    fun `network failure keeps everything queued and defers`() = testScope.runTest {
        queueStore.append(event("a"))
        authLessApi.throwOnIngest = IOException("offline")

        val outcome = repository().syncOnce()

        assertTrue(outcome is SyncOutcome.Deferred)
        assertTrue((outcome as SyncOutcome.Deferred).error is AppError.Network)
        assertEquals(1, queueStore.count())
    }

    @Test
    fun `400 discards the chunk rather than retrying forever`() = testScope.runTest {
        queueStore.append(event("a"))
        queueStore.append(event("b"))
        authLessApi.ingestResponses += errorResponse(400, """{"error_code":"VALIDATION_ERROR"}""")

        val outcome = repository().syncOnce()

        assertTrue(outcome is SyncOutcome.Discarded)
        assertTrue(queueStore.readAll().isEmpty())
    }

    // endregion

    // region settings

    @Test
    fun `refreshSettings persists the two consumed values`() = testScope.runTest {
        authLessApi.settingsResponse = Response.success(
            HistorySettingsDto(minSecondsToCount = 45, maxBatchSize = 200)
        )

        repository().refreshSettings()

        assertEquals(45, settingsStore.read().minSecondsToCount)
        assertEquals(200, settingsStore.read().maxBatchSize)
    }

    @Test
    fun `refreshSettings failure leaves the previous values intact`() = testScope.runTest {
        settingsStore.write(settings(minSeconds = 45, maxBatch = 200))
        authLessApi.throwOnSettings = IOException("offline")

        repository().refreshSettings()

        assertEquals(45, settingsStore.read().minSecondsToCount)
        assertEquals(200, settingsStore.read().maxBatchSize)
    }

    @Test
    fun `fresh install with a failing fetch falls back to defaults`() = testScope.runTest {
        authLessApi.throwOnSettings = IOException("offline")

        val repo = repository()
        repo.refreshSettings()

        assertEquals(30, repo.currentSettings().minSecondsToCount)
        assertEquals(50, repo.currentSettings().maxBatchSize)
    }

    @Test
    fun `settings are read through the anonymous client`() = testScope.runTest {
        authLessApi.settingsResponse = Response.success(HistorySettingsDto())

        repository(hasValidToken = true).refreshSettings()

        assertEquals(1, authLessApi.settingsCalls)
        assertEquals(0, authedApi.settingsCalls)
    }

    // endregion

    private fun settings(minSeconds: Int = 30, maxBatch: Int = 50) =
        com.ipb.castelobranco.features.hymnal.domain.model.HymnViewCollectionSettings(
            minSecondsToCount = minSeconds,
            maxBatchSize = maxBatch,
        )

    private fun event(id: String) = QueuedHymnViewEvent(
        clientEventId = id,
        hymnId = 42,
        deviceId = "device",
        viewedAt = "2026-08-09T19:34:12-03:00",
        durationSeconds = 30,
        appVersion = "1.0.0",
        platform = "android",
    )

    private fun errorResponse(code: Int, body: String = "{}"): Response<IngestResponseDto> =
        Response.error(code, body.toResponseBody("application/json".toMediaType()))

    class FakeApi : HymnalHistoryApi {
        val requests = mutableListOf<IngestRequestDto>()
        val ingestResponses = ArrayDeque<Response<IngestResponseDto>>()
        var throwOnIngest: Throwable? = null

        var settingsResponse: Response<HistorySettingsDto> = Response.success(HistorySettingsDto())
        var throwOnSettings: Throwable? = null
        var settingsCalls = 0

        override suspend fun submitEvents(body: IngestRequestDto): Response<IngestResponseDto> {
            requests += body
            throwOnIngest?.let { throw it }
            return ingestResponses.removeFirstOrNull() ?: Response.success(IngestResponseDto())
        }

        override suspend fun getSettings(): Response<HistorySettingsDto> {
            settingsCalls++
            throwOnSettings?.let { throw it }
            return settingsResponse
        }
    }

    private class FakeStorage : SnapshotStorage {
        private val jsons = mutableMapOf<String, String>()
        private val etags = mutableMapOf<String, String>()

        override suspend fun save(key: String, json: String) { jsons[key] = json }
        override suspend fun loadOrNull(key: String): String? = jsons[key]
        override suspend fun clear(key: String) { jsons.remove(key); etags.remove(key) }
        override suspend fun clearAll() { jsons.clear(); etags.clear() }
        override suspend fun sizeBytes(key: String): Long =
            (jsons[key]?.length ?: 0).toLong() + (etags[key]?.length ?: 0).toLong()
        override suspend fun loadETagOrNull(key: String): String? = etags[key]
        override suspend fun saveETag(key: String, etag: String) { etags[key] = etag }
        override fun getAbsolutePathForDebug(key: String): String = "/fake/$key.json"
    }
}
