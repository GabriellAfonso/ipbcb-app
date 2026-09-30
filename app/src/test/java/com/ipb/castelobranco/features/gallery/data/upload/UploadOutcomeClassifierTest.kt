package com.ipb.castelobranco.features.gallery.data.upload

import com.ipb.castelobranco.features.gallery.data.apiError
import com.ipb.castelobranco.features.gallery.data.dto.PhotoUploadResultDto
import com.ipb.castelobranco.features.gallery.data.dto.RejectedFileDto
import com.ipb.castelobranco.features.gallery.data.photoDto
import com.ipb.castelobranco.features.gallery.domain.upload.UploadOutcome
import kotlinx.serialization.SerializationException
import kotlinx.serialization.json.Json
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.ResponseBody.Companion.toResponseBody
import org.junit.Assert.assertEquals
import org.junit.Test
import retrofit2.Response
import java.io.IOException

class UploadOutcomeClassifierTest {

    private val classifier = UploadOutcomeClassifier(Json { ignoreUnknownKeys = true })

    private fun error(code: Int, body: String = ""): Response<PhotoUploadResultDto> =
        Response.error(code, body.toResponseBody("application/json".toMediaType()))

    @Test
    fun `201 with the new photo is sent`() {
        val outcome = classifier.classify(Response.success(PhotoUploadResultDto(accepted = listOf(photoDto(41, 7)))))

        assertEquals(41L, (outcome as UploadOutcome.Sent).photo.id)
    }

    @Test
    fun `201 repeating a live photo in another album is sent with that album`() {
        val outcome = classifier.classify(Response.success(PhotoUploadResultDto(accepted = listOf(photoDto(41, 9)))))

        assertEquals(9L, (outcome as UploadOutcome.Sent).photo.albumId)
    }

    @Test
    fun `207 with this file rejected fails with the server reason`() {
        val body = PhotoUploadResultDto(rejected = listOf(RejectedFileDto("a.jpg", "Formato não suportado.")))

        assertEquals(UploadOutcome.Failed("Formato não suportado."), classifier.classify(Response.success(207, body)))
    }

    @Test
    fun `400 with rejected fails with the first reason`() {
        val body = apiError(
            "VALIDATION_ERROR",
            "Nenhuma imagem foi aceita.",
            "rejected" to """[{"filename":"a.jpg","reason":"A imagem excede 10 MB."}]""",
        )

        assertEquals(UploadOutcome.Failed("A imagem excede 10 MB."), classifier.classify(error(400, body)))
    }

    @Test
    fun `400 without rejected fails with the generic text`() {
        val body = apiError("VALIDATION_ERROR", "Field 'client_upload_id' must be 1-64 characters")

        assertEquals(
            UploadOutcome.Failed(UploadOutcomeClassifier.GENERIC_FAILURE_MESSAGE),
            classifier.classify(error(400, body)),
        )
    }

    @Test
    fun `409 fails with the trashed-original detail`() {
        val detail = "Esta foto já foi enviada e depois apagada; ela está na lixeira."
        val body = apiError("CONFLICT", detail, "client_upload_id" to "\"u1\"")

        assertEquals(UploadOutcome.Failed(detail), classifier.classify(error(409, body)))
    }

    @Test
    fun `404 is album gone, 403 access lost, 401 stop`() {
        assertEquals(UploadOutcome.AlbumGone, classifier.classify(error(404, apiError("NOT_FOUND", "x"))))
        assertEquals(
            UploadOutcome.AccessLost("Sem permissão."),
            classifier.classify(error(403, apiError("PERMISSION_DENIED", "Sem permissão."))),
        )
        assertEquals(UploadOutcome.Stop, classifier.classify(error(401)))
    }

    @Test
    fun `server errors, rate limit and network failures are retried`() {
        assertEquals(UploadOutcome.Retry, classifier.classify(error(500)))
        assertEquals(UploadOutcome.Retry, classifier.classify(error(503)))
        assertEquals(UploadOutcome.Retry, classifier.classify(error(429)))
        assertEquals(UploadOutcome.Retry, classifier.classify(IOException("offline")))
    }

    @Test
    fun `an unexpected exception fails without retry`() {
        assertEquals(
            UploadOutcome.Failed(UploadOutcomeClassifier.GENERIC_FAILURE_MESSAGE),
            classifier.classify(SerializationException("bad body")),
        )
    }
}
