package com.ipb.castelobranco.core.presentation.components

import com.ipb.castelobranco.core.domain.gallery.GalleryMissing
import com.ipb.castelobranco.core.domain.gallery.GalleryUsage
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class GalleryStorageDialogsTest {

    private val mb = 1024L * 1024

    @Test
    fun `delete comes with both types checked and each size`() {
        val state = GalleryStorageDialogs.deleteDialog(
            GalleryUsage(320 * mb, 1200 * mb),
            GalleryStorageDialogs.initialDelete(),
        )

        assertEquals(listOf("320 MB", "1,2 GB"), state.options.map { it.detail })
        assertTrue(state.options.all { it.checked })
        assertTrue(state.canConfirm)
    }

    @Test
    fun `delete with nothing checked cannot be confirmed, and only videos says the switch turns off`() {
        val none = GalleryStorageDialogs.deleteDialog(null, GalleryStorageChoice(photos = false, videos = false))
        val videos = GalleryStorageDialogs.deleteDialog(null, GalleryStorageChoice(photos = false, videos = true))

        assertFalse(none.canConfirm)
        assertEquals(GalleryStorageDialogs.DELETE_NOTE_VIDEOS, videos.note)
    }

    @Test
    fun `download pre-checks missing photos, never videos, and disables what is complete`() {
        val missing = GalleryMissing(photos = 0, videos = 3, isPaused = false)
        val choice = GalleryStorageDialogs.initialDownload(missing)
        val state = GalleryStorageDialogs.downloadDialog(missing, autoDownloadVideos = false, choice)

        assertEquals(GalleryStorageDialogs.PHOTOS_DONE, state.options[0].detail)
        assertFalse(state.options[0].enabled)
        assertFalse(state.options[1].checked)
        assertTrue(state.options[1].enabled)
        assertFalse(state.canConfirm)
    }

    @Test
    fun `checking videos says new videos follow, and with the switch on videos are disabled`() {
        val missing = GalleryMissing(photos = 2, videos = 3, isPaused = false)
        val checked = GalleryStorageDialogs.downloadDialog(missing, false, GalleryStorageChoice(true, true))
        val auto = GalleryStorageDialogs.downloadDialog(missing, true, GalleryStorageChoice(true, true))

        assertEquals(GalleryStorageDialogs.DOWNLOAD_NOTE_VIDEOS, checked.note)
        assertFalse(auto.options[1].enabled)
        assertEquals(GalleryStorageDialogs.VIDEOS_AUTO, auto.options[1].detail)
        assertNull(auto.note)
    }

    @Test
    fun `after a full delete both types may come back`() {
        val missing = GalleryMissing(photos = 0, videos = 0, isPaused = true)
        val state = GalleryStorageDialogs.downloadDialog(missing, false, GalleryStorageDialogs.initialDownload(missing))

        assertTrue(state.options.all { it.enabled })
        assertTrue(state.options[0].checked)
    }

    @Test
    fun `mobile data applies only while photos are checked`() {
        val missing = GalleryMissing(photos = 2, videos = 0, isPaused = false)
        val state = GalleryStorageDialogs.downloadDialog(missing, false, GalleryStorageChoice(false, false, true))

        assertFalse(state.extra!!.enabled)
        assertFalse(state.extra!!.checked)
    }

    @Test
    fun `the done message names what was deleted`() {
        assertEquals("Galeria apagada", GalleryStorageDialogs.deletedMessage(GalleryStorageChoice(true, true)))
        assertEquals("Vídeos apagados", GalleryStorageDialogs.deletedMessage(GalleryStorageChoice(false, true)))
        assertEquals("Fotos apagadas", GalleryStorageDialogs.deletedMessage(GalleryStorageChoice(true, false)))
    }
}
