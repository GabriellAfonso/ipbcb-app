package com.ipb.castelobranco.core.domain.setlist

import kotlinx.coroutines.flow.Flow

/**
 * The device's copy of the current Sunday setlist, for worship members. Lives in core because it is
 * written by push handling, the foreground refresh and the worship hub's save, and read by the worship
 * hub's lists.
 */
interface SundaySetlistRepository {

    /** The stored setlist, whatever its date; `null` when none. Available offline. */
    val stored: Flow<SundaySetlist?>

    /**
     * Reads the current setlist from the server and stores it. "No current setlist" and a `403` (not a
     * worship member any more) clear the stored copy; any other failure keeps it.
     */
    suspend fun refreshCurrent(): Result<Unit>

    /** Replaces the stored copy, e.g. with the answer of a successful save. */
    suspend fun store(setlist: SundaySetlist)

    suspend fun clear()
}
