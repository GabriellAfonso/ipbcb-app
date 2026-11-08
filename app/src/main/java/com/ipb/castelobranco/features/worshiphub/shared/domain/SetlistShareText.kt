package com.ipb.castelobranco.features.worshiphub.shared.domain

import java.time.LocalDate
import java.time.format.DateTimeFormatter

private val SHARE_DATE: DateTimeFormatter = DateTimeFormatter.ofPattern("dd/MM/yy")

/** One song of a shared setlist; [tone] may be blank. */
data class SetlistShareSong(val title: String, val artist: String, val tone: String)

/** The WhatsApp-formatted setlist text, the same from the Repertório tab and the hub's band. */
fun setlistShareText(date: LocalDate, songs: List<SetlistShareSong>): String {
    val header = "*Louvor — ${date.format(SHARE_DATE)}*"
    val lines = songs.joinToString(separator = "\n\n") { song ->
        val title = if (song.tone.isBlank()) "*${song.title}*" else "*${song.title} (${song.tone})*"
        "$title\n_- ${song.artist}_"
    }
    return if (lines.isBlank()) header else "$header\n\n$lines"
}
