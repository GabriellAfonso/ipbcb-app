package com.ipb.castelobranco.features.gallery.data.manage

import com.ipb.castelobranco.core.di.IoDispatcher
import com.ipb.castelobranco.core.domain.error.AppError
import com.ipb.castelobranco.core.domain.error.toAppError
import com.ipb.castelobranco.core.network.error.toAppError
import com.ipb.castelobranco.features.gallery.data.api.GalleryApi
import com.ipb.castelobranco.features.gallery.data.api.GalleryEndpoints
import com.ipb.castelobranco.features.gallery.data.dto.albumOrderBody
import com.ipb.castelobranco.features.gallery.data.dto.photoOrderBody
import com.ipb.castelobranco.features.gallery.data.dto.toCreateBody
import com.ipb.castelobranco.features.gallery.data.dto.toDomain
import com.ipb.castelobranco.features.gallery.data.dto.toPatchBody
import com.ipb.castelobranco.features.gallery.data.local.GalleryMediaStore
import com.ipb.castelobranco.features.gallery.domain.manage.AlbumDraft
import com.ipb.castelobranco.features.gallery.domain.manage.AlbumEdit
import com.ipb.castelobranco.features.gallery.domain.manage.CoverSource
import com.ipb.castelobranco.features.gallery.domain.manage.GalleryManageRepository
import com.ipb.castelobranco.features.gallery.domain.manage.PhotoEdit
import com.ipb.castelobranco.features.gallery.domain.manage.isCycle
import com.ipb.castelobranco.features.gallery.domain.manage.isNotFound
import com.ipb.castelobranco.features.gallery.domain.manage.isOrderMismatch
import com.ipb.castelobranco.features.gallery.domain.model.GalleryAlbum
import com.ipb.castelobranco.features.gallery.domain.model.GalleryLocalChange
import com.ipb.castelobranco.features.gallery.domain.model.GalleryPhoto
import com.ipb.castelobranco.features.gallery.domain.repository.GalleryRepository
import com.ipb.castelobranco.features.gallery.domain.trash.TrashEntry
import com.ipb.castelobranco.features.gallery.domain.trash.TrashKey
import com.ipb.castelobranco.features.gallery.domain.trash.TrashKind
import com.ipb.castelobranco.features.gallery.domain.usecase.SyncGalleryUseCase
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.MultipartBody
import okhttp3.RequestBody.Companion.asRequestBody
import retrofit2.Response
import timber.log.Timber
import java.io.File
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class GalleryManageRepositoryImpl @Inject constructor(
    private val api: GalleryApi,
    private val repository: GalleryRepository,
    private val syncGallery: SyncGalleryUseCase,
    private val mediaStore: GalleryMediaStore,
    @param:IoDispatcher private val ioDispatcher: CoroutineDispatcher,
) : GalleryManageRepository {

    override suspend fun createAlbum(draft: AlbumDraft): Result<GalleryAlbum> =
        write({ api.createAlbum(draft.toCreateBody()) }) { dto ->
            dto.toDomain().let { it to GalleryLocalChange.UpsertAlbum(it) }
        }

    override suspend fun editAlbum(edit: AlbumEdit): Result<GalleryAlbum> =
        write({ api.patchAlbum(edit.albumId, edit.toPatchBody()) }) { dto ->
            dto.toDomain().let { it to GalleryLocalChange.UpsertAlbum(it) }
        }

    override suspend fun reorderAlbums(parentId: Long?, ids: List<Long>): Result<Unit> =
        write({ api.orderAlbums(albumOrderBody(parentId, ids)) }) {
            Unit to GalleryLocalChange.ReorderAlbums(parentId, ids)
        }

    override suspend fun reorderPhotos(albumId: Long, ids: List<Long>): Result<Unit> =
        write({ api.orderPhotos(albumId, photoOrderBody(ids)) }) {
            Unit to GalleryLocalChange.ReorderPhotos(albumId, ids)
        }

    override suspend fun setCover(albumId: Long, image: File, deleteAfter: Boolean): Result<GalleryAlbum> =
        try {
            write({ api.putCover(albumId, imagePart(image)) }) { dto ->
                dto.toDomain().let { it to GalleryLocalChange.UpsertAlbum(it) }
            }
        } finally {
            if (deleteAfter) withContext(ioDispatcher) { image.delete() }
        }

    // No album comes back: the resolved cover (from a sub-album, or none) arrives with the sync.
    override suspend fun removeCover(albumId: Long): Result<Unit> =
        write({ api.deleteCover(albumId) }) { Unit to null }

    override suspend fun deleteAlbum(albumId: Long): Result<Unit> =
        delete({ api.deleteAlbum(albumId) }, GalleryLocalChange.RemoveAlbumTree(albumId), syncAfter = true)

    override suspend fun editPhoto(edit: PhotoEdit, syncAfter: Boolean): Result<GalleryPhoto> =
        write({ api.patchPhoto(edit.photoId, edit.toPatchBody()) }, syncAfter) { dto ->
            dto.toDomain().let { it to GalleryLocalChange.UpsertPhoto(it) }
        }

    override suspend fun deletePhoto(photoId: Long, syncAfter: Boolean): Result<Unit> =
        delete({ api.deletePhoto(photoId) }, GalleryLocalChange.RemovePhotos(setOf(photoId)), syncAfter)

    override suspend fun originalForCover(photoId: Long): Result<CoverSource> {
        val local = repository.localState.value
        local.originals[photoId]?.let { return Result.success(CoverSource(it, isTemp = false)) }
        val photo = local.index?.photos?.get(photoId)
            ?: return Result.failure(AppError.Server(code = HTTP_NOT_FOUND))
        return try {
            val response = api.downloadFile(photo.imageUrl)
            val body = response.body()
            if (!response.isSuccessful || body == null) return Result.failure(response.toAppError())
            val file = withContext(ioDispatcher) {
                mediaStore.newTempFile(".${photo.fileExtension()}").also { temp ->
                    body.byteStream().use { input -> temp.outputStream().use { input.copyTo(it) } }
                }
            }
            Result.success(CoverSource(file, isTemp = true))
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            Result.failure(e.toWriteError())
        }
    }

    override suspend fun trash(): Result<List<TrashEntry>> =
        request { api.getTrash() }.map { entries -> entries.mapNotNull { it.toDomain() } }

    override suspend fun restore(key: TrashKey): Result<Unit> = when (key.kind) {
        TrashKind.ALBUM -> write({ api.restoreAlbum(key.id) }) { dto ->
            Unit to GalleryLocalChange.UpsertAlbum(dto.toDomain())
        }
        TrashKind.PHOTO -> write({ api.restorePhoto(key.id) }) { dto ->
            Unit to GalleryLocalChange.UpsertPhoto(dto.toDomain())
        }
    }

    override suspend fun syncAfterWrite() {
        syncGallery.afterWrite()
    }

    /**
     * Runs [call]; on success applies the change [onSuccess] derives from the body (if any) and, with
     * [syncAfter], asks for a sync. A refusal leaves the local copy alone.
     */
    private suspend fun <B, T> write(
        call: suspend () -> Response<B>,
        syncAfter: Boolean = true,
        onSuccess: (B) -> Pair<T, GalleryLocalChange?>,
    ): Result<T> {
        val outcome: Result<T> = request(call).mapCatching { body ->
            val (value, change) = onSuccess(body)
            change?.let { repository.applyLocal(it) }
            value
        }
        afterRequest(outcome, syncAfter)
        return outcome
    }

    /** A delete: a 404 means someone already deleted it, so it is applied and reported as done. */
    private suspend fun delete(
        call: suspend () -> Response<Unit>,
        removal: GalleryLocalChange,
        syncAfter: Boolean,
    ): Result<Unit> {
        val outcome = request(call)
            .recoverCatching { error -> if ((error as? AppError)?.isNotFound() == true) Unit else throw error }
            .map { repository.applyLocal(removal); Unit }
        afterRequest(outcome, syncAfter)
        return outcome
    }

    /** The body of a 2xx (`Unit` for a 204), or the call's failure as an [AppError]. */
    private suspend fun <B> request(call: suspend () -> Response<B>): Result<B> = try {
        val response = call()
        val body = response.body()
        when {
            !response.isSuccessful -> Result.failure(response.toAppError())
            body == null -> Result.failure(AppError.Unknown(message = EMPTY_BODY_MESSAGE))
            else -> Result.success(body)
        }
    } catch (e: CancellationException) {
        throw e
    } catch (e: Exception) {
        Result.failure(e.toWriteError())
    }

    /** Syncs after a success, and after a refusal that means the tree changed under the user. */
    private suspend fun afterRequest(outcome: Result<*>, syncAfter: Boolean) {
        val error = outcome.exceptionOrNull() as? AppError
        if (error != null) Timber.w("Gallery write refused: %s", error.javaClass.simpleName)
        val treeChanged = error != null && (error.isCycle() || error.isNotFound() || error.isOrderMismatch())
        if ((outcome.isSuccess && syncAfter) || treeChanged) syncGallery.afterWrite()
    }

    private fun Throwable.toWriteError(): AppError = when (val error = toAppError()) {
        is AppError.Network -> AppError.Network(message = error.message, cause = error, userMessage = OFFLINE_MESSAGE)
        else -> error
    }

    private fun imagePart(file: File): MultipartBody.Part = MultipartBody.Part.createFormData(
        GalleryEndpoints.PART_IMAGE,
        file.name,
        file.asRequestBody(mimeOf(file).toMediaType()),
    )

    companion object {
        const val OFFLINE_MESSAGE = "Sem conexão"
        private const val HTTP_NOT_FOUND = 404
        private const val EMPTY_BODY_MESSAGE = "Resposta vazia de uma escrita da galeria"

        fun mimeOf(file: File): String = when (file.extension.lowercase()) {
            "png" -> "image/png"
            "webp" -> "image/webp"
            "gif" -> "image/gif"
            else -> "image/jpeg"
        }
    }
}
