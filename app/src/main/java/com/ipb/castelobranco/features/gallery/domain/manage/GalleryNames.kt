package com.ipb.castelobranco.features.gallery.domain.manage

/** Album and photo names: trimmed, 1–100 characters, as the server requires. */
object GalleryNames {
    const val MAX_LENGTH = 100

    fun validate(raw: String): NameCheck {
        val trimmed = raw.trim()
        return when {
            trimmed.isEmpty() -> NameCheck.Empty
            trimmed.length > MAX_LENGTH -> NameCheck.TooLong
            else -> NameCheck.Valid(trimmed)
        }
    }
}

sealed interface NameCheck {
    data class Valid(val name: String) : NameCheck
    data object Empty : NameCheck
    data object TooLong : NameCheck
}
