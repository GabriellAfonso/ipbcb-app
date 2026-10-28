package com.ipb.castelobranco.features.worshiphub.register.presentation.util

import com.ipb.castelobranco.features.worshiphub.register.presentation.state.SundaySongRowState
import com.ipb.castelobranco.core.domain.model.Song

fun selectSong(
    rows: List<SundaySongRowState>,
    position: Int,
    song: Song
): List<SundaySongRowState> {
    val label = SongLabelFormatter.format(song)
    return rows.map { row ->
        if (row.position == position) row.copy(songQuery = label, selectedSongId = song.id) else row
    }
}

fun updateTone(
    rows: List<SundaySongRowState>,
    position: Int,
    tone: String
): List<SundaySongRowState> = rows.map { row ->
    if (row.position == position) row.copy(tone = tone) else row
}

fun addRow(rows: List<SundaySongRowState>): List<SundaySongRowState> {
    val nextPos = (rows.maxOfOrNull { it.position } ?: 0) + 1
    return rows + SundaySongRowState(position = nextPos)
}

/** Any row can go while another stays; the rest are renumbered 1..n. */
fun removeRow(rows: List<SundaySongRowState>, position: Int): List<SundaySongRowState> {
    if (rows.size <= 1 || rows.none { it.position == position }) return rows
    return rows.filterNot { it.position == position }
        .mapIndexed { index, row -> row.copy(position = index + 1) }
}
