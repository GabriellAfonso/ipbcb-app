package com.ipb.castelobranco.core.data.push

import android.content.Context
import androidx.hilt.work.HiltWorker
import androidx.work.BackoffPolicy
import androidx.work.Constraints
import androidx.work.CoroutineWorker
import androidx.work.ExistingWorkPolicy
import androidx.work.NetworkType
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import com.ipb.castelobranco.core.domain.auth.AuthStatusProvider
import com.ipb.castelobranco.core.domain.error.AppError
import com.ipb.castelobranco.core.domain.setlist.SyncSundaySetlistUseCase
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject
import java.util.concurrent.TimeUnit
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Re-reads the current Sunday setlist after a `setlist_saved` push. Gated on a valid access token like
 * any background caller: an expired one would make the refresh sign the user out. The next return to
 * the app covers that case.
 */
@HiltWorker
class SetlistRefreshWorker @AssistedInject constructor(
    @Assisted context: Context,
    @Assisted params: WorkerParameters,
    private val authStatus: AuthStatusProvider,
    private val syncSundaySetlist: SyncSundaySetlistUseCase,
) : CoroutineWorker(context, params) {

    override suspend fun doWork(): Result {
        if (!authStatus.hasValidAccessToken()) return Result.success()
        val error = syncSundaySetlist().exceptionOrNull() ?: return Result.success()
        return if (error is AppError.Network) Result.retry() else Result.success()
    }

    companion object {
        const val WORK_NAME = "setlist_refresh"
    }
}

fun interface SetlistRefreshScheduler {
    fun schedule()
}

@Singleton
class WorkManagerSetlistRefreshScheduler @Inject constructor(
    private val workManager: WorkManager,
) : SetlistRefreshScheduler {

    override fun schedule() {
        val request = OneTimeWorkRequestBuilder<SetlistRefreshWorker>()
            .setConstraints(Constraints.Builder().setRequiredNetworkType(NetworkType.CONNECTED).build())
            .setBackoffCriteria(BackoffPolicy.EXPONENTIAL, BACKOFF_SECONDS, TimeUnit.SECONDS)
            .build()
        workManager.enqueueUniqueWork(SetlistRefreshWorker.WORK_NAME, ExistingWorkPolicy.REPLACE, request)
    }

    private companion object {
        const val BACKOFF_SECONDS = 30L
    }
}
