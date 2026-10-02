package com.ipb.castelobranco.core.data.setlist

import com.ipb.castelobranco.core.domain.error.AppError
import com.ipb.castelobranco.core.domain.setlist.SetlistItem
import com.ipb.castelobranco.core.domain.setlist.SundaySetlist
import java.time.LocalDate
import java.time.format.DateTimeParseException

/**
 * Shared by every reader of the Setlist object (core, worship hub, admin).
 *
 * @throws AppError.Unknown when the date is not `YYYY-MM-DD` — a setlist without a usable date is never
 * stored or shown.
 */
fun SetlistDto.toDomain(): SundaySetlist {
    val parsedDate = try {
        LocalDate.parse(date)
    } catch (e: DateTimeParseException) {
        throw AppError.Unknown(message = "Invalid setlist date: $date", cause = e)
    }
    return SundaySetlist(
        date = parsedDate,
        items = items
            .sortedBy { it.position }
            .map { SetlistItem(it.position, it.songId, it.title, it.artist, it.tone) },
        savedByName = savedByName,
        savedAt = savedAt,
    )
}

fun SundaySetlist.toDto(): SetlistDto = SetlistDto(
    date = date.toString(),
    items = items.map { SetlistItemDto(it.position, it.songId, it.title, it.artist, it.tone) },
    savedByName = savedByName,
    savedAt = savedAt,
)
