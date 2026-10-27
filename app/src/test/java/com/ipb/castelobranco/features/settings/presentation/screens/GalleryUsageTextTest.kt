package com.ipb.castelobranco.features.settings.presentation.screens

import com.ipb.castelobranco.core.domain.gallery.GalleryUsage
import org.junit.Assert.assertEquals
import org.junit.Test

class GalleryUsageTextTest {

    private val mb = 1024L * 1024

    @Test
    fun `measuring shows the plain label`() {
        assertEquals("Fotos e vídeos", galleryUsageText(null))
    }

    @Test
    fun `photos and videos show both sizes`() {
        assertEquals("Fotos 312 MB · Vídeos 12 MB", galleryUsageText(GalleryUsage(312 * mb, 12 * mb)))
    }

    @Test
    fun `no videos leaves the videos part out`() {
        assertEquals("Fotos 312 MB", galleryUsageText(GalleryUsage(312 * mb, 0)))
    }

    @Test
    fun `no photos leaves the photos part out`() {
        assertEquals("Vídeos 12 MB", galleryUsageText(GalleryUsage(0, 12 * mb)))
    }

    @Test
    fun `nothing on the device reads as such`() {
        assertEquals("nada no aparelho", galleryUsageText(GalleryUsage(0, 0)))
    }
}
