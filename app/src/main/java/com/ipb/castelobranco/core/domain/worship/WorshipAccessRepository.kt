package com.ipb.castelobranco.core.domain.worship

import kotlinx.coroutines.flow.Flow

/**
 * The user's worship ministry flags. Lives in core so worship hub and push code read them without
 * importing the profile feature, which implements it.
 */
interface WorshipAccessRepository {

    /** [WorshipAccess.NONE] while there is no profile data. Emits only on change. */
    val worshipAccess: Flow<WorshipAccess>

    /**
     * The flags now, loading the stored profile from disk when it is not in memory yet — a process
     * started by a push or a worker never ran the boot preload.
     */
    suspend fun current(): WorshipAccess
}
