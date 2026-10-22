package com.ipb.castelobranco.core.network

import com.ipb.castelobranco.features.auth.data.local.TokenStorage
import okhttp3.Interceptor
import okhttp3.Response
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class AuthInterceptor @Inject constructor(
    private val tokenStorage: TokenStorage
) : Interceptor {

    override fun intercept(chain: Interceptor.Chain): Response {
        val original = chain.request()

        // Se alguém já colocou Authorization manualmente, não sobrescreve
        if (original.header("Authorization") != null) {
            return chain.proceed(original)
        }

        val access = tokenStorage.peekOrNull()?.access

        val authed = if (!access.isNullOrBlank()) {
            original.newBuilder()
                .header("Authorization", "Bearer $access")
                .build()
        } else {
            original
        }

        return chain.proceed(authed)
    }
}