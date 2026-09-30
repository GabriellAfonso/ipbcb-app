package com.ipb.castelobranco.features.gallery.domain.manage

import com.ipb.castelobranco.core.domain.error.AppError

/*
 * What kind of refusal an [AppError] from a gallery write is, read from the status and the
 * structured extras `ResponseExt` kept. No text here — the screen decides it.
 */

private const val HTTP_BAD_REQUEST = 400
private const val HTTP_FORBIDDEN = 403
private const val HTTP_NOT_FOUND = 404
private const val EXTRA_CHAIN = "chain"
private val ORDER_EXTRAS = setOf("missing", "unexpected", "repeated")

fun AppError.isForbidden(): Boolean = this is AppError.Auth && code == HTTP_FORBIDDEN

fun AppError.isNotFound(): Boolean = this is AppError.Server && code == HTTP_NOT_FOUND

/** 400 listing `missing` / `unexpected` / `repeated`: the siblings changed since the list was read. */
fun AppError.isOrderMismatch(): Boolean =
    this is AppError.Server && code == HTTP_BAD_REQUEST && extras.orEmpty().keys.any { it in ORDER_EXTRAS }

/** 400 with the ancestor `chain`: the album would end up inside its own subtree. */
fun AppError.isCycle(): Boolean =
    this is AppError.Server && code == HTTP_BAD_REQUEST && extras.orEmpty().containsKey(EXTRA_CHAIN)

/** Any other structured 400 of a name write: the name is taken by a sibling. */
fun AppError.isValidation(): Boolean =
    this is AppError.Server && code == HTTP_BAD_REQUEST && !isOrderMismatch() && !isCycle()
