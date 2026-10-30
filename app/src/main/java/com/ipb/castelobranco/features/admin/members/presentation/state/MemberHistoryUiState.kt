package com.ipb.castelobranco.features.admin.members.presentation.state

data class MemberHistoryUiState(
    val isLoading: Boolean = true,
    val error: String? = null,
    val lines: List<HistoryLineUi> = emptyList(),
    /** For the band; null when the members list is not in memory. */
    val memberName: String? = null,
) {
    /** Consecutive lines of the same day, newest first like the lines. */
    val days: List<HistoryDayUi>
        get() = lines.fold(mutableListOf<HistoryDayUi>()) { days, line ->
            val last = days.lastOrNull()
            if (last?.day == line.day) {
                days[days.lastIndex] = last.copy(lines = last.lines + line)
            } else {
                days += HistoryDayUi(line.day, listOf(line))
            }
            days
        }

    /** "5 alterações desde 14/03/2026": the oldest line is the last one. */
    val summary: String?
        get() = lines.lastOrNull()?.let { oldest ->
            val count = if (lines.size == 1) "1 alteração" else "${lines.size} alterações"
            "$count desde ${oldest.day}"
        }
}

data class HistoryLineUi(
    val editor: String,
    val text: String,
    /** "25/09/2026 14:05". */
    val time: String,
    val initials: String = "",
    val isEditorUnknown: Boolean = false,
    /** "alterou Situação"; the values go in [oldValue] → [newValue]. */
    val action: String = text,
    val oldValue: String? = null,
    val newValue: String? = null,
    /** "25/09/2026" and "14:05". */
    val day: String = time.substringBefore(' '),
    val hour: String = time.substringAfter(' '),
)

data class HistoryDayUi(val day: String, val lines: List<HistoryLineUi>)
