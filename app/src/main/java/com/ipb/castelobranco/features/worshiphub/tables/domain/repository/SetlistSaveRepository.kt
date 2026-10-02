package com.ipb.castelobranco.features.worshiphub.tables.domain.repository

import com.ipb.castelobranco.features.worshiphub.tables.domain.model.SaveSetlistResult
import com.ipb.castelobranco.features.worshiphub.tables.domain.model.SetlistEntry
import java.time.LocalDate

interface SetlistSaveRepository {

    /** Saves [entries] as the setlist of [date], replacing any setlist already saved for it. */
    suspend fun save(date: LocalDate, entries: List<SetlistEntry>): SaveSetlistResult
}
