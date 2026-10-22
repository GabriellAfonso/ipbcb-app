package com.ipb.castelobranco.features.admin.schedule.presentation.state

import com.ipb.castelobranco.features.admin.schedule.domain.model.ScheduleType
import java.time.DayOfWeek
import java.time.LocalDate

object AdminScheduleSkeleton {

    private val DAY_TO_TYPE = mapOf(
        DayOfWeek.TUESDAY to ScheduleType.TUESDAY_PRAYER,
        DayOfWeek.THURSDAY to ScheduleType.THURSDAY_PRAYER,
        DayOfWeek.SUNDAY to ScheduleType.SUNDAY_LITURGY,
    )

    fun build(year: Int, month: Int): List<EditableScheduleUiState> {
        val firstDay = LocalDate.of(year, month, 1)
        return (1..firstDay.lengthOfMonth()).mapNotNull { day ->
            val date = LocalDate.of(year, month, day)
            val type = DAY_TO_TYPE[date.dayOfWeek] ?: return@mapNotNull null
            EditableScheduleUiState(
                date = date.toString(),
                day = day,
                scheduleTypeName = type.displayName,
                scheduleTypeId = type.id,
                selectedMember = null
            )
        }
    }
}
