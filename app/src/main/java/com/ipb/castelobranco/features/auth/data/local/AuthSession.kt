package com.ipb.castelobranco.features.auth.data.local

import android.util.Base64
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import kotlinx.serialization.json.longOrNull
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class AuthSession @Inject constructor(
    private val tokenStorage: TokenStorage
) {

    suspend fun hasValidAccessToken(): Boolean {
        val tokens = tokenStorage.loadOrNull() ?: return false
        return isAccessTokenLocallyValid(tokens.access)
    }

    private fun isAccessTokenLocallyValid(accessToken: String): Boolean {
        if (accessToken.isBlank()) return false

        val expSeconds = jwtExpSecondsOrNull(accessToken)
            ?: return true

        val nowSeconds = System.currentTimeMillis() / 1000
        return expSeconds > nowSeconds
    }

    private fun jwtExpSecondsOrNull(token: String): Long? {
        return runCatching {
            val parts = token.split('.')
            if (parts.size < 2) return null

            val payloadBytes = Base64.decode(
                parts[1],
                Base64.URL_SAFE or Base64.NO_WRAP or Base64.NO_PADDING
            )

            val payload = payloadBytes.decodeToString()
            val element = Json.parseToJsonElement(payload).jsonObject
            element["exp"]?.jsonPrimitive?.longOrNull
        }.getOrNull()
    }

    val isLoggedInFlow: Flow<Boolean> =
        tokenStorage.tokensFlow.map { tokens ->
            val accessOk = !tokens?.access.isNullOrBlank()
            val refreshOk = !tokens?.refresh.isNullOrBlank()
            accessOk && refreshOk
        }

    suspend fun isLoggedIn(): Boolean {
        val tokens = tokenStorage.loadOrNull()
        return !tokens?.access.isNullOrBlank() && tokens.refresh.isNotBlank()
    }

    suspend fun logout() {
        tokenStorage.clear()
    }
}
