package com.ipb.castelobranco.features.gallery.data.download

import com.ipb.castelobranco.core.domain.error.AppError

/** What the gallery worker returns to WorkManager. Kept free of WorkManager types so it is unit-testable. */
sealed interface WorkDecision {
    data object Success : WorkDecision
    data object Retry : WorkDecision
    data class Fail(val message: String, val code: Int) : WorkDecision
}

/**
 * - Completed: success, unless photos failed on the network and retries remain.
 * - Rate limited: always retry — every run saves the photos before the limit, so the sequence ends,
 *   and a rate limit must never surface as an error.
 * - Forbidden: fail with 403 — retrying cannot make anyone a member.
 * - Unauthenticated: fail with 401 — the session could not be renewed; the screen shows the login path.
 */
fun GalleryDownloadRun.toWorkDecision(runAttemptCount: Int, maxRetries: Int): WorkDecision = when (this) {
    is GalleryDownloadRun.Completed ->
        if (networkFailures > 0 && runAttemptCount < maxRetries) WorkDecision.Retry else WorkDecision.Success

    is GalleryDownloadRun.Stopped -> when (reason) {
        StopReason.RATE_LIMITED -> WorkDecision.Retry
        StopReason.FORBIDDEN -> WorkDecision.Fail(error.userMessage ?: NO_ACCESS_MESSAGE, HTTP_FORBIDDEN)
        StopReason.UNAUTHENTICATED ->
            WorkDecision.Fail(error.userMessage ?: SESSION_EXPIRED_MESSAGE, HTTP_UNAUTHORIZED)
    }
}

private const val HTTP_UNAUTHORIZED = 401
private const val HTTP_FORBIDDEN = 403
private const val SESSION_EXPIRED_MESSAGE = "Sua sessão expirou. Entre novamente para acessar a galeria."
private const val NO_ACCESS_MESSAGE = "Disponível apenas para membros."

/**
 * A failed read of the photo list (the sync that builds the index before a first download):
 * 401 and 403 fail at once with their code, since retrying cannot fix either; anything else retries up
 * to [maxRetries] and then fails.
 */
fun AppError.toListFailureDecision(runAttemptCount: Int, maxRetries: Int): WorkDecision {
    val code = when (this) {
        is AppError.Auth -> code
        is AppError.Server -> code
        else -> NO_HTTP_CODE
    }
    return when (code) {
        HTTP_UNAUTHORIZED -> WorkDecision.Fail(userMessage ?: SESSION_EXPIRED_MESSAGE, HTTP_UNAUTHORIZED)
        HTTP_FORBIDDEN -> WorkDecision.Fail(userMessage ?: NO_ACCESS_MESSAGE, HTTP_FORBIDDEN)
        else -> if (runAttemptCount < maxRetries) {
            WorkDecision.Retry
        } else {
            WorkDecision.Fail(userMessage ?: LIST_FAILED_MESSAGE, code)
        }
    }
}

private const val NO_HTTP_CODE = 0
private const val LIST_FAILED_MESSAGE = "Falha ao baixar galeria"
