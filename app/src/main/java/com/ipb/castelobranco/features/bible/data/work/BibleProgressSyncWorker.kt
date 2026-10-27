package com.ipb.castelobranco.features.bible.data.work

import android.content.Context
import androidx.hilt.work.HiltWorker
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.ipb.castelobranco.features.bible.domain.usecase.SyncBibleProgressUseCase
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject
import timber.log.Timber

/** Sends queued reading/watched marks in the background; every failure becomes a retry. */
@HiltWorker
class BibleProgressSyncWorker @AssistedInject constructor(
    @Assisted context: Context,
    @Assisted params: WorkerParameters,
    private val syncProgress: SyncBibleProgressUseCase,
) : CoroutineWorker(context, params) {

    override suspend fun doWork(): Result =
        runCatching {
            if (syncProgress()) Result.success() else Result.retry()
        }.getOrElse {
            Timber.d(it, "Bible progress sync worker failed")
            Result.retry()
        }

    companion object {
        const val WORK_NAME = "bible_progress_sync"
    }
}
