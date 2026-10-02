package com.ipb.castelobranco.features.worshiphub.tables.domain.usecase

import com.ipb.castelobranco.core.domain.util.WallClock
import com.ipb.castelobranco.features.worshiphub.tables.domain.model.DraftRow
import com.ipb.castelobranco.features.worshiphub.tables.domain.model.RepertoireDraft
import com.ipb.castelobranco.features.worshiphub.tables.domain.repository.RepertoireDraftRepository
import javax.inject.Inject

/**
 * The draft to restore: `null` when there is none or it expired (an expired draft is deleted). A row
 * whose song is not in the catalog comes back empty.
 */
class RestoreRepertoireDraftUseCase @Inject constructor(
    private val repository: RepertoireDraftRepository,
    private val clock: WallClock,
) {
    suspend operator fun invoke(catalogSongIds: Set<Int>): List<DraftRow>? {
        val draft = repository.load() ?: return null
        if (DraftExpiry.isExpired(draft.updatedAtMillis, clock.nowMillis())) {
            repository.clear()
            return null
        }
        return draft.rows.map { row ->
            if (row.songId == null || row.songId in catalogSongIds) row
            else DraftRow(position = row.position, songId = null, tone = "", isFixed = false)
        }
    }
}

/** Stores the rows as changed now, which renews the hour. */
class SaveRepertoireDraftUseCase @Inject constructor(
    private val repository: RepertoireDraftRepository,
    private val clock: WallClock,
) {
    suspend operator fun invoke(rows: List<DraftRow>) {
        repository.save(RepertoireDraft(rows = rows, updatedAtMillis = clock.nowMillis()))
    }
}

class ClearRepertoireDraftUseCase @Inject constructor(
    private val repository: RepertoireDraftRepository,
) {
    suspend operator fun invoke() = repository.clear()
}
