package com.ipb.castelobranco.features.worshiphub.shared.domain

import com.ipb.castelobranco.core.domain.setlist.SundaySetlist
import java.time.LocalDate

/** The "Repertório de domingo" section of a song content list (lyrics or chord charts). */
data class SundaySection(
    val date: LocalDate,
    val entries: List<SundaySectionEntry>,
) {
    val songIds: Set<Int> get() = entries.mapTo(mutableSetOf()) { it.songId }
}

/** @param contentId the lyrics or chord chart opened on tap. */
data class SundaySectionEntry(
    val contentId: Int,
    val songId: Int,
    val title: String,
    val tone: String,
)

/**
 * The setlist's songs, in position order, that have content in this list ([contentBySongId] maps a
 * song to its lyrics or chord chart). `null` when there is no setlist or none of its songs has content.
 */
fun buildSundaySection(setlist: SundaySetlist?, contentBySongId: Map<Int, Int>): SundaySection? {
    if (setlist == null) return null
    val entries = setlist.items
        .sortedBy { it.position }
        .mapNotNull { item ->
            contentBySongId[item.songId]?.let { contentId ->
                SundaySectionEntry(contentId = contentId, songId = item.songId, title = item.title, tone = item.tone)
            }
        }
    return entries.takeIf { it.isNotEmpty() }?.let { SundaySection(setlist.date, it) }
}
