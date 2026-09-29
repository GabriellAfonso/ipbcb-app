package com.ipb.castelobranco.features.admin.reports.hymnal.presentation.viewmodel

import com.ipb.castelobranco.core.domain.access.AccessLevel
import com.ipb.castelobranco.core.domain.access.ObserveAccessUseCase
import com.ipb.castelobranco.core.domain.access.Role
import com.ipb.castelobranco.core.domain.access.Scope
import com.ipb.castelobranco.core.domain.error.AppError
import com.ipb.castelobranco.core.testing.FakeAccessRepository
import com.ipb.castelobranco.core.testing.accessOf
import com.ipb.castelobranco.features.admin.reports.hymnal.presentation.state.HymnalReportEvent
import app.cash.turbine.test
import com.ipb.castelobranco.core.domain.snapshot.SnapshotState
import com.ipb.castelobranco.features.admin.reports.hymnal.FakeHymnCatalogRepository
import com.ipb.castelobranco.features.admin.reports.hymnal.FakeHymnalHistoryAdminRepository
import com.ipb.castelobranco.features.admin.reports.hymnal.FakeHymnalReportRepository
import com.ipb.castelobranco.features.admin.reports.hymnal.SUNDAY_NIGHT_ID
import com.ipb.castelobranco.features.admin.reports.hymnal.SUNDAY_NIGHT_NAME
import com.ipb.castelobranco.features.admin.reports.hymnal.TODAY
import com.ipb.castelobranco.features.admin.reports.hymnal.catalog
import com.ipb.castelobranco.features.admin.reports.hymnal.domain.model.ReportPeriod
import com.ipb.castelobranco.features.admin.reports.hymnal.domain.model.ReportReading
import com.ipb.castelobranco.features.admin.reports.hymnal.domain.model.ReportSlice
import com.ipb.castelobranco.features.admin.reports.hymnal.domain.model.ReportEmptyReason
import com.ipb.castelobranco.features.admin.reports.hymnal.domain.model.TopHymn
import com.ipb.castelobranco.features.admin.reports.hymnal.domain.usecase.BuildCalendarUseCase
import com.ipb.castelobranco.features.admin.reports.hymnal.domain.usecase.BuildEvolutionSeriesUseCase
import com.ipb.castelobranco.features.admin.reports.hymnal.domain.usecase.BuildHighlightsUseCase
import com.ipb.castelobranco.features.admin.reports.hymnal.domain.usecase.BuildHymnProfileUseCase
import com.ipb.castelobranco.features.admin.reports.hymnal.domain.usecase.BuildHymnRankingUseCase
import com.ipb.castelobranco.features.admin.reports.hymnal.domain.usecase.BuildHymnalCoverageUseCase
import com.ipb.castelobranco.features.admin.reports.hymnal.domain.usecase.BuildInVsOutsideUseCase
import com.ipb.castelobranco.features.admin.reports.hymnal.domain.usecase.BuildReportReadingsUseCase
import com.ipb.castelobranco.features.admin.reports.hymnal.domain.usecase.BuildServiceBulletinsUseCase
import com.ipb.castelobranco.features.admin.reports.hymnal.domain.usecase.FilterOccurrencesUseCase
import com.ipb.castelobranco.features.admin.reports.hymnal.domain.usecase.GetAllTimeTopHymnsUseCase
import com.ipb.castelobranco.features.admin.reports.hymnal.domain.usecase.GetOccurrenceReportUseCase
import com.ipb.castelobranco.features.admin.reports.hymnal.domain.usecase.GetServiceWindowsUseCase
import com.ipb.castelobranco.features.admin.reports.hymnal.domain.usecase.ResolveEmptyReasonUseCase
import com.ipb.castelobranco.features.admin.reports.hymnal.domain.usecase.ResolveReportPeriodUseCase
import com.ipb.castelobranco.features.admin.reports.hymnal.fixedDateProvider
import com.ipb.castelobranco.features.admin.reports.hymnal.occurrence
import com.ipb.castelobranco.features.admin.reports.hymnal.outsideOccurrence
import com.ipb.castelobranco.features.admin.reports.hymnal.report
import com.ipb.castelobranco.features.admin.reports.hymnal.serviceWindow
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import java.time.LocalDate

@OptIn(ExperimentalCoroutinesApi::class)
class HymnalReportViewModelTest {

    private val testDispatcher = StandardTestDispatcher()

    private lateinit var reportRepository: FakeHymnalReportRepository
    private lateinit var adminRepository: FakeHymnalHistoryAdminRepository
    private lateinit var catalogRepository: FakeHymnCatalogRepository

