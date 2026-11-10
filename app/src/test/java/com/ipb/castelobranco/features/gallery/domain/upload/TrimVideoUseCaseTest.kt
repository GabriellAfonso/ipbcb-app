package com.ipb.castelobranco.features.gallery.domain.upload

import com.ipb.castelobranco.features.gallery.presentation.viewmodel.FakeVideoTrimmer
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class TrimVideoUseCaseTest {

    private val trimmer = FakeVideoTrimmer()
    private val useCase = TrimVideoUseCase(trimmer)
    private val video = TrimmableVideo("Culto.MOV", 60_000, "2026-03-14", "video/hevc")

    private suspend fun run() = useCase("content://v", video, 1_000, 5_000) { }

    @Test
    fun `a cut takes an mp4 name and the original's date`() = runTest {
        val outcome = run() as TrimOutcome.Cut

        assertEquals(UploadEntry.Cut("trim-1.mp4", "Culto.mp4", "2026-03-14"), outcome.entry)
        assertEquals(listOf(1_000L to 5_000L), trimmer.requests)
    }

    @Test
    fun `an output over the limit is deleted and reported with its size`() = runTest {
        trimmer.cutSize = VideoUploadCheck.MAX_BYTES + 1

        assertEquals(TrimOutcome.TooLarge(VideoUploadCheck.MAX_BYTES + 1), run())
        assertEquals(listOf("trim-1.mp4"), trimmer.deleted)
    }

    @Test
    fun `an output of exactly the limit is accepted`() = runTest {
        trimmer.cutSize = VideoUploadCheck.MAX_BYTES

        assertTrue(run() is TrimOutcome.Cut)
    }

    @Test
    fun `a failed export is reported`() = runTest {
        trimmer.fail = true

        assertEquals(TrimOutcome.Failed, run())
    }

    @Test
    fun `discard deletes every cut`() = runTest {
        useCase.discard(listOf("a", "b"))

        assertEquals(listOf("a", "b"), trimmer.deleted)
    }
}
