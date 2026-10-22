package com.ipb.castelobranco.features.schedule.domain.repository

import com.ipb.castelobranco.core.domain.snapshot.RefreshResult
import com.ipb.castelobranco.core.domain.snapshot.SnapshotState
import com.ipb.castelobranco.features.schedule.domain.model.MonthSchedule
import kotlinx.coroutines.flow.Flow

interface ScheduleRepository {
    // Agora expõe como Flow, mas a implementação entregará um StateFlow
    fun observeMonthSchedule(): Flow<SnapshotState<MonthSchedule>>

    // Novo: Para pegar o valor síncrono no initialValue da ViewModel
    fun getCurrentSnapshot(): SnapshotState<MonthSchedule>

    // Novo: Para o "aquecimento" no boot do app
    suspend fun preload()

    suspend fun refreshMonthSchedule(): RefreshResult

    suspend fun clearScheduleCache()
}