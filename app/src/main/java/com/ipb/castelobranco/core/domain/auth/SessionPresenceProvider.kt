package com.ipb.castelobranco.core.domain.auth

/**
 * Whether a session exists (tokens stored), exposed from `core/` so features can stop background
 * work after sign-out without importing `features/auth`. Unlike [AuthStatusProvider], an expired
 * access token still counts: the authenticated client renews it on the next request.
 */
fun interface SessionPresenceProvider {
    suspend fun isLoggedIn(): Boolean
}
