package com.ipb.castelobranco.features.admin.schedule.presentation.state

import com.ipb.castelobranco.features.admin.schedule.domain.model.Member

data class EditableScheduleUiState(
    val date: String,
    val day: Int,
    val scheduleTypeName: String,
    val scheduleTypeId: Int = 0,
    val selectedMember: Member? = null
)
