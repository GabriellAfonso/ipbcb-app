package com.ipb.castelobranco.features.profile.data.api

import com.ipb.castelobranco.features.profile.data.dto.ChangePasswordRequest
import com.ipb.castelobranco.features.profile.data.dto.CreateAccessRequestBody
import com.ipb.castelobranco.features.profile.data.dto.DeleteAccountWithGoogleRequest
import com.ipb.castelobranco.features.profile.data.dto.DeleteAccountWithPasswordRequest
import com.ipb.castelobranco.features.profile.data.dto.MeProfileDto
import com.ipb.castelobranco.features.profile.data.dto.OwnMemberDto
import com.ipb.castelobranco.features.profile.data.dto.ProfilePhotoResponseDto
import com.ipb.castelobranco.features.profile.data.dto.SessionTokensDto
import com.ipb.castelobranco.features.profile.data.dto.UpdateProfileRequest
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
 * Scriptable [ProfileApi] for the profile photo download and the account endpoints. An endpoint
 * without a script fails loudly so a test cannot reach it by accident.
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

    /** Account endpoints: each call is recorded, then answered by its script. */
    var updateProfileScript: (() -> Response<MeProfileDto>)? = null
    var changePasswordScript: (() -> Response<SessionTokensDto>)? = null
    var deleteAccountScript: (() -> Response<Unit>)? = null
    var accessRequestScript: (() -> Response<Unit>)? = null
    var ownMemberScript: (() -> Response<OwnMemberDto>)? = null

    val updateProfileBodies = mutableListOf<UpdateProfileRequest>()
    val changePasswordBodies = mutableListOf<ChangePasswordRequest>()
    val deleteWithPasswordBodies = mutableListOf<DeleteAccountWithPasswordRequest>()
    val deleteWithGoogleBodies = mutableListOf<DeleteAccountWithGoogleRequest>()
    val accessRequestBodies = mutableListOf<CreateAccessRequestBody>()

    override suspend fun updateMeProfile(body: UpdateProfileRequest): Response<MeProfileDto> {
        updateProfileBodies += body
        return scripted(updateProfileScript, "updateMeProfile")
    }

    override suspend fun changePassword(body: ChangePasswordRequest): Response<SessionTokensDto> {
        changePasswordBodies += body
        return scripted(changePasswordScript, "changePassword")
    }

    override suspend fun deleteAccountWithPassword(body: DeleteAccountWithPasswordRequest): Response<Unit> {
        deleteWithPasswordBodies += body
        return scripted(deleteAccountScript, "deleteAccountWithPassword")
    }

    override suspend fun deleteAccountWithGoogle(body: DeleteAccountWithGoogleRequest): Response<Unit> {
        deleteWithGoogleBodies += body
        return scripted(deleteAccountScript, "deleteAccountWithGoogle")
    }

    override suspend fun createAccessRequest(body: CreateAccessRequestBody): Response<Unit> {
        accessRequestBodies += body
        return scripted(accessRequestScript, "createAccessRequest")
    }

    override suspend fun getOwnMember(): Response<OwnMemberDto> = scripted(ownMemberScript, "getOwnMember")

    private fun <T> scripted(script: (() -> Response<T>)?, name: String): Response<T> =
        script?.invoke() ?: throw UnsupportedOperationException("$name is not scripted in FakeProfileApi")

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
