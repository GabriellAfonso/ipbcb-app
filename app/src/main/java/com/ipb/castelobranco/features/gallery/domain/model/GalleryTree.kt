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

    /**
     * Where [albumId] may be moved: "Raiz" first, then every reachable album in pre-order, except the
     * album itself and everything below it — a cycle can never be picked. The current parent is
     * listed but not selectable.
     */
    fun moveTargetsForAlbum(albumId: Long): List<TreeTarget> {
        val currentParent = album(albumId)?.parentId
        val root = TreeTarget(albumId = null, name = ROOT_NAME, depth = 0, selectable = currentParent != null)
        return listOf(root) + walk(skip = albumId).map { (album, depth) ->
            TreeTarget(album.id, album.name, depth + 1, selectable = album.id != currentParent)
        }
    }

    /** Where photos of [currentAlbumId] may be moved: every album, the current one not selectable. */
    fun moveTargetsForPhotos(currentAlbumId: Long): List<TreeTarget> =
        walk(skip = null).map { (album, depth) ->
            TreeTarget(album.id, album.name, depth, selectable = album.id != currentAlbumId)
        }

    /** Sub-albums and photos below [albumId], at every level; the album itself is not counted. */
    fun subtreeCounts(albumId: Long): SubtreeCounts {
        if (album(albumId) == null) return SubtreeCounts(0, 0)
        var subAlbums = 0
        var photos = photosByAlbum[albumId].orEmpty().size
        val pending = ArrayDeque(childrenByParent[albumId].orEmpty())
        val seen = mutableSetOf(albumId)
        while (pending.isNotEmpty()) {
            val child = pending.removeFirst()
            if (!seen.add(child.id)) continue
            subAlbums++
            photos += photosByAlbum[child.id].orEmpty().size
            pending += childrenByParent[child.id].orEmpty()
        }
        return SubtreeCounts(subAlbums, photos)
    }

    /** Pre-order walk with depth (roots at 0), leaving out the subtree of [skip]. */
    private fun walk(skip: Long?): List<Pair<GalleryAlbum, Int>> {
        val result = mutableListOf<Pair<GalleryAlbum, Int>>()
        val visited = mutableSetOf<Long>()
        fun visit(album: GalleryAlbum, depth: Int) {
            if (album.id == skip || !visited.add(album.id)) return
            result += album to depth
            childrenByParent[album.id].orEmpty().forEach { visit(it, depth + 1) }
        }
        roots().forEach { visit(it, 0) }
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
        const val ROOT_NAME = "Raiz"
        val ALBUM_ORDER = compareBy<GalleryAlbum>({ it.position }, { it.id })
        val PHOTO_ORDER = compareBy<GalleryPhoto>({ it.position }, { it.id })
    }
}

/** A destination in the move picker. [albumId] `null` = the root. */
data class TreeTarget(val albumId: Long?, val name: String, val depth: Int, val selectable: Boolean)

data class SubtreeCounts(val subAlbums: Int, val photos: Int)
