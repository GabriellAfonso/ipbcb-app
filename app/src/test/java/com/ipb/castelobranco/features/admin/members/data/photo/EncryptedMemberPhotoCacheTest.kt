package com.ipb.castelobranco.features.admin.members.data.photo

import com.ipb.castelobranco.core.testing.FakeAeadCipher
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertArrayEquals
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import java.io.File

class EncryptedMemberPhotoCacheTest {

    @get:Rule
    val folder = TemporaryFolder()

    private val cipher = FakeAeadCipher()
    private var now = 1_700_000_000_000L
    private val dispatcher = StandardTestDispatcher()
    private val dir get() = File(folder.root, EncryptedMemberPhotoCache.DIRECTORY)

    private fun cache(maxBytes: Long = EncryptedMemberPhotoCache.DEFAULT_MAX_BYTES) =
        EncryptedMemberPhotoCache(dir, cipher, maxBytes, nowMillis = { now }, io = dispatcher)

    private fun photo(size: Int = 64, etag: String? = "\"v1\"") =
        CachedPhoto(etag = etag, mimeType = "image/jpeg", bytes = ByteArray(size) { (it % LETTERS + 'A'.code).toByte() })

    private companion object {
        const val LETTERS = 7
    }

    private fun url(name: String) = "https://gabrielafonso.com.br/ipbcb/media/members/$name.jpg"

    @Test
    fun `put then get returns the photo with etag and mime`() = runTest(dispatcher) {
        val cache = cache()
        val original = photo()
        cache.put(url("a"), original)

        val read = cache.get(url("a"))!!
        assertEquals("\"v1\"", read.etag)
        assertEquals("image/jpeg", read.mimeType)
        assertArrayEquals(original.bytes, read.bytes)
    }

    @Test
    fun `file name is a hash and the content is sealed`() = runTest(dispatcher) {
        cache().put(url("maria-souza"), photo())

        val file = dir.listFiles()!!.single()
        assertTrue(file.name.matches(Regex("[0-9a-f]{64}")))
        assertFalse(file.name.contains("maria"))
        val raw = String(file.readBytes(), Charsets.ISO_8859_1)
        assertFalse(raw.contains("image/jpeg"))
        assertFalse(raw.contains("ABCDEFG"))
    }

    @Test
    fun `same path on another host is the same photo`() = runTest(dispatcher) {
        val cache = cache()
        cache.put(url("a"), photo())

        assertNotNull(cache.get("http://localhost:8000/ipbcb/media/members/a.jpg"))
    }

    @Test
    fun `least recently used goes first past the limit`() = runTest(dispatcher) {
        val cache = cache(maxBytes = 250)
        cache.put(url("old"), photo(100)); now += 10_000
        cache.put(url("used"), photo(100)); now += 10_000
        cache.get(url("old")); now += 10_000
        cache.put(url("new"), photo(100))

        assertNotNull(cache.get(url("old")))
        assertNull(cache.get(url("used")))
        assertNotNull(cache.get(url("new")))
    }

    @Test
    fun `retainOnly drops photos no longer in the roll`() = runTest(dispatcher) {
        val cache = cache()
        cache.put(url("a"), photo())
        cache.put(url("b"), photo())

        cache.retainOnly(setOf(url("b")))

        assertNull(cache.get(url("a")))
        assertNotNull(cache.get(url("b")))
    }

    @Test
    fun `remove drops one photo`() = runTest(dispatcher) {
        val cache = cache()
        cache.put(url("a"), photo())

        cache.remove(url("a"))

        assertNull(cache.get(url("a")))
    }

    @Test
    fun `wipe deletes the directory and the key`() = runTest(dispatcher) {
        val cache = cache()
        cache.put(url("a"), photo())

        cache.wipe()

        assertFalse(dir.exists())
        assertEquals(1, cipher.destroyed)
        assertNull(cache.get(url("a")))
    }

    @Test
    fun `unreadable entry is a miss and is deleted`() = runTest(dispatcher) {
        val cache = cache()
        cache.put(url("a"), photo())
        cipher.loseKey()

        assertNull(cache.get(url("a")))
        assertTrue(dir.listFiles().isNullOrEmpty())
    }
}
