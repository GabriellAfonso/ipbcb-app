package com.ipb.castelobranco.core.data.snapshot

import com.ipb.castelobranco.core.data.local.SnapshotStorage
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class LocalSnapshotCacheTest {

    private lateinit var storage: FakeStorage
    private lateinit var codec: FakeCodec
    private lateinit var cache: LocalSnapshotCache<String>

    @Before
    fun setUp() {
        storage = FakeStorage()
        codec = FakeCodec()
        cache = LocalSnapshotCache(storage, codec, key = "test_key")
    }

    @Test
    fun `load returns null when storage is empty`() = runTest {
        val result = cache.load()

        assertNull(result)
    }

    @Test
    fun `load decodes via codec when storage has data`() = runTest {
        storage.jsons["test_key"] = "encoded:hello"

        val result = cache.load()

        assertEquals("hello", result)
    }

    @Test
    fun `save encodes value and persists to storage`() = runTest {
        cache.save("hello", etag = null)

        assertEquals("encoded:hello", storage.jsons["test_key"])
    }

    @Test
    fun `save with etag also saves etag to storage`() = runTest {
        cache.save("hello", etag = "W/\"abc\"")

        assertEquals("W/\"abc\"", storage.etags["test_key"])
    }

    @Test
    fun `save with null etag does not save etag`() = runTest {
        cache.save("hello", etag = null)

        assertNull(storage.etags["test_key"])
    }

    @Test
    fun `clear delegates to storage`() = runTest {
        storage.jsons["test_key"] = "encoded:data"
        storage.etags["test_key"] = "some-etag"

        cache.clear()

        assertNull(storage.jsons["test_key"])
        assertNull(storage.etags["test_key"])
    }

    @Test
    fun `exists reports stored data without decoding it`() = runTest {
        storage.jsons["test_key"] = "encoded:hello"

        assertTrue(cache.exists())
        assertEquals(0, codec.decodes)
    }

    @Test
    fun `exists is false when storage is empty`() = runTest {
        assertFalse(cache.exists())
    }

    // region fakes

    class FakeStorage : SnapshotStorage {
        val jsons = mutableMapOf<String, String>()
        val etags = mutableMapOf<String, String>()

        override suspend fun save(key: String, json: String) { jsons[key] = json }
        override suspend fun loadOrNull(key: String): String? = jsons[key]
        override suspend fun clear(key: String) { jsons.remove(key); etags.remove(key) }
        override suspend fun clearAll() { jsons.clear(); etags.clear() }
        override suspend fun loadETagOrNull(key: String): String? = etags[key]
        override suspend fun saveETag(key: String, etag: String) { etags[key] = etag }
        override fun getAbsolutePathForDebug(key: String): String = "/fake/$key.json"
    }

    class FakeCodec : SnapshotCodec<String> {
        override fun encode(value: String): String = "encoded:$value"
        var decodes = 0
        override fun decode(raw: String): String = raw.removePrefix("encoded:").also { decodes++ }
    }

    // endregion
}
