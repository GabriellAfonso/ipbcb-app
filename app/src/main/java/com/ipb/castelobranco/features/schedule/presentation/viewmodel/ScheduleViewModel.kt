package com.ipb.castelobranco.features.schedule.presentation.viewmodel

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.ipb.castelobranco.core.domain.error.AppError
import com.ipb.castelobranco.core.domain.snapshot.SnapshotState
import com.ipb.castelobranco.core.presentation.navigation.AppRoutes
import com.ipb.castelobranco.core.presentation.error.toUserMessage
import com.ipb.castelobranco.features.schedule.domain.model.MonthSchedule
import com.ipb.castelobranco.features.schedule.domain.repository.ScheduleRepository
import com.ipb.castelobranco.features.schedule.presentation.components.NextScheduleUi
import com.ipb.castelobranco.features.schedule.presentation.components.ScheduleBandUi
import com.ipb.castelobranco.features.schedule.presentation.components.ScheduleSectionUi
import com.ipb.castelobranco.features.schedule.presentation.mapper.toSectionsUi
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.format.TextStyle
import java.time.temporal.ChronoUnit
import java.util.Calendar
import java.util.Locale
import timber.log.Timber
import javax.inject.Inject

/**
 * First of [days] that is today or later. A schedule for a future month has no past dates;
 * one for a past month has no upcoming date (null).
 */
internal fun resolveNextDay(
    days: List<Int>,
    scheduleYear: Int,
    scheduleMonth: Int,
    todayYear: Int,
    todayMonth: Int,
    todayDay: Int,
): Int? {
    val scheduleKey = scheduleYear * 12 + scheduleMonth
    val todayKey = todayYear * 12 + todayMonth
    val threshold = when {
        scheduleKey > todayKey -> 0
        scheduleKey < todayKey -> return null
        else -> todayDay
    }
    return days.sorted().firstOrNull { it >= threshold }
}

/**
 * Index of the first of [sections] (tab order) with an entry on [date], or null when [date] is outside the
 * schedule's month or has no entry.
 */
internal fun findSectionIndexForDate(
    sections: List<ScheduleSectionUi>,
    scheduleYear: Int,
    scheduleMonth: Int,
    date: LocalDate,
): Int? {
    if (date.year != scheduleYear || date.monthValue != scheduleMonth) return null
    return sections.indexOfFirst { section -> section.rows.any { it.day == date.dayOfMonth } }.takeIf { it >= 0 }
}

private val BAND_LOCALE = Locale.forLanguageTag("pt-BR")
private const val SERVING_PREFIX = "Escalado: "
private const val WEEKDAY_SUFFIX = "-feira"
private const val TODAY_LABEL = "É hoje"
private const val TOMORROW_LABEL = "Amanhã"

/**
 * The home band's schedule card: the first date from [today] on, in any section, whose row is [memberId]'s; failing
 * that, the next date of [nextSection] and who serves on it. Null when neither exists (`specs/core/spec.md` §6.3).
 */
internal fun buildScheduleBand(
    sections: List<ScheduleSectionUi>,
    scheduleYear: Int,
    scheduleMonth: Int,
    memberId: Long?,
    nextSection: ScheduleSectionUi?,
    today: LocalDate,
): ScheduleBandUi? {
    val upcoming = sections.flatMap { section ->
        section.rows.mapNotNull { row ->
            runCatching { LocalDate.of(scheduleYear, scheduleMonth, row.day) }.getOrNull()
                ?.takeUnless { it.isBefore(today) }
                ?.let { date -> Triple(section, row, date) }
        }
    }
    val own = memberId?.let { id -> upcoming.filter { it.second.memberId == id }.minByOrNull { it.third } }
    if (own != null) {
        val (section, _, date) = own
        return bandOf(section, date, isOwn = true, detail = countdownLabel(today, date))
    }
    val next = upcoming.filter { it.first == nextSection }.minByOrNull { it.third } ?: return null
    val (section, row, date) = next
    return bandOf(section, date, isOwn = false, detail = SERVING_PREFIX + row.member)
}

