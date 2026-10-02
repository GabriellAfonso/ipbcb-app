package com.ipb.castelobranco.features.worshiphub.tables.domain.repository

import com.ipb.castelobranco.features.worshiphub.tables.domain.model.RepertoireDraft

interface RepertoireDraftRepository {

    /** The stored draft, or `null` when there is none or it cannot be read. */
    suspend fun load(): RepertoireDraft?

    suspend fun save(draft: RepertoireDraft)

    suspend fun clear()
}
