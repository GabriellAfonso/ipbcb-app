package com.ipb.castelobranco.features.gallery.data.work

import android.content.Context
import androidx.hilt.work.HiltWorker
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import androidx.work.workDataOf
import com.ipb.castelobranco.core.network.error.toAppError
import com.ipb.castelobranco.features.gallery.data.api.GalleryApi
import com.ipb.castelobranco.features.gallery.data.download.GalleryPhotoDownloader
import com.ipb.castelobranco.features.gallery.data.download.WorkDecision
import com.ipb.castelobranco.features.gallery.data.download.toWorkDecision
import com.ipb.castelobranco.features.gallery.domain.repository.GalleryRepository
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject
import kotlinx.coroutines.CancellationException
import timber.log.Timber

@HiltWorker
class GalleryDownloadWorker @AssistedInject constructor(
    @Assisted context: Context,
    @Assisted params: WorkerParameters,
    private val api: GalleryApi,
    private val downloader: GalleryPhotoDownloader,
    private val repository: GalleryRepository,
) : CoroutineWorker(context, params) {

    override suspend fun doWork(): Result {
        return try {
            val response = api.getAllPhotos()
            if (!response.isSuccessful) {
                val error = response.toAppError()
                val errorMessage = error.userMessage ?: error.message ?: "HTTP ${response.code()}"
                val code = response.code()
                if (code == HTTP_UNAUTHORIZED || code == HTTP_FORBIDDEN) {
                    return failure(errorMessage, code)
                }
                return if (runAttemptCount < MAX_RETRIES) Result.retry() else failure(errorMessage, code)
            }

            val photos = response.body() ?: return Result.success()

            // Atualiza os flows do repositório após cada álbum — a UI mostra novos álbuns em tempo real
            val run = downloader.download(
                photos = photos,
                onProgress = { downloaded, total ->
                    setProgress(workDataOf(KEY_DOWNLOADED to downloaded, KEY_TOTAL to total))
                },
                onAlbumDone = { repository.preload() },
            )
            // Em qualquer desfecho, as fotos salvas antes de uma parada aparecem na grid
            repository.preload()

            when (val decision = run.toWorkDecision(runAttemptCount, MAX_RETRIES)) {
                WorkDecision.Success -> Result.success()
                WorkDecision.Retry -> Result.retry()
                is WorkDecision.Fail -> failure(decision.message, decision.code)
            }
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            Timber.e(e, "Gallery download failed (attempt=%d)", runAttemptCount)
            if (runAttemptCount < MAX_RETRIES) Result.retry() else Result.failure()
        }
    }

    private fun failure(message: String, code: Int): Result =
        Result.failure(workDataOf(KEY_ERROR to message, KEY_ERROR_CODE to code))

    companion object {
        const val WORK_NAME = "gallery_auto_download"
        const val KEY_DOWNLOADED = "downloaded"
        const val KEY_TOTAL = "total"
        const val KEY_ERROR = "error"
        const val KEY_ERROR_CODE = "error_code"
        private const val MAX_RETRIES = 3
        private const val HTTP_UNAUTHORIZED = 401
        private const val HTTP_FORBIDDEN = 403
    }
}
