package com.ipb.castelobranco.features.hymnal.data.repository

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import androidx.datastore.preferences.core.Preferences
import com.ipb.castelobranco.core.data.local.SnapshotStorage
import com.ipb.castelobranco.core.domain.auth.AuthStatusProvider
import com.ipb.castelobranco.features.hymnal.data.local.HymnViewQueueStore
import com.ipb.castelobranco.features.hymnal.data.local.HymnViewSettingsStore
import com.ipb.castelobranco.features.hymnal.data.local.QueuedHymnViewEvent
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.Job
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runTest
import kotlinx.serialization.json.Json
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test
import java.io.File

/**
 * Guards against a background upload signing the member out.
 *
 * `AuthInterceptor` attaches whatever access token is stored, valid or not. If the server answers
 * 401, `TokenAuthenticator` attempts a refresh and **clears the token store** when that refresh
 * fails — a silent logout triggered by telemetry the member never asked for. The repository must
 * therefore gate on the token still being valid, not merely on the member being "logged in".
 */
@OptIn(ExperimentalCoroutinesApi::class)
class HymnViewHistoryClientSelectionTest {

    private val testScope = TestScope(UnconfinedTestDispatcher() + Job())

    private lateinit var tempFile: File
    private lateinit var dataStore: DataStore<Preferences>
    private lateinit var queueStore: HymnViewQueueStore
    private lateinit var settingsStore: HymnViewSettingsStore
    private lateinit var authedApi: HymnViewHistoryRepositoryImplTest.FakeApi
    private lateinit var authLessApi: HymnViewHistoryRepositoryImplTest.FakeApi

    private val json = Json {
        ignoreUnknownKeys = true
        encodeDefaults = true
        isLenient = true
        explicitNulls = false
    }

    @Before
    fun setUp() {
        tempFile = File.createTempFile("client_selection_test", ".preferences_pb")
            .also { it.deleteOnExit() }
        dataStore = PreferenceDataStoreFactory.create(scope = testScope, produceFile = { tempFile })
        queueStore = HymnViewQueueStore(FakeStorage(), json)
        settingsStore = HymnViewSettingsStore(dataStore)
        authedApi = HymnViewHistoryRepositoryImplTest.FakeApi()
        authLessApi = HymnViewHistoryRepositoryImplTest.FakeApi()
    }

    @After
    fun tearDown() {
        tempFile.delete()
    }

    private fun repository(hasValidToken: Boolean) = HymnViewHistoryRepositoryImpl(
        authedApi = authedApi,
        authLessApi = authLessApi,
        queueStore = queueStore,
        settingsStore = settingsStore,
        authStatusProvider = AuthStatusProvider { hasValidToken },
    )

    @Test
    fun `a valid access token uses the authenticated client`() = testScope.runTest {
        queueStore.append(event())

        repository(hasValidToken = true).syncOnce()

        assertEquals(1, authedApi.requests.size)
        assertEquals(0, authLessApi.requests.size)
    }

    @Test
    fun `an expired access token uses the anonymous client`() = testScope.runTest {
        queueStore.append(event())

        // hasValidAccessToken() is false for an expired token even though tokens are stored.
        repository(hasValidToken = false).syncOnce()

        assertEquals(0, authedApi.requests.size)
        assertEquals(1, authLessApi.requests.size)
    }

    @Test
    fun `no session at all uses the anonymous client`() = testScope.runTest {
        queueStore.append(event())

        repository(hasValidToken = false).syncOnce()

        assertEquals(1, authLessApi.requests.size)
    }

    private fun event() = QueuedHymnViewEvent(
        clientEventId = "0b7f2c1e-6a3d-4f89-9b21-4c0f5e6d7a88",
        hymnId = 42,
        deviceId = "device",
        viewedAt = "2026-08-09T19:34:12-03:00",
        durationSeconds = 30,
        appVersion = "1.0.0",
        platform = "android",
    )

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
