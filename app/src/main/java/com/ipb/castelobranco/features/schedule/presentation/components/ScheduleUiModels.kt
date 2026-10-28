package com.ipb.castelobranco.features.schedule.presentation.components

import androidx.compose.runtime.Immutable

@Immutable
data class ScheduleRowUi(
    val day: Int,
    val member: String,
    val memberId: Long? = null,
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

/**
 * The schedule card on the home's green band: the user's own next date ([isOwn]) or, when they have none, the next
 * service and who serves. Already formatted.
 *
 * @param monthLabel "NOV". @param title "Domingo, 15/11 · 18h". @param detail "Faltam 18 dias" or
 * "Escalado: Marcos Vinícius".
 */
@Immutable
data class ScheduleBandUi(
    val isOwn: Boolean,
    val monthLabel: String,
    val day: Int,
    val title: String,
    val detail: String,
)
