package com.ipb.castelobranco.features.gallery.data.api

import com.ipb.castelobranco.features.gallery.data.dto.GalleryChangesDto
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
        private val IMAGE_JPEG = "image/jpeg".toMediaType()
        private val APPLICATION_JSON = "application/json".toMediaType()
    }
}
