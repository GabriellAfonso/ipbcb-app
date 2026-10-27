package com.ipb.castelobranco.features.gallery.data.download

import com.ipb.castelobranco.core.domain.error.AppError
import com.ipb.castelobranco.core.network.error.toAppError
import com.ipb.castelobranco.features.gallery.data.api.GalleryApi
import com.ipb.castelobranco.features.gallery.data.local.GalleryMediaStore
import com.ipb.castelobranco.features.gallery.domain.model.GalleryPhoto
import kotlinx.coroutines.CancellationException
import okhttp3.ResponseBody
import timber.log.Timber
import java.io.File
import java.io.FileOutputStream
import java.io.IOException
import javax.inject.Inject

/** Bytes the app may still write on the device's internal storage. */
fun interface DeviceSpace {
    fun usableBytes(): Long
}

/** How a pass over the videos ended. [downloaded] = videos of the list complete on disk at the end. */
sealed interface VideoDownloadRun {
    val downloaded: Int
    val total: Int

    data class Completed(
        override val downloaded: Int,
        override val total: Int,
        val networkFailures: Int,
    ) : VideoDownloadRun

    /** Finishing the next video would leave less than [GalleryVideoDownloader.MIN_FREE_BYTES] free. */
    data class LowSpace(override val downloaded: Int, override val total: Int) : VideoDownloadRun

    /** A refusal that describes the requester (401/403/429), as for photos. */
    data class Stopped(
        val reason: StopReason,
        val error: AppError,
        override val downloaded: Int,
        override val total: Int,
    ) : VideoDownloadRun
}

/**
 * Downloads videos one by one into `gallery/videos/`. A video accumulates in its partial file and gets
 * its final name only once whole, so a partial never counts. An interrupted video resumes with
 * `Range: bytes=N-` from the partial's size: `206` appends, `200` (range ignored) starts over, `416`
 * with the partial already at the known size completes it, otherwise the partial is dropped.
 */
class GalleryVideoDownloader @Inject constructor(
    private val api: GalleryApi,
    private val storage: GalleryMediaStore,
    private val space: DeviceSpace,
) {

    suspend fun download(
        videos: List<GalleryPhoto>,
        onVideoDone: suspend () -> Unit = {},
    ): VideoDownloadRun {
        val total = videos.size
        var downloaded = 0
        var networkFailures = 0
        for (video in videos) {
            when (val outcome = downloadOne(video)) {
                VideoOutcome.Present -> downloaded++
                VideoOutcome.Saved -> {
                    downloaded++
                    onVideoDone()
                }
                is VideoOutcome.Failed -> if (outcome.isNetwork) networkFailures++
                VideoOutcome.LowSpace -> return VideoDownloadRun.LowSpace(downloaded, total)
                is VideoOutcome.Stop ->
                    return VideoDownloadRun.Stopped(outcome.reason, outcome.error, downloaded, total)
            }
        }
        return VideoDownloadRun.Completed(downloaded, total, networkFailures)
    }

    private suspend fun downloadOne(video: GalleryPhoto): VideoOutcome {
        val ext = video.fileExtension()
        if (storage.videoFile(video.id, ext).exists()) return VideoOutcome.Present
        val partial = storage.partialVideo(video.id, ext)
        val have = partial.length()
        val needed = ((video.fileSize ?: UNKNOWN_SIZE_BYTES) - have).coerceAtLeast(0)
        if (space.usableBytes() - needed < MIN_FREE_BYTES) return VideoOutcome.LowSpace

        return try {
            val response = api.downloadFile(video.imageUrl, range = if (have > 0) "bytes=$have-" else null)
            val body = response.body()
            when {
                response.code() in STOP_CODES ->
                    VideoOutcome.Stop(STOP_CODES.getValue(response.code()), response.toAppError())
                response.code() == HTTP_RANGE_NOT_SATISFIABLE -> {
                    if (video.fileSize != null && have == video.fileSize) {
                        storage.completeVideo(video.id, ext)
                        VideoOutcome.Saved
                    } else {
                        partial.delete()
                        VideoOutcome.Failed(isNetwork = false)
                    }
                }
                !response.isSuccessful || body == null -> {
                    Timber.w("Gallery video %d skipped: HTTP %d", video.id, response.code())
                    VideoOutcome.Failed(isNetwork = false)
                }
                else -> {
                    write(body, partial, append = response.code() == HTTP_PARTIAL_CONTENT)
                    val size = partial.length()
                    if (video.fileSize == null || size == video.fileSize) {
                        storage.completeVideo(video.id, ext)
                        VideoOutcome.Saved
                    } else {
                        // Longer than announced: start over next time. Shorter: the body was cut.
                        if (size > video.fileSize) partial.delete()
                        VideoOutcome.Failed(isNetwork = size < video.fileSize)
                    }
                }
            }
        } catch (e: CancellationException) {
            throw e
        } catch (e: IOException) {
            Timber.w(e, "Gallery video %d paused: network failure", video.id)
            VideoOutcome.Failed(isNetwork = true)
        }
    }

    private fun write(body: ResponseBody, partial: File, append: Boolean) {
        body.byteStream().use { input ->
            FileOutputStream(partial, append).use { output -> input.copyTo(output) }
        }
    }

    private sealed interface VideoOutcome {
        data object Present : VideoOutcome
        data object Saved : VideoOutcome
        data class Failed(val isNetwork: Boolean) : VideoOutcome
        data object LowSpace : VideoOutcome
        data class Stop(val reason: StopReason, val error: AppError) : VideoOutcome
    }

    companion object {
        /** Video downloads never leave less than this free on the device. */
        const val MIN_FREE_BYTES = 1024L * 1024 * 1024

        /** A video without a known size is assumed to be as big as the upload limit. */
        const val UNKNOWN_SIZE_BYTES = 100L * 1024 * 1024
        private const val HTTP_PARTIAL_CONTENT = 206
        private const val HTTP_RANGE_NOT_SATISFIABLE = 416
        private const val HTTP_UNAUTHORIZED = 401
        private const val HTTP_FORBIDDEN = 403
        private const val HTTP_TOO_MANY_REQUESTS = 429
        private val STOP_CODES: Map<Int, StopReason> = mapOf(
            HTTP_UNAUTHORIZED to StopReason.UNAUTHENTICATED,
            HTTP_FORBIDDEN to StopReason.FORBIDDEN,
            HTTP_TOO_MANY_REQUESTS to StopReason.RATE_LIMITED,
        )
    }
}
