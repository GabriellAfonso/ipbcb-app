package com.ipb.castelobranco.features.worshiphub.tables.domain.usecase

import com.ipb.castelobranco.features.worshiphub.tables.domain.model.DraftRow

/** What the server accepts for a setlist: at least one song, and each song with a key of 1 to 3 characters. */
object RepertoireValidation {

    const val MAX_TONE_LENGTH = 3

    fun canSave(rows: List<DraftRow>): Boolean {
        val filled = rows.filter { it.songId != null }
        return filled.isNotEmpty() && filled.all { it.tone.trim().length in 1..MAX_TONE_LENGTH }
    }
}