private fun bandOf(section: ScheduleSectionUi, date: LocalDate, isOwn: Boolean, detail: String): ScheduleBandUi {
    val dayMonth = "%02d/%02d".format(date.dayOfMonth, date.monthValue)
    val time = section.time.trim().takeIf { it.isNotEmpty() }?.let { " · $it" }.orEmpty()
    return ScheduleBandUi(
        isOwn = isOwn,
        monthLabel = date.month.getDisplayName(TextStyle.SHORT, BAND_LOCALE).removeSuffix(".").uppercase(BAND_LOCALE),
        day = date.dayOfMonth,
        title = "${section.shortTitle.removeSuffix(WEEKDAY_SUFFIX)}, $dayMonth$time",
        detail = detail,
    )
}

private fun countdownLabel(today: LocalDate, date: LocalDate): String =
    when (val days = ChronoUnit.DAYS.between(today, date)) {
        0L -> TODAY_LABEL
        1L -> TOMORROW_LABEL
        else -> "Faltam $days dias"
    }

sealed interface ScheduleUiState {
    object Loading : ScheduleUiState
    object Empty : ScheduleUiState
    /**
     * @param nextDays upcoming day of each section (same order as [sections]); null when every date has passed.
     * @param initialTabIndex section opened first: the one with an entry on the reminder's date when the screen was
     * opened from a rota reminder, otherwise the next service.
     */
    data class Success(
        val sections: List<ScheduleSectionUi>,
        val data: MonthSchedule,
        val nextDays: List<Int?> = emptyList(),
        val initialTabIndex: Int = 0,
    ) : ScheduleUiState
    data class Error(val message: String, val httpCode: Int? = null) : ScheduleUiState
}

