package com.ipb.castelobranco.features.schedule.presentation.components

import androidx.compose.runtime.Immutable

@Immutable
data class ScheduleRowUi(
    val day: Int,
    val member: String,
    /** Abbreviated weekday of [day] ("ter"); blank when the date is invalid. */
    val weekday: String = "",
)

@Immutable
data class ScheduleSectionUi(
    val title: String,
    val time: String,
    val rows: List<ScheduleRowUi>,
    /** First word of [title] ("Terça"), used on the tab and in the "next" card. */
    val shortTitle: String = title.trim().substringBefore(' '),
)

/** Section shown on the home highlight; [nextDay] is the upcoming date, null when every date has passed. */
@Immutable
data class NextScheduleUi(
    val section: ScheduleSectionUi,
    val nextDay: Int?
)