    private val occurrences = listOf(
        occurrence(number = "50", on = TODAY),
        occurrence(number = "12", on = TODAY),
        outsideOccurrence(number = "120", on = TODAY.minusDays(3)),
    )

    @Before
    fun setup() {
        Dispatchers.setMain(testDispatcher)
        reportRepository = FakeHymnalReportRepository(
            occurrencesResult = { range -> Result.success(report(occurrences, range)) },
            topHymnsResult = Result.success(
                listOf(TopHymn("50", "Grandioso És Tu", 42))
            ),
        )
        adminRepository = FakeHymnalHistoryAdminRepository(
            windowsResult = Result.success(listOf(serviceWindow()))
        )
        catalogRepository = FakeHymnCatalogRepository(catalog("50", "12", "120", "7"))
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    private fun viewModel(): HymnalReportViewModel = HymnalReportViewModel(
        resolvePeriod = ResolveReportPeriodUseCase(fixedDateProvider()),
        getOccurrenceReport = GetOccurrenceReportUseCase(reportRepository),
        getAllTimeTopHymns = GetAllTimeTopHymnsUseCase(reportRepository),
        getServiceWindows = GetServiceWindowsUseCase(adminRepository),
        buildReadings = BuildReportReadingsUseCase(
            filterOccurrences = FilterOccurrencesUseCase(),
            buildHighlights = BuildHighlightsUseCase(),
            buildRanking = BuildHymnRankingUseCase(),
            buildEvolution = BuildEvolutionSeriesUseCase(),
            buildBulletins = BuildServiceBulletinsUseCase(),
            buildCalendar = BuildCalendarUseCase(),
            buildInVsOutside = BuildInVsOutsideUseCase(BuildHymnRankingUseCase()),
            resolveEmptyReason = ResolveEmptyReasonUseCase(),
        ),
        buildCoverage = BuildHymnalCoverageUseCase(),
        buildHymnProfile = BuildHymnProfileUseCase(),
        hymnCatalog = catalogRepository,
        dateProvider = fixedDateProvider(),
        defaultDispatcher = testDispatcher,
        observeAccess = ObserveAccessUseCase(access),
    )

    private val access = FakeAccessRepository()

    // region access (spec 006)

    @Test
    fun `collection settings are offered only to owner`() = runTest {
        access.state.value = accessOf(Role.MEDIA, Scope.HYMNAL_HISTORY_REPORT to AccessLevel.VIEW)
        val viewModel = viewModel()
        advanceUntilIdle()
        assertEquals(false, viewModel.uiState.value.canOpenSettings)

        access.state.value = accessOf(Role.ADMIN, Scope.HYMNAL_HISTORY_REPORT to AccessLevel.OWNER)
        advanceUntilIdle()
        assertTrue(viewModel.uiState.value.canOpenSettings)
    }

    @Test
    fun `a refused report load leaves the reports area`() = runTest {
        reportRepository = FakeHymnalReportRepository(
            occurrencesResult = { Result.failure(AppError.Auth(code = 403, userMessage = "Sem permissão")) },
            topHymnsResult = Result.success(emptyList()),
        )
        val viewModel = viewModel()

        viewModel.events.test {
            assertEquals(HymnalReportEvent.LeaveArea("Sem permissão"), awaitItem())
            cancelAndIgnoreRemainingEvents()
        }
    }

    // endregion

    // region loading

    @Test
    fun `opening the report fetches the period and the one before it`() = runTest {
        val viewModel = viewModel()
        advanceUntilIdle()

        assertEquals(2, reportRepository.requestedRanges.size)
        assertEquals(3, viewModel.uiState.value.ranking?.totalHymns)
    }

    @Test
    fun `a failed fetch surfaces a message and no readings`() = runTest {
        reportRepository.occurrencesResult = { Result.failure(AppError.Server(code = 500)) }

        val viewModel = viewModel()
        advanceUntilIdle()

        assertNotNull(viewModel.uiState.value.error)
        assertNull(viewModel.uiState.value.ranking)
    }

    // endregion

    // region no refetching

    @Test
    fun `changing the slice issues no request at all`() = runTest {
        val viewModel = viewModel()
        advanceUntilIdle()
        val before = reportRepository.requestedRanges.size

        viewModel.onSliceSelected(ReportSlice.OutsideService)
        advanceUntilIdle()

        assertEquals(before, reportRepository.requestedRanges.size)
        assertEquals(listOf("120"), viewModel.uiState.value.ranking?.bars?.map { it.hymnNumber })
    }

    @Test
    fun `changing the reading issues no request at all`() = runTest {
        val viewModel = viewModel()
        advanceUntilIdle()
        val before = reportRepository.requestedRanges.size

        listOf(
            ReportReading.RANKING,
            ReportReading.EVOLUTION,
            ReportReading.SERVICES,
            ReportReading.CALENDAR,
            ReportReading.IN_VS_OUTSIDE,
            ReportReading.HIGHLIGHTS,
        ).forEach { reading ->
            viewModel.onReadingSelected(reading)
            advanceUntilIdle()
        }

        assertEquals(before, reportRepository.requestedRanges.size)
    }

    @Test
    fun `expanding the ranking issues no request`() = runTest {
        val viewModel = viewModel()
        advanceUntilIdle()
        val before = reportRepository.requestedRanges.size

        viewModel.onRankingExpandToggled()
        advanceUntilIdle()

        assertEquals(before, reportRepository.requestedRanges.size)
        assertTrue(viewModel.uiState.value.rankingExpanded)
    }

    @Test
    fun `changing the period issues exactly one more pair of requests`() = runTest {
        val viewModel = viewModel()
        advanceUntilIdle()
        val before = reportRepository.requestedRanges.size

        viewModel.onPeriodSelected(ReportPeriod.ThisWeek)
        advanceUntilIdle()

        assertEquals(before + 2, reportRepository.requestedRanges.size)
    }

    @Test
    fun `an over-long custom range is refused locally and issues nothing`() = runTest {
        val viewModel = viewModel()
        advanceUntilIdle()
        val before = reportRepository.requestedRanges.size

        viewModel.onPeriodSelected(
            ReportPeriod.Custom(from = LocalDate.of(2024, 1, 1), to = LocalDate.of(2026, 1, 1))
        )
        advanceUntilIdle()

        assertEquals(before, reportRepository.requestedRanges.size)
        assertNotNull(viewModel.uiState.value.rangeError)
    }

    // endregion

    // region slices and windows

    @Test
    fun `only active services are offered as slices`() = runTest {
        adminRepository.windowsResult = Result.success(
            listOf(serviceWindow(), serviceWindow(id = 9, name = "Antigo", active = false))
        )

        val viewModel = viewModel()
        advanceUntilIdle()

        assertEquals(
            listOf(ReportSlice.Service(SUNDAY_NIGHT_ID, SUNDAY_NIGHT_NAME)),
            viewModel.uiState.value.availableServices,
        )
    }

    @Test
    fun `an empty slice on an active service says both things could be true`() = runTest {
        adminRepository.windowsResult = Result.success(listOf(serviceWindow(id = 9, name = "Culto de Oração")))

        val viewModel = viewModel()
        advanceUntilIdle()

        viewModel.onSliceSelected(ReportSlice.Service(id = 9, name = "Culto de Oração"))
        advanceUntilIdle()

        assertEquals(
            ReportEmptyReason.ServiceWithoutRecords("Culto de Oração"),
            viewModel.uiState.value.emptyReason,
        )
    }

    // endregion

    // region coverage

    @Test
    fun `the coverage reading loads the all-time sources once`() = runTest {
        val viewModel = viewModel()
        advanceUntilIdle()

        viewModel.onReadingSelected(ReportReading.COVERAGE)
        advanceUntilIdle()
        viewModel.onReadingSelected(ReportReading.RANKING)
        viewModel.onReadingSelected(ReportReading.COVERAGE)
        advanceUntilIdle()

        assertEquals(1, reportRepository.topHymnsCalls)
        assertNotNull(viewModel.uiState.value.coverage)
    }

    @Test
    fun `an unavailable catalogue degrades coverage without breaking the report`() = runTest {
        catalogRepository = FakeHymnCatalogRepository(
            SnapshotState.Error(AppError.Network())
        )

        val viewModel = viewModel()
        advanceUntilIdle()

        viewModel.onReadingSelected(ReportReading.COVERAGE)
        advanceUntilIdle()

        assertEquals(
            ReportEmptyReason.CatalogUnavailable,
            viewModel.uiState.value.coverageEmptyReason,
        )
        assertNull(viewModel.uiState.value.coverage)
        assertNotNull(viewModel.uiState.value.ranking)
    }

    // endregion

    // region hymn card

    @Test
    fun `selecting a hymn builds its profile from the loaded sources`() = runTest {
        val viewModel = viewModel()
        advanceUntilIdle()

        viewModel.onHymnSelected("50")
        advanceUntilIdle()

        val profile = viewModel.uiState.value.hymnProfile
        assertNotNull(profile)
        assertEquals(42, profile?.allTimeCount)
    }

    // endregion
}
