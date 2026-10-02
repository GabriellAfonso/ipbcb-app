package com.ipb.castelobranco.features.worshiphub.tables.domain.model

/**
 * The Repertório rows as the user left them on this device. Not related to any server setlist.
 *
 * @param updatedAtMillis wall-clock time of the last change made by the user.
 */
data class RepertoireDraft(
    val rows: List<DraftRow>,
    val updatedAtMillis: Long,
)

/** One position of the draft; [songId] is `null` for an empty row. */
data class DraftRow(
    val position: Int,
    val songId: Int?,
    val tone: String,
    val isFixed: Boolean,
)
