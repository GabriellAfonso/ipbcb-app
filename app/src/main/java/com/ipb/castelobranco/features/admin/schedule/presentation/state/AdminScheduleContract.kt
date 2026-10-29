package com.ipb.castelobranco.features.admin.schedule.presentation.state

import androidx.compose.runtime.Immutable
import com.ipb.castelobranco.features.admin.schedule.domain.model.Member
import java.time.LocalDate
import java.time.temporal.ChronoUnit

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
    /** This month was saved and not touched since: "Salva", and the button turns into "Compartilhar". */
    val isSaved: Boolean = false,
    val showSaveConfirmation: Boolean = false,
    val saveResult: SaveResult? = null,
    val snackbarMessage: String? = null
) {
    val canSave: Boolean
        get() = items.isNotEmpty() && items.all { it.selectedMember != null }

    val canShare: Boolean
        get() = isSaved && !hasUnsavedChanges && canSave

    val filledCount: Int
        get() = items.count { it.selectedMember != null }

    /** How many services each member leads this month, by member id. */
    val countsByMember: Map<Int, Int>
        get() = items.mapNotNull { it.selectedMember?.id }.groupingBy { it }.eachCount()

    /** The member at [index] also leads another service up to 3 days apart. */
    fun isRepeatedThisWeek(index: Int): Boolean {
        val item = items.getOrNull(index) ?: return false
        val memberId = item.selectedMember?.id ?: return false
        val date = item.localDate() ?: return false
        return items.withIndex().any { (other, it) ->
            other != index && it.selectedMember?.id == memberId &&
                it.localDate()?.let { d -> kotlin.math.abs(ChronoUnit.DAYS.between(date, d)) <= SAME_WEEK_DAYS } == true
        }
    }

    val monthLabel: String
        get() = MONTHS[month - 1] + " $year"

    companion object {
        private const val SAME_WEEK_DAYS = 3L
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
    data class MemberCleared(val itemIndex: Int) : AdminScheduleEvent
    data object GenerateSchedule : AdminScheduleEvent
    data object SaveRequested : AdminScheduleEvent
    data object SaveConfirmationDismissed : AdminScheduleEvent
    data object SaveSchedule : AdminScheduleEvent
    data object SaveResultDismissed : AdminScheduleEvent
    data object SnackbarShown : AdminScheduleEvent
}
