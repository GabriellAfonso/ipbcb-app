package com.ipb.castelobranco.features.admin.members.data.photo

import com.ipb.castelobranco.core.domain.error.AppError
import com.ipb.castelobranco.core.domain.error.toAppError
import com.ipb.castelobranco.features.admin.members.domain.repository.MemberPhotoRevisions
import com.ipb.castelobranco.features.admin.members.domain.repository.MemberPhotoStore
import com.ipb.castelobranco.features.admin.members.domain.usecase.ValidateMemberPhotoUseCase
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.Response
import timber.log.Timber
import java.io.IOException
import java.util.concurrent.ConcurrentHashMap

/**
 * Where member photos come from: the encrypted [cache] first, the signed-in [client] otherwise.
 * A cached photo is returned at once and revalidated with `If-None-Match` in [scope], so the screen
 * never waits for the server; when the server's answer changes the device copy, [revisions] bumps
 * that URL and the screen loads it again.
 *
 * A 401/403 (after the authenticator's renewal) means the leader lost access: the whole cache and
 * its key go. All other changes to the device copy also pass through here, so [revisions] stays
 * true to what is on disk.
 *
 * [scope] outlives any screen on purpose: the revalidation is started by Coil, not by a ViewModel
 * (specs/012-encrypted-session-photo-cache/plan.md, Complexity Tracking).
 */
class MemberPhotoSource(
    private val cache: EncryptedMemberPhotoCache,
    private val client: OkHttpClient,
    private val scope: CoroutineScope,
    private val io: CoroutineDispatcher = Dispatchers.IO,
) : MemberPhotoStore, MemberPhotoRevisions {

    private val _revisions = MutableStateFlow<Map<String, Int>>(emptyMap())
    override val revisions: StateFlow<Map<String, Int>> = _revisions.asStateFlow()

    private val inFlight = ConcurrentHashMap.newKeySet<String>()

    /** The photo for [url], or null when there is none to show (initials instead). */
    suspend fun load(url: String): CachedPhoto? {
        cache.get(url)?.let { cached ->
            revalidate(url, cached.etag)
            return cached
        }
        return fetch(url).getOrNull()
    }

    /** The bytes to save with "Baixar": the copy on screen if there is one, else the server's. */
    suspend fun bytesFor(url: String): Result<CachedPhoto> =
        cache.get(url)?.let { Result.success(it) } ?: fetch(url)

    override suspend fun remove(url: String) {
        cache.remove(url)
        bump(url)
    }

    override suspend fun retainOnly(urls: Set<String>) {
        cache.retainOnly(urls)
    }

    override suspend fun wipe() {
        cache.wipe()
        _revisions.update { current -> current.mapValues { it.value + 1 } }
        Timber.i("Member photo cache wiped")
    }

    private suspend fun fetch(url: String): Result<CachedPhoto> = runCatching {
        val photo = withContext(io) {
            client.newCall(Request.Builder().url(url).build()).execute().use { response ->
                if (!response.isSuccessful) throw failureFor(response.code)
                response.toPhoto()
            }
        }
        cache.put(url, photo)
        photo
    }.recoverCatching { throwable ->
        val error = throwable.toAppError()
        if (error.isAccessLost()) wipe()
        throw error
    }

    private fun revalidate(url: String, etag: String?) {
        if (!inFlight.add(url)) return
        scope.launch {
            try {
                revalidateNow(url, etag)
            } catch (e: IOException) {
                Timber.d("Member photo revalidation skipped (%s)", e.javaClass.simpleName)
            } finally {
                inFlight.remove(url)
            }
        }
    }

    private suspend fun revalidateNow(url: String, etag: String?) {
        val request = Request.Builder().url(url)
            .apply { if (etag != null) header(IF_NONE_MATCH, etag) }
            .build()
        val outcome = withContext(io) {
            client.newCall(request).execute().use { response ->
                when {
                    response.code == HTTP_NOT_MODIFIED -> Outcome.Unchanged
                    response.isSuccessful -> Outcome.Changed(response.toPhoto())
                    response.code == HTTP_UNAUTHORIZED || response.code == HTTP_FORBIDDEN -> Outcome.AccessLost
                    response.code == HTTP_TOO_MANY_REQUESTS || response.code >= HTTP_SERVER_ERROR -> Outcome.Unchanged
                    else -> Outcome.Gone
                }
            }
        }
        when (outcome) {
            Outcome.Unchanged -> Unit
            is Outcome.Changed -> {
                cache.put(url, outcome.photo)
                bump(url)
            }
            Outcome.Gone -> remove(url)
            Outcome.AccessLost -> wipe()
        }
    }

    private fun bump(url: String) {
        _revisions.update { it + (url to (it[url] ?: 0) + 1) }
    }

    private fun Response.toPhoto(): CachedPhoto {
        val bytes = body.bytes()
        val mime = header(CONTENT_TYPE)?.substringBefore(';')?.trim()?.takeIf { it.startsWith(IMAGE_PREFIX) }
            ?: ValidateMemberPhotoUseCase().invoke(bytes).getOrDefault(ValidateMemberPhotoUseCase.MIME_JPEG)
        return CachedPhoto(etag = header(ETAG), mimeType = mime, bytes = bytes)
    }

    private fun failureFor(code: Int): AppError =
        if (code == HTTP_UNAUTHORIZED || code == HTTP_FORBIDDEN) AppError.Auth(code = code) else AppError.Server(code)

    private fun AppError.isAccessLost(): Boolean = this is AppError.Auth

    private sealed interface Outcome {
        data object Unchanged : Outcome
        data object Gone : Outcome
        data object AccessLost : Outcome
        class Changed(val photo: CachedPhoto) : Outcome
    }

    private companion object {
        const val IF_NONE_MATCH = "If-None-Match"
        const val ETAG = "ETag"
        const val CONTENT_TYPE = "Content-Type"
        const val IMAGE_PREFIX = "image/"
        const val HTTP_NOT_MODIFIED = 304
        const val HTTP_UNAUTHORIZED = 401
        const val HTTP_FORBIDDEN = 403
        const val HTTP_TOO_MANY_REQUESTS = 429
        const val HTTP_SERVER_ERROR = 500
    }
}
