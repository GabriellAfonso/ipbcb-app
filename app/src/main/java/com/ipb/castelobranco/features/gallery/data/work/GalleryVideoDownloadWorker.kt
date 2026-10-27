package com.ipb.castelobranco.features.gallery.data.work

import android.content.Context
import androidx.hilt.work.HiltWorker
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import androidx.work.workDataOf
import com.ipb.castelobranco.features.gallery.data.download.GalleryVideoDownloadJob
import com.ipb.castelobranco.features.gallery.data.download.VideoWorkDecision
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject
import kotlinx.coroutines.CancellationException
import timber.log.Timber

/** Downloads the gallery's videos. Always on an unmetered network; see `GalleryVideoDownloadJob`. */
@HiltWorker
class GalleryVideoDownloadWorker @AssistedInject constructor(
    @Assisted context: Context,
    @Assisted params: WorkerParameters,
    private val job: GalleryVideoDownloadJob,
) : CoroutineWorker(context, params) {

    override suspend fun doWork(): Result = try {
        when (val decision = job.run(runAttemptCount, MAX_RETRIES)) {
            VideoWorkDecision.Success -> Result.success()
            VideoWorkDecision.Retry -> Result.retry()
            // Ends the work: the next sync enqueues again and checks the space once more.
            VideoWorkDecision.LowSpace -> Result.failure(workDataOf(KEY_LOW_SPACE to true))
            is VideoWorkDecision.Fail ->
                Result.failure(workDataOf(KEY_ERROR to decision.message, KEY_ERROR_CODE to decision.code))
        }
    } catch (e: CancellationException) {
        throw e
    } catch (e: Exception) {
        Timber.e(e, "Gallery video download failed (attempt=%d)", runAttemptCount)
        if (runAttemptCount < MAX_RETRIES) Result.retry() else Result.failure()
    }

    companion object {
        const val WORK_NAME = "gallery_video_download"
        const val KEY_LOW_SPACE = "low_space"
        const val KEY_ERROR = "error"
        const val KEY_ERROR_CODE = "error_code"
        private const val MAX_RETRIES = 3
    }
}
