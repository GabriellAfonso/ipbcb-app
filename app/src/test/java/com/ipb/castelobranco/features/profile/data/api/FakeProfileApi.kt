package com.ipb.castelobranco.features.profile.data.api

import com.ipb.castelobranco.features.profile.data.dto.MeProfileDto
import com.ipb.castelobranco.features.profile.data.dto.ProfilePhotoResponseDto
import okhttp3.Headers.Companion.headersOf
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.MultipartBody
import okhttp3.Protocol
import okhttp3.Request
import okhttp3.ResponseBody
import okhttp3.ResponseBody.Companion.toResponseBody
import retrofit2.Response
import java.io.IOException

/**
 * Scriptable [ProfileApi] for the profile photo download. Only [downloadFile] is supported; the
 * other endpoints fail loudly so a test cannot reach them by accident.
 */
class FakeProfileApi : ProfileApi {

    private var script: () -> Response<ResponseBody> = {
        Response.success(DEFAULT_BYTES.toResponseBody(IMAGE_JPEG))
    }

    var lastIfNoneMatch: String? = null
        private set
    var downloadCalls: Int = 0
        private set

    fun respondBytes(bytes: ByteArray = DEFAULT_BYTES, etag: String? = null) {
        script = {
            val body = bytes.toResponseBody(IMAGE_JPEG)
            if (etag == null) Response.success(body) else Response.success(body, headersOf(ETAG_HEADER, etag))
        }
    }

    fun respondNotModified() {
        script = { Response.error("".toResponseBody(null), rawResponse(HTTP_NOT_MODIFIED)) }
    }

    fun respondError(code: Int, body: String = """{"detail":"HTTP $code"}""") {
        script = { Response.error(code, body.toResponseBody(APPLICATION_JSON)) }
    }

    fun respondIOException() {
        script = { throw IOException("Simulated network failure") }
    }

    override suspend fun downloadFile(absoluteUrl: String, ifNoneMatch: String?): Response<ResponseBody> {
        downloadCalls++
        lastIfNoneMatch = ifNoneMatch
        return script()
    }

    override suspend fun uploadProfilePhoto(photo: MultipartBody.Part): Response<ProfilePhotoResponseDto> =
        throw UnsupportedOperationException("uploadProfilePhoto is not scripted in FakeProfileApi")

    override suspend fun deleteProfilePhoto(): Response<Unit> =
        throw UnsupportedOperationException("deleteProfilePhoto is not scripted in FakeProfileApi")

    override suspend fun getMeProfile(ifNoneMatch: String?): Response<MeProfileDto> =
        throw UnsupportedOperationException("getMeProfile is not scripted in FakeProfileApi")

    private fun rawResponse(code: Int): okhttp3.Response =
        okhttp3.Response.Builder()
            .code(code)
            .message("HTTP $code")
            .protocol(Protocol.HTTP_1_1)
            .request(Request.Builder().url("https://example.com/media/profiles/me.jpg").build())
            .build()

    companion object {
        val DEFAULT_BYTES: ByteArray = "fake-profile-photo".toByteArray()
        private const val ETAG_HEADER = "ETag"
        private const val HTTP_NOT_MODIFIED = 304
        private val IMAGE_JPEG = "image/jpeg".toMediaType()
        private val APPLICATION_JSON = "application/json".toMediaType()
    }
}
