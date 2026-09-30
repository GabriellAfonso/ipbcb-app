package com.ipb.castelobranco.features.gallery.data.work

import androidx.work.BackoffPolicy
import androidx.work.Constraints
import androidx.work.ExistingWorkPolicy
import androidx.work.NetworkType
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import com.ipb.castelobranco.features.gallery.domain.upload.GalleryUploadScheduler
import java.util.concurrent.TimeUnit
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class WorkManagerGalleryUploadScheduler @Inject constructor(
    private val workManager: WorkManager,
) : GalleryUploadScheduler {

    /**
     * Any network: the manager chose to send now. `APPEND_OR_REPLACE` so photos added while a run is
     * finishing get a run of their own instead of being ignored.
     */
    override fun enqueue() {
        val request = OneTimeWorkRequestBuilder<GalleryUploadWorker>()
            .setConstraints(
                Constraints.Builder()
                    .setRequiredNetworkType(NetworkType.CONNECTED)
                    .build()
            )
            .setBackoffCriteria(BackoffPolicy.EXPONENTIAL, BACKOFF_SECONDS, TimeUnit.SECONDS)
            .build()
        workManager.enqueueUniqueWork(GalleryUploadWorker.WORK_NAME, ExistingWorkPolicy.APPEND_OR_REPLACE, request)
    }

    override fun cancel() {
        workManager.cancelUniqueWork(GalleryUploadWorker.WORK_NAME)
    }

    private companion object {
        const val BACKOFF_SECONDS = 30L
    }
}
