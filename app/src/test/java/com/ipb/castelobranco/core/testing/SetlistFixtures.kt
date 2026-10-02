package com.ipb.castelobranco.core.testing

import com.ipb.castelobranco.core.domain.setlist.SetlistItem
import com.ipb.castelobranco.core.domain.setlist.SundaySetlist
import java.time.LocalDate

/** A setlist for [date] with one item per song id, positions 1..n, tone "G". */
fun setlistOf(date: LocalDate, vararg songIds: Int) = SundaySetlist(
    date = date,
    items = songIds.mapIndexed { index, id ->
        SetlistItem(position = index + 1, songId = id, title = "Song $id", artist = "Artist $id", tone = "G")
    },
    savedByName = "Ana",
    savedAt = "2026-10-01T19:42:10-03:00",
)
