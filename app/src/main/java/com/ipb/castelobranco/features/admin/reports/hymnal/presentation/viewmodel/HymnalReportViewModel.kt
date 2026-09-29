package com.ipb.castelobranco.features.admin.reports.hymnal.presentation.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.ipb.castelobranco.core.di.DefaultDispatcher
import com.ipb.castelobranco.core.domain.access.AccessLevel
import com.ipb.castelobranco.core.domain.access.ObserveAccessUseCase
import com.ipb.castelobranco.core.domain.access.Scope
import com.ipb.castelobranco.core.domain.error.toAppError
import com.ipb.castelobranco.core.domain.model.HymnCatalogEntry
import com.ipb.castelobranco.core.domain.repository.HymnCatalogRepository
import com.ipb.castelobranco.core.domain.snapshot.SnapshotState
import com.ipb.castelobranco.core.domain.util.DateProvider
import com.ipb.castelobranco.core.presentation.error.toUserMessage
import com.ipb.castelobranco.features.admin.reports.hymnal.domain.model.BucketGranularity
import com.ipb.castelobranco.features.admin.reports.hymnal.domain.model.DateRange
import com.ipb.castelobranco.features.admin.reports.hymnal.domain.model.HymnOccurrence
import com.ipb.castelobranco.features.admin.reports.hymnal.domain.model.HymnRanking
import com.ipb.castelobranco.features.admin.reports.hymnal.domain.model.ReportEmptyReason
import com.ipb.castelobranco.features.admin.reports.hymnal.domain.model.ReportPeriod
import com.ipb.castelobranco.features.admin.reports.hymnal.domain.model.ReportReading
import com.ipb.castelobranco.features.admin.reports.hymnal.domain.model.ReportSlice
import com.ipb.castelobranco.features.admin.reports.hymnal.domain.model.ServiceWindow
import com.ipb.castelobranco.features.admin.reports.hymnal.domain.model.TopHymn
import com.ipb.castelobranco.features.admin.reports.hymnal.domain.usecase.BuildHymnProfileUseCase
import com.ipb.castelobranco.features.admin.reports.hymnal.domain.usecase.BuildHymnalCoverageUseCase
import com.ipb.castelobranco.features.admin.reports.hymnal.domain.usecase.BuildReportReadingsUseCase
import com.ipb.castelobranco.features.admin.reports.hymnal.domain.usecase.GetAllTimeTopHymnsUseCase
import com.ipb.castelobranco.features.admin.reports.hymnal.domain.usecase.GetOccurrenceReportUseCase
import com.ipb.castelobranco.features.admin.reports.hymnal.domain.usecase.GetServiceWindowsUseCase
import com.ipb.castelobranco.features.admin.reports.hymnal.domain.usecase.OccurrenceReportBundle
import com.ipb.castelobranco.features.admin.reports.hymnal.domain.usecase.PeriodResolution
import com.ipb.castelobranco.features.admin.reports.hymnal.domain.usecase.ResolveReportPeriodUseCase
import com.ipb.castelobranco.features.admin.reports.hymnal.domain.usecase.ResolvedPeriod
import com.ipb.castelobranco.features.admin.reports.hymnal.presentation.state.HymnalReportEvent
import com.ipb.castelobranco.features.admin.reports.hymnal.presentation.state.HymnalReportUiState
import com.ipb.castelobranco.features.admin.reports.hymnal.presentation.util.isPermissionRefusal
import com.ipb.castelobranco.features.admin.reports.hymnal.presentation.util.toFullDate
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.time.LocalDate
import javax.inject.Inject

/**
 * One period in memory, pivoted locally.
 *
 * **Only [onPeriodSelected] reaches the network** — plus the one lazy load of the all-time
 * sources the coverage reading and the hymn card need, which no other reading uses. Changing the
 * slice, the reading or the ranking cap recomputes over occurrences already loaded, which is why
 * the report stays usable with the network off after the first load.
 *
 * Graph-scoped, so a hymn card opened from any list reuses this loaded period.
 */
