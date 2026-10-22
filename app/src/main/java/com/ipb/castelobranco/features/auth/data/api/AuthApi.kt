package com.ipb.castelobranco.features.auth.data.api

import com.ipb.castelobranco.features.auth.data.dto.LoginRequest
import com.ipb.castelobranco.features.auth.data.dto.RefreshRequest
import com.ipb.castelobranco.features.auth.data.dto.RegisterRequest
import com.ipb.castelobranco.features.auth.domain.model.AuthTokens
import kotlinx.serialization.Serializable
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.POST
@Serializable
data class GoogleLoginRequest(val id_token: String)
interface AuthApi {

    @POST(AuthEndpoints.AUTH_LOGIN_PATH)
    suspend fun login(
        @Body request: LoginRequest
    ): Response<AuthTokens>

    @POST(AuthEndpoints.AUTH_REGISTER_PATH)
    suspend fun register(
        @Body request: RegisterRequest
    ): Response<AuthTokens>

    @POST(AuthEndpoints.AUTH_REFRESH_PATH)
    suspend fun refresh(
        @Body request: RefreshRequest
    ): Response<AuthTokens>

    @POST(AuthEndpoints.GOOGLE_AUTH_LOGIN)
    suspend fun loginWithGoogle(@Body body: GoogleLoginRequest): Response<AuthTokens>
}