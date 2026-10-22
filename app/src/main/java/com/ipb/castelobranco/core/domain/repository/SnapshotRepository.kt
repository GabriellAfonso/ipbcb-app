package com.ipb.castelobranco.core.domain.repository

import kotlinx.coroutines.flow.Flow

interface SnapshotRepository<T> {
    fun observe(): Flow<T?>
    suspend fun refresh(): Boolean
}