package com.ipb.castelobranco.features.gallery.data.download

import com.ipb.castelobranco.core.domain.error.AppError
import com.ipb.castelobranco.core.network.error.toAppError
import com.ipb.castelobranco.features.gallery.data.api.GalleryApi
import com.ipb.castelobranco.features.gallery.data.dto.GalleryPhotoDto
import com.ipb.castelobranco.features.gallery.data.local.GalleryPhotoStorage
import kotlinx.coroutines.CancellationException
import timber.log.Timber
import java.io.IOException
import javax.inject.Inject

/**
 * The one per-photo download loop, shared by the background worker and the repository.
 *
 * A photo counts as downloaded only when it is on disk. A photo that fails (404, other error, I/O)
 * is skipped and tried again on the next run. A refusal that describes the requester rather than the
 * photo ([STOP_CODES]) ends the run at once — every following request would get the same answer.
 */
class GalleryPhotoDownloader @Inject constructor(
    private val api: GalleryApi,
    private val storage: GalleryPhotoStorage,
) {

    suspend fun download(
        photos: List<GalleryPhotoDto>,
        onProgress: suspend (downloaded: Int, total: Int) -> Unit,
        onAlbumDone: suspend () -> Unit = {},
    ): GalleryDownloadRun {
        val total = photos.size
        var downloaded = 0
        var failed = 0
        var networkFailures = 0

        onProgress(downloaded, total)

        for ((_, albumPhotos) in photos.groupBy { it.albumId }) {
            for (photo in albumPhotos) {
                when (val outcome = downloadOne(photo)) {
                    PhotoOutcome.Present, PhotoOutcome.Saved -> {
                        downloaded++
                        onProgress(downloaded, total)
                    }

                    is PhotoOutcome.Failed -> {
                        failed++
                        if (outcome.isNetwork) networkFailures++
                    }

                    is PhotoOutcome.Stop -> return GalleryDownloadRun.Stopped(
                        reason = outcome.reason,
                        error = outcome.error,
                        downloaded = downloaded,
                        total = total,
                    )
                }
            }
            onAlbumDone()
        }

        return GalleryDownloadRun.Completed(downloaded, total, failed, networkFailures)
    }

    private suspend fun downloadOne(photo: GalleryPhotoDto): PhotoOutcome {
        if (storage.exists(photo.albumId, photo.id)) return PhotoOutcome.Present

        return try {
            val response = api.downloadFile(photo.imageUrl)
            val body = response.body()
            when {
                response.code() in STOP_CODES ->
                    PhotoOutcome.Stop(STOP_CODES.getValue(response.code()), response.toAppError())

                !response.isSuccessful || body == null -> {
                    Timber.w("Gallery photo %d skipped: HTTP %d", photo.id, response.code())
                    PhotoOutcome.Failed(isNetwork = false)
                }

                else -> {
                    body.byteStream().use { input ->
                        storage.save(photo.albumId, photo.id, photo.fileExtension(), input)
                    }
                    storage.savePhotoMetadata(photo.albumId, photo.id, photo)
                    PhotoOutcome.Saved
                }
            }
        } catch (e: CancellationException) {
            throw e
        } catch (e: IOException) {
            Timber.w(e, "Gallery photo %d skipped: network failure", photo.id)
            PhotoOutcome.Failed(isNetwork = true)
        }
    }

    private sealed interface PhotoOutcome {
        data object Present : PhotoOutcome
        data object Saved : PhotoOutcome
        data class Failed(val isNetwork: Boolean) : PhotoOutcome
        data class Stop(val reason: StopReason, val error: AppError) : PhotoOutcome
    }

    private companion object {
        const val HTTP_UNAUTHORIZED = 401
        const val HTTP_FORBIDDEN = 403
        const val HTTP_TOO_MANY_REQUESTS = 429

        /** 401 only arrives here after TokenAuthenticator already tried — and failed — to refresh. */
        val STOP_CODES: Map<Int, StopReason> = mapOf(
            HTTP_UNAUTHORIZED to StopReason.UNAUTHENTICATED,
            HTTP_FORBIDDEN to StopReason.FORBIDDEN,
            HTTP_TOO_MANY_REQUESTS to StopReason.RATE_LIMITED,
        )
    }
}
