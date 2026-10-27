package com.ipb.castelobranco.features.gallery.domain.tags

import com.ipb.castelobranco.features.gallery.data.galleryAlbum
import com.ipb.castelobranco.features.gallery.data.galleryPhoto
import com.ipb.castelobranco.features.gallery.domain.model.GalleryIndex
import com.ipb.castelobranco.features.gallery.domain.model.GalleryMember
import com.ipb.castelobranco.features.gallery.domain.model.GalleryTree
import com.ipb.castelobranco.features.gallery.domain.model.MediaType
import org.junit.Assert.assertEquals
import org.junit.Test

class MediaFilterTest {

    private val ana = GalleryMember(1, "Ana")
    private val items = listOf(
        galleryPhoto(10, albumId = 1).copy(members = listOf(ana)),
        galleryPhoto(11, albumId = 1).copy(mediaType = MediaType.VIDEO, members = listOf(ana)),
        galleryPhoto(12, albumId = 1).copy(mediaType = MediaType.VIDEO),
    )
    private val tree = GalleryTree(
        GalleryIndex(mapOf(1L to galleryAlbum(1)), items.associateBy { it.id }, "c"),
    )

    @Test
    fun `people AND type`() {
        assertEquals(listOf(11L), GalleryPeople.matching(tree, setOf(1), MediaFilter.VIDEOS).map { it.id })
        assertEquals(listOf(10L), GalleryPeople.matching(tree, setOf(1), MediaFilter.PHOTOS).map { it.id })
        assertEquals(listOf(10L, 11L), GalleryPeople.matching(tree, setOf(1), MediaFilter.ALL).map { it.id })
    }

    @Test
    fun `type alone lists every item of that type in tree order, Tudo alone lists nothing`() {
        assertEquals(listOf(11L, 12L), GalleryPeople.matching(tree, emptySet(), MediaFilter.VIDEOS).map { it.id })
        assertEquals(emptyList<Long>(), GalleryPeople.matching(tree, emptySet(), MediaFilter.ALL).map { it.id })
    }
}
