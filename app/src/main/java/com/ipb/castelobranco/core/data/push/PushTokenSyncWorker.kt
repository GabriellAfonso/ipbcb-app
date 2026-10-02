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
import com.ipb.castelobranco.core.domain.push.PushRegistrationScheduler
import com.ipb.castelobranco.core.domain.push.RegisterDeviceUseCase
import com.ipb.castelobranco.core.domain.push.RegistrationOutcome
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject
import java.util.concurrent.TimeUnit
import javax.inject.Inject
import javax.inject.Singleton

/** Sends this device's push token to the server; WorkManager retries it until there is network. */
@HiltWorker
class PushTokenSyncWorker @AssistedInject constructor(
    @Assisted context: Context,
    @Assisted params: WorkerParameters,
    private val registerDevice: RegisterDeviceUseCase,
) : CoroutineWorker(context, params) {

    override suspend fun doWork(): Result = when (registerDevice()) {
        RegistrationOutcome.DONE -> Result.success()
        RegistrationOutcome.RETRY -> Result.retry()
        RegistrationOutcome.FAILED -> Result.failure()
    }

    companion object {
        const val WORK_NAME = "push_token_sync"
    }
}

@Singleton
class WorkManagerPushRegistrationScheduler @Inject constructor(
    private val workManager: WorkManager,
) : PushRegistrationScheduler {

    /** `REPLACE`: a rotated token must win over a registration still waiting with the old one. */
    override fun schedule() {
        val request = OneTimeWorkRequestBuilder<PushTokenSyncWorker>()
            .setConstraints(Constraints.Builder().setRequiredNetworkType(NetworkType.CONNECTED).build())
            .setBackoffCriteria(BackoffPolicy.EXPONENTIAL, BACKOFF_SECONDS, TimeUnit.SECONDS)
            .build()
        workManager.enqueueUniqueWork(PushTokenSyncWorker.WORK_NAME, ExistingWorkPolicy.REPLACE, request)
    }

    override fun cancel() {
        workManager.cancelUniqueWork(PushTokenSyncWorker.WORK_NAME)
    }

    private companion object {
        const val BACKOFF_SECONDS = 30L
    }
}
