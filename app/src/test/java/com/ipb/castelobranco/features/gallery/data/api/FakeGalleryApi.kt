package com.ipb.castelobranco.features.gallery.data.api

import com.ipb.castelobranco.features.gallery.data.dto.GalleryAlbumDto
import com.ipb.castelobranco.features.gallery.data.dto.GalleryChangesDto
import com.ipb.castelobranco.features.gallery.data.dto.GalleryPhotoDto
import com.ipb.castelobranco.features.gallery.data.dto.GalleryPhotoMemberDto
import com.ipb.castelobranco.features.gallery.data.dto.GalleryTrashEntryDto
import com.ipb.castelobranco.features.gallery.data.dto.PhotoUploadResultDto
import kotlinx.serialization.json.JsonObject
import okhttp3.MultipartBody
import okhttp3.RequestBody
import okio.Buffer
import kotlinx.coroutines.CompletableDeferred
import okhttp3.MediaType
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.ResponseBody
import okhttp3.ResponseBody.Companion.toResponseBody
import okio.BufferedSource
import okio.buffer
import okio.source
import retrofit2.Response
import java.io.ByteArrayInputStream
import java.io.IOException
import java.io.InputStream

/**
 * Scriptable [GalleryApi] for tests. Each media URL answers what the test scripted for it; an
 * unscripted URL answers 200 with [DEFAULT_BYTES]. Every media request is recorded in [requestedUrls].
 */
class FakeGalleryApi : GalleryApi {

    private val changesQueue = ArrayDeque<suspend () -> Response<GalleryChangesDto>>()
    private val _sinceReceived = mutableListOf<String?>()

    /** The `since` of every feed request, in order (`null` = full read). */
    val sinceReceived: List<String?> get() = _sinceReceived

    fun enqueueChanges(body: GalleryChangesDto) {
        changesQueue += { Response.success(body) }
    }

    fun enqueueChangesError(code: Int, body: String = """{"error_code":"PERMISSION_DENIED","detail":"HTTP $code"}""") {
        changesQueue += { Response.error(code, body.toResponseBody(APPLICATION_JSON)) }
    }

    fun enqueueChangesIOException() {
        changesQueue += { throw IOException("Simulated network failure on the feed") }
    }

    /** The next feed request suspends until [gate] completes — to test a sync in flight. */
    fun enqueueChangesAfter(gate: CompletableDeferred<Unit>, body: GalleryChangesDto) {
        changesQueue += {
            gate.await()
            Response.success(body)
        }
    }

    private val scripts = mutableMapOf<String, () -> Response<ResponseBody>>()
    private val _requestedUrls = mutableListOf<String>()
    val requestedUrls: List<String> get() = _requestedUrls

    fun respondBytes(url: String, bytes: ByteArray = DEFAULT_BYTES) {
        scripts[url] = { Response.success(bytes.toResponseBody(IMAGE_JPEG)) }
    }

    fun respondError(url: String, code: Int, body: String = """{"detail":"HTTP $code"}""") {
        scripts[url] = { Response.error(code, body.toResponseBody(APPLICATION_JSON)) }
    }

    fun respondIOException(url: String) {
        scripts[url] = { throw IOException("Simulated network failure for $url") }
    }

    /** 200 whose body stream breaks after [bytes] were read — a connection cut mid-download. */
    fun respondTruncated(url: String, bytes: ByteArray = DEFAULT_BYTES) {
        scripts[url] = { Response.success(TruncatedBody(bytes)) }
    }

    override suspend fun getChanges(since: String?): Response<GalleryChangesDto> {
        _sinceReceived += since
        val next = changesQueue.removeFirstOrNull() ?: error("Unexpected feed request (since=$since)")
        return next()
    }

    override suspend fun downloadFile(absoluteUrl: String): Response<ResponseBody> {
        _requestedUrls += absoluteUrl
        val script = scripts[absoluteUrl] ?: return Response.success(DEFAULT_BYTES.toResponseBody(IMAGE_JPEG))
        return script()
    }

    // region writes

    /** One recorded write: which call, the path id (if any), and its JSON body or multipart fields. */
    data class WriteCall(
        val name: String,
        val id: Long? = null,
        val body: JsonObject? = null,
        val parts: Map<String, String> = emptyMap(),
        val fileName: String? = null,
    )

    private val writeScripts = mutableMapOf<String, ArrayDeque<suspend () -> Response<*>>>()
    private val _writes = mutableListOf<WriteCall>()
    val writes: List<WriteCall> get() = _writes

    /** Scripts the next answer of [call] (e.g. `"createAlbum"`); unscripted calls fail the test. */
    fun <T> respond(call: String, answer: suspend () -> Response<T>) {
        writeScripts.getOrPut(call) { ArrayDeque() } += answer
    }

