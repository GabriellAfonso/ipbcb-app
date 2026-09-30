package com.ipb.castelobranco.features.gallery.data.dto

import com.ipb.castelobranco.features.gallery.domain.manage.AlbumDraft
import com.ipb.castelobranco.features.gallery.domain.manage.AlbumEdit
import com.ipb.castelobranco.features.gallery.domain.manage.Field
import com.ipb.castelobranco.features.gallery.domain.manage.PhotoEdit
import kotlinx.serialization.json.JsonNull
import org.junit.Assert.assertEquals
import org.junit.Test

class GalleryWriteBodiesTest {

    @Test
    fun `create body has the name only when the rest is empty`() {
        val body = AlbumDraft(name = "Retiro", description = " ", eventDate = null, parentId = null).toCreateBody()

        assertEquals("""{"name":"Retiro"}""", body.toString())
    }

    @Test
    fun `create body carries parent, description and date when set`() {
        val body = AlbumDraft(name = "Sábado", description = "Culto", eventDate = "2026-03-14", parentId = 7)
            .toCreateBody()

        assertEquals(
            """{"name":"Sábado","parent_id":7,"description":"Culto","event_date":"2026-03-14"}""",
            body.toString(),
        )
    }

    @Test
    fun `album patch sends explicit nulls to move to the root and clear the date`() {
        val body = AlbumEdit(albumId = 3, eventDate = Field.Set(null), parentId = Field.Set(null)).toPatchBody()

        assertEquals(JsonNull, body["parent_id"])
        assertEquals(JsonNull, body["event_date"])
        assertEquals(setOf("parent_id", "event_date"), body.keys)
    }

    @Test
    fun `album patch leaves unchanged fields out`() {
        val body = AlbumEdit(albumId = 3, name = Field.Set("Novo")).toPatchBody()

        assertEquals("""{"name":"Novo"}""", body.toString())
    }

    @Test
    fun `album order body always has parent_id`() {
        assertEquals("""{"parent_id":null,"ids":[5,1,3]}""", albumOrderBody(null, listOf(5, 1, 3)).toString())
        assertEquals("""{"parent_id":2,"ids":[4]}""", albumOrderBody(2, listOf(4)).toString())
    }

    @Test
    fun `photo bodies`() {
        assertEquals("""{"ids":[12,10]}""", photoOrderBody(listOf(12, 10)).toString())
        assertEquals(
            """{"date_taken":null}""",
            PhotoEdit(photoId = 1, dateTaken = Field.Set(null)).toPatchBody().toString(),
        )
        assertEquals("""{"album_id":9}""", PhotoEdit(photoId = 1, albumId = Field.Set(9)).toPatchBody().toString())
    }
}
