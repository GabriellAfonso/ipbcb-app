package com.ipb.castelobranco.features.gallery.data.upload

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class UploadPreparationPlannerTest {

    private fun reencode(
        width: Int,
        height: Int,
        orientation: Int = 1,
        mime: String = "image/jpeg",
        size: Long = 5_000_000,
    ) = UploadPreparationPlanner.plan(width, height, orientation, mime, size) as PreparationPlan.Reencode

    @Test
    fun `a 24 MP photo is scaled so its long side is 4000, aspect kept`() {
        val plan = reencode(6000, 4000)

        assertEquals(4000, plan.targetWidth)
        assertEquals(2667, plan.targetHeight)
        assertEquals(1, plan.sampleSize)
    }

    @Test
    fun `a huge photo is sampled before decoding, never below the target`() {
        val plan = reencode(12000, 9000)

        assertEquals(2, plan.sampleSize)
        assertEquals(4000, plan.targetWidth)
        assertEquals(3000, plan.targetHeight)
    }

    @Test
    fun `a small photo keeps its size but is still re-encoded`() {
        val plan = reencode(3000, 2000)

        assertEquals(3000, plan.targetWidth)
        assertEquals(2000, plan.targetHeight)
        assertEquals(1, plan.sampleSize)
    }

    @Test
    fun `EXIF rotations turn the pixels upright and swap the target for quarter turns`() {
        assertEquals(180, reencode(6000, 4000, orientation = 3).rotationDegrees)

        val portrait = reencode(6000, 4000, orientation = 6)
        assertEquals(90, portrait.rotationDegrees)
        assertEquals(2667, portrait.targetWidth)
        assertEquals(4000, portrait.targetHeight)

        assertEquals(270, reencode(6000, 4000, orientation = 8).rotationDegrees)
    }

    @Test
    fun `mirrored orientations flip after rotating`() {
        assertEquals(0 to true, reencode(100, 50, 2).let { it.rotationDegrees to it.flipHorizontal })
        assertEquals(180 to true, reencode(100, 50, 4).let { it.rotationDegrees to it.flipHorizontal })
        assertEquals(90 to true, reencode(100, 50, 5).let { it.rotationDegrees to it.flipHorizontal })
        assertEquals(270 to true, reencode(100, 50, 7).let { it.rotationDegrees to it.flipHorizontal })
        assertEquals(0 to false, reencode(100, 50, 1).let { it.rotationDegrees to it.flipHorizontal })
    }

    @Test
    fun `a GIF within limits is sent as is`() {
        val plan = UploadPreparationPlanner.plan(1000, 1000, 1, "image/gif", 5_000_000)

        assertEquals(PreparationPlan.SendAsIs, plan)
    }

    @Test
    fun `a GIF over 10 MB or 50 MP is converted`() {
        assertTrue(UploadPreparationPlanner.plan(1000, 1000, 1, "image/gif", 12_000_000) is PreparationPlan.Reencode)
        assertTrue(UploadPreparationPlanner.plan(8000, 7000, 1, "image/gif", 1_000_000) is PreparationPlan.Reencode)
    }

    @Test
    fun `the quality ladder starts at 90 and ends at 60`() {
        assertEquals(90, UploadPreparationPlanner.QUALITY_LADDER.first())
        assertEquals(60, UploadPreparationPlanner.QUALITY_LADDER.last())
    }
}
