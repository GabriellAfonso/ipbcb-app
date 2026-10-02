package com.ipb.castelobranco.features.worshiphub.tables.domain

import com.ipb.castelobranco.core.testing.setlistOf
import com.ipb.castelobranco.features.worshiphub.tables.domain.model.SaveSetlistResult
import com.ipb.castelobranco.features.worshiphub.tables.domain.model.SetlistEntry
import com.ipb.castelobranco.features.worshiphub.tables.domain.repository.SetlistSaveRepository
import java.time.LocalDate

class FakeSetlistSaveRepository(var result: SaveSetlistResult? = null) : SetlistSaveRepository {

    val calls = mutableListOf<Pair<LocalDate, List<SetlistEntry>>>()

    /** Without a preset [result], answers as the server would: the setlist as saved. */
    override suspend fun save(date: LocalDate, entries: List<SetlistEntry>): SaveSetlistResult {
        calls += date to entries
        return result ?: SaveSetlistResult.Saved(setlistOf(date, *entries.map { it.songId }.toIntArray()))
    }
}
