package com.ipb.castelobranco.features.auth.domain.repository

import com.ipb.castelobranco.features.auth.domain.model.AuthTokens

interface AuthRepository {
    suspend fun signIn(username: String, password: String): Result<AuthTokens>
    suspend fun signInWithGoogle(idToken: String): Result<AuthTokens>
    suspend fun signUp(
        username: String,
        firstName: String,
        lastName: String,
        password: String,
        passwordConfirm: String
    ): Result<AuthTokens>
    suspend fun getAuthToken(): String?
    suspend fun signOut()
}