@HiltViewModel
class HymnalReportViewModel @Inject constructor(
    private val resolvePeriod: ResolveReportPeriodUseCase,
    private val getOccurrenceReport: GetOccurrenceReportUseCase,
    private val getAllTimeTopHymns: GetAllTimeTopHymnsUseCase,
    private val getServiceWindows: GetServiceWindowsUseCase,
    private val buildReadings: BuildReportReadingsUseCase,
    private val buildCoverage: BuildHymnalCoverageUseCase,
    private val buildHymnProfile: BuildHymnProfileUseCase,
    private val hymnCatalog: HymnCatalogRepository,
    private val dateProvider: DateProvider,
    @DefaultDispatcher private val defaultDispatcher: CoroutineDispatcher,
    observeAccess: ObserveAccessUseCase,
) : ViewModel() {

    private val _uiState = MutableStateFlow(HymnalReportUiState())
    val uiState: StateFlow<HymnalReportUiState> = _uiState.asStateFlow()

    // Buffered: a refusal on the first load can arrive before the screen starts collecting.
    private val _events = Channel<HymnalReportEvent>(Channel.BUFFERED)
    val events: Flow<HymnalReportEvent> = _events.receiveAsFlow()

    /** The loaded period. Every period-bounded reading is derived from this and nothing else. */
    private var bundle: OccurrenceReportBundle? = null
    private var serviceWindows: List<ServiceWindow> = emptyList()

    /** All-time sources, fetched once and only when a reading actually needs them. */
    private var allTimeTopHymns: List<TopHymn>? = null
    private var visibleWindow: List<HymnOccurrence>? = null

    init {
        load(ReportPeriod.ThisMonth)
        loadServiceWindows()
        viewModelScope.launch {
            observeAccess().collect { access ->
                val isOwner = access.allows(Scope.HYMNAL_HISTORY_REPORT, AccessLevel.OWNER)
                _uiState.update { it.copy(canOpenSettings = isOwner) }
            }
        }
    }

    // region selections

    fun onPeriodSelected(period: ReportPeriod) = load(period)

    fun onSliceSelected(slice: ReportSlice) {
        _uiState.update { it.copy(slice = slice) }
        recompute()
    }

    fun onReadingSelected(reading: ReportReading) {
        _uiState.update { it.copy(reading = reading, focusedDay = null) }
        if (reading == ReportReading.COVERAGE) loadCoverage()
    }

    fun onRankingExpandToggled() {
        _uiState.update { it.copy(rankingExpanded = !it.rankingExpanded) }
        recompute()
    }

    /** Tapping a calendar day opens that day's bulletin. */
    fun onDaySelected(day: LocalDate) {
        _uiState.update { it.copy(reading = ReportReading.SERVICES, focusedDay = day) }
    }

    fun onFocusedDayCleared() {
        _uiState.update { it.copy(focusedDay = null) }
    }

    fun onRetry() = load(_uiState.value.period)

    // endregion

    // region loading

    private fun load(period: ReportPeriod) {
        val resolution = resolvePeriod(period)
        if (resolution is PeriodResolution.Invalid) {
            _uiState.update { it.copy(rangeError = resolution.message) }
            return
        }

        val resolved = (resolution as PeriodResolution.Resolved).period
        _uiState.update {
            it.copy(
                isLoading = true,
                error = null,
                rangeError = null,
                period = period,
                rangeLabel = resolved.range.label(),
            )
        }

        viewModelScope.launch {
            getOccurrenceReport(resolved)
                .onSuccess { loaded ->
                    bundle = loaded
                    _uiState.update { it.copy(isLoading = false) }
                    recompute()
                }
                .onFailure { throwable ->
                    bundle = null
                    val error = throwable.toAppError()
                    _uiState.update { it.copy(isLoading = false, error = error.toUserMessage()) }
                    if (error.isPermissionRefusal()) {
                        _events.send(HymnalReportEvent.LeaveArea(error.toUserMessage()))
                    }
                }
        }
    }

    private fun loadServiceWindows() {
        viewModelScope.launch {
            getServiceWindows().onSuccess { windows ->
                serviceWindows = windows
                _uiState.update { state ->
                    state.copy(
                        availableServices = windows
                            .filter { it.active }
                            .map { ReportSlice.Service(id = it.id, name = it.name) },
                    )
                }
                recompute()
            }
        }
    }

    /** Called when the reports screen is reopened after a service window changed. */
    fun refreshServiceWindows() = loadServiceWindows()

    private fun loadCoverage() {
        if (allTimeTopHymns != null && visibleWindow != null) {
            recomputeCoverage()
            return
        }
        if (_uiState.value.isCoverageLoading) return

        _uiState.update { it.copy(isCoverageLoading = true, coverageError = null) }
        viewModelScope.launch {
            val failure = loadAllTimeSources()
            _uiState.update { it.copy(isCoverageLoading = false, coverageError = failure) }
            if (failure == null) recomputeCoverage()
        }
    }

    /**
     * The all-time ranking and the last year of occurrences, fetched together.
     *
     * The ranking is the only source in the app for "todo o histórico"; the year of occurrences is
     * the only source for a date. They answer different questions and are never summed together.
     *
     * @return the message to show, or `null` on success.
     */
    private suspend fun loadAllTimeSources(): String? = coroutineScope {
        val today = dateProvider.today()
        val window = DateRange(
            from = today.minusDays((DateRange.MAX_DAYS - 1).toLong()),
            to = today,
        )

        val topHymnsDeferred = async { getAllTimeTopHymns() }
        val occurrencesDeferred = async {
            getOccurrenceReport(
                ResolvedPeriod(
                    range = window,
                    granularity = BucketGranularity.MONTH,
                    precedingRange = window,
                )
            )
        }

        val topHymns = topHymnsDeferred.await()
        val occurrences = occurrencesDeferred.await()

        topHymns.fold(
            onSuccess = { hymns ->
                allTimeTopHymns = hymns
                visibleWindow = occurrences.getOrNull()?.current?.occurrences.orEmpty()
                null
            },
            onFailure = { it.toAppError().toUserMessage() },
        )
    }

    // endregion

    // region derivation

    private fun recompute() {
        val loaded = bundle ?: return
        viewModelScope.launch {
            val state = _uiState.value
            val readings = withContext(defaultDispatcher) {
                buildReadings(
                    bundle = loaded,
                    slice = state.slice,
                    serviceWindows = serviceWindows,
                    rankingLimit = if (state.rankingExpanded) Int.MAX_VALUE
                    else HymnRanking.DEFAULT_LIMIT,
                    collectionIsEmpty = allTimeTopHymns?.isEmpty(),
                )
            }

            _uiState.update {
                it.copy(
                    highlights = readings.highlights,
                    ranking = readings.ranking,
                    evolution = readings.evolution,
                    bulletins = readings.bulletins,
                    calendar = readings.calendar,
                    inVsOutside = readings.inVsOutside,
                    emptyReason = readings.emptyReason,
                )
            }
        }
    }

    private fun recomputeCoverage() {
        val allTime = allTimeTopHymns ?: return
        val window = visibleWindow ?: return

        viewModelScope.launch {
            val catalog = catalogOrNull()
            if (catalog == null) {
                _uiState.update {
                    it.copy(
                        coverage = null,
                        coverageEmptyReason = ReportEmptyReason.CatalogUnavailable,
                    )
                }
                return@launch
            }

            val coverage = withContext(defaultDispatcher) {
                buildCoverage(catalog = catalog, allTime = allTime, visibleWindow = window)
            }

            _uiState.update {
                it.copy(
                    coverage = coverage,
                    coverageEmptyReason = if (allTime.isEmpty()) {
                        ReportEmptyReason.NoCollectionAtAll
                    } else {
                        null
                    },
                )
            }
        }
    }

    /**
     * The catalogue comes through `core/`, never through `features/hymnal` — an administration
     * feature may not import another feature area.
     */
    private suspend fun catalogOrNull(): List<HymnCatalogEntry>? =
        when (val state = hymnCatalog.observeHymnCatalog().first { it !is SnapshotState.Loading }) {
            is SnapshotState.Data -> state.value
            else -> null
        }

    // endregion

    // region hymn card

    fun onHymnSelected(hymnNumber: String, fallbackTitle: String? = null) {
        _uiState.update {
            it.copy(isHymnProfileLoading = true, hymnProfileError = null, hymnProfile = null)
        }

        viewModelScope.launch {
            val failure = if (allTimeTopHymns == null || visibleWindow == null) {
                loadAllTimeSources()
            } else {
                null
            }

            if (failure != null) {
                _uiState.update {
                    it.copy(isHymnProfileLoading = false, hymnProfileError = failure)
                }
                return@launch
            }

            val profile = withContext(defaultDispatcher) {
                buildHymnProfile(
                    hymnNumber = hymnNumber,
                    allTime = allTimeTopHymns.orEmpty(),
                    visibleWindow = visibleWindow.orEmpty(),
                    fallbackTitle = fallbackTitle,
                )
            }

            _uiState.update { it.copy(isHymnProfileLoading = false, hymnProfile = profile) }
        }
    }

    // endregion

    private fun DateRange.label(): String = "${from.toFullDate()} – ${to.toFullDate()}"
}
