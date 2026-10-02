package com.ipb.castelobranco.core.domain.setlist

import java.time.LocalDate

/**
 * The songs chosen for one Sunday's service, as the server stores them
 * (`backend/specs/017-sunday-setlist-push`).
 *
 * @param items ordered by [SetlistItem.position].
 * @param savedByName the author's display name; `null` when the author account was deleted.
 * @param savedAt ISO 8601 instant of the last save, as received.
 */
data class SundaySetlist(
    val date: LocalDate,
    val items: List<SetlistItem>,
    val savedByName: String?,
    val savedAt: String,
)

data class SetlistItem(
    val position: Int,
    val songId: Int,
    val title: String,
    val artist: String,
    val tone: String,
)