@HiltViewModel
class ScheduleViewModel @Inject constructor(
    private val repository: ScheduleRepository,
    savedStateHandle: SavedStateHandle = SavedStateHandle(),
) : ViewModel() {

    /** Set when opened from a rota reminder: only picks the opening tab. */
    private val openDate: LocalDate? = savedStateHandle.get<String>(AppRoutes.ARG_SCHEDULE_DATE)
        ?.let { runCatching { LocalDate.parse(it) }.getOrNull() }

    private val _isRefreshing = MutableStateFlow(false)
    val isRefreshing = _isRefreshing.asStateFlow()

    val uiState: StateFlow<ScheduleUiState> = repository.observeMonthSchedule()
        .map { snapshot ->
            mapSnapshotToUiState(snapshot)
        }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.Eagerly,
            initialValue = mapSnapshotToUiState(repository.getCurrentSnapshot())
        )
    /** Starts from the snapshot already in memory: a null first value would flash "Escala indisponível". */
    val nextSection: StateFlow<NextScheduleUi?> = uiState
        .map(::toNextSection)
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.Eagerly,
            initialValue = toNextSection(uiState.value)
        )

    /** The logged-in user's member record; set by the home, null when logged out or not linked. */
    private val memberId = MutableStateFlow<Long?>(null)

    /** The home band's schedule card (`specs/core/spec.md` §6.3). */
    val band: StateFlow<ScheduleBandUi?> = combine(uiState, memberId, ::toBand)
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.Eagerly,
            initialValue = toBand(uiState.value, memberId.value)
        )

    fun setMemberId(id: Long?) {
        memberId.value = id
    }

    private fun toBand(state: ScheduleUiState, memberId: Long?): ScheduleBandUi? {
        if (state !is ScheduleUiState.Success) return null
        return buildScheduleBand(
            sections = state.sections,
            scheduleYear = state.data.year,
            scheduleMonth = state.data.month,
            memberId = memberId,
            nextSection = findNextSection(state.sections),
            today = LocalDate.now(),
        )
    }

    private fun toNextSection(state: ScheduleUiState): NextScheduleUi? {
        if (state !is ScheduleUiState.Success) return null
        return findNextSection(state.sections)?.let { section ->
            val calendar = Calendar.getInstance()
            NextScheduleUi(
                section = section,
                nextDay = resolveNextDay(
                    days = section.rows.map { it.day },
                    scheduleYear = state.data.year,
                    scheduleMonth = state.data.month,
                    todayYear = calendar.get(Calendar.YEAR),
                    todayMonth = calendar.get(Calendar.MONTH) + 1,
                    todayDay = calendar.get(Calendar.DAY_OF_MONTH),
                )
            )
        }
    }

    private fun findNextSection(sections: List<ScheduleSectionUi>): ScheduleSectionUi? {
        val calendar = Calendar.getInstance()
        val today = calendar.get(Calendar.DAY_OF_WEEK)
        val currentHour = calendar.get(Calendar.HOUR_OF_DAY)

        // If today is a meeting day and the meeting hasn't started yet, show today's section
        val todayKeyword = when (today) {
            Calendar.TUESDAY  -> "Terça"
            Calendar.THURSDAY -> "Quinta"
            Calendar.SUNDAY   -> "Domingo"
            else              -> null
        }
        if (todayKeyword != null && currentHour < 22) {
            val todaySection = sections.firstOrNull { it.title.contains(todayKeyword, ignoreCase = true) }
            if (todaySection != null) return todaySection
        }

        val nextKeyword = when (today) {
            Calendar.MONDAY              -> "Terça"
            Calendar.TUESDAY             -> "Quinta"
            Calendar.WEDNESDAY           -> "Quinta"
            Calendar.THURSDAY            -> "Domingo"
            Calendar.FRIDAY,
            Calendar.SATURDAY            -> "Domingo"
            Calendar.SUNDAY              -> "Terça"
            else                         -> "Terça"
        }
        return sections.firstOrNull { it.title.contains(nextKeyword, ignoreCase = true) }
    }


    fun refreshMonthSchedule(minDurationMs: Long = 600L) {
        if (_isRefreshing.value) return

        viewModelScope.launch {
            _isRefreshing.value = true
            val startTime = System.currentTimeMillis()

            try {
                repository.refreshMonthSchedule()
            } catch (e: Exception) {
                Timber.w(e, "Failed to refresh month schedule")
            } finally {
                val elapsed = System.currentTimeMillis() - startTime
                if (elapsed < minDurationMs) {
                    delay(minDurationMs - elapsed)
                }
                _isRefreshing.value = false
            }
        }
    }

    private fun mapSnapshotToUiState(snapshot: SnapshotState<MonthSchedule>): ScheduleUiState {
        return when (snapshot) {
            is SnapshotState.Data -> {
                val sections = snapshot.value.toSectionsUi()
                if (sections.isNotEmpty()) {
                    val calendar = Calendar.getInstance()
                    ScheduleUiState.Success(
                        sections = sections,
                        data = snapshot.value,
                        nextDays = sections.map { section ->
                            resolveNextDay(
                                days = section.rows.map { it.day },
                                scheduleYear = snapshot.value.year,
                                scheduleMonth = snapshot.value.month,
                                todayYear = calendar.get(Calendar.YEAR),
                                todayMonth = calendar.get(Calendar.MONTH) + 1,
                                todayDay = calendar.get(Calendar.DAY_OF_MONTH),
                            )
                        },
                        initialTabIndex = openDate
                            ?.let { findSectionIndexForDate(sections, snapshot.value.year, snapshot.value.month, it) }
                            ?: findNextSection(sections)
                                ?.let { sections.indexOf(it) }
                                ?.coerceAtLeast(0) ?: 0,
                    )
                } else {
                    ScheduleUiState.Empty
                }
            }

            is SnapshotState.Loading -> ScheduleUiState.Loading

            is SnapshotState.Error -> {
                val code = (snapshot.error as? AppError.Auth)?.code
                ScheduleUiState.Error(
                    message = snapshot.error.toUserMessage(),
                    httpCode = code,
                )
            }
        }
    }
}