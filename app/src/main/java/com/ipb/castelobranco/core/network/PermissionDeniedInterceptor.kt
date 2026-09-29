package com.ipb.castelobranco.core.network

import com.ipb.castelobranco.core.domain.auth.AuthEventBus
import com.ipb.castelobranco.core.network.error.parseApiError
import okhttp3.Interceptor
import okhttp3.Response
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Watches the authenticated client for scope refusals (403 `PERMISSION_DENIED`) and says so on the
 * [AuthEventBus], so the profile is read again and screens hide what the user lost. The response
 * passes through untouched: turning it into an error stays with `ResponseExt`.
 *
 * Covers every scoped call in one place, including the member photo loader, which never reaches a
 * ViewModel as a `Result`.
 */
@Singleton
class PermissionDeniedInterceptor @Inject constructor(
    private val authEventBus: AuthEventBus,
) : Interceptor {

    override fun intercept(chain: Interceptor.Chain): Response {
        val response = chain.proceed(chain.request())
        if (response.code == HTTP_FORBIDDEN) {
            val body = response.peekBody(MAX_PEEK_BYTES).string()
            if (parseApiError(body)?.errorCode == ERROR_CODE_PERMISSION_DENIED) {
                authEventBus.emit(AuthEventBus.Event.PermissionDenied)
            }
        }
        return response
    }

    private companion object {
        const val HTTP_FORBIDDEN = 403
        const val MAX_PEEK_BYTES = 4_096L
        const val ERROR_CODE_PERMISSION_DENIED = "PERMISSION_DENIED"
    }
}
