package com.ipb.castelobranco.features.gallery.data.local

import com.ipb.castelobranco.core.data.local.StorageDirConstants
import com.ipb.castelobranco.core.testing.tempDirContext
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import java.io.File

class GalleryLegacyMigrationTest {

    @get:Rule
    val folder = TemporaryFolder()

    private lateinit var galleryDir: File
    private lateinit var preferences: GalleryPreferences
    private lateinit var migration: GalleryLegacyMigration
    private var layoutVersion = 0

    @Before
    fun setup() {
        val context = tempDirContext(folder)
        galleryDir = File(context.filesDir, StorageDirConstants.GALLERY)
        preferences = mockk {
            coEvery { layoutVersion() } answers { layoutVersion }
            coEvery { setLayoutVersion(any()) } answers { layoutVersion = firstArg() }
        }
        migration = GalleryLegacyMigration(context, preferences)
    }

    private fun legacy(albumId: Long, photoId: Long, ext: String = "jpg", bytes: String = "p$photoId"): File {
        val dir = File(galleryDir, "$albumId").apply { mkdirs() }
        File(dir, "$photoId.json").writeText("{}")
        return File(dir, "$photoId.$ext").apply { writeText(bytes) }
    }

    private fun flat(name: String) = File(galleryDir, "photos/$name")

    @Test
    fun `originals move to the flat folder with the same bytes`() = runTest {
        legacy(3, 10)
        legacy(3, 11, ext = "png")
        legacy(4, 20)

        migration.runIfNeeded()

        assertEquals("p10", flat("10.jpg").readText())
        assertEquals("p11", flat("11.png").readText())
        assertEquals("p20", flat("20.jpg").readText())
    }

    @Test
    fun `metadata files and album folders are deleted`() = runTest {
        legacy(3, 10)

        migration.runIfNeeded()

        assertFalse(File(galleryDir, "3").exists())
        assertTrue(galleryDir.walkTopDown().none { it.extension == "json" })
    }

    @Test
    fun `marker is set and a second run does nothing`() = runTest {
        legacy(3, 10)
        migration.runIfNeeded()
        legacy(5, 50)

        migration.runIfNeeded()

        assertEquals(GalleryLegacyMigration.FLAT_LAYOUT_VERSION, layoutVersion)
        assertTrue(File(galleryDir, "5/50.jpg").exists())
        coVerify(exactly = 1) { preferences.setLayoutVersion(any()) }
    }

    @Test
    fun `a run interrupted halfway finishes with the same result`() = runTest {
        legacy(3, 10)
        legacy(3, 11)
        // Simula a morte do processo depois de mover a foto 10, antes de apagar a pasta.
        flat("10.jpg").parentFile!!.mkdirs()
        File(galleryDir, "3/10.jpg").copyTo(flat("10.jpg"))

        migration.runIfNeeded()

        assertEquals(setOf("10.jpg", "11.jpg"), File(galleryDir, "photos").list()!!.toSet())
        assertFalse(File(galleryDir, "3").exists())
    }

    @Test
    fun `photos and covers folders are not treated as album folders`() = runTest {
        flat("10.jpg").parentFile!!.mkdirs()
        flat("10.jpg").writeText("kept")
        File(galleryDir, "covers").mkdirs()
        File(galleryDir, "covers/abc.jpg").writeText("cover")

        migration.runIfNeeded()

        assertEquals("kept", flat("10.jpg").readText())
        assertTrue(File(galleryDir, "covers/abc.jpg").exists())
    }
}
