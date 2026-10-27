package com.ipb.castelobranco.features.gallery.domain.upload

/** What the phone read from a picked video, before sending it. */
data class VideoProbeResult(
    val container: VideoContainer,
    val byteSize: Long,
    /** MIME of the video track (`video/avc`), `null` without one. */
    val videoMime: String?,
    /** MIMEs of the audio tracks; empty when silent. */
    val audioMimes: List<String>,
    val durationMs: Long?,
    /** `yyyy-MM-dd`, when the file says when it was recorded. */
    val dateTaken: String? = null,
    /** The poster was written. */
    val hasPoster: Boolean,
)

enum class VideoContainer { MP4, MOV, OTHER }

/**
 * The server accepts MP4 and MOV up to 100 MB and never converts, so the phone refuses anything a member's
 * phone might not play — before it is sent, with a reason, so the person converts the file elsewhere and
 * comes back. No duration limit.
 */
object VideoUploadCheck {
    const val MAX_BYTES = 100L * 1024 * 1024
    const val TOO_LARGE = "Vídeo muito grande (máx. 100 MB). Reduza o arquivo e tente de novo."
    const val WRONG_CONTAINER = "Formato não suportado. Converta o vídeo para MP4."
    const val WRONG_CODEC = "O formato de áudio ou vídeo não é compatível. Converta o vídeo para MP4."
    const val NO_POSTER = "Não foi possível gerar a capa deste vídeo."
    const val UNREADABLE = "Não foi possível ler este vídeo."
    private val VIDEO_CODECS = setOf("video/avc", "video/hevc")
    private const val AAC = "audio/mp4a-latm"

    /** The refusal, or `null` when the video may be sent. Size first: the cheapest to fix. */
    fun refusal(probe: VideoProbeResult): String? = when {
        probe.byteSize > MAX_BYTES -> TOO_LARGE
        probe.container == VideoContainer.OTHER -> WRONG_CONTAINER
        probe.videoMime !in VIDEO_CODECS -> WRONG_CODEC
        probe.audioMimes.any { it != AAC } -> WRONG_CODEC
        probe.durationMs == null || probe.durationMs <= 0 -> UNREADABLE
        !probe.hasPoster -> NO_POSTER
        else -> null
    }
}
