package com.ipb.castelobranco.features.worshiphub.tables.domain

import com.ipb.castelobranco.features.worshiphub.tables.domain.model.RepertoireDraft
import com.ipb.castelobranco.features.worshiphub.tables.domain.repository.RepertoireDraftRepository

class FakeRepertoireDraftRepository(var stored: RepertoireDraft? = null) : RepertoireDraftRepository {

    val saves = mutableListOf<RepertoireDraft>()
    var clearCalls = 0
        private set

    override suspend fun load(): RepertoireDraft? = stored

    override suspend fun save(draft: RepertoireDraft) {
        saves += draft
        stored = draft
    }

    override suspend fun clear() {
        clearCalls++
        stored = null
    }
}
