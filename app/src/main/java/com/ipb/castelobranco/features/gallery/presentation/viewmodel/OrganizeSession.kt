package com.ipb.castelobranco.features.gallery.presentation.viewmodel

import com.ipb.castelobranco.features.gallery.domain.manage.OrderDraft
import com.ipb.castelobranco.features.gallery.domain.model.GalleryTree
import com.ipb.castelobranco.features.gallery.presentation.components.GalleryGridKeys

/**
 * "Organizar" open on one screen: the order being dragged, per group, and the order when the mode
 * opened. [albumId] `null` = the root (albums only). Items never cross from one group to the other.
 */
internal data class OrganizeSession(
    val albumId: Long?,
    val albumIds: List<Long>,
    val photoIds: List<Long>,
    val originalAlbumIds: List<Long>,
    val originalPhotoIds: List<Long>,
    val isSaving: Boolean = false,
    /**
     * Refused as mismatched: the list was rebuilt from the tree we had, and is rebuilt again when
     * the sync brings the server's — unless the user already started dragging.
     */
    val awaitingRefresh: Boolean = false,
) {
    val isUntouched: Boolean get() = albumIds == originalAlbumIds && photoIds == originalPhotoIds

    /** Moves the item with key [from] to where [to] is; ignored across groups or for other keys. */
    fun move(from: Any?, to: Any?): OrganizeSession {
        val fromAlbum = GalleryGridKeys.albumId(from)
        val toAlbum = GalleryGridKeys.albumId(to)
        if (fromAlbum != null && toAlbum != null) return copy(albumIds = albumIds.moved(fromAlbum, toAlbum))
        val fromPhoto = GalleryGridKeys.photoId(from)
        val toPhoto = GalleryGridKeys.photoId(to)
        if (fromPhoto != null && toPhoto != null) return copy(photoIds = photoIds.moved(fromPhoto, toPhoto))
        return this
    }

    fun toDraft() = OrderDraft(albumId, albumIds, photoIds, originalAlbumIds, originalPhotoIds)

    private fun List<Long>.moved(item: Long, target: Long): List<Long> {
        val from = indexOf(item)
        val to = indexOf(target)
        if (from < 0 || to < 0 || from == to) return this
        return toMutableList().apply { add(to, removeAt(from)) }
    }

    companion object {
        fun from(albumId: Long?, tree: GalleryTree): OrganizeSession {
            val albums = (if (albumId == null) tree.roots() else tree.children(albumId)).map { it.id }
            val photos = albumId?.let { id -> tree.photosOf(id).map { it.id } }.orEmpty()
            return OrganizeSession(albumId, albums, photos, albums, photos)
        }
    }
}
