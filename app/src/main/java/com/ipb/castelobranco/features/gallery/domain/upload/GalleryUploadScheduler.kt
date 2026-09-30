package com.ipb.castelobranco.features.gallery.domain.upload

/** Runs the upload queue in the background, on any network. */
interface GalleryUploadScheduler {
    fun enqueue()
    fun cancel()
}
