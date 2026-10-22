package com.ipb.castelobranco.features.admin.schedule.domain.repository

import com.ipb.castelobranco.features.admin.schedule.domain.model.Member
import com.ipb.castelobranco.features.admin.schedule.domain.model.ScheduleItem

interface AdminScheduleRepository {
    suspend fun getMembers(): Result<List<Member>>
    suspend fun generateSchedule(year: Int, month: Int): Result<List<ScheduleItem>>
    suspend fun saveSchedule(year: Int, month: Int, items: List<ScheduleItem>): Result<Unit>
}