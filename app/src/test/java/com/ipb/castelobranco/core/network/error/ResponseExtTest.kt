package com.ipb.castelobranco.core.network.error

import com.ipb.castelobranco.core.domain.error.AppError
import okhttp3.Protocol
import okhttp3.Request
import okhttp3.ResponseBody.Companion.toResponseBody
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import retrofit2.Response

class ResponseExtTest {

    // region status mapping

    @Test
    fun `toAppError maps 401 to Auth with the response code`() {
        val error = errorResponse(code = 401, body = "").toAppError()

        assertTrue(error is AppError.Auth)
        assertEquals(401, (error as AppError.Auth).code)
    }

    @Test
    fun `toAppError maps 403 to Auth with the response code`() {
        val error = errorResponse(code = 403, body = "").toAppError()

        assertTrue(error is AppError.Auth)
        assertEquals(403, (error as AppError.Auth).code)
    }

    @Test
    fun `toAppError maps other statuses to Server with the response code`() {
        val error = errorResponse(code = 500, body = "").toAppError()

        assertTrue(error is AppError.Server)
        assertEquals(500, (error as AppError.Server).code)
        assertEquals("HTTP 500", error.message)
    }

    // endregion

    // region detail extraction

    @Test
    fun `toAppError uses detail as message when the body has no error_code`() {
        val error = errorResponse(code = 400, body = """{"detail":"Data inválida"}""").toAppError()

        assertEquals("Data inválida", error.message)
    }

    @Test
    fun `toAppError does not expose detail as userMessage when the body has no error_code`() {
        val error = errorResponse(code = 400, body = """{"detail":"Data inválida"}""").toAppError()

        assertNull(error.userMessage)
    }

    @Test
    fun `toAppError exposes detail as userMessage when the body is a structured API error`() {
        val body = """{"error_code":"chart_exists","detail":"Já existe cifra para esta música"}"""

        val error = errorResponse(code = 400, body = body).toAppError()

        assertTrue(error is AppError.Server)
        assertEquals("chart_exists", (error as AppError.Server).errorCode)
        assertEquals("Já existe cifra para esta música", error.userMessage)
        assertEquals("Já existe cifra para esta música", error.message)
    }

    @Test
    fun `toAppError falls back to the raw body as message when it is not JSON`() {
        val error = errorResponse(code = 502, body = "<html>Bad Gateway</html>").toAppError()

        assertEquals("<html>Bad Gateway</html>", error.message)
        assertNull(error.userMessage)
    }

    @Test
    fun `toAppError falls back to the raw body as message when JSON has no detail`() {
        val error = errorResponse(code = 400, body = """{"error":"invalid"}""").toAppError()

        assertEquals("""{"error":"invalid"}""", error.message)
        assertNull(error.userMessage)
    }

    // endregion

    // region field errors

    @Test
    fun `toAppError carries field_errors from a structured API error`() {
        val body = """
            {
              "error_code":"VALIDATION_ERROR",
              "detail":"Validation failed.",
              "field_errors":{
                "min_seconds_to_count":["Value 0 is out of range. Expected an integer between 1 and 3600."]
              }
            }
        """.trimIndent()

        val error = errorResponse(code = 400, body = body).toAppError()

        assertTrue(error is AppError.Server)
        val fieldErrors = (error as AppError.Server).fieldErrors
        assertEquals(1, fieldErrors?.size)
        assertEquals(
            listOf("Value 0 is out of range. Expected an integer between 1 and 3600."),
            fieldErrors?.get("min_seconds_to_count"),
        )
    }

    @Test
    fun `toAppError leaves field_errors null when the structured body has none`() {
        val body = """{"error_code":"VALIDATION_ERROR","detail":"Validation failed."}"""

        val error = errorResponse(code = 400, body = body).toAppError()

        assertNull((error as AppError.Server).fieldErrors)
    }

    @Test
    fun `toAppError leaves field_errors null when the body is not a structured API error`() {
        val error = errorResponse(code = 400, body = """{"detail":"Data inválida"}""").toAppError()

        assertNull((error as AppError.Server).fieldErrors)
    }

    // endregion

    // region helpers

    private fun errorResponse(code: Int, body: String): Response<String> {
        val raw = okhttp3.Response.Builder()
            .request(Request.Builder().url("https://api.example.com/").build())
            .protocol(Protocol.HTTP_1_1)
            .code(code)
            .message("")
            .body("".toResponseBody())
            .build()
        return Response.error(body.toResponseBody(), raw)
    }

    // endregion

    // region extras

    @Test
    fun `toAppError carries the structured extras of a 400`() {
        val body = """{"error_code":"VALIDATION_ERROR","detail":"Order must list every sibling exactly once.",""" +
            """"missing":[4],"unexpected":[],"repeated":[]}"""

        val error = errorResponse(code = 400, body = body).toAppError() as AppError.Server

        assertEquals("[4]", error.extras?.get("missing"))
        assertEquals(setOf("missing", "unexpected", "repeated"), error.extras?.keys)
    }

    @Test
    fun `toAppError carries the extras of a 409 and keeps its detail as userMessage`() {
        val body = """{"error_code":"CONFLICT",""" +
            """"detail":"Esta foto já foi enviada e depois apagada; ela está na lixeira.",""" +
            """"client_upload_id":"abc"}"""

        val error = errorResponse(code = 409, body = body).toAppError() as AppError.Server

        assertEquals("CONFLICT", error.errorCode)
        assertEquals("abc", error.extras?.get("client_upload_id"))
        assertEquals("Esta foto já foi enviada e depois apagada; ela está na lixeira.", error.userMessage)
    }

    @Test
    fun `toAppError leaves extras null when the body has none`() {
        val body = """{"error_code":"NOT_FOUND","detail":"Não encontrado"}"""

        val error = errorResponse(code = 404, body = body).toAppError() as AppError.Server

        assertNull(error.extras)
    }

    // endregion
}
