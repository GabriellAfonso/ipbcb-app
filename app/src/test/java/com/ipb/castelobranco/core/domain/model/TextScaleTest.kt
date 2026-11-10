package com.ipb.castelobranco.core.domain.model

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class TextScaleTest {

    @Test
    fun `normalize rounds to the nearest step inside the range`() {
        assertEquals(100, TextScale.normalize(100))
        assertEquals(105, TextScale.normalize(103))
        assertEquals(100, TextScale.normalize(102))
        assertEquals(80, TextScale.normalize(40))
        assertEquals(180, TextScale.normalize(500))
    }

    @Test
    fun `a size saved in sp becomes the percent of its base`() {
        // Bible / studies (base 18sp): the old 14–32 range.
        assertEquals(120, TextScale.fromLegacySp(22f, 18f))
        assertEquals(80, TextScale.fromLegacySp(14f, 18f))
        assertEquals(180, TextScale.fromLegacySp(32f, 18f))
        // Hymnal (base 22sp): the old default and its slider ends.
        assertEquals(100, TextScale.fromLegacySp(22f, 22f))
        assertEquals(80, TextScale.fromLegacySp(16f, 22f))
        assertEquals(145, TextScale.fromLegacySp(32f, 22f))
    }

    @Test
    fun `the buttons step by five and stop at the ends`() {
        assertEquals(105, TextScale.grown(100))
        assertEquals(95, TextScale.shrunk(100))
        assertEquals(180, TextScale.grown(180))
        assertEquals(80, TextScale.shrunk(80))
        assertFalse(TextScale.canGrow(180))
        assertFalse(TextScale.canShrink(80))
        assertTrue(TextScale.canGrow(175))
        assertTrue(TextScale.canShrink(85))
    }

    @Test
    fun `the slider has a stop every five percent`() {
        // 80, 85, …, 180 are 21 values: 19 stops between the ends.
        assertEquals(19, TextScale.SLIDER_STEPS)
    }

    @Test
    fun `sp and factor follow the percent`() {
        assertEquals(27f, TextScale.sp(18f, 150))
        assertEquals(22f, TextScale.sp(22f, 100))
        assertEquals(1.2f, TextScale.factor(120))
    }
}
