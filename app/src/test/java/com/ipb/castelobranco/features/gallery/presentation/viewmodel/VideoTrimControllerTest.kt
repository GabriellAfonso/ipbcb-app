package com.ipb.castelobranco.features.gallery.presentation.viewmodel

import com.ipb.castelobranco.features.gallery.domain.upload.PickedMedia
import com.ipb.castelobranco.features.gallery.domain.upload.TrimVideoUseCase
import com.ipb.castelobranco.features.gallery.domain.upload.TrimmableVideo
import com.ipb.castelobranco.features.gallery.domain.upload.UploadBatch
import com.ipb.castelobranco.features.gallery.domain.upload.UploadEntry
import com.ipb.castelobranco.features.gallery.presentation.state.TrimHandle
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class VideoTrimControllerTest {

    private val trimmer = FakeVideoTrimmer()
    private val decided = mutableListOf<UploadBatch>()

    private fun video(id: String, mb: Long = 40, durationMs: Long = 60_000) = PickedMedia(
        id,
        "video/mp4",
        mb * MB,
        TrimmableVideo("$id.mp4", durationMs, "2026-03-14", "video/avc"),
    )

    private fun TestScope.controller(vararg media: PickedMedia): VideoTrimController =
        VideoTrimController(this, TrimVideoUseCase(trimmer)) { decided += it }
            .apply { open(UploadBatch.of(7, media.toList())) }

    private val VideoTrimController.current get() = checkNotNull(state.value)

    @Test
    fun `opens on the whole video, paused at the start`() = runTest {
        val state = controller(video("a")).current

        assertEquals(0L to 60_000L, state.startMs to state.endMs)
        assertTrue(state.isWhole)
        assertEquals(VideoTexts.USE_WHOLE, state.primaryLabel)
        assertNull(state.positionLabel)
    }

    @Test
    fun `moving a handle changes the label, the preview and the estimate`() = runTest {
        val trim = controller(video("a"))

        trim.setRange(15_000, 60_000)

        assertEquals(VideoTexts.CUT_AND_CONTINUE, trim.current.primaryLabel)
        assertEquals(15_000L, trim.current.previewMs)
        assertEquals(30L * MB, trim.current.estimatedBytes)
    }

    @Test
    fun `the selection is never shorter than one second`() = runTest {
        val trim = controller(video("a"))
        trim.setRange(0, 10_000)

        trim.setRange(9_900, 10_000)
        assertEquals(9_000L, trim.current.startMs)

        trim.setRange(9_000, 9_100)
        assertEquals(10_000L, trim.current.endMs)
    }

    @Test
    fun `a video of one second or less cannot move its handles`() = runTest {
        val trim = controller(video("a", durationMs = 800))

        trim.setRange(100, 800)

        assertTrue(trim.current.isWhole)
        assertFalse(trim.current.canMoveHandles)
    }

    @Test
    fun `dragging the selection keeps its length and stops at the ends`() = runTest {
        val trim = controller(video("a"))
        trim.setRange(10_000, 60_000)
        trim.setRange(10_000, 30_000)

        trim.moveSelection(25_000)
        assertEquals(25_000L to 45_000L, trim.current.startMs to trim.current.endMs)
        assertEquals(25_000L, trim.current.previewMs)

        trim.moveSelection(55_000)
        assertEquals(40_000L to 60_000L, trim.current.startMs to trim.current.endMs)

        trim.moveSelection(-5_000)
        assertEquals(0L to 20_000L, trim.current.startMs to trim.current.endMs)
    }

    @Test
    fun `a typed time moves that handle and the preview`() = runTest {
        val trim = controller(video("a"))

        trim.editTime(TrimHandle.START)
        assertEquals("0:00", trim.current.timeEditor?.text)
        trim.typeTime("12,5")
        trim.applyTime()

        assertEquals(12_500L, trim.current.startMs)
        assertEquals(12_500L, trim.current.previewMs)
        assertNull(trim.current.timeEditor)
    }

    @Test
    fun `a typed time that breaks a rule keeps the field open and the handles still`() = runTest {
        val trim = controller(video("a"))
        trim.setRange(10_000, 60_000)

        trim.editTime(TrimHandle.END)
        trim.typeTime("0:10,5")
        trim.applyTime()
        assertEquals(VideoTexts.endTooEarly("0:10"), trim.current.timeEditor?.error)

        trim.typeTime("2:00")
        assertNull(trim.current.timeEditor?.error)
        trim.applyTime()
        assertEquals(VideoTexts.timeOutsideVideo("1:00"), trim.current.timeEditor?.error)

        trim.typeTime("um minuto")
        trim.applyTime()
        assertEquals(VideoTexts.INVALID_TIME, trim.current.timeEditor?.error)
        assertEquals(60_000L, trim.current.endMs)

        trim.closeTimeEditor()
        assertNull(trim.current.timeEditor)
    }

    @Test
    fun `a typed start must stay one second before the end`() = runTest {
        val trim = controller(video("a"))
        trim.setRange(0, 20_000)

        trim.editTime(TrimHandle.START)
        trim.typeTime("19,5")
        trim.applyTime()

        assertEquals(VideoTexts.startTooLate("0:20"), trim.current.timeEditor?.error)
        assertEquals(0L, trim.current.startMs)
    }

    @Test
    fun `a video too short to cut opens no time field`() = runTest {
        val trim = controller(video("a", durationMs = 800))

        trim.editTime(TrimHandle.END)

        assertNull(trim.current.timeEditor)
    }

    @Test
    fun `using it whole never cuts and finishes the batch`() = runTest {
        controller(video("a")).confirm()

        assertTrue(trimmer.requests.isEmpty())
        assertEquals(listOf(UploadEntry.Picked("a")), decided.single().entries)
    }

    @Test
    fun `a cut shows progress and enters the batch`() = runTest {
        trimmer.hold = CompletableDeferred()
        val trim = controller(video("a"))
        trim.setRange(1_000, 60_000)

        trim.confirm()
        advanceUntilIdle()

        assertTrue(trim.current.isCutting)
        assertEquals(FakeVideoTrimmer.PROGRESS_SEEN, trim.current.progress)
        trimmer.hold?.complete(Unit)
        advanceUntilIdle()

        assertNull(trim.state.value)
        assertEquals(listOf(UploadEntry.Cut("trim-1.mp4", "a.mp4", "2026-03-14")), decided.single().entries)
    }

    @Test
    fun `cancelling returns to the handles as they were`() = runTest {
        trimmer.hold = CompletableDeferred()
        val trim = controller(video("a"))
        trim.setRange(1_000, 60_000)
        trim.confirm()
        advanceUntilIdle()

        trim.cancelCut()
        advanceUntilIdle()

        assertFalse(trim.current.isCutting)
        assertEquals(1_000L, trim.current.startMs)
        assertTrue(decided.isEmpty())
    }

    @Test
    fun `a failed cut stays in the editor with the message`() = runTest {
        trimmer.fail = true
        val trim = controller(video("a"))
        trim.setRange(1_000, 60_000)

        trim.confirm()
        advanceUntilIdle()

        assertEquals(VideoTexts.CUT_FAILED, trim.current.error)
        assertFalse(trim.current.isCutting)
    }

    @Test
    fun `moving a handle clears the error`() = runTest {
        trimmer.fail = true
        val trim = controller(video("a"))
        trim.setRange(1_000, 60_000)
        trim.confirm()
        advanceUntilIdle()

        trim.setRange(2_000, 60_000)

        assertNull(trim.current.error)
    }

    @Test
    fun `over the limit, whole is disabled and the cut waits for an estimate that fits`() = runTest {
        val trim = controller(video("a", mb = 400))

        assertFalse(trim.current.primaryEnabled)
        assertEquals(VideoTexts.TOO_BIG_TO_SEND, trim.current.tooBigMessage)
        trim.confirm()
        assertTrue(decided.isEmpty())

        trim.setRange(0, 30_000)
        assertFalse(trim.current.canCut)

        trim.setRange(0, 15_000)
        assertTrue(trim.current.canCut)
    }

    @Test
    fun `an output over the limit stays in the editor with its size`() = runTest {
        trimmer.cutSize = 120L * MB
        val trim = controller(video("a", mb = 400))
        trim.setRange(0, 15_000)

        trim.confirm()
        advanceUntilIdle()

        assertEquals(VideoTexts.cutTooLarge(120L * MB), trim.current.error)
        assertEquals(listOf("trim-1.mp4"), trimmer.deleted)
        assertTrue(decided.isEmpty())
    }

    @Test
    fun `the editor goes through the batch one video at a time`() = runTest {
        val trim = controller(video("a"), video("b"), video("c"))

        assertEquals("Vídeo 1 de 3", trim.current.positionLabel)
        assertTrue(trim.current.showSendAllWhole)
        trim.confirm()

        assertEquals("b", trim.current.source)
        assertEquals("Vídeo 2 de 3", trim.current.positionLabel)
        trim.confirm()

        assertFalse(trim.current.showSendAllWhole)
    }

    @Test
    fun `sending all whole decides everything left`() = runTest {
        val trim = controller(video("a"), video("b", mb = 400))

        trim.sendAllWhole()

        assertNull(trim.state.value)
        assertEquals(listOf(UploadEntry.Picked("a"), UploadEntry.Picked("b")), decided.single().entries)
        assertTrue(trimmer.requests.isEmpty())
    }

    @Test
    fun `discarding deletes the cuts of the batch and queues nothing`() = runTest {
        val trim = controller(video("a"), video("b"))
        trim.setRange(1_000, 60_000)
        trim.confirm()
        advanceUntilIdle()

        trim.askDiscard()
        assertTrue(trim.current.askingDiscard)
        trim.discard()
        advanceUntilIdle()

        assertNull(trim.state.value)
        assertEquals(listOf("trim-1.mp4"), trimmer.deleted)
        assertTrue(decided.isEmpty())
    }

    @Test
    fun `a last cut ending during the question waits for the answer`() = runTest {
        trimmer.hold = CompletableDeferred()
        val trim = controller(video("a"))
        trim.setRange(1_000, 60_000)
        trim.confirm()
        advanceUntilIdle()

        trim.askDiscard()
        trimmer.hold?.complete(Unit)
        advanceUntilIdle()

        assertTrue(decided.isEmpty())
        assertTrue(trim.current.askingDiscard)
        assertFalse(trim.current.isCutting)

        trim.keepEditing()

        assertEquals(1, decided.size)
    }

    @Test
    fun `discarding during a cut stops it`() = runTest {
        trimmer.hold = CompletableDeferred()
        val trim = controller(video("a"))
        trim.setRange(1_000, 60_000)
        trim.confirm()
        advanceUntilIdle()

        trim.discard()
        trimmer.hold?.complete(Unit)
        advanceUntilIdle()

        assertNull(trim.state.value)
        assertTrue(decided.isEmpty())
    }

    private companion object {
        const val MB = 1024L * 1024
    }
}
