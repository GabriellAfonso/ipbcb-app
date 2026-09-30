package com.ipb.castelobranco.features.gallery.domain.tags

import com.ipb.castelobranco.features.gallery.data.galleryAlbum
import com.ipb.castelobranco.features.gallery.data.galleryPhoto
import com.ipb.castelobranco.features.gallery.domain.model.GalleryIndex
import com.ipb.castelobranco.features.gallery.domain.model.GalleryMember
import com.ipb.castelobranco.features.gallery.domain.model.GalleryPhoto
import com.ipb.castelobranco.features.gallery.domain.model.GalleryTree
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class GalleryPeopleTest {

    private val ana = GalleryMember(1, "Ana")
    private val joao = GalleryMember(2, "João")
    private val bruno = GalleryMember(3, "Bruno")
    private val julia = GalleryMember(4, "Júlia")

    private fun photo(id: Long, albumId: Long, position: Int, vararg people: GalleryMember): GalleryPhoto =
        galleryPhoto(id, albumId = albumId, position = position).copy(members = people.toList())

    /**
     * Albums: 1 (root) with child 2; 3 (root, after 1); 9 has a missing parent (unreachable).
     * Photos in tree order: 10, 11 (album 1), 20 (album 2), 30 (album 3); 90 is unreachable.
     */
    private val tree = GalleryTree(
        GalleryIndex(
            albums = listOf(
                galleryAlbum(1),
                galleryAlbum(2, parentId = 1),
                galleryAlbum(3, position = 1),
                galleryAlbum(9, parentId = 99),
            ).associateBy { it.id },
            photos = listOf(
                photo(30, albumId = 3, position = 0, ana, joao, bruno),
                photo(11, albumId = 1, position = 1, ana, joao),
                photo(10, albumId = 1, position = 0, ana),
                photo(20, albumId = 2, position = 0, joao, bruno, ana),
                photo(90, albumId = 9, position = 0, ana, julia),
            ).associateBy { it.id },
            cursor = "c",
        )
    )

    private fun ids(members: Set<Long>) = GalleryPeople.photosWithAll(tree, members).map { it.id }

    @Test
    fun `one person - every photo with them, in tree order`() {
        assertEquals(listOf(10L, 11L, 20L, 30L), ids(setOf(1)))
    }

    @Test
    fun `two people - only photos with both`() {
        assertEquals(listOf(11L, 20L, 30L), ids(setOf(1, 2)))
    }

    @Test
    fun `three people - only photos with all three`() {
        assertEquals(listOf(20L, 30L), ids(setOf(1, 2, 3)))
    }

    @Test
    fun `nobody has all of them - empty, and no selection - empty`() {
        assertTrue(ids(setOf(3, 4)).isEmpty())
        assertTrue(ids(emptySet()).isEmpty())
    }

    @Test
    fun `tagged people have counts, are ordered by name, and skip unreachable photos`() {
        val people = GalleryPeople.taggedPeople(tree)

        assertEquals(
            listOf(TaggedPerson(1, "Ana", 4), TaggedPerson(3, "Bruno", 2), TaggedPerson(2, "João", 3)),
            people,
        )
    }

    @Test
    fun `people of some photos are distinct and ordered by name`() {
        val photos = tree.photosOf(1) + tree.photosOf(2)

        assertEquals(listOf(ana, bruno, joao), GalleryPeople.peopleIn(photos))
    }

    @Test
    fun `accents do not change the order`() {
        val people = arrayOf(GalleryMember(7, "Júlio"), GalleryMember(8, "Julia"), GalleryMember(9, "Ju"))

        val names = GalleryPeople.peopleIn(listOf(photo(1, albumId = 1, position = 0, *people))).map { it.name }

        assertEquals(listOf("Ju", "Julia", "Júlio"), names)
    }
}
