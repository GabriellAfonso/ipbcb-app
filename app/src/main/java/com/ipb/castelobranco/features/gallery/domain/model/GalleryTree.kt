package com.ipb.castelobranco.features.gallery.domain.model

/**
 * The album tree derived from a [GalleryIndex]. Every list is ordered by `position`, then `id`.
 * An album whose parent is not in the index, and a photo whose album is not, are unreachable and
 * never returned — the next full sync resolves them.
 */
class GalleryTree(private val index: GalleryIndex) {

    private val childrenByParent: Map<Long?, List<GalleryAlbum>> = index.albums.values
        .groupBy { it.parentId }
        .mapValues { (_, list) -> list.sortedWith(ALBUM_ORDER) }

    private val photosByAlbum: Map<Long, List<GalleryPhoto>> = index.photos.values
        .groupBy { it.albumId }
        .mapValues { (_, list) -> list.sortedWith(PHOTO_ORDER) }

    fun album(albumId: Long): GalleryAlbum? = index.albums[albumId]?.takeIf { isReachable(it) }

    fun roots(): List<GalleryAlbum> = childrenByParent[null].orEmpty()

    fun children(albumId: Long): List<GalleryAlbum> =
        if (album(albumId) == null) emptyList() else childrenByParent[albumId].orEmpty()

    fun photosOf(albumId: Long): List<GalleryPhoto> =
        if (album(albumId) == null) emptyList() else photosByAlbum[albumId].orEmpty()

    fun parentOf(albumId: Long): GalleryAlbum? = album(albumId)?.parentId?.let { index.albums[it] }

    /** Pre-order walk of the reachable tree: an album's photos, then each sub-album in order. */
    fun allPhotosInTreeOrder(): List<GalleryPhoto> {
        val result = mutableListOf<GalleryPhoto>()
        val visited = mutableSetOf<Long>()
        fun walk(album: GalleryAlbum) {
            if (!visited.add(album.id)) return
            result += photosByAlbum[album.id].orEmpty()
            childrenByParent[album.id].orEmpty().forEach(::walk)
        }
        roots().forEach(::walk)
        return result
    }

    private fun isReachable(album: GalleryAlbum): Boolean {
        var current: GalleryAlbum = album
        val seen = mutableSetOf(current.id)
        while (true) {
            val parentId = current.parentId ?: return true
            current = index.albums[parentId] ?: return false
            if (!seen.add(current.id)) return false
        }
    }

    private companion object {
        val ALBUM_ORDER = compareBy<GalleryAlbum>({ it.position }, { it.id })
        val PHOTO_ORDER = compareBy<GalleryPhoto>({ it.position }, { it.id })
    }
}
