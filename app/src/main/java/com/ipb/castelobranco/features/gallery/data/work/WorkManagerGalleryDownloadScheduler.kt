package com.ipb.castelobranco.features.gallery.data.work

import androidx.work.BackoffPolicy
import androidx.work.Constraints
import androidx.work.ExistingWorkPolicy
import androidx.work.NetworkType
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import com.ipb.castelobranco.features.gallery.domain.download.GalleryDownloadScheduler
import java.util.concurrent.TimeUnit
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class WorkManagerGalleryDownloadScheduler @Inject constructor(
    private val workManager: WorkManager,
) : GalleryDownloadScheduler {

    override fun enqueueWifiOnly(replaceExisting: Boolean) {
        val request = OneTimeWorkRequestBuilder<GalleryDownloadWorker>()
            .setConstraints(
                Constraints.Builder()
                    .setRequiredNetworkType(NetworkType.UNMETERED)
                    .build()
            )
            .setBackoffCriteria(BackoffPolicy.EXPONENTIAL, BACKOFF_SECONDS, TimeUnit.SECONDS)
            .build()
        val policy = if (replaceExisting) ExistingWorkPolicy.REPLACE else ExistingWorkPolicy.KEEP
        workManager.enqueueUniqueWork(GalleryDownloadWorker.WORK_NAME, policy, request)
    }

    override fun enqueueAnyNetwork() {
        val request = OneTimeWorkRequestBuilder<GalleryDownloadWorker>()
            .setConstraints(
                Constraints.Builder()
                    .setRequiredNetworkType(NetworkType.CONNECTED)
                    .build()
            )
            .setBackoffCriteria(BackoffPolicy.EXPONENTIAL, BACKOFF_SECONDS, TimeUnit.SECONDS)
            .build()
        workManager.enqueueUniqueWork(
            GalleryDownloadWorker.WORK_NAME,
            ExistingWorkPolicy.REPLACE,
            request
        )
    }

    override fun cancel() {
        workManager.cancelUniqueWork(GalleryDownloadWorker.WORK_NAME)
    }

    private companion object {
        /** Primeira espera após `429` ou falha de rede; dobra a cada tentativa. */
        const val BACKOFF_SECONDS = 60L
    }
}
