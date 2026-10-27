package com.ipb.castelobranco.features.bible.data.work

import android.content.Context
import androidx.work.BackoffPolicy
import androidx.work.Constraints
import androidx.work.ExistingWorkPolicy
import androidx.work.NetworkType
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import com.ipb.castelobranco.features.bible.domain.sync.BibleProgressSyncScheduler
import dagger.hilt.android.qualifiers.ApplicationContext
import timber.log.Timber
import java.util.concurrent.TimeUnit
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class WorkManagerBibleProgressSyncScheduler @Inject constructor(
    @param:ApplicationContext private val context: Context,
) : BibleProgressSyncScheduler {

    override fun scheduleSync() {
        runCatching {
            val request = OneTimeWorkRequestBuilder<BibleProgressSyncWorker>()
                .setConstraints(Constraints.Builder().setRequiredNetworkType(NetworkType.CONNECTED).build())
                .setBackoffCriteria(BackoffPolicy.EXPONENTIAL, BACKOFF_SECONDS, TimeUnit.SECONDS)
                .build()

            // KEEP, so a burst of marks cannot spawn a burst of jobs. A mark made while a run is finishing is picked
            // up by the drain loop, by the next mark, or by the enqueue in MyApp.onCreate().
            WorkManager.getInstance(context).enqueueUniqueWork(
                BibleProgressSyncWorker.WORK_NAME,
                ExistingWorkPolicy.KEEP,
                request,
            )
        }.onFailure { Timber.d(it, "Failed to schedule bible progress sync") }
    }

    private companion object {
        const val BACKOFF_SECONDS = 30L
    }
}
