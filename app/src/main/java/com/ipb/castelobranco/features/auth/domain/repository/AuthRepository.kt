package com.ipb.castelobranco.features.auth.domain.repository

import com.ipb.castelobranco.features.auth.domain.model.AuthTokens

interface AuthRepository {
    suspend fun signIn(username: String, password: String): Result<AuthTokens>
    suspend fun signInWithGoogle(idToken: String): Result<AuthTokens>
    suspend fun signUp(
        username: String,
        firstName: String,
        lastName: String,
        email: String,
        password: String,
        passwordConfirm: String
    ): Result<AuthTokens>

    /** Asks for recovery codes. Success says nothing about whether the address has an account. */
    suspend fun requestPasswordRecovery(email: String): Result<Unit>

    /** Sets a new password with a code. No session comes back: the person signs in afterwards. */
    suspend fun confirmPasswordRecovery(
        email: String,
        code: String,
        newPassword: String,
        newPasswordConfirm: String,
    ): Result<Unit>

    suspend fun getAuthToken(): String?
    suspend fun signOut()
}