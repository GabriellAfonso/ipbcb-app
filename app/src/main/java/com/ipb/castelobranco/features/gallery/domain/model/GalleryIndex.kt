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

    /**
     * Applies a write's result. The cursor is kept on purpose: the next feed still carries the write
     * and what the server derived from it. Idempotent, like [applyDelta].
     */
    fun apply(change: GalleryLocalChange): GalleryIndex = when (change) {
        is GalleryLocalChange.UpsertAlbum -> copy(albums = albums + (change.album.id to change.album))
        is GalleryLocalChange.UpsertPhoto -> copy(photos = photos + (change.photo.id to change.photo))
        is GalleryLocalChange.UpsertPhotos -> copy(photos = photos + change.photos.associateBy { it.id })
        is GalleryLocalChange.RemoveAlbumTree -> {
            val removed = subtreeIds(change.albumId)
            copy(albums = albums - removed, photos = photos.filterValues { it.albumId !in removed })
        }
        is GalleryLocalChange.RemovePhotos -> copy(photos = photos - change.ids)
        is GalleryLocalChange.ReorderAlbums -> copy(
            albums = albums + change.ids.withIndex().mapNotNull { (position, id) ->
                albums[id]?.let { id to it.copy(position = position) }
            }
        )
        is GalleryLocalChange.ReorderPhotos -> copy(
            photos = photos + change.ids.withIndex().mapNotNull { (position, id) ->
                photos[id]?.let { id to it.copy(position = position) }
            }
        )
    }

    /** [albumId] and every album below it; empty when [albumId] is not in the index. */
    private fun subtreeIds(albumId: Long): Set<Long> {
        if (albumId !in albums) return emptySet()
        val childrenByParent = albums.values.groupBy({ it.parentId }, { it.id })
        val result = mutableSetOf<Long>()
        val pending = ArrayDeque(listOf(albumId))
        while (pending.isNotEmpty()) {
            val id = pending.removeFirst()
            if (result.add(id)) pending += childrenByParent[id].orEmpty()
        }
        return result
    }

    companion object {
        /** A full read (feed without cursor) replaces the index: every id missing from it is dropped. */
        fun fromFullRead(full: GalleryDelta): GalleryIndex =
            GalleryIndex(albums = emptyMap(), photos = emptyMap(), cursor = null).applyDelta(full)
    }
}
