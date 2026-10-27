package com.ipb.castelobranco.features.gallery.domain.upload

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class VideoUploadCheckTest {

    private fun probe(
        container: VideoContainer = VideoContainer.MP4,
        size: Long = 10L * 1024 * 1024,
        video: String? = "video/avc",
        audio: List<String> = listOf("audio/mp4a-latm"),
        duration: Long? = 45_000,
        poster: Boolean = true,
    ) = VideoProbeResult(container, size, video, audio, duration, hasPoster = poster)

    @Test
    fun `an H264 or HEVC MP4 or MOV with AAC or no audio is accepted, whatever its length`() {
        assertNull(VideoUploadCheck.refusal(probe()))
        assertNull(VideoUploadCheck.refusal(probe(container = VideoContainer.MOV, video = "video/hevc")))
        assertNull(VideoUploadCheck.refusal(probe(audio = emptyList(), duration = 3_600_000)))
        assertNull(VideoUploadCheck.refusal(probe(size = VideoUploadCheck.MAX_BYTES)))
    }

    @Test
    fun `over 100 MB is refused first`() {
        assertEquals(
            VideoUploadCheck.TOO_LARGE,
            VideoUploadCheck.refusal(probe(size = VideoUploadCheck.MAX_BYTES + 1, container = VideoContainer.OTHER)),
        )
    }

    @Test
    fun `another container (MKV, WEBM, AVI) asks to convert to MP4`() {
        assertEquals(VideoUploadCheck.WRONG_CONTAINER, VideoUploadCheck.refusal(probe(container = VideoContainer.OTHER)))
    }

    @Test
    fun `AC3 audio or another video codec is not compatible`() {
        assertEquals(VideoUploadCheck.WRONG_CODEC, VideoUploadCheck.refusal(probe(audio = listOf("audio/ac3"))))
        assertEquals(VideoUploadCheck.WRONG_CODEC, VideoUploadCheck.refusal(probe(video = "video/x-vnd.on2.vp9")))
        assertEquals(VideoUploadCheck.WRONG_CODEC, VideoUploadCheck.refusal(probe(video = null)))
    }

    @Test
    fun `no poster refuses the video - a video without a poster must never exist`() {
        assertEquals(VideoUploadCheck.NO_POSTER, VideoUploadCheck.refusal(probe(poster = false)))
    }

    @Test
    fun `no duration reads as unreadable`() {
        assertEquals(VideoUploadCheck.UNREADABLE, VideoUploadCheck.refusal(probe(duration = null)))
    }
}
