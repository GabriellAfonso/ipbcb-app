package com.ipb.castelobranco.features.worshiphub.tables.domain.usecase

import com.ipb.castelobranco.core.domain.setlist.SundaySetlistRepository
import com.ipb.castelobranco.features.worshiphub.tables.domain.model.DraftRow
import com.ipb.castelobranco.features.worshiphub.tables.domain.model.SaveSetlistResult
import com.ipb.castelobranco.features.worshiphub.tables.domain.model.SetlistEntry
import com.ipb.castelobranco.features.worshiphub.tables.domain.repository.SetlistSaveRepository
import java.time.LocalDate
import javax.inject.Inject

/**
 * Sends the filled rows, each in its own position, as the setlist of [date]. On success the device's
 * Sunday setlist becomes the saved one at once, without waiting for the push.
 */
class SaveSundaySetlistUseCase @Inject constructor(
    private val repository: SetlistSaveRepository,
    private val sundaySetlist: SundaySetlistRepository,
) {
    suspend operator fun invoke(date: LocalDate, rows: List<DraftRow>): SaveSetlistResult {
        val entries = rows.mapNotNull { row ->
            row.songId?.let { SetlistEntry(position = row.position, songId = it, tone = row.tone.trim()) }
        }
        val result = repository.save(date, entries)
        if (result is SaveSetlistResult.Saved) sundaySetlist.store(result.setlist)
        return result
    }
}
