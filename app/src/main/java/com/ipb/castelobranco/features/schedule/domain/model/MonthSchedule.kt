package com.ipb.castelobranco.features.schedule.domain.model

data class MonthSchedule(
    val year: Int,
    val month: Int,
    val schedule: Map<String, ScheduleEntry>
)

data class ScheduleEntry(
    val time: String,
    val items: List<ScheduleItem>
)

data class ScheduleItem(
    val day: Int,
    val member: String,
    /** The member record's id; matches `MeProfile.memberId` on the user's own rows. */
    val memberId: Long? = null,
)
