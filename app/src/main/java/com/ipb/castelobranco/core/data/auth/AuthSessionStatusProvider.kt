package com.ipb.castelobranco.core.data.auth

import com.ipb.castelobranco.core.domain.auth.AuthStatusProvider
import com.ipb.castelobranco.core.domain.auth.SessionPresenceProvider
import com.ipb.castelobranco.features.auth.data.local.AuthSession
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class AuthSessionStatusProvider @Inject constructor(
    private val authSession: AuthSession,
) : AuthStatusProvider, SessionPresenceProvider {

    override suspend fun hasValidAccessToken(): Boolean = authSession.hasValidAccessToken()

    override suspend fun isLoggedIn(): Boolean = authSession.isLoggedIn()
}
