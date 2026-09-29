package com.ipb.castelobranco.features.gallery.domain.model

import com.ipb.castelobranco.features.gallery.data.galleryAlbum
import com.ipb.castelobranco.features.gallery.data.galleryPhoto
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class GalleryTreeTest {

    private fun tree(albums: List<GalleryAlbum>, photos: List<GalleryPhoto> = emptyList()) = GalleryTree(
        GalleryIndex(albums.associateBy { it.id }, photos.associateBy { it.id }, cursor = "c")
    )

    @Test
    fun `roots are ordered by position then id`() {
        val t = tree(
            listOf(galleryAlbum(3, position = 1), galleryAlbum(1, position = 2), galleryAlbum(2, position = 1))
        )

        assertEquals(listOf(2L, 3L, 1L), t.roots().map { it.id })
    }

    @Test
    fun `children and photos of an album are ordered by position then id`() {
        val t = tree(
            albums = listOf(
                galleryAlbum(1),
                galleryAlbum(5, parentId = 1, position = 1),
                galleryAlbum(4, parentId = 1, position = 0),
            ),
            photos = listOf(
                galleryPhoto(12, albumId = 1, position = 0),
                galleryPhoto(10, albumId = 1, position = 1),
                galleryPhoto(11, albumId = 1, position = 0),
            ),
        )

        assertEquals(listOf(4L, 5L), t.children(1).map { it.id })
        assertEquals(listOf(11L, 12L, 10L), t.photosOf(1).map { it.id })
    }

    @Test
    fun `numeric names never decide the order`() {
        val t = tree(
            albums = listOf(galleryAlbum(1)),
            photos = listOf(
                galleryPhoto(2, position = 1, name = "2.jpg"),
                galleryPhoto(1, position = 0, name = "10.jpg"),
            ),
        )

        assertEquals(listOf("10.jpg", "2.jpg"), t.photosOf(1).map { it.name })
    }

    @Test
    fun `parent of a sub-album and none for a root`() {
        val t = tree(listOf(galleryAlbum(1), galleryAlbum(2, parentId = 1)))

        assertEquals(1L, t.parentOf(2)?.id)
        assertNull(t.parentOf(1))
    }

    @Test
    fun `an album whose parent is missing and its photos are unreachable`() {
        val t = tree(
            albums = listOf(galleryAlbum(1), galleryAlbum(7, parentId = 99)),
            photos = listOf(galleryPhoto(70, albumId = 7), galleryPhoto(80, albumId = 42)),
        )

        assertNull(t.album(7))
        assertTrue(t.photosOf(7).isEmpty())
        assertEquals(listOf(1L), t.roots().map { it.id })
        assertTrue(t.allPhotosInTreeOrder().isEmpty())
    }

    @Test
    fun `all photos in pre-order - album photos, then each sub-album in order`() {
        val t = tree(
            albums = listOf(
                galleryAlbum(1, position = 0),
                galleryAlbum(2, position = 1),
                galleryAlbum(3, parentId = 1, position = 0),
            ),
            photos = listOf(
                galleryPhoto(20, albumId = 2),
                galleryPhoto(30, albumId = 3),
                galleryPhoto(10, albumId = 1),
            ),
        )

        assertEquals(listOf(10L, 30L, 20L), t.allPhotosInTreeOrder().map { it.id })
    }
}
