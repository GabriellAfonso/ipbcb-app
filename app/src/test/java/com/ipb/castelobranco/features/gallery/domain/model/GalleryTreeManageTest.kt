package com.ipb.castelobranco.features.gallery.domain.model

import com.ipb.castelobranco.features.gallery.data.galleryAlbum
import com.ipb.castelobranco.features.gallery.data.galleryPhoto
import org.junit.Assert.assertEquals
import org.junit.Test

class GalleryTreeManageTest {

    /**
     * A(1) ─┬─ B(2) ── C(3)
     *       └─ D(4)
     * E(5)
     * Photos: 10 in A, 11 and 12 in B, 13 in C, 14 in E.
     */
    private val tree = GalleryTree(
        GalleryIndex(
            albums = listOf(
                galleryAlbum(1, name = "A"),
                galleryAlbum(2, parentId = 1, name = "B"),
                galleryAlbum(3, parentId = 2, name = "C"),
                galleryAlbum(4, parentId = 1, position = 1, name = "D"),
                galleryAlbum(5, position = 1, name = "E"),
            ).associateBy { it.id },
            photos = listOf(
                galleryPhoto(10, albumId = 1),
                galleryPhoto(11, albumId = 2),
                galleryPhoto(12, albumId = 2),
                galleryPhoto(13, albumId = 3),
                galleryPhoto(14, albumId = 5),
            ).associateBy { it.id },
            cursor = "c",
        )
    )

    @Test
    fun `album targets - root first, pre-order with depth, self and descendants hidden`() {
        val targets = tree.moveTargetsForAlbum(2)

        assertEquals(
            listOf(
                TreeTarget(null, "Raiz", 0, selectable = true),
                TreeTarget(1, "A", 1, selectable = false),
                TreeTarget(4, "D", 2, selectable = true),
                TreeTarget(5, "E", 1, selectable = true),
            ),
            targets,
        )
    }

    @Test
    fun `a root album cannot be moved to the root again`() {
        val targets = tree.moveTargetsForAlbum(5)

        assertEquals(TreeTarget(null, "Raiz", 0, selectable = false), targets.first())
        assertEquals(listOf(1L, 2L, 3L, 4L), targets.drop(1).map { it.albumId })
    }

    @Test
    fun `photo targets - every album, no root, the current one disabled`() {
        val targets = tree.moveTargetsForPhotos(2)

        assertEquals(listOf(1L, 2L, 3L, 4L, 5L), targets.map { it.albumId })
        assertEquals(listOf(0, 1, 2, 1, 0), targets.map { it.depth })
        assertEquals(listOf(false), targets.filter { it.albumId == 2L }.map { it.selectable })
        assertEquals(4, targets.count { it.selectable })
    }

    @Test
    fun `subtree counts cover every level and exclude the album itself`() {
        assertEquals(SubtreeCounts(subAlbums = 3, photos = 4), tree.subtreeCounts(1))
        assertEquals(SubtreeCounts(subAlbums = 1, photos = 3), tree.subtreeCounts(2))
        assertEquals(SubtreeCounts(subAlbums = 0, photos = 0), tree.subtreeCounts(4))
        assertEquals(SubtreeCounts(subAlbums = 0, photos = 0), tree.subtreeCounts(99))
    }
}
