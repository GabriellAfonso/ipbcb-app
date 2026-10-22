package com.ipb.castelobranco.features.auth.data.local

import app.cash.turbine.test
import com.ipb.castelobranco.features.auth.domain.model.AuthTokens
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class AuthSessionTest {

    private lateinit var tokenStorage: TokenStorage
    private lateinit var authSession: AuthSession

    private val fakeTokens = AuthTokens(access = "access_token", refresh = "refresh_token")

    @Before
    fun setup() {
        tokenStorage = mockk(relaxed = true)
        every { tokenStorage.tokensFlow } returns flowOf(null)
        authSession = AuthSession(tokenStorage)
    }

    // region hasValidAccessToken

    @Test
    fun `hasValidAccessToken returns false when storage is empty`() = runTest {
        coEvery { tokenStorage.loadOrNull() } returns null

        assertFalse(authSession.hasValidAccessToken())
    }

    @Test
    fun `hasValidAccessToken returns false when access token is blank`() = runTest {
        coEvery { tokenStorage.loadOrNull() } returns fakeTokens.copy(access = "  ")

        assertFalse(authSession.hasValidAccessToken())
    }

    @Test
    fun `hasValidAccessToken returns true for non-blank token when JWT cannot be parsed on JVM`() = runTest {
        // android.util.Base64 is unavailable in JVM unit tests — jwtExpSecondsOrNull returns null
        // via runCatching, so the token is assumed locally valid (no expiry can be checked).
        coEvery { tokenStorage.loadOrNull() } returns fakeTokens

        assertTrue(authSession.hasValidAccessToken())
    }

    // endregion

    // region isLoggedIn

    @Test
    fun `isLoggedIn returns false when storage is empty`() = runTest {
        coEvery { tokenStorage.loadOrNull() } returns null

        assertFalse(authSession.isLoggedIn())
    }

    @Test
    fun `isLoggedIn returns false when access token is blank`() = runTest {
        coEvery { tokenStorage.loadOrNull() } returns fakeTokens.copy(access = "")

        assertFalse(authSession.isLoggedIn())
    }

    @Test
    fun `isLoggedIn returns false when refresh token is blank`() = runTest {
        coEvery { tokenStorage.loadOrNull() } returns fakeTokens.copy(refresh = "")

        assertFalse(authSession.isLoggedIn())
    }

    @Test
    fun `isLoggedIn returns true when both tokens are present`() = runTest {
        coEvery { tokenStorage.loadOrNull() } returns fakeTokens

        assertTrue(authSession.isLoggedIn())
    }

    // endregion

    // region isLoggedInFlow

    @Test
    fun `isLoggedInFlow emits false when flow emits null`() = runTest {
        every { tokenStorage.tokensFlow } returns flowOf(null)
        val session = AuthSession(tokenStorage)

        session.isLoggedInFlow.test {
            assertFalse(awaitItem())
            awaitComplete()
        }
    }

    @Test
    fun `isLoggedInFlow emits true when both tokens are present`() = runTest {
        every { tokenStorage.tokensFlow } returns flowOf(fakeTokens)
        val session = AuthSession(tokenStorage)

        session.isLoggedInFlow.test {
            assertTrue(awaitItem())
            awaitComplete()
        }
    }

    @Test
    fun `isLoggedInFlow emits false when access token is blank`() = runTest {
        every { tokenStorage.tokensFlow } returns flowOf(fakeTokens.copy(access = ""))
        val session = AuthSession(tokenStorage)

        session.isLoggedInFlow.test {
            assertFalse(awaitItem())
            awaitComplete()
        }
    }

    @Test
    fun `isLoggedInFlow emits false when refresh token is blank`() = runTest {
        every { tokenStorage.tokensFlow } returns flowOf(fakeTokens.copy(refresh = ""))
        val session = AuthSession(tokenStorage)

        session.isLoggedInFlow.test {
            assertFalse(awaitItem())
            awaitComplete()
        }
    }

    // endregion

    // region logout

    @Test
    fun `logout delegates to tokenStorage clear`() = runTest {
        authSession.logout()

        coVerify(exactly = 1) { tokenStorage.clear() }
    }

    // endregion
}
