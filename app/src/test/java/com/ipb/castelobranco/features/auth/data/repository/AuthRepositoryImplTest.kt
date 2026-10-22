package com.ipb.castelobranco.features.auth.data.repository

import com.ipb.castelobranco.core.domain.error.AppError
import com.ipb.castelobranco.features.auth.data.api.AuthApi
import com.ipb.castelobranco.features.auth.data.local.TokenStorage
import com.ipb.castelobranco.features.auth.domain.model.AuthTokens
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import okhttp3.ResponseBody.Companion.toResponseBody
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import retrofit2.Response

class AuthRepositoryImplTest {

    private lateinit var api: AuthApi
    private lateinit var tokenStorage: TokenStorage
    private lateinit var repository: AuthRepositoryImpl

    private val fakeTokens = AuthTokens(access = "access_token", refresh = "refresh_token")

    @Before
    fun setup() {
        api = mockk()
        tokenStorage = mockk(relaxed = true)
        repository = AuthRepositoryImpl(api, tokenStorage)
    }

    // region signIn

    @Test
    fun `signIn success returns tokens`() = runTest {
        coEvery { api.login(any()) } returns Response.success(fakeTokens)

        val result = repository.signIn("user", "pass")

        assertTrue(result.isSuccess)
        assertEquals(fakeTokens, result.getOrNull())
    }

    @Test
    fun `signIn success saves tokens to storage`() = runTest {
        coEvery { api.login(any()) } returns Response.success(fakeTokens)

        repository.signIn("user", "pass")

        coVerify(exactly = 1) { tokenStorage.save(fakeTokens) }
    }

    @Test
    fun `signIn 401 returns AppError Auth`() = runTest {
        coEvery { api.login(any()) } returns Response.error(401, "".toResponseBody())

        val result = repository.signIn("user", "pass")

        assertTrue(result.isFailure)
        assertTrue(result.exceptionOrNull() is AppError.Auth)
    }

    @Test
    fun `signIn 403 returns AppError Auth`() = runTest {
        coEvery { api.login(any()) } returns Response.error(403, "".toResponseBody())

        val result = repository.signIn("user", "pass")

        assertTrue(result.isFailure)
        assertTrue(result.exceptionOrNull() is AppError.Auth)
    }

    @Test
    fun `signIn 500 returns AppError Server`() = runTest {
        coEvery { api.login(any()) } returns Response.error(500, "".toResponseBody())

        val result = repository.signIn("user", "pass")

        assertTrue(result.isFailure)
        assertTrue(result.exceptionOrNull() is AppError.Server)
    }

    @Test
    fun `signIn null body returns failure`() = runTest {
        coEvery { api.login(any()) } returns Response.success(null)

        val result = repository.signIn("user", "pass")

        assertTrue(result.isFailure)
    }

    @Test
    fun `signIn blank access token returns failure`() = runTest {
        coEvery { api.login(any()) } returns Response.success(fakeTokens.copy(access = "  "))

        val result = repository.signIn("user", "pass")

        assertTrue(result.isFailure)
    }

    @Test
    fun `signIn blank refresh token returns failure`() = runTest {
        coEvery { api.login(any()) } returns Response.success(fakeTokens.copy(refresh = ""))

        val result = repository.signIn("user", "pass")

        assertTrue(result.isFailure)
    }

    // endregion

    // region signInWithGoogle

    @Test
    fun `signInWithGoogle success returns tokens`() = runTest {
        coEvery { api.loginWithGoogle(any()) } returns Response.success(fakeTokens)

        val result = repository.signInWithGoogle("id_token")

        assertTrue(result.isSuccess)
        assertEquals(fakeTokens, result.getOrNull())
    }

    @Test
    fun `signInWithGoogle success saves tokens to storage`() = runTest {
        coEvery { api.loginWithGoogle(any()) } returns Response.success(fakeTokens)

        repository.signInWithGoogle("id_token")

        coVerify(exactly = 1) { tokenStorage.save(fakeTokens) }
    }

    @Test
    fun `signInWithGoogle 401 returns AppError Auth`() = runTest {
        coEvery { api.loginWithGoogle(any()) } returns Response.error(401, "".toResponseBody())

        val result = repository.signInWithGoogle("id_token")

        assertTrue(result.isFailure)
        assertTrue(result.exceptionOrNull() is AppError.Auth)
    }

    @Test
    fun `signInWithGoogle 500 returns AppError Server`() = runTest {
        coEvery { api.loginWithGoogle(any()) } returns Response.error(500, "".toResponseBody())

        val result = repository.signInWithGoogle("id_token")

        assertTrue(result.isFailure)
        assertTrue(result.exceptionOrNull() is AppError.Server)
    }

    // endregion

    // region signUp

    @Test
    fun `signUp success returns tokens`() = runTest {
        coEvery { api.register(any()) } returns Response.success(fakeTokens)

        val result = repository.signUp("user", "First", "Last", "pass", "pass")

        assertTrue(result.isSuccess)
        assertEquals(fakeTokens, result.getOrNull())
    }

    @Test
    fun `signUp success saves tokens to storage`() = runTest {
        coEvery { api.register(any()) } returns Response.success(fakeTokens)

        repository.signUp("user", "First", "Last", "pass", "pass")

        coVerify(exactly = 1) { tokenStorage.save(fakeTokens) }
    }

    @Test
    fun `signUp 400 returns AppError Server`() = runTest {
        coEvery { api.register(any()) } returns Response.error(400, "".toResponseBody())

        val result = repository.signUp("user", "First", "Last", "pass", "pass")

        assertTrue(result.isFailure)
        assertTrue(result.exceptionOrNull() is AppError.Server)
    }

    @Test
    fun `signUp null body returns failure`() = runTest {
        coEvery { api.register(any()) } returns Response.success(null)

        val result = repository.signUp("user", "First", "Last", "pass", "pass")

        assertTrue(result.isFailure)
    }

    // endregion

    // region getAuthToken

    @Test
    fun `getAuthToken returns access token when storage has tokens`() = runTest {
        coEvery { tokenStorage.loadOrNull() } returns fakeTokens

        val token = repository.getAuthToken()

        assertEquals("access_token", token)
    }

    @Test
    fun `getAuthToken returns null when storage is empty`() = runTest {
        coEvery { tokenStorage.loadOrNull() } returns null

        val token = repository.getAuthToken()

        assertNull(token)
    }

    // endregion

    // region signOut

    @Test
    fun `signOut clears token storage`() = runTest {
        repository.signOut()

        coVerify(exactly = 1) { tokenStorage.clear() }
    }

    // endregion
}
