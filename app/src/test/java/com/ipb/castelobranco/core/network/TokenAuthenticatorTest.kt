package com.ipb.castelobranco.core.network

import com.ipb.castelobranco.features.auth.data.api.AuthApi
import com.ipb.castelobranco.features.auth.data.dto.RefreshRequest
import com.ipb.castelobranco.features.auth.data.local.TokenStorage
import com.ipb.castelobranco.features.auth.domain.model.AuthTokens
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import okhttp3.Protocol
import okhttp3.Request
import okhttp3.Response
import okhttp3.ResponseBody.Companion.toResponseBody
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Before
import org.junit.Test
import retrofit2.Response as RetrofitResponse

class TokenAuthenticatorTest {

    private lateinit var authApi: AuthApi
    private lateinit var tokenStorage: TokenStorage
    private lateinit var authenticator: TokenAuthenticator

    private val storedTokens = AuthTokens(access = "old-access", refresh = "valid-refresh")
    private val newTokens = AuthTokens(access = "new-access", refresh = "new-refresh")
    private val passwordChangeTokens = AuthTokens(access = "pw-access", refresh = "pw-refresh")

    @Before
    fun setUp() {
        authApi = mockk()
        tokenStorage = mockk()
        authenticator = TokenAuthenticator(authApi, tokenStorage)
    }

    // region retry guard

    @Test
    fun `authenticate returns null when responseCount is 2 or more`() {
        val request = buildRequest()
        val priorResponse = buildUnauthorizedResponse(request)
        val response = buildUnauthorizedResponse(request, prior = priorResponse)

        val result = authenticator.authenticate(null, response)

        assertNull(result)
    }

    // endregion

    // region no stored tokens

    @Test
    fun `authenticate returns null when no tokens stored`() {
        every { tokenStorage.peekOrNull() } returns null

        val result = authenticator.authenticate(null, buildUnauthorizedResponse(buildRequest()))

        assertNull(result)
    }

    // endregion

    // region token already refreshed by another coroutine

    @Test
    fun `authenticate retries with stored token when header differs from current access`() {
        every { tokenStorage.peekOrNull() } returns storedTokens

        val request = buildRequest(authHeader = "Bearer stale-access")
        val response = buildUnauthorizedResponse(request)

        val result = authenticator.authenticate(null, response)

        assertNotNull(result)
        assertEquals("Bearer old-access", result!!.header("Authorization"))
        coVerify(exactly = 0) { authApi.refresh(any()) }
    }

    // endregion

    // region blank refresh token

    @Test
    fun `authenticate returns null when stored refresh token is blank`() {
        every { tokenStorage.peekOrNull() } returns AuthTokens(access = "old-access", refresh = "")

        val request = buildRequest(authHeader = "Bearer old-access")
        val result = authenticator.authenticate(null, buildUnauthorizedResponse(request))

        assertNull(result)
    }

    // endregion

    // region api throws exception

    @Test
    fun `authenticate returns null when authApi refresh throws`() = runTest {
        every { tokenStorage.peekOrNull() } returns storedTokens
        coEvery { authApi.refresh(any()) } throws RuntimeException("network error")

        val request = buildRequest(authHeader = "Bearer old-access")
        val result = authenticator.authenticate(null, buildUnauthorizedResponse(request))

        assertNull(result)
    }

    // endregion

    // region http error responses

    @Test
    fun `authenticate clears storage and returns null on 401 refresh response`() = runTest {
        every { tokenStorage.peekOrNull() } returns storedTokens
        coEvery { authApi.refresh(RefreshRequest(refresh = "valid-refresh")) } returns
                buildRetrofitResponse(code = 401)
        coEvery { tokenStorage.swapIfRefresh("valid-refresh", null) } returns null

        val request = buildRequest(authHeader = "Bearer old-access")
        val result = authenticator.authenticate(null, buildUnauthorizedResponse(request))

        assertNull(result)
        coVerify { tokenStorage.swapIfRefresh("valid-refresh", null) }
    }

    @Test
    fun `authenticate clears storage and returns null on 400 refresh response`() = runTest {
        every { tokenStorage.peekOrNull() } returns storedTokens
        coEvery { authApi.refresh(RefreshRequest(refresh = "valid-refresh")) } returns
                buildRetrofitResponse(code = 400)
        coEvery { tokenStorage.swapIfRefresh("valid-refresh", null) } returns null

        val request = buildRequest(authHeader = "Bearer old-access")
        val result = authenticator.authenticate(null, buildUnauthorizedResponse(request))

        assertNull(result)
        coVerify { tokenStorage.swapIfRefresh("valid-refresh", null) }
    }

