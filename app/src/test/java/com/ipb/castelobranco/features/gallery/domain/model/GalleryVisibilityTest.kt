package com.ipb.castelobranco.features.gallery.domain.model

import com.ipb.castelobranco.features.gallery.data.galleryAlbum
import com.ipb.castelobranco.features.gallery.data.galleryPhoto
import org.junit.Assert.assertEquals
import org.junit.Assert.assertSame
import org.junit.Test

class GalleryVisibilityTest {

    private fun video(id: Long, albumId: Long) = galleryPhoto(id, albumId).copy(mediaType = MediaType.VIDEO)

    private fun index(albums: List<GalleryAlbum>, items: List<GalleryPhoto>) =
        GalleryIndex(albums.associateBy { it.id }, items.associateBy { it.id }, "c")

    @Test
    fun `no undownloaded video - the index is kept as is`() {
        val index = index(listOf(galleryAlbum(1)), listOf(galleryPhoto(10, 1)))

        assertSame(index, GalleryVisibility.visibleIndex(index, emptySet()))
    }

    @Test
    fun `an undownloaded video is hidden, a downloaded one stays`() {
        val index = index(listOf(galleryAlbum(1)), listOf(galleryPhoto(10, 1), video(11, 1), video(12, 1)))

        val visible = GalleryVisibility.visibleIndex(index, downloadedVideos = setOf(12))

        assertEquals(setOf(10L, 12L), visible.photos.keys)
        assertEquals(setOf(1L), visible.albums.keys)
    }

    @Test
    fun `an album holding only undownloaded videos is hidden`() {
        val index = index(listOf(galleryAlbum(1), galleryAlbum(2)), listOf(galleryPhoto(10, 1), video(20, 2)))

        val visible = GalleryVisibility.visibleIndex(index, emptySet())

        assertEquals(setOf(1L), visible.albums.keys)
    }

    @Test
    fun `an empty album stays visible`() {
        val index = index(listOf(galleryAlbum(1), galleryAlbum(2)), listOf(video(10, 1)))

        val visible = GalleryVisibility.visibleIndex(index, emptySet())

        assertEquals(setOf(2L), visible.albums.keys)
    }

    @Test
    fun `an album with hidden videos and a sub-album with photos stays, showing only the sub-album`() {
        val index = index(
            listOf(galleryAlbum(1), galleryAlbum(2, parentId = 1)),
            listOf(video(10, 1), galleryPhoto(20, 2)),
        )

        val visible = GalleryVisibility.visibleIndex(index, emptySet())

        assertEquals(setOf(1L, 2L), visible.albums.keys)
        assertEquals(setOf(20L), visible.photos.keys)
    }

    @Test
    fun `a tree of only undownloaded videos is hidden from the top, an empty sub-album keeps its parent`() {
        val index = index(
            listOf(
                galleryAlbum(1),
                galleryAlbum(2, parentId = 1),
                galleryAlbum(3),
                galleryAlbum(4, parentId = 3),
            ),
            listOf(video(10, 1), video(20, 2), video(30, 3)),
        )

        val visible = GalleryVisibility.visibleIndex(index, emptySet())

        // 1 and 2 hold only hidden videos; 3 keeps the empty 4 visible.
        assertEquals(setOf(3L, 4L), visible.albums.keys)
    }
}
