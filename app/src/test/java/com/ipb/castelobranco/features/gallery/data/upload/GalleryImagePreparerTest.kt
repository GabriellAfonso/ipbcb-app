package com.ipb.castelobranco.features.gallery.data.upload

import com.ipb.castelobranco.core.testing.tempDirContext
import com.ipb.castelobranco.features.gallery.data.local.GalleryMediaStore
import com.ipb.castelobranco.features.gallery.domain.upload.UploadItem
import com.ipb.castelobranco.features.gallery.domain.upload.UploadState
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import java.io.File

class GalleryImagePreparerTest {

    @get:Rule
    val folder = TemporaryFolder()

    /** Scripted codec: [sizes] = bytes written for each quality tried, in order. */
    private class FakeImageCodec : ImageCodec {
        var info: SourceInfo? = null
        var decodable = true
        var sizes = listOf(1_000_000L)
        val qualities = mutableListOf<Int>()
        var exifWritten: Map<String, String>? = null
        var released = false
        var decodedWith: PreparationPlan.Reencode? = null

        override fun readInfo(file: File) = info

        override fun decode(file: File, plan: PreparationPlan.Reencode): DecodedImage? {
            decodedWith = plan
            if (!decodable) return null
            return object : DecodedImage {
                override fun writeJpeg(quality: Int, output: File): Long {
                    val size = sizes.getOrElse(qualities.size) { sizes.last() }
                    qualities += quality
                    output.writeText("jpeg@$quality")
                    return size
                }

                override fun release() {
                    released = true
                }
            }
        }

        override fun writeExif(file: File, tags: Map<String, String>) {
            exifWritten = tags
        }
    }

    private lateinit var media: GalleryMediaStore
    private val codec = FakeImageCodec()
    private lateinit var preparer: GalleryImagePreparer

    private val item = UploadItem(
        uploadId = "u1",
        albumId = 7,
        displayName = "IMG_0042.HEIC",
        fileName = GalleryImagePreparer.rawFileName("u1", "heic"),
        state = UploadState.Waiting,
        enqueuedAt = 0,
    )

    private val dateTags = mapOf(
        "DateTimeOriginal" to "2026:03:14 10:00:00",
        "OffsetTimeOriginal" to "-03:00",
        "SubSecTimeOriginal" to "123",
        "DateTimeDigitized" to "2026:03:14 10:00:00",
        "DateTime" to "2026:03:14 10:00:00",
    )

    @Before
    fun setup() {
        media = GalleryMediaStore(tempDirContext(folder))
        media.saveUpload(item.fileName, "raw".byteInputStream())
        codec.info = SourceInfo(6000, 4000, 6, "image/heic", 3_000_000, dateTags)
        preparer = GalleryImagePreparer(codec, media)
    }

    @Test
    fun `re-encodes as JPEG at 90, keeps the capture date and writes the orientation as normal`() {
        val result = preparer.prepare(item) as PrepareResult.Ready

        assertEquals(listOf(90), codec.qualities)
        assertEquals("u1.jpg", result.file.name)
        assertEquals("IMG_0042.jpg", result.displayName)
        assertEquals(dateTags + ("Orientation" to "1"), codec.exifWritten)
        assertEquals(90, codec.decodedWith?.rotationDegrees)
        assertTrue(codec.released)
    }

    @Test
    fun `the raw copy is deleted once the prepared file exists`() {
        preparer.prepare(item)

        assertFalse(media.uploadFile(item.fileName).exists())
        assertTrue(media.uploadFile("u1.jpg").exists())
    }

    @Test
    fun `no location or device tag is ever written`() {
        codec.info = codec.info!!.copy(dateTags = emptyMap())

        preparer.prepare(item)

        assertEquals(mapOf("Orientation" to "1"), codec.exifWritten)
    }

    @Test
    fun `lowers the quality until the file fits in 10 MB`() {
        codec.sizes = listOf(12_000_000, 11_000_000, 9_000_000)

        val result = preparer.prepare(item)

        assertTrue(result is PrepareResult.Ready)
        assertEquals(listOf(90, 85, 80), codec.qualities)
    }

    @Test
    fun `nothing fits - too large, no file left`() {
        codec.sizes = listOf(20_000_000)

        assertEquals(PrepareResult.TooLarge, preparer.prepare(item))
        assertEquals(UploadPreparationPlanner.QUALITY_LADDER, codec.qualities)
        assertFalse(media.uploadFile("u1.jpg").exists())
        assertTrue(codec.released)
    }

    @Test
    fun `unreadable source or failed decode`() {
        codec.info = null
        assertEquals(PrepareResult.Unreadable, preparer.prepare(item))

        codec.info = SourceInfo(10, 10, 1, "image/jpeg", 100, emptyMap())
        codec.decodable = false
        assertEquals(PrepareResult.Unreadable, preparer.prepare(item))
    }

    @Test
    fun `a GIF within limits is sent untouched`() {
        codec.info = SourceInfo(500, 500, 1, "image/gif", 2_000_000, emptyMap())
        val gif = item.copy(displayName = "festa.gif", fileName = GalleryImagePreparer.rawFileName("u1", "gif"))
        media.saveUpload(gif.fileName, "gif".byteInputStream())

        val result = preparer.prepare(gif) as PrepareResult.Ready

        assertEquals(gif.fileName, result.file.name)
        assertEquals("festa.gif", result.displayName)
        assertTrue(codec.qualities.isEmpty())
    }
}
