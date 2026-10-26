package com.ipb.castelobranco.features.bible.data.work

import androidx.work.Constraints
import androidx.work.Data
import androidx.work.ExistingWorkPolicy
import androidx.work.NetworkType
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.workDataOf
import com.ipb.castelobranco.features.bible.domain.download.BibleDownloadScheduler
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class WorkManagerBibleDownloadScheduler @Inject constructor(
    private val workManager: WorkManager,
) : BibleDownloadScheduler {

    override fun enqueueWifiOnly(replaceExisting: Boolean, force: Boolean) {
        val request = OneTimeWorkRequestBuilder<BibleDownloadWorker>()
            .setConstraints(
                Constraints.Builder()
                    .setRequiredNetworkType(NetworkType.UNMETERED)
                    .build()
            )
            .setInputData(forceData(force))
            .build()
        val policy = if (replaceExisting) ExistingWorkPolicy.REPLACE else ExistingWorkPolicy.KEEP
        workManager.enqueueUniqueWork(BibleDownloadWorker.WORK_NAME, policy, request)
    }

    override fun enqueueAnyNetwork(force: Boolean) {
        val request = OneTimeWorkRequestBuilder<BibleDownloadWorker>()
            .setConstraints(
                Constraints.Builder()
                    .setRequiredNetworkType(NetworkType.CONNECTED)
                    .build()
            )
            .setInputData(forceData(force))
            .build()
        workManager.enqueueUniqueWork(
            BibleDownloadWorker.WORK_NAME,
            ExistingWorkPolicy.REPLACE,
            request,
        )
    }

    override fun enqueueFormatUpgrade() {
        val request = OneTimeWorkRequestBuilder<BibleDownloadWorker>()
            .setConstraints(
                Constraints.Builder()
                    .setRequiredNetworkType(NetworkType.UNMETERED)
                    .build()
            )
            .setInputData(forceData(false))
            .addTag(BibleDownloadWorker.TAG_FORMAT_UPGRADE)
            .build()
        workManager.enqueueUniqueWork(BibleDownloadWorker.WORK_NAME, ExistingWorkPolicy.KEEP, request)
    }

    override fun cancel() {
        workManager.cancelUniqueWork(BibleDownloadWorker.WORK_NAME)
    }

    private fun forceData(force: Boolean): Data =
        workDataOf(BibleDownloadWorker.INPUT_FORCE to force)
}