    fun <T> respondSuccess(call: String, body: T) = respond(call) { Response.success(body) }

    /** Like Retrofit, a 204 comes with a null body, even for a `Response<Unit>`. */
    fun respondNoContent(call: String) = respond(call) { Response.success<Unit>(HTTP_NO_CONTENT, null) }

    fun respondWriteError(call: String, code: Int, body: String) =
        respond<Any>(call) { Response.error(code, body.toResponseBody(APPLICATION_JSON)) }

    fun respondWriteIOException(call: String) =
        respond<Any>(call) { throw IOException("Simulated network failure on $call") }

    @Suppress("UNCHECKED_CAST")
    private suspend fun <T> answer(call: WriteCall): Response<T> {
        _writes += call
        val next = writeScripts[call.name]?.removeFirstOrNull() ?: error("Unexpected write: $call")
        return next() as Response<T>
    }

    override suspend fun createAlbum(body: JsonObject): Response<GalleryAlbumDto> =
        answer(WriteCall("createAlbum", body = body))

    override suspend fun patchAlbum(albumId: Long, body: JsonObject): Response<GalleryAlbumDto> =
        answer(WriteCall("patchAlbum", albumId, body))

    override suspend fun orderAlbums(body: JsonObject): Response<Unit> = answer(WriteCall("orderAlbums", body = body))

    override suspend fun putCover(albumId: Long, image: MultipartBody.Part): Response<GalleryAlbumDto> =
        answer(WriteCall("putCover", albumId, fileName = image.fileName()))

    override suspend fun deleteCover(albumId: Long): Response<Unit> = answer(WriteCall("deleteCover", albumId))

    override suspend fun deleteAlbum(albumId: Long): Response<Unit> = answer(WriteCall("deleteAlbum", albumId))

    override suspend fun uploadPhoto(
        albumId: RequestBody,
        clientUploadId: RequestBody,
        image: MultipartBody.Part,
    ): Response<PhotoUploadResultDto> = answer(
        WriteCall(
            "uploadPhoto",
            parts = mapOf("album_id" to albumId.text(), "client_upload_id" to clientUploadId.text()),
            fileName = image.fileName(),
        )
    )

    override suspend fun patchPhoto(photoId: Long, body: JsonObject): Response<GalleryPhotoDto> =
        answer(WriteCall("patchPhoto", photoId, body))

    override suspend fun orderPhotos(albumId: Long, body: JsonObject): Response<Unit> =
        answer(WriteCall("orderPhotos", albumId, body))

    override suspend fun deletePhoto(photoId: Long): Response<Unit> = answer(WriteCall("deletePhoto", photoId))

    override suspend fun getTrash(): Response<List<GalleryTrashEntryDto>> = answer(WriteCall("getTrash"))

    override suspend fun restoreAlbum(albumId: Long): Response<GalleryAlbumDto> =
        answer(WriteCall("restoreAlbum", albumId))

    override suspend fun restorePhoto(photoId: Long): Response<GalleryPhotoDto> =
        answer(WriteCall("restorePhoto", photoId))

    override suspend fun getTaggableMembers(): Response<List<GalleryPhotoMemberDto>> =
        answer(WriteCall("getTaggableMembers"))

    override suspend fun putPhotoMembers(photoId: Long, body: JsonObject): Response<GalleryPhotoDto> =
        answer(WriteCall("putPhotoMembers", photoId, body = body))

    override suspend fun changePhotoMembers(body: JsonObject): Response<List<GalleryPhotoDto>> =
        answer(WriteCall("changePhotoMembers", body = body))

    private fun RequestBody.text(): String = Buffer().also { writeTo(it) }.readUtf8()

    private fun MultipartBody.Part.fileName(): String? =
        headers?.get("Content-Disposition")?.substringAfter("filename=\"", "")?.substringBefore('"')

    // endregion

    private class TruncatedBody(private val bytes: ByteArray) : ResponseBody() {
        override fun contentType(): MediaType = IMAGE_JPEG
        override fun contentLength(): Long = bytes.size * 2L
        override fun source(): BufferedSource = object : InputStream() {
            private val head = ByteArrayInputStream(bytes)
            override fun read(): Int = head.read().takeIf { it >= 0 }
                ?: throw IOException("Simulated connection cut mid-body")
        }.source().buffer()
    }

    companion object {
        val DEFAULT_BYTES: ByteArray = "fake-image-bytes".toByteArray()
        private const val HTTP_NO_CONTENT = 204
        private val IMAGE_JPEG = "image/jpeg".toMediaType()
        private val APPLICATION_JSON = "application/json".toMediaType()
    }
}
