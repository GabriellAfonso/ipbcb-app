package com.ipb.castelobranco.features.gallery.domain.model

/**
 * The result of a write accepted by the server, applied to the device's copy before the next sync
 * brings it (and its derived changes) through the feed. Applying one never moves the cursor.
 */
sealed interface GalleryLocalChange {
    data class UpsertAlbum(val album: GalleryAlbum) : GalleryLocalChange
    data class UpsertPhoto(val photo: GalleryPhoto) : GalleryLocalChange

    /** The album, every album below it and every photo in any of them. */
    data class RemoveAlbumTree(val albumId: Long) : GalleryLocalChange
    data class RemovePhotos(val ids: Set<Long>) : GalleryLocalChange

    /** [ids] in their new order; `position` becomes the index in the list. */
    data class ReorderAlbums(val parentId: Long?, val ids: List<Long>) : GalleryLocalChange
    data class ReorderPhotos(val albumId: Long, val ids: List<Long>) : GalleryLocalChange
}
