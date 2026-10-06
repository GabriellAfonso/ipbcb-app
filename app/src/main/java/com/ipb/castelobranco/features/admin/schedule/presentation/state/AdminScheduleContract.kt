package com.ipb.castelobranco.features.admin.schedule.presentation.state

import androidx.compose.runtime.Immutable
import com.ipb.castelobranco.features.admin.schedule.domain.model.Member
import java.time.LocalDate

@Immutable
data class AdminScheduleUiState(
    val year: Int = defaultDate().year,
    val month: Int = defaultDate().monthValue,
    val members: List<Member> = emptyList(),
    val isLoadingMembers: Boolean = false,
    val items: List<EditableScheduleUiState> = emptyList(),
    val isGenerating: Boolean = false,
    val isSaving: Boolean = false,
    val hasUnsavedChanges: Boolean = false,
    val showSaveConfirmation: Boolean = false,
    val saveResult: SaveResult? = null,
    val snackbarMessage: String? = null
) {
    val canSave: Boolean
        get() = items.isNotEmpty() && items.all { it.selectedMember != null }

    val canShare: Boolean
        get() = !hasUnsavedChanges && canSave

    val monthLabel: String
        get() = MONTHS[month - 1] + " $year"

    companion object {
        private val MONTHS = listOf(
            "Janeiro", "Fevereiro", "Março", "Abril", "Maio", "Junho",
            "Julho", "Agosto", "Setembro", "Outubro", "Novembro", "Dezembro"
        )

        fun defaultDate(today: LocalDate = LocalDate.now()): LocalDate {
            val daysUntilNextMonth = today.withDayOfMonth(1).plusMonths(1).toEpochDay() - today.toEpochDay()
            return if (daysUntilNextMonth < 10) today.plusMonths(1) else today
        }
    }
}

sealed interface SaveResult {
    data object Success : SaveResult
    data class Error(val message: String) : SaveResult
}

sealed interface AdminScheduleEvent {
    data object LoadMembers : AdminScheduleEvent
    data class MonthChanged(val year: Int, val month: Int) : AdminScheduleEvent
    data class MemberSelected(val itemIndex: Int, val member: Member) : AdminScheduleEvent
    data object GenerateSchedule : AdminScheduleEvent
    data object SaveRequested : AdminScheduleEvent
    data object SaveConfirmationDismissed : AdminScheduleEvent
    data object SaveSchedule : AdminScheduleEvent
    data object SaveResultDismissed : AdminScheduleEvent
    data object SnackbarShown : AdminScheduleEvent
}
