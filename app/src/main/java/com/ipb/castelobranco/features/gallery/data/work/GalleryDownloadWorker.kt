package com.ipb.castelobranco.features.gallery.data.work

import android.content.Context
import androidx.hilt.work.HiltWorker
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import androidx.work.workDataOf
import com.ipb.castelobranco.features.gallery.data.download.GalleryDownloadJob
import com.ipb.castelobranco.features.gallery.data.download.WorkDecision
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject
import kotlinx.coroutines.CancellationException
import timber.log.Timber

@HiltWorker
class GalleryDownloadWorker @AssistedInject constructor(
    @Assisted context: Context,
    @Assisted params: WorkerParameters,
    private val job: GalleryDownloadJob,
) : CoroutineWorker(context, params) {

    override suspend fun doWork(): Result {
        return try {
            val decision = job.run(
                runAttemptCount = runAttemptCount,
                maxRetries = MAX_RETRIES,
                onProgress = { downloaded, total ->
                    setProgress(workDataOf(KEY_DOWNLOADED to downloaded, KEY_TOTAL to total))
                },
            )
            when (decision) {
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
    }
}
