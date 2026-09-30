package com.ipb.castelobranco.features.gallery.data.upload

import com.ipb.castelobranco.core.di.IoDispatcher
import com.ipb.castelobranco.core.domain.error.AppError
import com.ipb.castelobranco.features.gallery.domain.upload.GalleryUploadRepository
import com.ipb.castelobranco.features.gallery.domain.upload.GalleryUploadScheduler
import com.ipb.castelobranco.features.gallery.domain.upload.UploadItem
import com.ipb.castelobranco.features.gallery.domain.upload.UploadState
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
    private val scheduler: GalleryUploadScheduler,
    @param:IoDispatcher private val ioDispatcher: CoroutineDispatcher,
) : GalleryUploadRepository {

    override val items: StateFlow<List<UploadItem>> = queue.items

    /** Each copy is queued as soon as it lands, so the album shows the count growing. */
    override suspend fun enqueue(albumId: Long, sources: List<String>) {
        queue.load()
        sources.forEach { source ->
            val copy = copier.copy(source)
            queue.append(
                UploadItem(
                    uploadId = copy?.uploadId ?: UUID.randomUUID().toString(),
                    albumId = albumId,
                    displayName = copy?.displayName ?: source.substringAfterLast('/'),
                    fileName = copy?.fileName.orEmpty(),
                    state = if (copy != null) UploadState.Waiting else UploadState.Failed,
                    failure = if (copy != null) null else GalleryUploadRun.UNREADABLE_MESSAGE,
                    enqueuedAt = System.currentTimeMillis(),
                )
            )
        }
        scheduler.enqueue()
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
}
