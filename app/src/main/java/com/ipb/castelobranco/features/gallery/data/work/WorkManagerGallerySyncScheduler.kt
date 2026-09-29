package com.ipb.castelobranco.features.gallery.data.work

import androidx.work.Constraints
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.NetworkType
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import com.ipb.castelobranco.features.gallery.domain.sync.GallerySyncScheduler
import java.util.concurrent.TimeUnit
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class WorkManagerGallerySyncScheduler @Inject constructor(
    private val workManager: WorkManager,
) : GallerySyncScheduler {

    override fun schedulePeriodic() {
        val request = PeriodicWorkRequestBuilder<GallerySyncWorker>(SYNC_INTERVAL_HOURS, TimeUnit.HOURS)
            .setConstraints(
                Constraints.Builder()
                    .setRequiredNetworkType(NetworkType.CONNECTED)
                    .build()
            )
            .build()
        workManager.enqueueUniquePeriodicWork(
            GallerySyncWorker.WORK_NAME,
            ExistingPeriodicWorkPolicy.KEEP,
            request,
        )
    }

    override fun cancel() {
        workManager.cancelUniqueWork(GallerySyncWorker.WORK_NAME)
    }

    private companion object {
        const val SYNC_INTERVAL_HOURS = 6L
    }
}
