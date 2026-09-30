package com.ipb.castelobranco.features.gallery.domain.manage

import com.ipb.castelobranco.features.gallery.data.galleryAlbum
import com.ipb.castelobranco.features.gallery.data.galleryPhoto
import com.ipb.castelobranco.features.gallery.domain.model.GalleryAlbum
import com.ipb.castelobranco.features.gallery.domain.model.GalleryPhoto
import java.io.File

/** Records every call; answers from [failures] (id → error) or success. */
class FakeGalleryManageRepository : GalleryManageRepository {

    val calls = mutableListOf<String>()
    var syncs = 0
        private set

    /** Photo or album id → the error its next write fails with. */
    val failures = mutableMapOf<Long, Throwable>()

    /** Order calls fail with this, when set. */
    var orderFailure: Throwable? = null

    /** Runs after every successful write, with its call name — to change the local copy like the real one. */
    var onSuccess: suspend (String) -> Unit = {}

    private suspend fun <T> answer(id: Long, call: String, value: () -> T): Result<T> {
        calls += call
        failures[id]?.let { return Result.failure(it) }
        return Result.success(value()).also { onSuccess(call) }
    }

    override suspend fun createAlbum(draft: AlbumDraft): Result<GalleryAlbum> =
        answer(0, "create:${draft.name}") { galleryAlbum(99, draft.parentId, name = draft.name) }

    override suspend fun editAlbum(edit: AlbumEdit): Result<GalleryAlbum> =
        answer(edit.albumId, "editAlbum:${edit.albumId}") { galleryAlbum(edit.albumId) }

    override suspend fun reorderAlbums(parentId: Long?, ids: List<Long>): Result<Unit> {
        calls += "orderAlbums:$parentId:$ids"
        return orderFailure?.let { Result.failure(it) } ?: Result.success(Unit)
    }

    override suspend fun reorderPhotos(albumId: Long, ids: List<Long>): Result<Unit> {
        calls += "orderPhotos:$albumId:$ids"
        return orderFailure?.let { Result.failure(it) } ?: Result.success(Unit)
    }

    override suspend fun setCover(albumId: Long, image: File, deleteAfter: Boolean): Result<GalleryAlbum> =
        answer(albumId, "setCover:$albumId") { galleryAlbum(albumId) }

    override suspend fun removeCover(albumId: Long): Result<Unit> = answer(albumId, "removeCover:$albumId") { }

    override suspend fun deleteAlbum(albumId: Long): Result<Unit> = answer(albumId, "deleteAlbum:$albumId") { }

    override suspend fun editPhoto(edit: PhotoEdit, syncAfter: Boolean): Result<GalleryPhoto> =
        answer(edit.photoId, "editPhoto:${edit.photoId}:$syncAfter") {
            galleryPhoto(edit.photoId, albumId = (edit.albumId as? Field.Set)?.value ?: 1L)
        }

    override suspend fun deletePhoto(photoId: Long, syncAfter: Boolean): Result<Unit> =
        answer(photoId, "deletePhoto:$photoId:$syncAfter") { }

    override suspend fun originalForCover(photoId: Long): Result<CoverSource> =
        answer(photoId, "original:$photoId") { CoverSource(File("$photoId.jpg"), isTemp = false) }

    override suspend fun syncAfterWrite() {
        syncs++
    }
}
