package com.ipb.castelobranco.core.presentation.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.ipb.castelobranco.core.domain.error.AppError
import com.ipb.castelobranco.core.domain.model.Birthday
import com.ipb.castelobranco.core.domain.snapshot.SnapshotState
import com.ipb.castelobranco.core.domain.usecase.GetYearBirthdaysUseCase
import com.ipb.castelobranco.core.presentation.error.toUserMessage
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import timber.log.Timber
import java.time.LocalDate
import java.time.Month
import java.time.format.TextStyle
import java.util.Locale
import javax.inject.Inject

data class BirthdayMonthUi(
    val month: Int,
    val name: String,
    val birthdays: List<Birthday>,
    val isCurrent: Boolean,
)

data class BirthdaysUiState(
    val isLoading: Boolean = false,
    val months: List<BirthdayMonthUi> = emptyList(),
    /** Day of month of today when it falls in [currentMonth]; drives the badge highlight. */
    val today: Int = 0,
    val currentMonth: Int = 1,
    val error: String? = null,
    val showLoginButton: Boolean = false,
)

@HiltViewModel
class BirthdaysViewModel @Inject constructor(
    private val getYearBirthdays: GetYearBirthdaysUseCase,
) : ViewModel() {

    private val _isRefreshing = MutableStateFlow(false)
    val isRefreshing: StateFlow<Boolean> = _isRefreshing.asStateFlow()

    val uiState: StateFlow<BirthdaysUiState> = getYearBirthdays.observe()
        .map { it.toUiState() }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(STOP_TIMEOUT_MS),
            initialValue = getYearBirthdays.current().toUiState(),
        )

    fun refresh() {
        if (_isRefreshing.value) return
        viewModelScope.launch {
            _isRefreshing.value = true
            runCatching { getYearBirthdays.refresh() }
                .onFailure { Timber.w(it, "Failed to refresh year birthdays") }
            _isRefreshing.value = false
        }
    }

    private fun SnapshotState<List<Birthday>>.toUiState(): BirthdaysUiState {
        val now = LocalDate.now()
        val base = BirthdaysUiState(today = now.dayOfMonth, currentMonth = now.monthValue)
        return when (this) {
            is SnapshotState.Loading -> base.copy(isLoading = true)
            is SnapshotState.Data -> base.copy(months = value.groupByMonth(now.monthValue))
            is SnapshotState.Error -> base.copy(
                error = error.toUserMessage(),
                showLoginButton = error.let { it is AppError.Auth && it.code != HTTP_FORBIDDEN },
            )
        }
    }

    private fun List<Birthday>.groupByMonth(currentMonth: Int): List<BirthdayMonthUi> {
        val byMonth = groupBy { it.month }
        return Month.entries.map { month ->
            BirthdayMonthUi(
                month = month.value,
                name = month.getDisplayName(TextStyle.FULL_STANDALONE, PT_BR)
                    .replaceFirstChar { it.titlecase(PT_BR) },
                birthdays = byMonth[month.value].orEmpty().sortedBy { it.day },
                isCurrent = month.value == currentMonth,
            )
        }
    }

    private companion object {
        const val STOP_TIMEOUT_MS = 5_000L
        const val HTTP_FORBIDDEN = 403
        val PT_BR: Locale = Locale.forLanguageTag("pt-BR")
    }
}
