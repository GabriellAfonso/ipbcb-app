package com.ipb.castelobranco.features.gallery.domain.manage

import com.ipb.castelobranco.features.gallery.domain.model.GalleryAlbum
import com.ipb.castelobranco.features.gallery.domain.model.GalleryMember
import com.ipb.castelobranco.features.gallery.domain.model.GalleryPhoto
import com.ipb.castelobranco.features.gallery.domain.trash.TrashEntry
import com.ipb.castelobranco.features.gallery.domain.trash.TrashKey
import java.io.File

/**
 * Every gallery write except the upload queue. A success is already applied to the local copy
 * (cursor untouched) and, unless `syncAfter = false`, has asked for a sync that picks up what the
 * server derived from it. A failure is an `AppError`; nothing changed locally, except a sync when the
 * tree changed under the user (cycle, not found, order mismatch).
 */
interface GalleryManageRepository {
    suspend fun createAlbum(draft: AlbumDraft): Result<GalleryAlbum>
    suspend fun editAlbum(edit: AlbumEdit): Result<GalleryAlbum>
    suspend fun reorderAlbums(parentId: Long?, ids: List<Long>): Result<Unit>
    suspend fun reorderPhotos(albumId: Long, ids: List<Long>): Result<Unit>

    /** [image] is sent as is and deleted afterwards when [deleteAfter]. */
    suspend fun setCover(albumId: Long, image: File, deleteAfter: Boolean): Result<GalleryAlbum>
    suspend fun removeCover(albumId: Long): Result<Unit>

    /** A 404 (already gone) counts as done. */
    suspend fun deleteAlbum(albumId: Long): Result<Unit>
    suspend fun editPhoto(edit: PhotoEdit, syncAfter: Boolean = true): Result<GalleryPhoto>

    /** A 404 (already gone) counts as done. */
    suspend fun deletePhoto(photoId: Long, syncAfter: Boolean = true): Result<Unit>

    /** The photo's original on the device, downloaded to a temp file when missing. */
    suspend fun originalForCover(photoId: Long): Result<CoverSource>

    /** The server's trash, in its order (`owner` only). Never cached. */
    suspend fun trash(): Result<List<TrashEntry>>

    /**
     * Restores a trash entry. The album or photo that comes back is applied to the local copy; the sync
     * that follows brings what went with an album.
     */
    suspend fun restore(key: TrashKey): Result<Unit>

    /** Every member record that can be tagged (`manage` only), by name. Read on demand, never cached. */
    suspend fun taggableMembers(): Result<List<GalleryMember>>

    /** Replaces the photo's people with exactly [memberIds]; the answer is applied, then a sync runs. */
    suspend fun setPhotoMembers(photoId: Long, memberIds: List<Long>): Result<GalleryPhoto>

    /**
     * Adds and removes people in up to 200 photos, keeping every other tag. Atomic on the server. The
     * photos that come back are applied; no sync (the caller syncs once), except after a not-found.
     */
    suspend fun changePhotoMembers(
        photoIds: List<Long>,
        addMemberIds: List<Long>,
        removeMemberIds: List<Long>,
    ): Result<List<GalleryPhoto>>

    /** One sync for a batch that passed `syncAfter = false`. */
    suspend fun syncAfterWrite()
}

/** A file to send as a cover; [isTemp] files are deleted once sent. */
data class CoverSource(val file: File, val isTemp: Boolean)
