package com.ipb.castelobranco.features.gallery.data.download

import com.ipb.castelobranco.core.domain.error.AppError
import org.junit.Assert.assertEquals
import org.junit.Test

class GalleryDownloadWorkDecisionTest {

    private fun completed(networkFailures: Int = 0, failed: Int = networkFailures) =
        GalleryDownloadRun.Completed(
            downloaded = 10 - failed,
            total = 10,
            failed = failed,
            networkFailures = networkFailures,
        )

    private fun stopped(reason: StopReason, error: AppError) =
        GalleryDownloadRun.Stopped(reason = reason, error = error, downloaded = 4, total = 10)

    // region US1

    @Test
    fun `clean completed run - success`() {
        assertEquals(WorkDecision.Success, completed().toWorkDecision(runAttemptCount = 0, maxRetries = MAX_RETRIES))
    }

    @Test
    fun `completed with network failures under the cap - retry`() {
        assertEquals(
            WorkDecision.Retry,
            completed(networkFailures = 2).toWorkDecision(runAttemptCount = MAX_RETRIES - 1, maxRetries = MAX_RETRIES),
        )
    }

    @Test
    fun `completed with network failures at the cap - success, left for the next trigger`() {
        assertEquals(
            WorkDecision.Success,
            completed(networkFailures = 2).toWorkDecision(runAttemptCount = MAX_RETRIES, maxRetries = MAX_RETRIES),
        )
    }

    @Test
    fun `completed with only non-network failures - success`() {
        assertEquals(
            WorkDecision.Success,
            completed(networkFailures = 0, failed = 3).toWorkDecision(runAttemptCount = 0, maxRetries = MAX_RETRIES),
        )
    }

    @Test
    fun `rate limited - retry at any attempt, never capped`() {
        val run = stopped(StopReason.RATE_LIMITED, AppError.Server(code = 429))

        listOf(0, MAX_RETRIES, 10).forEach { attempt ->
            assertEquals(WorkDecision.Retry, run.toWorkDecision(runAttemptCount = attempt, maxRetries = MAX_RETRIES))
        }
    }

    // endregion

    // region US2

    @Test
    fun `forbidden - fail with 403 and the server's user message, never retried`() {
        val run = stopped(StopReason.FORBIDDEN, AppError.Auth(code = 403, userMessage = "Somente membros."))

        listOf(0, MAX_RETRIES).forEach { attempt ->
            assertEquals(
                WorkDecision.Fail("Somente membros.", 403),
                run.toWorkDecision(runAttemptCount = attempt, maxRetries = MAX_RETRIES),
            )
        }
    }

    @Test
    fun `forbidden without a user message - fail with the app's members-only text`() {
        val run = stopped(StopReason.FORBIDDEN, AppError.Auth(code = 403, message = "{\"detail\":\"raw\"}"))

        assertEquals(
            WorkDecision.Fail("Disponível apenas para membros.", 403),
            run.toWorkDecision(runAttemptCount = 0, maxRetries = MAX_RETRIES),
        )
    }

    // endregion

    // region US4

    @Test
    fun `unauthenticated - fail with 401 at any attempt, never retried`() {
        val run = stopped(StopReason.UNAUTHENTICATED, AppError.Auth(code = 401, message = "token expired"))

        listOf(0, MAX_RETRIES).forEach { attempt ->
            val decision = run.toWorkDecision(runAttemptCount = attempt, maxRetries = MAX_RETRIES)
            assertEquals(401, (decision as WorkDecision.Fail).code)
        }
    }

    // endregion

    private companion object {
        const val MAX_RETRIES = 3
    }
}
