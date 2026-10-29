package com.ipb.castelobranco.features.admin.schedule.presentation.state

import com.ipb.castelobranco.features.admin.schedule.domain.model.Member
import java.time.LocalDate
import java.time.format.DateTimeParseException

data class EditableScheduleUiState(
    val date: String,
    val day: Int,
    val scheduleTypeName: String,
    val scheduleTypeId: Int = 0,
    val selectedMember: Member? = null
) {
    /** The service's date, `null` when the server sent something unreadable. */
    fun localDate(): LocalDate? = try {
        LocalDate.parse(date.take(ISO_DATE_LENGTH))
    } catch (_: DateTimeParseException) {
        null
    }

    private companion object {
        const val ISO_DATE_LENGTH = 10
    }
}
