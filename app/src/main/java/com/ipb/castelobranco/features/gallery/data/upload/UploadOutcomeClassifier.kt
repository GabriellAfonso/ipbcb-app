package com.ipb.castelobranco.features.gallery.data.upload

import com.ipb.castelobranco.core.domain.error.AppError
import com.ipb.castelobranco.core.network.error.toAppError
import com.ipb.castelobranco.features.gallery.data.dto.PhotoUploadResultDto
import com.ipb.castelobranco.features.gallery.data.dto.RejectedFileDto
import com.ipb.castelobranco.features.gallery.data.dto.toDomain
import com.ipb.castelobranco.features.gallery.domain.upload.UploadOutcome
import kotlinx.serialization.builtins.ListSerializer
import kotlinx.serialization.json.Json
import retrofit2.Response
import timber.log.Timber
import java.io.IOException
import javax.inject.Inject

/**
 * Reads the answer to one upload (one file, one `client_upload_id`). Only network failures, 429 and
 * 5xx are retried; everything else is final. Rejection reasons and the trashed-original message are
 * Portuguese copy from the server and are shown as sent.
 */
class UploadOutcomeClassifier @Inject constructor(
    private val json: Json,
) {

    fun classify(response: Response<PhotoUploadResultDto>): UploadOutcome {
        if (response.isSuccessful) return classifyBody(response.body())
        val error = response.toAppError()
        return when (response.code()) {
            HTTP_UNAUTHORIZED -> UploadOutcome.Stop
            HTTP_FORBIDDEN -> UploadOutcome.AccessLost(error.userMessage ?: ACCESS_LOST_MESSAGE)
            HTTP_NOT_FOUND -> UploadOutcome.AlbumGone
            HTTP_CONFLICT -> UploadOutcome.Failed(error.userMessage ?: GENERIC_FAILURE_MESSAGE)
            HTTP_TOO_MANY_REQUESTS -> UploadOutcome.Retry
            in HTTP_SERVER_ERRORS -> UploadOutcome.Retry
            else -> UploadOutcome.Failed(rejectedReason(error) ?: logged(response.code()))
        }
    }

    fun classify(error: Throwable): UploadOutcome = when (error) {
        is IOException -> UploadOutcome.Retry
        is AppError.Network -> UploadOutcome.Retry
        else -> {
            Timber.w(error, "Gallery upload failed unexpectedly")
            UploadOutcome.Failed(GENERIC_FAILURE_MESSAGE)
        }
    }

    /** 201 (new or a repeat of a live photo) or 207 with this file accepted or rejected. */
    private fun classifyBody(body: PhotoUploadResultDto?): UploadOutcome {
        body?.accepted?.firstOrNull()?.let { return UploadOutcome.Sent(it.toDomain()) }
        val reason = body?.rejected?.firstOrNull()?.reason?.ifBlank { null }
        return UploadOutcome.Failed(reason ?: GENERIC_FAILURE_MESSAGE)
    }

    private fun rejectedReason(error: AppError): String? {
        val raw = (error as? AppError.Server)?.extras?.get(EXTRA_REJECTED) ?: return null
        return runCatching { json.decodeFromString(ListSerializer(RejectedFileDto.serializer()), raw) }
            .getOrNull()
            ?.firstOrNull()
            ?.reason
            ?.ifBlank { null }
    }

    private fun logged(code: Int): String {
        Timber.w("Gallery upload refused without a reason: HTTP %d", code)
        return GENERIC_FAILURE_MESSAGE
    }

    companion object {
        const val GENERIC_FAILURE_MESSAGE = "Não foi possível enviar esta foto."
        const val ACCESS_LOST_MESSAGE = "Você não tem permissão para enviar fotos."
        private const val EXTRA_REJECTED = "rejected"
        private const val HTTP_UNAUTHORIZED = 401
        private const val HTTP_FORBIDDEN = 403
        private const val HTTP_NOT_FOUND = 404
        private const val HTTP_CONFLICT = 409
        private const val HTTP_TOO_MANY_REQUESTS = 429
        private val HTTP_SERVER_ERRORS = 500..599
    }
}
