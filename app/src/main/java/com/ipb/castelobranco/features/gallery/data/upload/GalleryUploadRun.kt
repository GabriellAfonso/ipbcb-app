package com.ipb.castelobranco.features.gallery.data.upload

import com.ipb.castelobranco.core.domain.auth.SessionPresenceProvider
import com.ipb.castelobranco.features.gallery.data.api.GalleryApi
import com.ipb.castelobranco.features.gallery.data.api.GalleryEndpoints
import com.ipb.castelobranco.features.gallery.data.local.GalleryMediaStore
import com.ipb.castelobranco.features.gallery.data.manage.GalleryManageRepositoryImpl
import com.ipb.castelobranco.features.gallery.domain.model.GalleryLocalChange
import com.ipb.castelobranco.features.gallery.domain.model.GalleryPhoto
import com.ipb.castelobranco.features.gallery.domain.repository.GalleryRepository
import com.ipb.castelobranco.features.gallery.domain.upload.UploadItem
import com.ipb.castelobranco.features.gallery.domain.upload.UploadOutcome
import com.ipb.castelobranco.features.gallery.domain.upload.UploadState
import com.ipb.castelobranco.features.gallery.domain.usecase.SyncGalleryUseCase
import kotlinx.coroutines.CancellationException
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.MultipartBody
import okhttp3.RequestBody.Companion.asRequestBody
import okhttp3.RequestBody.Companion.toRequestBody
import java.io.File
import javax.inject.Inject

/**
 * One pass over the upload queue, independent of WorkManager: prepares and sends the pending items
 * one at a time, oldest first, reading the queue again before each so items added meanwhile are
 * served in the same pass. The session is checked before every request; after sign-out it writes
 * nothing.
 */
class GalleryUploadRun @Inject constructor(
    private val queue: GalleryUploadQueueStore,
    private val preparer: GalleryImagePreparer,
    private val api: GalleryApi,
    private val classifier: UploadOutcomeClassifier,
    private val repository: GalleryRepository,
    private val syncGallery: SyncGalleryUseCase,
    private val mediaStore: GalleryMediaStore,
    private val session: SessionPresenceProvider,
) {

    sealed interface Result {
        /** Nothing left to send; [failed] items failed during this pass. */
        data class Done(val failed: Int) : Result

        /** A network failure or server error: run again later, same ids. */
        data object Retry : Result

        /** No session: stop quietly. */
        data object Stopped : Result
    }

    /** [onProgress] gets the 1-based position of the item being sent and the size of the batch. */
    suspend fun run(onProgress: suspend (current: Int, total: Int) -> Unit): Result {
        var sent = 0
        var failed = 0
        var result: Result? = null
        while (result == null) {
            if (!session.isLoggedIn()) {
                result = Result.Stopped
                break
            }
            val next = queue.nextPending() ?: break
            val done = sent + failed
            onProgress(done + 1, done + queue.pendingCount())

            val item = ready(next)
            if (item == null) {
                failed++
                continue
            }
            when (val outcome = send(item)) {
                is UploadOutcome.Sent -> {
                    adopt(item, outcome.photo)
                    sent++
                }
                is UploadOutcome.Failed -> {
                    queue.markFailed(item.uploadId, outcome.reason)
                    failed++
                }
                UploadOutcome.AlbumGone -> failed += queue.failAllOfAlbum(item.albumId, ALBUM_GONE_MESSAGE)
                is UploadOutcome.AccessLost -> failed += queue.failAllPending(outcome.reason)
                UploadOutcome.Stop -> result = Result.Stopped
                UploadOutcome.Retry -> result = Result.Retry
            }
        }
        // One sync for the whole pass: covers of albums that got their first photo, positions.
        if (sent > 0) syncGallery.afterWrite()
        return result ?: Result.Done(failed)
    }

    /** The item ready to send (prepared once, then kept prepared across retries); `null` = failed. */
    private suspend fun ready(item: UploadItem): UploadItem? {
        if (item.state == UploadState.Prepared) return item
        return when (val prepared = preparer.prepare(item)) {
            is PrepareResult.Ready -> {
                queue.markPrepared(item.uploadId, prepared.file.name, prepared.displayName)
                item.copy(
                    state = UploadState.Prepared,
                    fileName = prepared.file.name,
                    displayName = prepared.displayName,
                )
            }
            PrepareResult.Unreadable -> fail(item, UNREADABLE_MESSAGE)
            PrepareResult.TooLarge -> fail(item, TOO_LARGE_MESSAGE)
        }
    }

    private suspend fun fail(item: UploadItem, reason: String): UploadItem? {
        queue.markFailed(item.uploadId, reason)
        return null
    }

    private suspend fun send(item: UploadItem): UploadOutcome {
        val file = mediaStore.uploadFile(item.fileName)
        if (!file.exists()) return UploadOutcome.Failed(UNREADABLE_MESSAGE)
        return try {
            val response = api.uploadPhoto(
                albumId = item.albumId.toString().toRequestBody(TEXT_PLAIN),
                clientUploadId = item.uploadId.toRequestBody(TEXT_PLAIN),
                image = imagePart(file, item.displayName),
            )
            classifier.classify(response)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            classifier.classify(e)
        }
    }

    /**
     * The server stores the file as sent, so the prepared file becomes the photo's original on the
     * device — no download back. It is moved under the index lock, once the index lists the photo.
     */
    private suspend fun adopt(item: UploadItem, photo: GalleryPhoto) {
        val file = mediaStore.uploadFile(item.fileName)
        val applied = repository.applyLocal(GalleryLocalChange.UpsertPhoto(photo)) {
            if (file.exists()) mediaStore.adoptOriginal(photo.id, photo.fileExtension(), file)
        }
        if (!applied) file.delete()
        queue.remove(item.uploadId)
    }

    private fun imagePart(file: File, displayName: String): MultipartBody.Part =
        MultipartBody.Part.createFormData(
            GalleryEndpoints.PART_IMAGE,
            displayName,
            file.asRequestBody(GalleryManageRepositoryImpl.mimeOf(file).toMediaType()),
        )

    companion object {
        const val ALBUM_GONE_MESSAGE = "O álbum foi apagado"
        const val UNREADABLE_MESSAGE = "Não foi possível ler esta imagem."
        const val TOO_LARGE_MESSAGE = "Imagem grande demais para enviar."
        private val TEXT_PLAIN = "text/plain".toMediaType()
    }
}
