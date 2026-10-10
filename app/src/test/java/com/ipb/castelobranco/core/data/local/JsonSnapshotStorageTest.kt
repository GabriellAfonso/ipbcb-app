package com.ipb.castelobranco.core.data.local

import android.content.Context
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import java.io.File

class JsonSnapshotStorageTest {

    private lateinit var context: Context
    private lateinit var tempDir: File
    private lateinit var storage: JsonSnapshotStorage

    @Before
    fun setUp() {
        tempDir = File(System.getProperty("java.io.tmpdir"), "json_snapshot_storage_test_${System.nanoTime()}")
        tempDir.mkdirs()

        context = mockk()
        every { context.filesDir } returns tempDir

        storage = JsonSnapshotStorage(context, Dispatchers.Unconfined)
    }

    @After
    fun tearDown() {
        tempDir.deleteRecursively()
    }

    // region save / loadOrNull

    @Test
    fun `save and loadOrNull round-trip returns stored json`() = runTest {
        storage.save("my_key", """{"data":"value"}""")

        val result = storage.loadOrNull("my_key")

        assertEquals("""{"data":"value"}""", result)
    }

    @Test
    fun `loadOrNull returns null for non-existent key`() = runTest {
        val result = storage.loadOrNull("missing_key")

        assertNull(result)
    }

    // endregion

    // region exists

    @Test
    fun `exists is true after save and false after clear`() = runTest {
        assertFalse(storage.exists("bible_naa"))

        storage.save("bible_naa", "[]")
        assertTrue(storage.exists("bible_naa"))

        storage.clear("bible_naa")
        assertFalse(storage.exists("bible_naa"))
    }

    // endregion

    // region clear

    @Test
    fun `clear removes json file so loadOrNull returns null`() = runTest {
        storage.save("to_clear", "some content")
        storage.clear("to_clear")

        val result = storage.loadOrNull("to_clear")

        assertNull(result)
    }

    @Test
    fun `clear on non-existent key does not throw`() = runTest {
        storage.clear("never_saved")
    }

    @Test
    fun `clear also removes etag file`() = runTest {
        storage.save("keyed", "content")
        storage.saveETag("keyed", "etag-abc")

        storage.clear("keyed")

        assertNull(storage.loadETagOrNull("keyed"))
    }

    // endregion

    // region saveETag / loadETagOrNull

    @Test
    fun `saveETag and loadETagOrNull round-trip returns stored etag`() = runTest {
        storage.saveETag("data_key", "W/\"abc123\"")

        val result = storage.loadETagOrNull("data_key")

        assertEquals("W/\"abc123\"", result)
    }

    @Test
    fun `loadETagOrNull returns null for non-existent key`() = runTest {
        val result = storage.loadETagOrNull("no_etag_key")

        assertNull(result)
    }

    // endregion

    // region clearAll

    @Test
    fun `clearAll removes all stored files`() = runTest {
        storage.save("key1", "aaa")
        storage.save("key2", "bbb")
        storage.saveETag("key1", "etag1")

        storage.clearAll()

        assertNull(storage.loadOrNull("key1"))
        assertNull(storage.loadOrNull("key2"))
        assertNull(storage.loadETagOrNull("key1"))
    }

    // endregion

    // region safeKey normalization

    @Test
    fun `safeKey normalizes uppercase spaces and special characters`() = runTest {
        storage.save("My Key/With:Specials!", "content")

        val loaded = storage.loadOrNull("My Key/With:Specials!")

        assertEquals("content", loaded)
    }

    @Test
    fun `safeKey with only special characters throws IllegalArgumentException`() = runTest {
        try {
            storage.save("!!!###", "content")
            assert(false) { "Expected IllegalArgumentException" }
        } catch (e: IllegalArgumentException) {
            assertTrue(e.message?.contains("inválida") == true)
        }
    }

    // endregion

    // region getAbsolutePathForDebug

    @Test
    fun `getAbsolutePathForDebug returns path ending in json`() {
        val path = storage.getAbsolutePathForDebug("debug_key")

        assertTrue(path.endsWith(".json"))
        assertTrue(path.contains("debug_key"))
    }

    // endregion
}
