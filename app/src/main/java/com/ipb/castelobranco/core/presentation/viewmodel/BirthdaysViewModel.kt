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
import java.time.Year
import java.time.format.TextStyle
import java.util.Locale
import javax.inject.Inject

/** One line of a month card; [isPast] dims a birthday already gone this year. */
data class BirthdayDayUi(
    val name: String,
    val day: Int,
    val isPast: Boolean,
    val isToday: Boolean,
)

data class BirthdayMonthUi(
    val month: Int,
    val name: String,
    val birthdays: List<BirthdayDayUi>,
    val isCurrent: Boolean,
)

/** A chip in the band: "02/11 · seg" and the first name. */
data class UpcomingBirthdayUi(val dateLabel: String, val firstName: String)

/** The green band over the grid: today's date, who has a birthday today and the next ones. */
data class BirthdaysHeaderUi(
    val dateLabel: String,
    val todayNames: List<String>,
    val upcoming: List<UpcomingBirthdayUi>,
)

data class BirthdaysUiState(
    val isLoading: Boolean = false,
    val months: List<BirthdayMonthUi> = emptyList(),
    val header: BirthdaysHeaderUi? = null,
    /** Day of month of today when it falls in [currentMonth]; drives the badge highlight. */
    val today: Int = 0,
    val currentMonth: Int = 1,
    val error: String? = null,
    val showLoginButton: Boolean = false,
    /** Signed in without member access (403): the screen offers an access request. */
    val isMembersOnly: Boolean = false,
)

@HiltViewModel
class BirthdaysViewModel @Inject constructor(
    private val getYearBirthdays: GetYearBirthdaysUseCase,
) : ViewModel() {

    private val _isRefreshing = MutableStateFlow(false)
    val isRefreshing: StateFlow<Boolean> = _isRefreshing.asStateFlow()

    val uiState: StateFlow<BirthdaysUiState> = getYearBirthdays.observe()
        .map { it.toUiState(LocalDate.now()) }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(STOP_TIMEOUT_MS),
            initialValue = getYearBirthdays.current().toUiState(LocalDate.now()),
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

    private fun SnapshotState<List<Birthday>>.toUiState(now: LocalDate): BirthdaysUiState {
        val base = BirthdaysUiState(today = now.dayOfMonth, currentMonth = now.monthValue)
        return when (this) {
            is SnapshotState.Loading -> base.copy(isLoading = true)
            is SnapshotState.Data -> base.copy(
                months = value.groupByMonth(now),
                header = birthdaysHeader(value, now),
            )
            is SnapshotState.Error -> base.copy(
                error = error.toUserMessage(),
                showLoginButton = error.let { it is AppError.Auth && it.code != HTTP_FORBIDDEN },
                isMembersOnly = error.let { it is AppError.Auth && it.code == HTTP_FORBIDDEN },
            )
        }
    }

    private fun List<Birthday>.groupByMonth(now: LocalDate): List<BirthdayMonthUi> {
        val byMonth = groupBy { it.month }
        return Month.entries.map { month ->
            val isCurrent = month.value == now.monthValue
            BirthdayMonthUi(
                month = month.value,
                name = month.getDisplayName(TextStyle.FULL_STANDALONE, PT_BR)
                    .replaceFirstChar { it.titlecase(PT_BR) },
                birthdays = byMonth[month.value].orEmpty().sortedBy { it.day }.map { birthday ->
                    BirthdayDayUi(
                        name = birthday.name,
                        day = birthday.day,
                        isPast = month.value < now.monthValue || (isCurrent && birthday.day < now.dayOfMonth),
                        isToday = isCurrent && birthday.day == now.dayOfMonth,
                    )
                },
                isCurrent = isCurrent,
            )
        }
    }

    private companion object {
        const val STOP_TIMEOUT_MS = 5_000L
        const val HTTP_FORBIDDEN = 403
    }
}

private val PT_BR: Locale = Locale.forLanguageTag("pt-BR")
private const val UPCOMING_COUNT = 4
private const val WEEKDAY_SUFFIX = "-feira"

/**
 * The band: "Sexta, 30 de outubro", today's names and the next [UPCOMING_COUNT] birthdays after today, wrapping
 * into next year after December.
 */
internal fun birthdaysHeader(birthdays: List<Birthday>, today: LocalDate): BirthdaysHeaderUi {
    val (todays, others) = birthdays.partition { it.occurrenceIn(today.year) == today }
    val upcoming = others
        .mapNotNull { birthday -> birthday.nextOccurrence(today)?.let { birthday to it } }
        .sortedWith(compareBy({ it.second }, { it.first.name }))
        .take(UPCOMING_COUNT)
        .map { (birthday, date) ->
            UpcomingBirthdayUi(
                dateLabel = "%02d/%02d · %s".format(date.dayOfMonth, date.monthValue, shortWeekday(date)),
                firstName = birthday.name.substringBefore(' '),
            )
        }
    val weekday = today.dayOfWeek.getDisplayName(TextStyle.FULL, PT_BR).removeSuffix(WEEKDAY_SUFFIX)
        .replaceFirstChar { it.titlecase(PT_BR) }
    val month = today.month.getDisplayName(TextStyle.FULL, PT_BR).lowercase(PT_BR)
    return BirthdaysHeaderUi("$weekday, ${today.dayOfMonth} de $month", todays.map { it.name }, upcoming)
}

/** The date in [year]; Feb 29 falls on Feb 28 in a common year. `null` for an invalid month/day. */
private fun Birthday.occurrenceIn(year: Int): LocalDate? = runCatching {
    val month = Month.of(month)
    LocalDate.of(year, month, day.coerceAtMost(month.length(Year.isLeap(year.toLong()))))
}.getOrNull()

/** The next date strictly after [today]. */
private fun Birthday.nextOccurrence(today: LocalDate): LocalDate? {
    val thisYear = occurrenceIn(today.year) ?: return null
    return if (thisYear.isAfter(today)) thisYear else occurrenceIn(today.year + 1)
}

private fun shortWeekday(date: LocalDate): String =
    date.dayOfWeek.getDisplayName(TextStyle.SHORT, PT_BR).lowercase(PT_BR).removeSuffix(".")
