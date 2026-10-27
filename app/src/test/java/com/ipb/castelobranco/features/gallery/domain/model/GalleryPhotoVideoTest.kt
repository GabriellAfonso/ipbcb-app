package com.ipb.castelobranco.features.gallery.domain.model

import com.ipb.castelobranco.features.gallery.data.dto.GalleryPhotoDto
import com.ipb.castelobranco.features.gallery.data.dto.toDomain
import com.ipb.castelobranco.features.gallery.data.dto.toDto
import kotlinx.serialization.json.Json
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class GalleryPhotoVideoTest {

    private val json = Json { ignoreUnknownKeys = true }

    @Test
    fun `an item stored before videos reads as a photo without size`() {
        val dto = json.decodeFromString(
            GalleryPhotoDto.serializer(),
            """{"id":1,"name":"a.jpg","album_id":2,"image_url":"https://x/a.jpg"}""",
        )

        val photo = dto.toDomain()

        assertEquals(MediaType.PHOTO, photo.mediaType)
        assertNull(photo.durationMs)
        assertNull(photo.fileSize)
        assertFalse(photo.isVideo)
    }

    @Test
    fun `a video from the feed keeps type, duration and size, and survives the snapshot`() {
        val dto = json.decodeFromString(
            GalleryPhotoDto.serializer(),
            """{"id":5,"name":"v.mov","album_id":2,"image_url":"https://x/v.MOV",
               "media_type":"video","duration_ms":45000,"file_size":41943040}""",
        )

        val video = dto.toDomain()

        assertTrue(video.isVideo)
        assertEquals(45_000L, video.durationMs)
        assertEquals(41_943_040L, video.fileSize)
        assertEquals("mov", video.fileExtension())
        assertEquals("video/quicktime", video.mimeType())
        assertEquals(video, video.toDto().toDomain())
    }

    @Test
    fun `an unknown media type is a photo`() {
        assertEquals(MediaType.PHOTO, MediaType.fromApi("audio"))
        assertEquals(MediaType.PHOTO, MediaType.fromApi(null))
    }

    @Test
    fun `a video without a known extension is mp4`() {
        val video = GalleryPhoto(1, "v", "", 1, "", "https://x/v", null, null, null, 0, emptyList(), MediaType.VIDEO)

        assertEquals("mp4", video.fileExtension())
        assertEquals("video/mp4", video.mimeType())
    }

    @Test
    fun `duration reads as m-ss, and h-mm-ss from one hour`() {
        assertEquals("0:07", VideoDuration.format(7_400))
        assertEquals("1:24", VideoDuration.format(84_000))
        assertEquals("1:02:03", VideoDuration.format(3_723_000))
        assertEquals("0:00", VideoDuration.format(-5))
    }
}
