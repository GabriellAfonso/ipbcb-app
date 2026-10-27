package com.ipb.castelobranco.features.gallery.domain.model

/**
 * What a member sees: a video only once its file is whole on the device, and an album only when it
 * holds something visible — a visible item or a visible sub-album — or nothing at all yet (an album
 * just created, waiting for uploads). "Organizar" and the move picker use the full index instead.
 */
object GalleryVisibility {

    /** [index] without undownloaded videos and without the albums left with only those below them. */
    fun visibleIndex(index: GalleryIndex, downloadedVideos: Set<Long>): GalleryIndex {
        val hiddenItems = index.photos.values.filter { it.isVideo && it.id !in downloadedVideos }.map { it.id }
        if (hiddenItems.isEmpty()) return index
        val photos = index.photos - hiddenItems.toSet()
        val hiddenAlbums = hiddenAlbums(index, photos)
        return index.copy(
            albums = index.albums - hiddenAlbums,
            photos = photos.filterValues { it.albumId !in hiddenAlbums },
        )
    }

    private fun hiddenAlbums(index: GalleryIndex, visiblePhotos: Map<Long, GalleryPhoto>): Set<Long> {
        val children = index.albums.values.groupBy({ it.parentId }, { it.id })
        val allItems = index.photos.values.groupingBy { it.albumId }.eachCount()
        val visibleItems = visiblePhotos.values.groupingBy { it.albumId }.eachCount()
        val hidden = mutableSetOf<Long>()

        /** Returns (has any item below, has anything visible below), marking hidden albums on the way. */
        fun visit(albumId: Long, path: Set<Long>): Pair<Boolean, Boolean> {
            var anyItem = (allItems[albumId] ?: 0) > 0
            var anyVisible = (visibleItems[albumId] ?: 0) > 0
            for (child in children[albumId].orEmpty()) {
                if (child in path) continue
                val (childItems, childVisible) = visit(child, path + child)
                anyItem = anyItem || childItems
                // An empty sub-album is visible, and so makes its parent visible.
                anyVisible = anyVisible || childVisible
            }
            val visible = anyVisible || !anyItem
            if (!visible) hidden += albumId
            return anyItem to visible
        }

        index.albums.values.filter { it.parentId == null || it.parentId !in index.albums }
            .forEach { visit(it.id, setOf(it.id)) }
        return hidden
    }
}
