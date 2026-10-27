package com.ipb.castelobranco.features.gallery.domain.download

/** Schedules the video download job — always on an unmetered network (WiFi). */
interface GalleryVideoDownloadScheduler {
    /** [replaceExisting] `false` keeps a job already queued or running. */
    fun enqueue(replaceExisting: Boolean = false)

    fun cancel()
}
