package com.ipb.castelobranco.features.gallery.presentation.state

import com.ipb.castelobranco.core.domain.access.Access
import com.ipb.castelobranco.core.domain.access.AccessLevel
import com.ipb.castelobranco.core.domain.access.Scope

/**
 * What the user may change in the gallery, from their level on `gallery`. A UI filter only: the
 * server checks every write. Without a level, no management control is composed.
 */
data class GalleryPermissions(
    /** `manage`: create, edit, move, organize, covers, upload, edit and move photos. */
    val canManage: Boolean,
    /** `owner`: also delete albums and photos, remove a cover. */
    val canDelete: Boolean,
) {
    companion object {
        val NONE = GalleryPermissions(canManage = false, canDelete = false)
    }
}

fun Access.toGalleryPermissions() = GalleryPermissions(
    canManage = allows(Scope.GALLERY, AccessLevel.MANAGE),
    canDelete = allows(Scope.GALLERY, AccessLevel.OWNER),
)
