package com.ipb.castelobranco.features.admin.members.data.photo

import com.ipb.castelobranco.core.domain.error.AppError
import com.ipb.castelobranco.core.testing.FakeAeadCipher
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import okhttp3.Interceptor
import okhttp3.OkHttpClient
import okhttp3.Protocol
import okhttp3.Request
import okhttp3.Response
import okhttp3.ResponseBody.Companion.toResponseBody
import org.junit.Assert.assertArrayEquals
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import java.io.File
import java.io.IOException

class MemberPhotoSourceTest {

    @get:Rule
    val folder = TemporaryFolder()

    private val dispatcher = StandardTestDispatcher()
    private val cipher = FakeAeadCipher()
    private val requests = mutableListOf<Request>()
    private var respond: (Request) -> Response = { ok(it, V1) }

    private val client = OkHttpClient.Builder()
        .addInterceptor(Interceptor { chain ->
            requests += chain.request()
            respond(chain.request())
        })
        .build()

    private val cache by lazy { EncryptedMemberPhotoCache(File(folder.root, "photos"), cipher, io = dispatcher) }

    private fun TestScope.source() = MemberPhotoSource(cache, client, this, io = dispatcher)

    @Test
    fun `miss downloads and keeps an encrypted copy`() = runTest(dispatcher) {
        val photo = source().load(URL)!!

        assertArrayEquals(V1, photo.bytes)
        assertEquals("image/jpeg", photo.mimeType)
        assertNotNull(cache.get(URL))
    }

    @Test
    fun `hit returns the copy and revalidates with its etag`() = runTest(dispatcher) {
        val source = source()
        source.load(URL)
        respond = { status(it, 304) }

        val photo = source.load(URL)!!
        advanceUntilIdle()

        assertArrayEquals(V1, photo.bytes)
        assertEquals(ETAG, requests.last().header("If-None-Match"))
        assertTrue(source.revisions.value[URL] == null)
    }

    @Test
    fun `changed photo replaces the copy and bumps the revision`() = runTest(dispatcher) {
        val source = source()
        source.load(URL)
        respond = { ok(it, V2, etag = "\"v2\"") }

        source.load(URL)
        advanceUntilIdle()

        assertArrayEquals(V2, cache.get(URL)!!.bytes)
        assertEquals(1, source.revisions.value[URL])
    }

    @Test
    fun `photo gone from the server is dropped`() = runTest(dispatcher) {
        val source = source()
        source.load(URL)
        respond = { status(it, 404) }

        source.load(URL)
        advanceUntilIdle()

        assertNull(cache.get(URL))
        assertEquals(1, source.revisions.value[URL])
    }

    @Test
    fun `refused revalidation wipes the cache and its key`() = runTest(dispatcher) {
        val source = source()
        source.load(URL)
        respond = { status(it, 403) }

        source.load(URL)
        advanceUntilIdle()

        assertNull(cache.get(URL))
        assertEquals(1, cipher.destroyed)
    }

    @Test
    fun `rate limit and network errors keep the copy`() = runTest(dispatcher) {
        val source = source()
        source.load(URL)
        respond = { status(it, 429) }
        source.load(URL)
        advanceUntilIdle()
        respond = { throw IOException("offline") }
        source.load(URL)
        advanceUntilIdle()

        assertNotNull(cache.get(URL))
    }

    @Test
    fun `refused download wipes and reports access lost`() = runTest(dispatcher) {
        val source = source()
        respond = { status(it, 401) }

        val result = source.bytesFor(URL)

        assertTrue(result.exceptionOrNull() is AppError.Auth)
        assertEquals(1, cipher.destroyed)
    }

    @Test
    fun `missing photo shows initials without an error`() = runTest(dispatcher) {
        respond = { status(it, 404) }

        assertNull(source().load(URL))
    }

    @Test
    fun `remove drops the copy and bumps the revision`() = runTest(dispatcher) {
        val source = source()
        source.load(URL)

        source.remove(URL)

        assertNull(cache.get(URL))
        assertEquals(1, source.revisions.value[URL])
    }

    private fun ok(request: Request, bytes: ByteArray, etag: String = ETAG) = Response.Builder()
        .request(request)
        .protocol(Protocol.HTTP_1_1)
        .code(200)
        .message("OK")
        .header("ETag", etag)
        .header("Content-Type", "image/jpeg")
        .body(bytes.toResponseBody())
        .build()

    private fun status(request: Request, code: Int) = Response.Builder()
        .request(request)
        .protocol(Protocol.HTTP_1_1)
        .code(code)
        .message("status")
        .body(ByteArray(0).toResponseBody())
        .build()

    private companion object {
        const val URL = "https://gabrielafonso.com.br/ipbcb/media/members/abc.jpg"
        const val ETAG = "\"v1\""
        val V1 = byteArrayOf(0xFF.toByte(), 0xD8.toByte(), 0xFF.toByte(), 1)
        val V2 = byteArrayOf(0xFF.toByte(), 0xD8.toByte(), 0xFF.toByte(), 2)
    }
}
