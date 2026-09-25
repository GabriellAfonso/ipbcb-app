package com.ipb.castelobranco.features.gallery.data.local

import com.ipb.castelobranco.core.data.local.StorageDirConstants
import com.ipb.castelobranco.core.testing.tempDirContext
import org.junit.Assert.assertArrayEquals
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Assert.fail
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import java.io.ByteArrayInputStream
import java.io.File
import java.io.IOException
import java.io.InputStream

class GalleryPhotoStorageTest {

    @get:Rule
    val folder = TemporaryFolder()

    private lateinit var storage: GalleryPhotoStorage
    private lateinit var albumDir: File
    private lateinit var cacheDir: File

    @Before
    fun setup() {
        val context = tempDirContext(folder)
        storage = GalleryPhotoStorage(context)
        albumDir = File(context.filesDir, "${StorageDirConstants.GALLERY}/$ALBUM_ID")
        cacheDir = context.cacheDir
    }

    @Test
    fun `save writes the full stream and the photo exists`() {
        val bytes = "complete-photo".toByteArray()

        val file = storage.save(ALBUM_ID, PHOTO_ID, "jpg", ByteArrayInputStream(bytes))

        assertEquals(File(albumDir, "$PHOTO_ID.jpg"), file)
        assertArrayEquals(bytes, file.readBytes())
        assertTrue(storage.exists(ALBUM_ID, PHOTO_ID))
        assertTrue(cacheDir.listFiles().orEmpty().isEmpty())
    }

    @Test
    fun `save with a stream cut mid-copy leaves no photo behind`() {
        try {
            storage.save(ALBUM_ID, PHOTO_ID, "jpg", BrokenStream("partial".toByteArray()))
            fail("Expected the IOException from the stream to propagate")
        } catch (_: IOException) {
            // expected
        }

        assertFalse(storage.exists(ALBUM_ID, PHOTO_ID))
        assertTrue(albumDir.listFiles().orEmpty().none { it.name.startsWith("$PHOTO_ID.") })
        assertTrue(cacheDir.listFiles().orEmpty().isEmpty())
    }

    @Test
    fun `save replaces an existing photo atomically`() {
        storage.save(ALBUM_ID, PHOTO_ID, "jpg", ByteArrayInputStream("old".toByteArray()))

        val file = storage.save(ALBUM_ID, PHOTO_ID, "jpg", ByteArrayInputStream("new".toByteArray()))

        assertArrayEquals("new".toByteArray(), file.readBytes())
    }

    private class BrokenStream(bytes: ByteArray) : InputStream() {
        private val head = ByteArrayInputStream(bytes)
        override fun read(): Int = head.read().takeIf { it >= 0 } ?: throw IOException("connection cut")
    }

    private companion object {
        const val ALBUM_ID = 3L
        const val PHOTO_ID = 12L
    }
}
