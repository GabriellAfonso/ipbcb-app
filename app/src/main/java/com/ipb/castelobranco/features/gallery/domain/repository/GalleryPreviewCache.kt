package com.ipb.castelobranco.features.gallery.domain.repository

/** The cache of photo previews loaded from the server. Member content: emptied on sign-out. */
fun interface GalleryPreviewCache {
    fun clear()
}
