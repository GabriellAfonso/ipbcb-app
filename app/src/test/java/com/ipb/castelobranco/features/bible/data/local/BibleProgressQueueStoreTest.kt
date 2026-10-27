package com.ipb.castelobranco.features.bible.data.local

import com.ipb.castelobranco.core.data.local.SnapshotStorage
import com.ipb.castelobranco.features.bible.domain.model.ProgressChange
import com.ipb.castelobranco.features.bible.domain.model.ProgressTarget
import kotlinx.coroutines.test.runTest
import kotlinx.serialization.json.Json
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class BibleProgressQueueStoreTest {

    private val json = Json {
        ignoreUnknownKeys = true
        encodeDefaults = true
        isLenient = true
        explicitNulls = false
    }
    private val storage = FakeStorage()
    private val store = BibleProgressQueueStore(storage, json)

    private fun chapter(n: Int, done: Boolean, at: String) = ProgressChange(ProgressTarget.chapter("gn", n), done, at)

    @Test
    fun `a newer change of the same target replaces the older one`() = runTest {
        store.upsert(listOf(chapter(1, done = true, at = "t1"), chapter(2, done = true, at = "t1")))
        store.upsert(listOf(chapter(1, done = false, at = "t2")))

        assertEquals(listOf(chapter(2, true, "t1"), chapter(1, false, "t2")), store.readAll())
    }

    @Test
    fun `removing what was sent keeps a change made during the request`() = runTest {
        store.upsert(listOf(chapter(1, done = true, at = "t1")))
        val sent = store.readAll()
        store.upsert(listOf(chapter(1, done = false, at = "t2")))

        store.remove(sent)

        assertEquals(listOf(chapter(1, false, "t2")), store.readAll())
    }

    @Test
    fun `queue survives a restart and pending mirrors the file`() = runTest {
        store.upsert(listOf(ProgressChange(ProgressTarget.Video(42L), done = true, changedAt = "t1")))

        val restarted = BibleProgressQueueStore(storage, json)
        restarted.load()

        assertEquals(listOf(ProgressChange(ProgressTarget.Video(42L), true, "t1")), restarted.pending.value)
    }

    @Test
    fun `an unreadable file counts as empty`() = runTest {
        storage.jsons[BibleProgressQueueStore.STORAGE_KEY] = "{not json"

        assertTrue(store.readAll().isEmpty())
    }

    @Test
    fun `clear empties the file and pending`() = runTest {
        store.upsert(listOf(chapter(1, done = true, at = "t1")))

        store.clear()

        assertTrue(store.readAll().isEmpty())
        assertEquals(emptyList<ProgressChange>(), store.pending.value)
    }

    private class FakeStorage : SnapshotStorage {
        val jsons = mutableMapOf<String, String>()
        private val etags = mutableMapOf<String, String>()

        override suspend fun save(key: String, json: String) { jsons[key] = json }
        override suspend fun loadOrNull(key: String): String? = jsons[key]
        override suspend fun clear(key: String) { jsons.remove(key); etags.remove(key) }
        override suspend fun clearAll() { jsons.clear(); etags.clear() }
        override suspend fun sizeBytes(key: String): Long = (jsons[key]?.length ?: 0).toLong()
        override suspend fun loadETagOrNull(key: String): String? = etags[key]
        override suspend fun saveETag(key: String, etag: String) { etags[key] = etag }
        override fun getAbsolutePathForDebug(key: String): String = "/fake/$key.json"
    }
}
