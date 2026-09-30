package com.ipb.castelobranco.features.gallery.data.work

import android.content.Context
import androidx.hilt.work.HiltWorker
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import androidx.work.workDataOf
import com.ipb.castelobranco.features.gallery.data.upload.GalleryUploadRun
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject
import kotlinx.coroutines.CancellationException
import timber.log.Timber

/**
 * Sends the gallery upload queue. Runs in the foreground (a user-visible data transfer) so a long
 * queue keeps going with the screen off; when the platform refuses a foreground start from the
 * background, it carries on as a plain worker.
 */
@HiltWorker
class GalleryUploadWorker @AssistedInject constructor(
    @Assisted context: Context,
    @Assisted params: WorkerParameters,
    private val run: GalleryUploadRun,
    private val notifications: GalleryUploadNotifications,
) : CoroutineWorker(context, params) {

    override suspend fun doWork(): Result {
        try {
            setForeground(notifications.foregroundInfo(current = 0, total = 0))
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            Timber.w(e, "Gallery upload running without foreground service")
        }

        val result = try {
            run.run { current, total ->
                setProgress(workDataOf(KEY_CURRENT to current, KEY_TOTAL to total))
                notifications.showProgress(current, total)
            }
        } finally {
            notifications.clearProgress()
        }
        return when (result) {
            is GalleryUploadRun.Result.Done -> {
                if (result.failed > 0) notifications.showFailures(result.failed)
                Result.success()
            }
            GalleryUploadRun.Result.Retry -> Result.retry()
            GalleryUploadRun.Result.Stopped -> Result.success()
        }
    }

    companion object {
        const val WORK_NAME = "gallery_upload"
        const val KEY_CURRENT = "current"
        const val KEY_TOTAL = "total"
    }
}
