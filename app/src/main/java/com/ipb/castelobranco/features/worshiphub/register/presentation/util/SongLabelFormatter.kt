package com.ipb.castelobranco.features.worshiphub.register.presentation.util

import com.ipb.castelobranco.core.domain.model.Song

object SongLabelFormatter {
    fun format(song: Song): String =
        if (song.artist.isBlank()) song.title else "${song.title} [${song.artist}]"
}