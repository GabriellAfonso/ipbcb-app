package com.ipb.castelobranco.features.gallery.data

import androidx.work.WorkInfo
import androidx.work.WorkManager
import com.ipb.castelobranco.core.domain.gallery.GalleryMissing
import com.ipb.castelobranco.core.domain.gallery.GalleryStoragePort
import com.ipb.castelobranco.core.domain.gallery.GalleryUsage
import com.ipb.castelobranco.core.domain.gallery.GalleryVideoSummary
import com.ipb.castelobranco.features.gallery.data.work.GalleryVideoDownloadWorker
import com.ipb.castelobranco.features.gallery.domain.usecase.GalleryStorageUseCase
import com.ipb.castelobranco.features.gallery.domain.usecase.GalleryVideoSettingsUseCase
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import javax.inject.Inject

class GalleryStoragePortImpl @Inject constructor(
    private val storage: GalleryStorageUseCase,
    private val videoSettings: GalleryVideoSettingsUseCase,
    workManager: WorkManager,
) : GalleryStoragePort {

    private val isDownloadingVideos: Flow<Boolean> = workManager
        .getWorkInfosForUniqueWorkFlow(GalleryVideoDownloadWorker.WORK_NAME)
        .map { infos -> infos.any { it.state == WorkInfo.State.RUNNING } }

    override val videos: Flow<GalleryVideoSummary> =
        combine(storage.videos, isDownloadingVideos) { summary, running -> summary.copy(isDownloading = running) }

    override val autoDownloadVideos: Flow<Boolean> = videoSettings.autoDownload

    override suspend fun setAutoDownloadVideos(enabled: Boolean) = videoSettings.setAutoDownload(enabled)

    override suspend fun usage(): GalleryUsage = storage.usage()

    override suspend fun missing(): GalleryMissing = storage.missing()

    override suspend fun delete(photos: Boolean, videos: Boolean) = storage.delete(photos, videos)

    override suspend fun download(photos: Boolean, videos: Boolean, useMobileData: Boolean) =
        storage.download(photos, videos, useMobileData)
}
