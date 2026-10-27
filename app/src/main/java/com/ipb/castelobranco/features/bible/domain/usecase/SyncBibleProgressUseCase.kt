package com.ipb.castelobranco.features.bible.domain.usecase

import com.ipb.castelobranco.features.bible.domain.repository.BibleProgressRepository
import com.ipb.castelobranco.features.bible.domain.repository.ProgressSyncOutcome
import javax.inject.Inject

/**
 * Drains the progress queue chunk by chunk, then fetches the account's state so changes from other devices and the
 * server's conflict decisions show. Returns true when nothing is left to retry; the worker turns false into a
 * retry. Never throws.
 */
class SyncBibleProgressUseCase @Inject constructor(
    private val repository: BibleProgressRepository,
) {

    suspend operator fun invoke(): Boolean {
        var pass = 0
        var sent = false
        while (pass < MAX_PASSES && repository.queuedCount() > 0) {
            when (repository.syncOnce()) {
                ProgressSyncOutcome.Delivered, ProgressSyncOutcome.Discarded -> {
                    sent = true
                    pass++
                }
                is ProgressSyncOutcome.Deferred -> {
                    if (sent) repository.refresh()
                    return false
                }
            }
        }
        if (sent) repository.refresh()
        return repository.queuedCount() == 0
    }

    private companion object {
        /** Bounded so a pathological queue cannot spin a worker forever; the rest goes on the next run. */
        const val MAX_PASSES = 20
    }
}
