package com.ipb.castelobranco.features.gallery.data.work

import androidx.work.BackoffPolicy
import androidx.work.Constraints
import androidx.work.ExistingWorkPolicy
import androidx.work.NetworkType
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import com.ipb.castelobranco.features.gallery.domain.download.GalleryVideoDownloadScheduler
import java.util.concurrent.TimeUnit
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class WorkManagerGalleryVideoDownloadScheduler @Inject constructor(
    private val workManager: WorkManager,
) : GalleryVideoDownloadScheduler {

    override fun enqueue(replaceExisting: Boolean) {
        val request = OneTimeWorkRequestBuilder<GalleryVideoDownloadWorker>()
            .setConstraints(Constraints.Builder().setRequiredNetworkType(NetworkType.UNMETERED).build())
            .setBackoffCriteria(BackoffPolicy.EXPONENTIAL, BACKOFF_SECONDS, TimeUnit.SECONDS)
            .build()
        val policy = if (replaceExisting) ExistingWorkPolicy.REPLACE else ExistingWorkPolicy.KEEP
        workManager.enqueueUniqueWork(GalleryVideoDownloadWorker.WORK_NAME, policy, request)
    }

    override fun cancel() {
        workManager.cancelUniqueWork(GalleryVideoDownloadWorker.WORK_NAME)
    }

    private companion object {
        const val BACKOFF_SECONDS = 60L
    }
}
