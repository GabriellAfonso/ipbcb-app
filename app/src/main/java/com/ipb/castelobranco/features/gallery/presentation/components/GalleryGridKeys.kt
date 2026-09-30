package com.ipb.castelobranco.features.gallery.presentation.components

/** Lazy-grid keys of the gallery grids, shared by the screens and the "Organizar" drag. */
object GalleryGridKeys {
    const val HEADER = "header"
    private const val ALBUM_PREFIX = "album-"
    private const val PHOTO_PREFIX = "photo-"

    fun album(id: Long) = "$ALBUM_PREFIX$id"

    fun photo(id: Long) = "$PHOTO_PREFIX$id"

    fun albumId(key: Any?): Long? = (key as? String)?.removePrefixOrNull(ALBUM_PREFIX)?.toLongOrNull()

    fun photoId(key: Any?): Long? = (key as? String)?.removePrefixOrNull(PHOTO_PREFIX)?.toLongOrNull()

    private fun String.removePrefixOrNull(prefix: String): String? =
        if (startsWith(prefix)) removePrefix(prefix) else null
}
