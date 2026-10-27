package com.ipb.castelobranco.features.bible.data.repository

import com.ipb.castelobranco.core.di.IoDispatcher
import com.ipb.castelobranco.core.domain.error.AppError
import com.ipb.castelobranco.core.domain.error.toAppError
import com.ipb.castelobranco.core.domain.snapshot.NetworkResult
import com.ipb.castelobranco.core.domain.snapshot.SnapshotCache
import com.ipb.castelobranco.core.domain.snapshot.SnapshotFetcher
import com.ipb.castelobranco.core.network.error.toAppError
import com.ipb.castelobranco.features.bible.data.api.BibleVideosManageApi
import com.ipb.castelobranco.features.bible.data.dto.BibleVideoDto
import com.ipb.castelobranco.features.bible.data.dto.addVideoBody
import com.ipb.castelobranco.features.bible.data.dto.toBody
import com.ipb.castelobranco.features.bible.data.dto.toDomain
import com.ipb.castelobranco.features.bible.data.dto.videoOrderBody
import com.ipb.castelobranco.features.bible.domain.model.BiblePlace
import com.ipb.castelobranco.features.bible.domain.model.BibleVideo
import com.ipb.castelobranco.features.bible.domain.repository.AddVideoResult
import com.ipb.castelobranco.features.bible.domain.repository.BibleVideoRepository
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json
import retrofit2.Response
import timber.log.Timber
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class BibleVideoRepositoryImpl @Inject constructor(
    private val cache: SnapshotCache<List<BibleVideoDto>>,
    private val fetcher: SnapshotFetcher<List<BibleVideoDto>>,
    private val api: BibleVideosManageApi,
    private val json: Json,
    @param:IoDispatcher private val ioDispatcher: CoroutineDispatcher,
) : BibleVideoRepository {

    private val _videos = MutableStateFlow<List<BibleVideo>>(emptyList())
    override val videos: StateFlow<List<BibleVideo>> = _videos.asStateFlow()

    private var loaded = false
    private val refreshMutex = Mutex()

    override suspend fun preload() {
        if (loaded) return
        withContext(ioDispatcher) { cache.load() }?.let { _videos.value = it.toDomain() }
        loaded = true
    }

    override suspend fun refresh() = refreshMutex.withLock {
        preload()
        val etag = withContext(ioDispatcher) { cache.loadETag() }
        when (val result = fetcher.fetch(etag)) {
            is NetworkResult.Success -> {
                withContext(ioDispatcher) { cache.save(result.body, result.etag) }
                _videos.value = result.body.toDomain()
            }
            NetworkResult.NotModified -> Unit
            is NetworkResult.Failure ->
                Timber.d("Bible videos refresh failed: %s", result.throwable.javaClass.simpleName)
        }
    }

    override suspend fun addVideo(url: String, place: BiblePlace): AddVideoResult {
        val outcome = request { api.addVideo(addVideoBody(url.trim(), listOf(place))) }
        val error = outcome.exceptionOrNull() as? AppError
        // An empty 2xx body comes back as Unit (see request): not a video.
        val created = (outcome.getOrNull() as Any?) as? BibleVideoDto
        return when {
            created != null -> {
                refresh()
                AddVideoResult.Added(created.toDomain())
            }
            outcome.isSuccess -> {
                refresh()
                AddVideoResult.Failed(AppError.Unknown(message = "Empty body on video creation"))
            }
            error is AppError.Server && error.errorCode == ERROR_VIDEO_EXISTS -> {
                refresh()
                AddVideoResult.AlreadyExists(error.existingVideo())
            }
            else -> AddVideoResult.Failed(error ?: AppError.Unknown())
        }
    }

    override suspend fun setPlaces(videoId: Long, places: List<BiblePlace>): Result<Unit> =
        write { api.setPlaces(videoId, places.toBody()) }

    override suspend fun removePlace(videoId: Long, place: BiblePlace): Result<Unit> =
        write { api.removePlace(videoId, place.book, place.chapter) }

    override suspend fun deleteVideo(videoId: Long): Result<Unit> =
        write { api.deleteVideo(videoId) }

    override suspend fun reorder(place: BiblePlace, videoIds: List<Long>): Result<Unit> =
        write { api.setOrder(videoOrderBody(place, videoIds)) }

    /** Any outcome refreshes: a refusal usually means the list changed under the user. */
    private suspend fun write(call: suspend () -> Response<Unit>): Result<Unit> {
        val outcome = request { call() }.map { }
        outcome.exceptionOrNull()?.let { Timber.w("Bible video write refused: %s", it.javaClass.simpleName) }
        refresh()
        return outcome
    }

    /** The body of a 2xx (`Unit` for an empty one), or the failure as an [AppError]. */
    @Suppress("UNCHECKED_CAST")
    private suspend fun <B> request(call: suspend () -> Response<B>): Result<B> = try {
        val response = call()
        when {
            !response.isSuccessful -> Result.failure(response.toAppError())
            else -> Result.success(response.body() ?: Unit as B)
        }
    } catch (e: CancellationException) {
        throw e
    } catch (t: Throwable) {
        Result.failure(t.toAppError())
    }

    /** The existing video the server sends beside `error_code` in a 409, when readable. */
    private fun AppError.Server.existingVideo(): BibleVideo? = extras?.get(EXISTING_VIDEO_KEY)?.let { raw ->
        runCatching { json.decodeFromString(BibleVideoDto.serializer(), raw).toDomain() }.getOrNull()
    }

    companion object {
        const val ERROR_VIDEO_EXISTS = "BIBLE_VIDEO_EXISTS"
        private const val EXISTING_VIDEO_KEY = "video"
    }
}
