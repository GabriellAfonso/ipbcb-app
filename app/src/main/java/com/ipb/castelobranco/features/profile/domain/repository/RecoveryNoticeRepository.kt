package com.ipb.castelobranco.features.profile.domain.repository

import kotlinx.coroutines.flow.Flow

/** Whether the one-time recovery e-mail notice was answered on this device. Survives logout on purpose. */
interface RecoveryNoticeRepository {
    val answered: Flow<Boolean>

    suspend fun markAnswered()
}
