package com.ipb.castelobranco.features.gallery.data.upload

import com.ipb.castelobranco.core.di.IoDispatcher
import com.ipb.castelobranco.core.domain.error.AppError
import com.ipb.castelobranco.features.gallery.data.local.GalleryMediaStore
import com.ipb.castelobranco.features.gallery.domain.upload.GalleryUploadRepository
import com.ipb.castelobranco.features.gallery.domain.upload.GalleryUploadScheduler
import com.ipb.castelobranco.features.gallery.domain.upload.PickedMedia
import com.ipb.castelobranco.features.gallery.domain.upload.TrimmableVideo
import com.ipb.castelobranco.features.gallery.domain.upload.UploadEntry
import com.ipb.castelobranco.features.gallery.domain.upload.UploadItem
import com.ipb.castelobranco.features.gallery.domain.upload.UploadKind
import com.ipb.castelobranco.features.gallery.domain.upload.UploadState
import com.ipb.castelobranco.features.gallery.domain.upload.VideoProbeResult
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
    private val videoReader: PickedVideoReader,
    private val mediaStore: GalleryMediaStore,
    private val scheduler: GalleryUploadScheduler,
    @param:IoDispatcher private val ioDispatcher: CoroutineDispatcher,
) : GalleryUploadRepository {

    override val items: StateFlow<List<UploadItem>> = queue.items

    override suspend fun inspect(sources: List<String>): List<PickedMedia> = sources.map { source ->
        val media = copier.inspect(source)
        if (media.isVideo) media.copy(trimmable = trimmable(media)) else media
    }

    private suspend fun trimmable(media: PickedMedia): TrimmableVideo? {
        val info = withContext(ioDispatcher) { videoReader.read(media.source) } ?: return null
        val durationMs = info.durationMs
        val videoMime = info.videoMime
        if (durationMs == null || videoMime == null || !VideoUploadCheck.canTrim(media.mimeType, info)) return null
        return TrimmableVideo(copier.videoDisplayName(media.source), durationMs, info.dateTaken, videoMime)
    }

    /** Each entry is queued as soon as it is ready, so the album shows the count growing. */
    override suspend fun enqueue(albumId: Long, entries: List<UploadEntry>, videosWifiOnly: Boolean) {
        queue.load()
        var wifiOnly = false
        entries.forEach { entry ->
            val item = when (entry) {
                is UploadEntry.Picked -> pickedItem(albumId, entry.source, videosWifiOnly)
                is UploadEntry.Cut -> cutItem(albumId, entry, videosWifiOnly)
            }
            if (item.wifiOnly && item.state != UploadState.Failed) wifiOnly = true
            queue.append(item)
        }
        scheduler.enqueue()
        if (wifiOnly) scheduler.enqueueWifiOnly()
    }

    private suspend fun pickedItem(albumId: Long, source: String, wifiOnly: Boolean): UploadItem {
        val copy = copier.copy(source)
        return when {
            copy == null -> UploadItem(
                uploadId = UUID.randomUUID().toString(),
                albumId = albumId,
                displayName = source.substringAfterLast('/'),
                fileName = "",
                state = UploadState.Failed,
                failure = GalleryUploadRun.UNREADABLE_MESSAGE,
                enqueuedAt = System.currentTimeMillis(),
            )
            copy.isVideo -> checkedVideo(albumId, copy, wifiOnly)
            else -> UploadItem(
                uploadId = copy.uploadId,
                albumId = albumId,
                displayName = copy.displayName,
                fileName = copy.fileName,
                state = UploadState.Waiting,
                enqueuedAt = System.currentTimeMillis(),
            )
        }
    }

    /** Already in app storage and needs no URI grant. The date is the original's, never the output's. */
    private suspend fun cutItem(albumId: Long, entry: UploadEntry.Cut, wifiOnly: Boolean): UploadItem {
        val copy = PickedImageCopier.Copy(UUID.randomUUID().toString(), entry.fileName, entry.displayName)
        return checkedVideo(albumId, copy, wifiOnly) { entry.dateTaken }
    }

    /**
     * A video is checked now, while the person is still looking: refused ones are listed with the
     * reason, and the others are ready to send as they are (nothing to prepare but the poster).
     */
    private suspend fun checkedVideo(
        albumId: Long,
        copy: PickedImageCopier.Copy,
        wifiOnly: Boolean,
        dateOf: (VideoProbeResult) -> String? = { it.dateTaken },
    ): UploadItem {
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
            dateTaken = dateOf(result),
            wifiOnly = wifiOnly,
        )
    }

    override suspend fun dismiss(uploadId: String) = queue.dismiss(uploadId)

    override suspend fun sweepOrphanCuts() {
        queue.load()
        val queued = queue.items.value.map { it.fileName }.toSet()
        withContext(ioDispatcher) {
            mediaStore.cutFiles().filterNot { it.name in queued }.forEach(mediaStore::delete)
        }
    }

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
