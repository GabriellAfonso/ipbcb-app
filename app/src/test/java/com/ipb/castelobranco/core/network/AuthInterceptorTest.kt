package com.ipb.castelobranco.core.network

import com.ipb.castelobranco.features.auth.data.local.TokenStorage
import com.ipb.castelobranco.features.auth.domain.model.AuthTokens
import io.mockk.every
import io.mockk.mockk
import io.mockk.slot
import io.mockk.verify
import okhttp3.Interceptor
import okhttp3.Protocol
import okhttp3.Request
import okhttp3.Response
import okhttp3.ResponseBody.Companion.toResponseBody
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Before
import org.junit.Test

class AuthInterceptorTest {

    private lateinit var tokenStorage: TokenStorage
    private lateinit var interceptor: AuthInterceptor
    private lateinit var chain: Interceptor.Chain

    @Before
    fun setUp() {
        tokenStorage = mockk()
        interceptor = AuthInterceptor(tokenStorage)
        chain = mockk()
    }

    @Test
    fun `adds Authorization header when token is available`() {
        every { tokenStorage.peekOrNull() } returns AuthTokens(access = "my-token", refresh = "r")

        val requestSlot = slot<Request>()
        every { chain.request() } returns buildRequest()
        every { chain.proceed(capture(requestSlot)) } returns buildOkResponse()

        interceptor.intercept(chain)

        assertEquals("Bearer my-token", requestSlot.captured.header("Authorization"))
    }

    @Test
    fun `does not add Authorization header when token is null`() {
        every { tokenStorage.peekOrNull() } returns null

        val requestSlot = slot<Request>()
        every { chain.request() } returns buildRequest()
        every { chain.proceed(capture(requestSlot)) } returns buildOkResponse()

        interceptor.intercept(chain)

        assertNull(requestSlot.captured.header("Authorization"))
    }

    @Test
    fun `does not add Authorization header when access token is blank`() {
        every { tokenStorage.peekOrNull() } returns AuthTokens(access = "  ", refresh = "r")

        val requestSlot = slot<Request>()
        every { chain.request() } returns buildRequest()
        every { chain.proceed(capture(requestSlot)) } returns buildOkResponse()

        interceptor.intercept(chain)

        assertNull(requestSlot.captured.header("Authorization"))
    }

    @Test
    fun `does not overwrite manually set Authorization header`() {
        every { tokenStorage.peekOrNull() } returns AuthTokens(access = "other-token", refresh = "r")

        val originalRequest = buildRequest(authHeader = "Bearer manual-token")
        val requestSlot = slot<Request>()
        every { chain.request() } returns originalRequest
        every { chain.proceed(capture(requestSlot)) } returns buildOkResponse()

        interceptor.intercept(chain)

        assertEquals("Bearer manual-token", requestSlot.captured.header("Authorization"))
        verify(exactly = 0) { tokenStorage.peekOrNull() }
    }

    // region helpers

    private fun buildRequest(authHeader: String? = null): Request =
        Request.Builder()
            .url("https://api.example.com/data")
            .apply { if (authHeader != null) header("Authorization", authHeader) }
            .build()

    private fun buildOkResponse(): Response =
        Response.Builder()
            .request(buildRequest())
            .protocol(Protocol.HTTP_1_1)
            .code(200)
            .message("OK")
            .body("".toResponseBody())
            .build()

    // endregion
}
