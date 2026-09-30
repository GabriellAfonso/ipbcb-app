package com.ipb.castelobranco.features.gallery.domain.manage

import com.ipb.castelobranco.features.gallery.data.galleryAlbum
import com.ipb.castelobranco.features.gallery.data.galleryPhoto
import com.ipb.castelobranco.features.gallery.domain.model.GalleryAlbum
import com.ipb.castelobranco.features.gallery.domain.model.GalleryMember
import com.ipb.castelobranco.features.gallery.domain.model.GalleryPhoto
import com.ipb.castelobranco.features.gallery.domain.trash.TrashEntry
import com.ipb.castelobranco.features.gallery.domain.trash.TrashKey
import kotlinx.coroutines.CompletableDeferred
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

    /** What [trash] answers next; each read is counted in [trashReads]. */
    var trashResult: Result<List<TrashEntry>> = Result.success(emptyList())
    var trashReads = 0
        private set

    /** Trash key → the error its next restore fails with. */
    val restoreFailures = mutableMapOf<TrashKey, Throwable>()

    /** Suspends every restore until completed, when set — to test a restore in flight. */
    var restoreGate: CompletableDeferred<Unit>? = null

    override suspend fun trash(): Result<List<TrashEntry>> {
        trashReads++
        return trashResult
    }

    override suspend fun restore(key: TrashKey): Result<Unit> {
        calls += "restore:${key.kind}:${key.id}"
        restoreGate?.await()
        restoreFailures[key]?.let { return Result.failure(it) }
        return Result.success(Unit).also { onSuccess("restore") }
    }

    /** What [taggableMembers] answers next; each read is counted in [taggableReads]. */
    var taggableResult: Result<List<GalleryMember>> = Result.success(emptyList())
    var taggableReads = 0
        private set

    /** The error the next tag writes fail with, one per call in order; empty = success. */
    val tagFailures = ArrayDeque<Throwable?>()

    /** Every `changePhotoMembers` call: photo ids, added and removed member ids. */
    val memberChanges = mutableListOf<Triple<List<Long>, List<Long>, List<Long>>>()

    override suspend fun taggableMembers(): Result<List<GalleryMember>> {
        taggableReads++
        return taggableResult
    }

    override suspend fun setPhotoMembers(photoId: Long, memberIds: List<Long>): Result<GalleryPhoto> {
        calls += "setMembers:$photoId:$memberIds"
        tagFailures.removeFirstOrNull()?.let { return Result.failure(it) }
        return Result.success(galleryPhoto(photoId)).also { onSuccess("setMembers") }
    }

    override suspend fun changePhotoMembers(
        photoIds: List<Long>,
        addMemberIds: List<Long>,
        removeMemberIds: List<Long>,
    ): Result<List<GalleryPhoto>> {
        calls += "changeMembers:${photoIds.size}:$addMemberIds:$removeMemberIds"
        memberChanges += Triple(photoIds, addMemberIds, removeMemberIds)
        tagFailures.removeFirstOrNull()?.let { return Result.failure(it) }
        return Result.success(photoIds.map { galleryPhoto(it) }).also { onSuccess("changeMembers") }
    }

    override suspend fun syncAfterWrite() {
        syncs++
    }
}