    @Test
    fun `authenticate does not clear storage and returns null on 500 refresh response`() = runTest {
        every { tokenStorage.peekOrNull() } returns storedTokens
        coEvery { authApi.refresh(RefreshRequest(refresh = "valid-refresh")) } returns
                buildRetrofitResponse(code = 500)

        val request = buildRequest(authHeader = "Bearer old-access")
        val result = authenticator.authenticate(null, buildUnauthorizedResponse(request))

        assertNull(result)
        coVerify(exactly = 0) { tokenStorage.swapIfRefresh(any(), any()) }
    }

    // endregion

    // region successful refresh

    @Test
    fun `authenticate returns null when refresh response body is null`() = runTest {
        every { tokenStorage.peekOrNull() } returns storedTokens
        coEvery { authApi.refresh(RefreshRequest(refresh = "valid-refresh")) } returns
                buildRetrofitResponse(code = 200, body = null)

        val request = buildRequest(authHeader = "Bearer old-access")
        val result = authenticator.authenticate(null, buildUnauthorizedResponse(request))

        assertNull(result)
    }

    @Test
    fun `authenticate saves new tokens and returns request with new Bearer on successful refresh`() = runTest {
        every { tokenStorage.peekOrNull() } returns storedTokens
        coEvery { authApi.refresh(RefreshRequest(refresh = "valid-refresh")) } returns
                buildRetrofitResponse(code = 200, body = newTokens)
        coEvery { tokenStorage.swapIfRefresh("valid-refresh", newTokens) } returns newTokens

        val request = buildRequest(authHeader = "Bearer old-access")
        val result = authenticator.authenticate(null, buildUnauthorizedResponse(request))

        assertNotNull(result)
        assertEquals("Bearer new-access", result!!.header("Authorization"))
        coVerify { tokenStorage.swapIfRefresh("valid-refresh", newTokens) }
    }

    // endregion

    // region password change racing a refresh

    @Test
    fun `a revoked refresh does not sign out when a new pair is already stored`() = runTest {
        // The request failed with the old access; while it renewed, a password change stored its own pair.
        every { tokenStorage.peekOrNull() } returns storedTokens
        coEvery { authApi.refresh(RefreshRequest(refresh = "valid-refresh")) } returns
                buildRetrofitResponse(code = 401)
        coEvery { tokenStorage.swapIfRefresh("valid-refresh", null) } returns passwordChangeTokens

        val request = buildRequest(authHeader = "Bearer old-access")
        val result = authenticator.authenticate(null, buildUnauthorizedResponse(request))

        assertNotNull(result)
        assertEquals("Bearer pw-access", result!!.header("Authorization"))
        coVerify(exactly = 0) { tokenStorage.clear() }
        coVerify(exactly = 0) { tokenStorage.save(any()) }
    }

    @Test
    fun `a renewed pair arriving after the password change pair does not overwrite it`() = runTest {
        // The renewal answered 200 just before the server revoked it; the password change pair was saved first.
        every { tokenStorage.peekOrNull() } returns storedTokens
        coEvery { authApi.refresh(RefreshRequest(refresh = "valid-refresh")) } returns
                buildRetrofitResponse(code = 200, body = newTokens)
        coEvery { tokenStorage.swapIfRefresh("valid-refresh", newTokens) } returns passwordChangeTokens

        val request = buildRequest(authHeader = "Bearer old-access")
        val result = authenticator.authenticate(null, buildUnauthorizedResponse(request))

        assertNotNull(result)
        assertEquals("Bearer pw-access", result!!.header("Authorization"))
        coVerify(exactly = 0) { tokenStorage.save(any()) }
    }

    @Test
    fun `a renewal finishing after a logout does not bring the session back`() = runTest {
        every { tokenStorage.peekOrNull() } returns storedTokens
        coEvery { authApi.refresh(RefreshRequest(refresh = "valid-refresh")) } returns
                buildRetrofitResponse(code = 200, body = newTokens)
        coEvery { tokenStorage.swapIfRefresh("valid-refresh", newTokens) } returns null

        val request = buildRequest(authHeader = "Bearer old-access")
        val result = authenticator.authenticate(null, buildUnauthorizedResponse(request))

        assertNull(result)
    }

    // endregion

    // region helpers

    private fun buildRequest(authHeader: String? = null): Request =
        Request.Builder()
            .url("https://api.example.com/protected")
            .apply { if (authHeader != null) header("Authorization", authHeader) }
            .build()

    private fun buildUnauthorizedResponse(request: Request, prior: Response? = null): Response =
        Response.Builder()
            .request(request)
            .protocol(Protocol.HTTP_1_1)
            .code(401)
            .message("Unauthorized")
            .body("".toResponseBody())
            .apply { if (prior != null) priorResponse(prior) }
            .build()

    private fun buildRetrofitResponse(code: Int, body: AuthTokens? = null): RetrofitResponse<AuthTokens> =
        when {
            code in 200..299 -> RetrofitResponse.success(body)
            else -> RetrofitResponse.error(code, "".toResponseBody())
        }

    // endregion
}
