package com.ipb.castelobranco.core.network.error

import com.ipb.castelobranco.core.domain.error.AppError
import org.json.JSONObject
import retrofit2.Response

/**
 * Single place where an HTTP error response is turned into an [AppError]. No other layer parses
 * error bodies.
 *
 * The technical [AppError.message] prefers the `detail` field — from the structured error body when
 * one is present, otherwise from a plain JSON body — and falls back to the raw body or `HTTP <code>`.
 * [AppError.userMessage] is only filled from a structured error body (one carrying `error_code`),
 * where `detail` is copy the API owns; a raw body never reaches the screen.
 *
 * `field_errors`, when the structured body carries them, ride on [AppError.Server.fieldErrors] so
 * a form can show each message on its own field instead of in a generic alert.
 */
fun Response<*>.toAppError(): AppError {
    val code = code()
    val raw = errorBody()?.string()
    val parsed = parseApiError(raw)
    val structuredDetail = parsed?.detail?.ifBlank { null }
    val message = structuredDetail
        ?: raw?.let { extractDetail(it) }
        ?: raw?.ifBlank { null }
        ?: "HTTP $code"

    return if (code == 401 || code == 403) {
        AppError.Auth(code = code, message = message, userMessage = structuredDetail)
    } else {
        AppError.Server(
            code = code,
            message = message,
            errorCode = parsed?.errorCode,
            fieldErrors = parsed?.fieldErrors,
            extras = parsed?.extras?.ifEmpty { null },
            userMessage = structuredDetail,
        )
    }
}

/** Reads `detail` out of a plain JSON body that carries no `error_code`. */
private fun extractDetail(body: String): String? = try {
    JSONObject(body).optString("detail", "").ifBlank { null }
} catch (_: Exception) {
    null
}
