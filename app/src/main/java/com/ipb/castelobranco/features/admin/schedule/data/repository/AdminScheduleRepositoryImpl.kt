package com.ipb.castelobranco.features.admin.schedule.data.repository

import com.ipb.castelobranco.features.admin.schedule.data.api.AdminScheduleApi
import com.ipb.castelobranco.features.admin.schedule.data.dto.GenerateScheduleRequestDto
import com.ipb.castelobranco.features.admin.schedule.data.dto.SaveScheduleItemDto
import com.ipb.castelobranco.features.admin.schedule.data.dto.SaveScheduleRequestDto
import com.ipb.castelobranco.features.admin.schedule.data.mapper.toDomain
import com.ipb.castelobranco.features.admin.schedule.domain.model.Member
import com.ipb.castelobranco.features.admin.schedule.domain.model.ScheduleItem
import com.ipb.castelobranco.features.admin.schedule.domain.model.ScheduleType
import com.ipb.castelobranco.features.admin.schedule.domain.repository.AdminScheduleRepository
import com.ipb.castelobranco.core.domain.error.mapError
import com.ipb.castelobranco.core.network.error.toAppError
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class AdminScheduleRepositoryImpl @Inject constructor(
    private val api: AdminScheduleApi
) : AdminScheduleRepository {
    override suspend fun getMembers(): Result<List<Member>> = runCatching {
        api.getMembers().members.map { it.toDomain() }
    }.mapError()

    override suspend fun generateSchedule(year: Int, month: Int): Result<List<ScheduleItem>> = runCatching {
        val response = api.generateSchedule(
            GenerateScheduleRequestDto(year = year, month = month)
        )
        response.items.map { item ->
            ScheduleItem(
                date = item.date,
                day = item.day,
                scheduleTypeName = item.scheduleType.name,
                scheduleTypeId = ScheduleType.idByName(item.scheduleType.name),
                selectedMember = Member(item.member.id, item.member.name)
            )
        }
    }.mapError()

    override suspend fun saveSchedule(year: Int, month: Int, items: List<ScheduleItem>): Result<Unit> = runCatching {
        val body = SaveScheduleRequestDto(
            year = year,
            month = month,
            items = items.map { item ->
                SaveScheduleItemDto(
                    date = item.date,
                    scheduleTypeId = item.scheduleTypeId,
                    memberId = item.selectedMember!!.id
                )
            }
        )
        val response = api.saveSchedule(body)
        if (!response.isSuccessful) {
            throw response.toAppError()
        }
    }.mapError()
}