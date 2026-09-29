package com.ipb.castelobranco.features.gallery.domain.model

/** One answer of the change feed, without the `full_sync_required` flag. */
data class GalleryDelta(
    val albums: List<GalleryAlbum>,
    val photos: List<GalleryPhoto>,
    val deletedAlbumIds: Set<Long>,
    val deletedPhotoIds: Set<Long>,
    val cursor: String,
)

/**
 * The device's copy of the gallery: every live album and photo, and the feed cursor that describes
 * them. Saved as one unit, so the cursor can never be ahead of the data.
 */
data class GalleryIndex(
    val albums: Map<Long, GalleryAlbum>,
    val photos: Map<Long, GalleryPhoto>,
    val cursor: String?,
) {

    /**
     * Upserts by id and removes the deleted ids. Idempotent — the feed may repeat items changed
     * shortly before the cursor. If an id is both changed and deleted in one answer, delete wins.
     */
    fun applyDelta(delta: GalleryDelta): GalleryIndex = GalleryIndex(
        albums = (albums + delta.albums.associateBy { it.id }) - delta.deletedAlbumIds,
        photos = (photos + delta.photos.associateBy { it.id }) - delta.deletedPhotoIds,
        cursor = delta.cursor,
    )

    companion object {
        /** A full read (feed without cursor) replaces the index: every id missing from it is dropped. */
        fun fromFullRead(full: GalleryDelta): GalleryIndex =
            GalleryIndex(albums = emptyMap(), photos = emptyMap(), cursor = null).applyDelta(full)
    }
}
