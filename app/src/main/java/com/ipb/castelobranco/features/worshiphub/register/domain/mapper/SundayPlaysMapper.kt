package com.ipb.castelobranco.features.worshiphub.register.domain.mapper

import com.ipb.castelobranco.features.worshiphub.register.presentation.state.SundayRegistrationUiState
import com.ipb.castelobranco.features.worshiphub.register.presentation.state.SundaySongRowState
import com.ipb.castelobranco.core.domain.model.Song
import com.ipb.castelobranco.features.worshiphub.register.domain.model.SundayPlayPushItem
import java.time.LocalDate

fun dateIso(selectedDate: LocalDate?): String =
    selectedDate?.format(SundayRegistrationUiState.Companion.ISO_DATE)?.trim().orEmpty()

fun toSundayPlayItems(rows: List<SundaySongRowState>, availableSongs: List<Song>): List<SundayPlayPushItem> {
    return rows.mapNotNull { row ->
        val hasAnySongInput = row.songQuery.isNotBlank() || row.selectedSongId != null
        val hasTone = row.tone.isNotBlank()
        val bothEmpty = !hasAnySongInput && !hasTone
        if (bothEmpty) return@mapNotNull null

        val sid = row.selectedSongId ?: return@mapNotNull null
        val songExists = availableSongs.any { it.id == sid }
        if (!songExists) return@mapNotNull null

        SundayPlayPushItem(
            songId = sid,
            position = row.position,
            tone = row.tone.trim()
        )
    }
}
