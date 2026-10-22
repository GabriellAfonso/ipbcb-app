package com.ipb.castelobranco.features.admin.schedule.domain.model

data class ScheduleItem(
    val date: String,
    val day: Int,
    val scheduleTypeName: String,
    val scheduleTypeId: Int = 0,
    val selectedMember: Member? = null
)
