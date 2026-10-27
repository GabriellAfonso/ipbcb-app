package com.ipb.castelobranco.features.gallery.data.upload

import com.ipb.castelobranco.features.gallery.domain.upload.VideoProbeResult
import java.io.File

/** Reads a picked video and writes its poster; `null` when the file cannot be read as a video at all. */
fun interface VideoProbe {
    fun probe(video: File, poster: File): VideoProbeResult?
}
