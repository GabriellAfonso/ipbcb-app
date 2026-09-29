package com.ipb.castelobranco.core.domain.access

import kotlinx.coroutines.flow.Flow

/**
 * Source of the user's [Access]. Lives in core so the panel, the admin areas and the worship hub
 * read it without importing the profile feature, which implements it.
 */
interface AccessRepository {

    /** Current access, [Access.NONE] whenever there is no profile data. Emits only on change. */
    val access: Flow<Access>

    /** Reads the profile again. Single-flight: a call made while one is running returns at once. */
    suspend fun refresh()
}
