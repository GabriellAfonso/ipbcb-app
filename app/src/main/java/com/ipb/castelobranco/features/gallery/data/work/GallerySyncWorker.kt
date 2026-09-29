package com.ipb.castelobranco.features.gallery.data.work

import android.content.Context
import androidx.hilt.work.HiltWorker
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.ipb.castelobranco.features.gallery.domain.usecase.SyncGalleryUseCase
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject

/**
 * The 6-hour background sync. Always succeeds: a failed sync changes nothing on the device, and the
 * next period (or the next app open) tries again.
 */
@HiltWorker
class GallerySyncWorker @AssistedInject constructor(
    @Assisted context: Context,
    @Assisted params: WorkerParameters,
    private val syncGallery: SyncGalleryUseCase,
) : CoroutineWorker(context, params) {

    override suspend fun doWork(): Result {
        syncGallery()
        return Result.success()
    }

    companion object {
        const val WORK_NAME = "gallery_sync_periodic"
    }
}
