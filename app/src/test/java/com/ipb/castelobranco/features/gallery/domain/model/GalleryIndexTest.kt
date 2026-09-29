package com.ipb.castelobranco.features.gallery.domain.model

import com.ipb.castelobranco.features.gallery.data.galleryAlbum
import com.ipb.castelobranco.features.gallery.data.galleryPhoto
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class GalleryIndexTest {

    private fun delta(
        cursor: String = "c2",
        albums: List<GalleryAlbum> = emptyList(),
        photos: List<GalleryPhoto> = emptyList(),
        deletedAlbumIds: Set<Long> = emptySet(),
        deletedPhotoIds: Set<Long> = emptySet(),
    ) = GalleryDelta(albums, photos, deletedAlbumIds, deletedPhotoIds, cursor)

    private val base = GalleryIndex.fromFullRead(
        delta(
            cursor = "c1",
            albums = listOf(galleryAlbum(1), galleryAlbum(2)),
            photos = listOf(galleryPhoto(10, albumId = 1), galleryPhoto(11, albumId = 1)),
        )
    )

    @Test
    fun `full read keeps every item and the cursor`() {
        assertEquals(setOf(1L, 2L), base.albums.keys)
        assertEquals(setOf(10L, 11L), base.photos.keys)
        assertEquals("c1", base.cursor)
    }

    @Test
    fun `delta upserts new and changed items by id`() {
        val renamed = galleryAlbum(1, name = "Retiro")
        val result = base.applyDelta(
            delta(albums = listOf(renamed, galleryAlbum(3)), photos = listOf(galleryPhoto(12)))
        )

        assertEquals("Retiro", result.albums.getValue(1).name)
        assertEquals(setOf(1L, 2L, 3L), result.albums.keys)
        assertEquals(setOf(10L, 11L, 12L), result.photos.keys)
        assertEquals("c2", result.cursor)
    }

    @Test
    fun `delta removes deleted ids`() {
        val result = base.applyDelta(delta(deletedAlbumIds = setOf(2), deletedPhotoIds = setOf(11)))

        assertEquals(setOf(1L), result.albums.keys)
        assertEquals(setOf(10L), result.photos.keys)
    }

    @Test
    fun `applying the same delta twice equals applying it once`() {
        val d = delta(
            albums = listOf(galleryAlbum(3)),
            photos = listOf(galleryPhoto(12), galleryPhoto(10, position = 5)),
            deletedPhotoIds = setOf(11, 999),
        )

        assertEquals(base.applyDelta(d), base.applyDelta(d).applyDelta(d))
    }

    @Test
    fun `delete wins over an upsert of the same id`() {
        val result = base.applyDelta(delta(photos = listOf(galleryPhoto(11)), deletedPhotoIds = setOf(11)))

        assertFalse(11L in result.photos)
    }

    @Test
    fun `photo moved to another album keeps its entry with the new album`() {
        val result = base.applyDelta(delta(photos = listOf(galleryPhoto(10, albumId = 2))))

        assertEquals(2L, result.photos.getValue(10).albumId)
        assertEquals(2, result.photos.size)
    }

    @Test
    fun `a new full read drops every id missing from it`() {
        val result = GalleryIndex.fromFullRead(delta(cursor = "c9", albums = listOf(galleryAlbum(2))))

        assertEquals(setOf(2L), result.albums.keys)
        assertTrue(result.photos.isEmpty())
        assertEquals("c9", result.cursor)
    }
}
