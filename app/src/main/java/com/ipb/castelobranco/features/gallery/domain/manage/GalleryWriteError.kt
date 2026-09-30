package com.ipb.castelobranco.features.gallery.domain.manage

/** Why the server did not accept a gallery write, with the text to show. */
sealed interface GalleryWriteError {
    val message: String

    data class Offline(override val message: String) : GalleryWriteError

    /** The level on `gallery` is gone; access is refreshed by the network layer. */
    data class Forbidden(override val message: String) : GalleryWriteError

    /** The item (or its album) was deleted meanwhile. */
    data class NotFound(override val message: String) : GalleryWriteError
    data class DuplicateName(override val message: String) : GalleryWriteError

    /** The move would put an album inside its own subtree — the tree changed under the user. */
    data class Cycle(override val message: String) : GalleryWriteError

    /** An order that no longer lists exactly the live siblings. */
    data class OrderMismatch(override val message: String) : GalleryWriteError

    data class Other(override val message: String) : GalleryWriteError
}
