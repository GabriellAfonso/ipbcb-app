package com.ipb.castelobranco.core.domain.setlist

import com.ipb.castelobranco.core.domain.auth.SessionPresenceProvider
import com.ipb.castelobranco.core.domain.worship.WorshipAccessRepository
import javax.inject.Inject

/**
 * Brings the device's Sunday setlist in line with the server: only worship members with a session
 * hold one. Used on start, on return to the foreground and on a `setlist_saved` push.
 */
class SyncSundaySetlistUseCase @Inject constructor(
    private val repository: SundaySetlistRepository,
    private val sessionPresence: SessionPresenceProvider,
    private val worshipAccess: WorshipAccessRepository,
) {
    suspend operator fun invoke(): Result<Unit> {
        val eligible = sessionPresence.isLoggedIn() && worshipAccess.current().isWorshipMember
        if (!eligible) {
            repository.clear()
            return Result.success(Unit)
        }
        return repository.refreshCurrent()
    }
}
