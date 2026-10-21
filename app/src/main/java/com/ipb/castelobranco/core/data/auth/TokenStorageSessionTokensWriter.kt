package com.ipb.castelobranco.core.data.auth

import com.ipb.castelobranco.core.domain.auth.SessionTokensWriter
import com.ipb.castelobranco.features.auth.data.local.TokenStorage
import com.ipb.castelobranco.features.auth.domain.model.AuthTokens
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class TokenStorageSessionTokensWriter @Inject constructor(
    private val tokenStorage: TokenStorage,
) : SessionTokensWriter {

    override suspend fun replace(access: String, refresh: String) {
        tokenStorage.save(AuthTokens(access = access, refresh = refresh))
    }
}
