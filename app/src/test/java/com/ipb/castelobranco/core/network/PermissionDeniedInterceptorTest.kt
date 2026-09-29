package com.ipb.castelobranco.core.network

import com.ipb.castelobranco.core.domain.auth.AuthEventBus
import io.mockk.every
import io.mockk.mockk
import okhttp3.Interceptor
import okhttp3.Protocol
import okhttp3.Request
import okhttp3.Response
import okhttp3.ResponseBody.Companion.toResponseBody
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import org.junit.Assert.assertEquals
import org.junit.Test

class PermissionDeniedInterceptorTest {

    private class RecordingBus : AuthEventBus {
        val emitted = mutableListOf<AuthEventBus.Event>()
        override val events: SharedFlow<AuthEventBus.Event> = MutableSharedFlow()
        override fun emit(event: AuthEventBus.Event) {
            emitted += event
        }
    }

    private val bus = RecordingBus()
    private val interceptor = PermissionDeniedInterceptor(bus)

    @Test
    fun `a structured permission refusal is announced and the body stays readable`() {
        val body = """{"error_code":"PERMISSION_DENIED","detail":"Você não tem permissão para esta ação."}"""

        val response = interceptor.intercept(chainAnswering(403, body))

        assertEquals(listOf(AuthEventBus.Event.PermissionDenied), bus.emitted)
        assertEquals(body, response.body!!.string())
    }

    @Test
    fun `a 403 without body is not announced`() {
        interceptor.intercept(chainAnswering(403, ""))

        assertEquals(emptyList<AuthEventBus.Event>(), bus.emitted)
    }

    @Test
    fun `a 403 with another error code is not announced`() {
        interceptor.intercept(chainAnswering(403, """{"error_code":"NOT_A_MEMBER","detail":"x"}"""))

        assertEquals(emptyList<AuthEventBus.Event>(), bus.emitted)
    }

    @Test
    fun `401 and 200 are not announced`() {
        interceptor.intercept(chainAnswering(401, """{"error_code":"PERMISSION_DENIED","detail":"x"}"""))
        interceptor.intercept(chainAnswering(200, "{}"))

        assertEquals(emptyList<AuthEventBus.Event>(), bus.emitted)
    }

    private fun chainAnswering(code: Int, body: String): Interceptor.Chain {
        val request = Request.Builder().url("https://api.example.com/api/admin/members/").build()
        val response = Response.Builder()
            .request(request)
            .protocol(Protocol.HTTP_1_1)
            .code(code)
            .message("HTTP $code")
            .body(body.toResponseBody())
            .build()
        return mockk {
            every { request() } returns request
            every { proceed(any()) } returns response
        }
    }
}
