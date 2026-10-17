package com.ipb.castelobranco.features.hymnal.data.local

import com.ipb.castelobranco.core.data.local.SnapshotStorage
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.test.runTest
import kotlinx.serialization.json.Json
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class HymnViewQueueStoreTest {

    private lateinit var storage: FakeStorage
    private lateinit var store: HymnViewQueueStore

    private val json = Json {
        ignoreUnknownKeys = true
        encodeDefaults = true
        isLenient = true
        explicitNulls = false
    }

    @Before
    fun setUp() {
        storage = FakeStorage()
        store = HymnViewQueueStore(storage, json)
    }

    @Test
    fun `append then read returns events in insertion order`() = runTest {
        store.append(event("a"))
        store.append(event("b"))
        store.append(event("c"))

        assertEquals(listOf("a", "b", "c"), store.readAll().map { it.clientEventId })
    }

    @Test
    fun `queue survives a restart`() = runTest {
        store.append(event("a"))
        store.append(event("b"))
        store.append(event("c"))

        // A new store over the same storage stands in for an app restart.
        val restarted = HymnViewQueueStore(storage, json)

        assertEquals(listOf("a", "b", "c"), restarted.readAll().map { it.clientEventId })
    }

    @Test
    fun `cap drops the oldest events`() = runTest {
        repeat(2500) { store.append(event("id-$it")) }

        val remaining = store.readAll()

        assertEquals(HymnViewQueueStore.MAX_QUEUE_SIZE, remaining.size)
        assertEquals("id-500", remaining.first().clientEventId)
        assertEquals("id-2499", remaining.last().clientEventId)
    }

    @Test
    fun `concurrent appends do not lose events`() = runTest {
        val appends = (0 until 100).map { index ->
            async { store.append(event("id-$index")) }
        }
        appends.awaitAll()

        val stored = store.readAll()

        assertEquals(100, stored.size)
        assertEquals(100, stored.map { it.clientEventId }.toSet().size)
    }

    @Test
    fun `remove deletes only the given ids and preserves order`() = runTest {
        listOf("a", "b", "c", "d", "e").forEach { store.append(event(it)) }

        store.remove(setOf("b", "d"))

        assertEquals(listOf("a", "c", "e"), store.readAll().map { it.clientEventId })
    }

    @Test
    fun `remove with an empty set changes nothing`() = runTest {
        store.append(event("a"))

        store.remove(emptySet())

        assertEquals(1, store.count())
    }

    @Test
    fun `reading a never-written store returns empty`() = runTest {
        assertTrue(store.readAll().isEmpty())
        assertEquals(0, store.count())
    }

    @Test
    fun `corrupt queue file reads as empty instead of throwing`() = runTest {
        storage.jsons[HymnViewQueueStore.STORAGE_KEY] = "{ this is not valid json"

        assertTrue(store.readAll().isEmpty())
    }

    @Test
    fun `appending onto a corrupt file recovers rather than failing`() = runTest {
        storage.jsons[HymnViewQueueStore.STORAGE_KEY] = "not json at all"

        store.append(event("a"))

        assertEquals(listOf("a"), store.readAll().map { it.clientEventId })
    }

    private fun event(id: String) = QueuedHymnViewEvent(
        clientEventId = id,
        hymnId = 42,
        deviceId = "device",
        viewedAt = "2026-08-09T19:34:12-03:00",
        durationSeconds = 30,
        appVersion = "1.0.0",
        platform = "android",
    )

    private class FakeStorage : SnapshotStorage {
        val jsons = mutableMapOf<String, String>()
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
