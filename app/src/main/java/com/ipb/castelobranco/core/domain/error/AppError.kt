package com.ipb.castelobranco.core.domain.error

import java.io.IOException

/**
 * Sealed hierarchy of domain-level errors. Repositories must map all raw [Throwable]s
 * to one of these subtypes so callers can react to specific failure categories without
 * depending on HTTP or I/O implementation details.
 */
sealed class AppError(
    message: String?,
    cause: Throwable?,
    /**
     * Text written by the app to be shown to the user. Null means "use the generic text for this
     * category". NEVER fill this with server-provided content — [message] exists for that, and it
     * is technical: it belongs in logs, not on screen. The only exception is the `detail` field of
     * a structured API error (one carrying `error_code`), which the API owns as user-facing copy.
     */
    val userMessage: String? = null,
) : Exception(message, cause) {

    /** Connectivity failure: no network, DNS error, socket timeout, etc. */
    class Network(
        message: String? = "Erro de rede",
        cause: Throwable? = null,
        userMessage: String? = null,
    ) : AppError(message, cause, userMessage)

    /** Authentication / authorisation failure (HTTP 401 or 403). */
    class Auth(
        val code: Int = 401,
        message: String? = "Falha de autenticação",
        cause: Throwable? = null,
        userMessage: String? = null,
    ) : AppError(message, cause, userMessage)

    /** The server responded with an error status code (4xx / 5xx, except 401/403). */
    class Server(
        val code: Int,
        message: String? = "Erro no servidor ($code)",
        cause: Throwable? = null,
        val errorCode: String? = null,
        /**
         * Per-field validation messages from a structured error body, keyed by the field name the
         * API uses. A form maps each key back to its own field; a screen with no form ignores it
         * and falls back to [userMessage] or the generic text. Filled only in
         * `core/network/error/ResponseExt.kt` — no other layer parses an error body.
         */
        val fieldErrors: Map<String, List<String>>? = null,
        userMessage: String? = null,
        /**
         * The other top-level fields of a structured error body (`rejected`, `missing`, `chain`…),
         * as JSON text. Filled only in `core/network/error/ResponseExt.kt`.
         */
        val extras: Map<String, String>? = null,
    ) : AppError(message, cause, userMessage)

    /** An unexpected error that does not fit the categories above. */
    class Unknown(
        message: String? = "Erro desconhecido",
        cause: Throwable? = null,
        userMessage: String? = null,
    ) : AppError(message, cause, userMessage)
}

/**
 * Maps any raw [Throwable] to an [AppError], preserving the original as [cause].
 * Already-mapped [AppError]s are returned unchanged.
 */
fun Throwable.toAppError(): AppError = when (this) {
    is AppError -> this
    is IOException -> AppError.Network(message = message, cause = this)
    else -> AppError.Unknown(message = message, cause = this)
}

/**
 * Maps the failure inside this [Result] to an [AppError] via [toAppError].
 * Successful results are returned unchanged.
 */
fun <T> Result<T>.mapError(): Result<T> =
    recoverCatching { throw it.toAppError() }
