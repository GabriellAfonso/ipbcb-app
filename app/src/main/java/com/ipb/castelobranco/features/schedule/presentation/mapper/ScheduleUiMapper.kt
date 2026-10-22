package com.ipb.castelobranco.features.schedule.presentation.mapper

import com.ipb.castelobranco.features.schedule.domain.model.MonthSchedule
import com.ipb.castelobranco.features.schedule.domain.model.ScheduleEntry
import com.ipb.castelobranco.features.schedule.presentation.components.ScheduleRowUi
import com.ipb.castelobranco.features.schedule.presentation.components.ScheduleSectionUi
import java.util.Locale


private val SORT_ORDER = listOf("terça", "quinta", "domingo")
private val PT_BR_LOCALE = Locale.forLanguageTag("pt-BR")

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
                        ScheduleRowUi(day = item.day, member = item.member)
                    }
            )
        }
}
