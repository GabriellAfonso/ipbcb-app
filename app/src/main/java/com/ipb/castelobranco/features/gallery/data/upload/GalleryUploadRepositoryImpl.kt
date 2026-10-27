package com.ipb.castelobranco.features.gallery.data.upload

import com.ipb.castelobranco.core.di.IoDispatcher
import com.ipb.castelobranco.core.domain.error.AppError
import com.ipb.castelobranco.features.gallery.data.local.GalleryMediaStore
import com.ipb.castelobranco.features.gallery.domain.upload.GalleryUploadRepository
import com.ipb.castelobranco.features.gallery.domain.upload.GalleryUploadScheduler
import com.ipb.castelobranco.features.gallery.domain.upload.PickedMedia
import com.ipb.castelobranco.features.gallery.domain.upload.UploadItem
import com.ipb.castelobranco.features.gallery.domain.upload.UploadKind
import com.ipb.castelobranco.features.gallery.domain.upload.UploadState
import com.ipb.castelobranco.features.gallery.domain.upload.VideoUploadCheck
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.withContext
import java.io.File
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class GalleryUploadRepositoryImpl @Inject constructor(
    private val queue: GalleryUploadQueueStore,
    private val copier: PickedImageCopier,
    private val preparer: GalleryImagePreparer,
    private val probe: VideoProbe,
    private val mediaStore: GalleryMediaStore,
    private val scheduler: GalleryUploadScheduler,
    @param:IoDispatcher private val ioDispatcher: CoroutineDispatcher,
) : GalleryUploadRepository {

    override val items: StateFlow<List<UploadItem>> = queue.items

    override suspend fun inspect(sources: List<String>): List<PickedMedia> = sources.map { copier.inspect(it) }

    /** Each copy is queued as soon as it lands, so the album shows the count growing. */
    override suspend fun enqueue(albumId: Long, sources: List<String>, videosWifiOnly: Boolean) {
        queue.load()
        var wifiOnly = false
        sources.forEach { source ->
            val copy = copier.copy(source)
            val item = when {
                copy == null -> UploadItem(
                    uploadId = UUID.randomUUID().toString(),
                    albumId = albumId,
                    displayName = source.substringAfterLast('/'),
                    fileName = "",
                    state = UploadState.Failed,
                    failure = GalleryUploadRun.UNREADABLE_MESSAGE,
                    enqueuedAt = System.currentTimeMillis(),
                )
                copy.isVideo -> checkedVideo(albumId, copy, videosWifiOnly)
                else -> UploadItem(
                    uploadId = copy.uploadId,
                    albumId = albumId,
                    displayName = copy.displayName,
                    fileName = copy.fileName,
                    state = UploadState.Waiting,
                    enqueuedAt = System.currentTimeMillis(),
                )
            }
            if (item.wifiOnly && item.state != UploadState.Failed) wifiOnly = true
            queue.append(item)
        }
        scheduler.enqueue()
        if (wifiOnly) scheduler.enqueueWifiOnly()
    }

    /**
     * A video is checked now, while the person is still looking: refused ones are listed with the
     * reason, and the others are ready to send as they are (nothing to prepare but the poster).
     */
    private suspend fun checkedVideo(albumId: Long, copy: PickedImageCopier.Copy, wifiOnly: Boolean): UploadItem {
        val video = mediaStore.uploadFile(copy.fileName)
        val posterName = posterFileName(copy.uploadId)
        val poster = mediaStore.uploadFile(posterName)
        val result = withContext(ioDispatcher) { probe.probe(video, poster) }
        val refusal = result?.let(VideoUploadCheck::refusal) ?: VideoUploadCheck.UNREADABLE.takeIf { result == null }
        val base = UploadItem(
            uploadId = copy.uploadId,
            albumId = albumId,
            displayName = copy.displayName,
            fileName = copy.fileName,
            state = UploadState.Prepared,
            enqueuedAt = System.currentTimeMillis(),
            kind = UploadKind.VIDEO,
        )
        if (refusal != null || result == null) {
            withContext(ioDispatcher) {
                video.delete()
                poster.delete()
            }
            return base.copy(fileName = "", state = UploadState.Failed, failure = refusal)
        }
        return base.copy(
            posterFileName = posterName,
            durationMs = result.durationMs,
            dateTaken = result.dateTaken,
            wifiOnly = wifiOnly,
        )
    }

    override suspend fun dismiss(uploadId: String) = queue.dismiss(uploadId)

    override suspend fun prepareCover(source: String): Result<File> {
        val copy = copier.copy(source) ?: return unreadable()
        val item = UploadItem(
            uploadId = copy.uploadId,
            albumId = 0,
            displayName = copy.displayName,
            fileName = copy.fileName,
            state = UploadState.Waiting,
            enqueuedAt = System.currentTimeMillis(),
        )
        return when (val prepared = withContext(ioDispatcher) { preparer.prepare(item) }) {
            is PrepareResult.Ready -> Result.success(prepared.file)
            PrepareResult.TooLarge -> Result.failure(AppError.Unknown(userMessage = GalleryUploadRun.TOO_LARGE_MESSAGE))
            PrepareResult.Unreadable -> unreadable()
        }
    }

    override suspend fun clear() = queue.clear()

    private fun unreadable(): Result<File> =
        Result.failure(AppError.Unknown(userMessage = GalleryUploadRun.UNREADABLE_MESSAGE))

    companion object {
        fun posterFileName(uploadId: String) = "$uploadId-poster.jpg"
    }
}
