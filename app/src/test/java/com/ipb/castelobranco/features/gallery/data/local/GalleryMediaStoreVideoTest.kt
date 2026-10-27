package com.ipb.castelobranco.features.gallery.data.local

import com.ipb.castelobranco.core.testing.tempDirContext
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import java.io.ByteArrayInputStream

class GalleryMediaStoreVideoTest {

    @get:Rule
    val folder = TemporaryFolder()

    private lateinit var store: GalleryMediaStore

    @Before
    fun setup() {
        store = GalleryMediaStore(tempDirContext(folder))
    }

    @Test
    fun `a partial is never listed as a video until it is completed`() {
        store.partialVideo(5, "mp4").writeText("half")

        assertTrue(store.videoFiles().isEmpty())
        assertEquals(setOf(5L), store.partialVideos().keys)

        store.completeVideo(5, "mp4")

        assertEquals("half", store.videoFiles().getValue(5).readText())
        assertTrue(store.partialVideos().isEmpty())
    }

    @Test
    fun `sizes split videos (partials included) from everything else`() {
        store.saveOriginal(1, "jpg", ByteArrayInputStream(ByteArray(10)))
        store.videoFile(2, "mov").apply { parentFile?.mkdirs() }.writeBytes(ByteArray(30))
        store.partialVideo(3, "mp4").writeBytes(ByteArray(5))

        assertEquals(35L, store.videoBytes())
        assertEquals(45L, store.totalBytes())
    }

    @Test
    fun `clearing photos keeps videos and clearing videos keeps photos`() {
        store.saveOriginal(1, "jpg", ByteArrayInputStream(ByteArray(1)))
        store.partialVideo(2, "mp4").writeBytes(ByteArray(1))

        store.clearPhotos()
        assertTrue(store.originalFiles().isEmpty())
        assertEquals(1, store.partialVideos().size)

        store.saveOriginal(1, "jpg", ByteArrayInputStream(ByteArray(1)))
        store.clearVideos()
        assertEquals(1, store.originalFiles().size)
        assertTrue(store.partialVideos().isEmpty())
    }

    @Test
    fun `an uploaded video becomes the device copy`() {
        val source = store.uploadFile("u-src.mp4").apply { parentFile?.mkdirs() }
        source.writeText("mine")

        store.adoptVideo(9, "mp4", source)

        assertFalse(source.exists())
        assertEquals("mine", store.videoFiles().getValue(9).readText())
    }
}
