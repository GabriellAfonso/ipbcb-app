package com.ipb.castelobranco.features.gallery.domain.model

import com.ipb.castelobranco.features.gallery.data.galleryAlbum
import com.ipb.castelobranco.features.gallery.data.galleryPhoto
import org.junit.Assert.assertEquals
import org.junit.Test

class GalleryIndexApplyTest {

    /**
     * 1 ─┬─ 2 ── 3
     *    └─ 4
     * 5
     * Photos: 10 in 1, 11 in 2, 12 in 3, 13 in 4, 14 in 5.
     */
    private val index = GalleryIndex(
        albums = listOf(
            galleryAlbum(1),
            galleryAlbum(2, parentId = 1),
            galleryAlbum(3, parentId = 2),
            galleryAlbum(4, parentId = 1, position = 1),
            galleryAlbum(5, position = 1),
        ).associateBy { it.id },
        photos = listOf(
            galleryPhoto(10, albumId = 1),
            galleryPhoto(11, albumId = 2),
            galleryPhoto(12, albumId = 3),
            galleryPhoto(13, albumId = 4),
            galleryPhoto(14, albumId = 5),
        ).associateBy { it.id },
        cursor = "c7",
    )

    private val allChanges = listOf(
        GalleryLocalChange.UpsertAlbum(galleryAlbum(6, parentId = 5, name = "Novo")),
        GalleryLocalChange.UpsertPhoto(galleryPhoto(15, albumId = 5)),
        GalleryLocalChange.RemoveAlbumTree(2),
        GalleryLocalChange.RemovePhotos(setOf(14)),
        GalleryLocalChange.ReorderAlbums(parentId = 1, ids = listOf(4, 2)),
        GalleryLocalChange.ReorderPhotos(albumId = 1, ids = listOf(10)),
    )

    @Test
    fun `every change keeps the cursor`() {
        allChanges.forEach { change ->
            assertEquals(change.toString(), "c7", index.apply(change).cursor)
        }
    }

    @Test
    fun `applying a change twice equals applying it once`() {
        allChanges.forEach { change ->
            assertEquals(change.toString(), index.apply(change), index.apply(change).apply(change))
        }
    }

    @Test
    fun `upserts replace by id`() {
        val renamed = galleryAlbum(2, parentId = 1, name = "Sábado")

        val result = index.apply(GalleryLocalChange.UpsertAlbum(renamed))

        assertEquals("Sábado", result.albums.getValue(2).name)
        assertEquals(index.albums.size, result.albums.size)
    }

    @Test
    fun `removing an album tree drops every descendant and their photos, and nothing else`() {
        val result = index.apply(GalleryLocalChange.RemoveAlbumTree(1))

        assertEquals(setOf(5L), result.albums.keys)
        assertEquals(setOf(14L), result.photos.keys)
    }

    @Test
    fun `removing a sub-album keeps its siblings and parent`() {
        val result = index.apply(GalleryLocalChange.RemoveAlbumTree(2))

        assertEquals(setOf(1L, 4L, 5L), result.albums.keys)
        assertEquals(setOf(10L, 13L, 14L), result.photos.keys)
    }

    @Test
    fun `unknown ids leave the index equal`() {
        assertEquals(index, index.apply(GalleryLocalChange.RemoveAlbumTree(99)))
        assertEquals(index, index.apply(GalleryLocalChange.RemovePhotos(setOf(99))))
        assertEquals(index, index.apply(GalleryLocalChange.ReorderAlbums(null, listOf(99))))
    }

    @Test
    fun `reorder sets position to the index in the list and leaves unlisted items alone`() {
        val result = index.apply(GalleryLocalChange.ReorderAlbums(parentId = 1, ids = listOf(4, 2)))

        assertEquals(0, result.albums.getValue(4).position)
        assertEquals(1, result.albums.getValue(2).position)
        assertEquals(index.albums.getValue(5), result.albums.getValue(5))
    }

    @Test
    fun `photo reorder sets positions`() {
        val withTwo = index.apply(GalleryLocalChange.UpsertPhoto(galleryPhoto(16, albumId = 1, position = 1)))

        val result = withTwo.apply(GalleryLocalChange.ReorderPhotos(albumId = 1, ids = listOf(16, 10)))

        assertEquals(0, result.photos.getValue(16).position)
        assertEquals(1, result.photos.getValue(10).position)
    }

    @Test
    fun `a later feed repeating an upserted item gives the same index`() {
        val photo = galleryPhoto(15, albumId = 5)
        val local = index.apply(GalleryLocalChange.UpsertPhoto(photo))

        val fed = local.applyDelta(
            GalleryDelta(
                albums = emptyList(),
                photos = listOf(photo),
                deletedAlbumIds = emptySet(),
                deletedPhotoIds = emptySet(),
                cursor = "c7",
            )
        )

        assertEquals(local, fed)
    }
}
