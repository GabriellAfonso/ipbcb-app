package com.ipb.castelobranco.features.gallery.presentation.viewmodel

import com.ipb.castelobranco.core.domain.error.AppError
import com.ipb.castelobranco.core.presentation.error.toUserMessage
import com.ipb.castelobranco.features.gallery.domain.manage.GalleryWriteError
import com.ipb.castelobranco.features.gallery.domain.manage.isCycle
import com.ipb.castelobranco.features.gallery.domain.manage.isForbidden
import com.ipb.castelobranco.features.gallery.domain.manage.isNotFound
import com.ipb.castelobranco.features.gallery.domain.manage.isOrderMismatch
import com.ipb.castelobranco.features.gallery.domain.manage.isValidation

internal const val NOT_FOUND_MESSAGE = "Este item não existe mais."
internal const val ORDER_CHANGED_MESSAGE = "A ordem mudou enquanto você editava. Confira e salve de novo."

/**
 * The refusal of a gallery write and the text to show. The server's Portuguese `detail` is used
 * whenever it is structured (`toUserMessage`); the English order-mismatch detail never reaches the
 * screen.
 */
internal fun AppError.toGalleryWriteError(): GalleryWriteError = when {
    this is AppError.Network -> GalleryWriteError.Offline(toUserMessage())
    isForbidden() -> GalleryWriteError.Forbidden(toUserMessage())
    isNotFound() -> GalleryWriteError.NotFound(NOT_FOUND_MESSAGE)
    isOrderMismatch() -> GalleryWriteError.OrderMismatch(ORDER_CHANGED_MESSAGE)
    isCycle() -> GalleryWriteError.Cycle(toUserMessage())
    isValidation() -> GalleryWriteError.DuplicateName(toUserMessage())
    else -> GalleryWriteError.Other(toUserMessage())
}
