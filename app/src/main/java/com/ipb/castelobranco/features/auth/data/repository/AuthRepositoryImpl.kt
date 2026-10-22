package com.ipb.castelobranco.features.auth.data.repository

import timber.log.Timber
import com.ipb.castelobranco.BuildConfig
import com.ipb.castelobranco.features.auth.data.api.AuthApi
import com.ipb.castelobranco.features.auth.data.api.GoogleLoginRequest
import com.ipb.castelobranco.features.auth.domain.model.AuthTokens
import com.ipb.castelobranco.features.auth.data.dto.LoginRequest
import com.ipb.castelobranco.features.auth.data.dto.RegisterRequest
import com.ipb.castelobranco.features.auth.data.local.TokenStorage
import com.ipb.castelobranco.features.auth.domain.repository.AuthRepository
import com.ipb.castelobranco.core.domain.error.AppError
import com.ipb.castelobranco.core.domain.error.mapError
import com.ipb.castelobranco.core.network.error.toAppError
import retrofit2.Response
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class AuthRepositoryImpl @Inject constructor(
    private val api: AuthApi,
    private val tokenStorage: TokenStorage
) : AuthRepository {

    override suspend fun signIn(
        username: String,
        password: String
    ): Result<AuthTokens> =
        authenticate {
            api.login(LoginRequest(username, password))
        }

    override suspend fun signInWithGoogle(idToken: String): Result<AuthTokens> {
        if (BuildConfig.DEBUG) Timber.d("Sending idToken to backend: %s...", idToken.take(20))
        return authenticate {
            api.loginWithGoogle(GoogleLoginRequest(id_token = idToken))
        }
    }

    override suspend fun signUp(
        username: String,
        firstName: String,
        lastName: String,
        password: String,
        passwordConfirm: String
    ): Result<AuthTokens> =
        authenticate {
            api.register(
                RegisterRequest(
                    username = username,
                    firstName = firstName,
                    lastName = lastName,
                    password = password,
                    passwordConfirm = passwordConfirm
                )
            )
        }

    override suspend fun getAuthToken(): String? =
        tokenStorage.loadOrNull()?.access

    override suspend fun signOut() {
        tokenStorage.clear()
    }

    private suspend fun authenticate(
        call: suspend () -> Response<AuthTokens>
    ): Result<AuthTokens> =
        runCatching {
            val response = call()
            if (BuildConfig.DEBUG) Timber.d("Response code: %d", response.code())

            if (!response.isSuccessful) {
                if (BuildConfig.DEBUG) Timber.e("Server error: %d", response.code())
                throw response.toAppError()
            }

            val tokens = response.body()
                ?: throw AppError.Server(code = response.code(), message = "Resposta vazia")

            require(tokens.access.isNotBlank()) { "Access token ausente" }
            require(tokens.refresh.isNotBlank()) { "Refresh token ausente" }

            tokenStorage.save(tokens)
            tokens
        }.mapError()
}