package com.ipb.castelobranco.features.settings.presentation.components

import org.junit.Assert.assertEquals
import org.junit.Test

class DiskUsageTest {

    @Test
    fun `zero bytes reads as nothing on the device`() {
        assertEquals("nada no aparelho", formatDiskUsage(0))
    }

    @Test
    fun `bytes below one kilobyte keep the B unit`() {
        assertEquals("512 B", formatDiskUsage(512))
    }

    @Test
    fun `sizes below ten keep one decimal with a comma`() {
        assertEquals("8,4 MB", formatDiskUsage((8.4 * 1024 * 1024).toLong()))
    }

    @Test
    fun `sizes from ten up are whole numbers`() {
        assertEquals("312 MB", formatDiskUsage(312L * 1024 * 1024))
    }

    @Test
    fun `gigabytes are the largest unit`() {
        assertEquals("1,5 GB", formatDiskUsage(1536L * 1024 * 1024))
    }

    @Test
    fun `withDiskUsage leaves the description alone while measuring`() {
        assertEquals("Apaga e baixa novamente", withDiskUsage("Apaga e baixa novamente", null))
    }

    @Test
    fun `withDiskUsage appends the size once known`() {
        assertEquals("Apaga as fotos em cache · 312 MB", withDiskUsage("Apaga as fotos em cache", 312L * 1024 * 1024))
    }
}
