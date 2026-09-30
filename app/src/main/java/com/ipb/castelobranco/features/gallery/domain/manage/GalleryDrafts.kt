package com.ipb.castelobranco.features.gallery.domain.manage

import com.ipb.castelobranco.core.domain.error.AppError

/** A field of an edit: left as it is, or set — `Set(null)` clears it (or moves to the root). */
sealed interface Field<out T> {
    data object Unchanged : Field<Nothing>
    data class Set<T>(val value: T) : Field<T>
}

/** A new album. [eventDate] is `yyyy-MM-dd`; [parentId] `null` = root. */
data class AlbumDraft(
    val name: String,
    val description: String,
    val eventDate: String?,
    val parentId: Long?,
)

data class AlbumEdit(
    val albumId: Long,
    val name: Field<String> = Field.Unchanged,
    val description: Field<String> = Field.Unchanged,
    val eventDate: Field<String?> = Field.Unchanged,
    val parentId: Field<Long?> = Field.Unchanged,
)

data class PhotoEdit(
    val photoId: Long,
    val name: Field<String> = Field.Unchanged,
    val description: Field<String> = Field.Unchanged,
    val dateTaken: Field<String?> = Field.Unchanged,
    val albumId: Field<Long> = Field.Unchanged,
)

/** Outcome of a batch move or delete: every photo was attempted (unless access was lost). */
data class BatchResult(
    val succeeded: Int,
    /** One error per photo that failed, in order; the screen groups them by text. */
    val failures: List<AppError>,
) {
    val total: Int get() = succeeded + failures.size
}
