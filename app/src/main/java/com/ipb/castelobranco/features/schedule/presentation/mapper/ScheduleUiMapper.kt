package com.ipb.castelobranco.features.schedule.presentation.mapper

import com.ipb.castelobranco.features.schedule.domain.model.MonthSchedule
import com.ipb.castelobranco.features.schedule.domain.model.ScheduleEntry
import com.ipb.castelobranco.features.schedule.presentation.components.ScheduleRowUi
import com.ipb.castelobranco.features.schedule.presentation.components.ScheduleSectionUi
import java.time.LocalDate
import java.time.format.TextStyle
import java.util.Locale


private val SORT_ORDER = listOf("terça", "quinta", "domingo")
private val PT_BR_LOCALE = Locale.forLanguageTag("pt-BR")
private const val WEEKDAY_ABBR_LENGTH = 3

fun MonthSchedule.toSectionsUi(): List<ScheduleSectionUi> {
    return schedule.entries
        .map { entry ->
            val titleLower = entry.key.trim().lowercase(PT_BR_LOCALE)
            val weight = SORT_ORDER.indexOfFirst { titleLower.startsWith(it) }
                .let { if (it == -1) Int.MAX_VALUE else it }
            weight to entry
        }
        .sortedWith(
            compareBy<Pair<Int, Map.Entry<String, ScheduleEntry>>> { it.first }
                .thenBy { it.second.key.lowercase(PT_BR_LOCALE) }
        )
        .map { (weight, entry) ->
            val scheduleEntry = entry.value
            ScheduleSectionUi(
                title = entry.key,
                time = scheduleEntry.time,
                rows = scheduleEntry.items
                    .sortedBy { it.day }
                    .map { item ->
                        ScheduleRowUi(
                            day = item.day,
                            member = item.member,
                            weekday = weekdayAbbr(year, month, item.day),
                        )
                    }
            )
        }
}

/** "ter", "qui", "dom"… — blank when the date does not exist. */
internal fun weekdayAbbr(year: Int, month: Int, day: Int): String =
    runCatching {
        LocalDate.of(year, month, day).dayOfWeek
            .getDisplayName(TextStyle.SHORT, PT_BR_LOCALE)
            .lowercase(PT_BR_LOCALE)
            .removeSuffix(".")
            .take(WEEKDAY_ABBR_LENGTH)
    }.getOrDefault("")
