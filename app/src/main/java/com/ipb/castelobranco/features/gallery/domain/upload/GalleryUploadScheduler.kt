package com.ipb.castelobranco.features.gallery.domain.upload

/** Runs the upload queue in the background: on any network, and on WiFi for "Esperar WiFi" videos. */
interface GalleryUploadScheduler {
    fun enqueue()

    /** A run that waits for an unmetered network, for the videos held until WiFi. */
    fun enqueueWifiOnly()

    fun cancel()
}
