package com.ipb.castelobranco.features.gallery.domain.model

import java.io.File

/** What the device holds: the index and the files already on disk. */
data class GalleryLocalState(
    /** `null` = never synced, or cleared by logout. */
    val index: GalleryIndex?,
    /** Photo id → original on disk. */
    val originals: Map<Long, File>,
    /** `cover_url` → cover on disk. */
    val covers: Map<String, File>,
    /** Video id → the complete file on disk. A video not listed here is not shown (see `GalleryVisibility`). */
    val videos: Map<Long, File> = emptyMap(),
) {
    companion object {
        val EMPTY = GalleryLocalState(index = null, originals = emptyMap(), covers = emptyMap())
    }
}
