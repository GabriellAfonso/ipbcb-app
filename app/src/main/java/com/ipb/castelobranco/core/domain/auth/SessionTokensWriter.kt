package com.ipb.castelobranco.core.domain.auth

/**
 * Replaces the stored session pair, exposed from `core/` so a feature can install a pair the server handed it (a
 * password change) without importing `features/auth`.
 */
fun interface SessionTokensWriter {
    suspend fun replace(access: String, refresh: String)
}
