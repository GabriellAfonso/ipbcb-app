package com.ipb.castelobranco.features.gallery.data.local

import com.ipb.castelobranco.core.data.local.StorageDirConstants
import com.ipb.castelobranco.core.testing.tempDirContext
import com.ipb.castelobranco.features.gallery.data.coverUrl
import org.junit.Assert.assertArrayEquals
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotEquals
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

class GalleryMediaStoreTest {

    @get:Rule
    val folder = TemporaryFolder()

    private lateinit var store: GalleryMediaStore
    private lateinit var galleryDir: File
    private lateinit var cacheDir: File

    @Before
    fun setup() {
        val context = tempDirContext(folder)
        store = GalleryMediaStore(context)
        galleryDir = File(context.filesDir, StorageDirConstants.GALLERY)
        cacheDir = context.cacheDir
    }

    @Test
    fun `original is saved flat by photo id`() {
        val bytes = "photo".toByteArray()

        val file = store.saveOriginal(42L, "png", ByteArrayInputStream(bytes))

        assertEquals(File(galleryDir, "photos/42.png"), file)
        assertArrayEquals(bytes, file.readBytes())
        assertTrue(store.hasOriginal(42L, "png"))
        assertEquals(mapOf(42L to file), store.originalFiles())
    }

    @Test
    fun `a stream cut mid-body leaves no original and no temp file`() {
        val broken = object : InputStream() {
            private var sent = 0
            override fun read(): Int = if (sent++ < 4) 'x'.code else throw IOException("cut")
        }

        try {
            store.saveOriginal(1L, "jpg", broken)
            fail("expected IOException")
        } catch (_: IOException) {
        }

        assertFalse(store.hasOriginal(1L, "jpg"))
        assertTrue(cacheDir.listFiles().orEmpty().isEmpty())
        assertTrue(galleryDir.walkTopDown().none { it.isFile })
    }

    @Test
    fun `files that are not photo ids are ignored in the originals listing`() {
        File(galleryDir, "photos").mkdirs()
        File(galleryDir, "photos/readme.txt").writeText("x")

        assertTrue(store.originalFiles().isEmpty())
    }

    @Test
    fun `cover is named by a hash of its url - a new url is a new file`() {
        val first = store.saveCover(coverUrl("a"), ByteArrayInputStream("a".toByteArray()))
        val second = store.coverFile(coverUrl("b"))

        assertEquals(File(galleryDir, "covers/${store.coverName(coverUrl("a"))}"), first)
        assertTrue(first.exists())
        assertNotEquals(first, second)
        assertEquals(listOf(first), store.coverFiles())
    }

    @Test
    fun `clearAll deletes the whole gallery folder`() {
        store.saveOriginal(1L, "jpg", ByteArrayInputStream("p".toByteArray()))
        store.saveCover(coverUrl("a"), ByteArrayInputStream("c".toByteArray()))

        store.clearAll()

        assertFalse(galleryDir.exists())
    }
}
