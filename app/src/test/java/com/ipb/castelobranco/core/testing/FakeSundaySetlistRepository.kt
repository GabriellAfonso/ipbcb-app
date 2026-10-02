package com.ipb.castelobranco.core.testing

import com.ipb.castelobranco.core.domain.setlist.SundaySetlist
import com.ipb.castelobranco.core.domain.setlist.SundaySetlistRepository
import kotlinx.coroutines.flow.MutableStateFlow

class FakeSundaySetlistRepository(initial: SundaySetlist? = null) : SundaySetlistRepository {

    val state = MutableStateFlow(initial)
    var refreshResult: Result<Unit> = Result.success(Unit)
    var refreshCalls = 0
        private set
    var clearCalls = 0
        private set

    override val stored = state

    override suspend fun refreshCurrent(): Result<Unit> {
        refreshCalls++
        return refreshResult
    }

    override suspend fun store(setlist: SundaySetlist) {
        state.value = setlist
    }

    override suspend fun clear() {
        clearCalls++
        state.value = null
    }
}
