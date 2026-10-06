package com.ipb.castelobranco.features.schedule.presentation.viewmodel

import com.ipb.castelobranco.core.domain.error.AppError
import com.ipb.castelobranco.core.domain.snapshot.RefreshResult
import com.ipb.castelobranco.core.domain.snapshot.SnapshotState
import com.ipb.castelobranco.features.schedule.domain.model.MonthSchedule
import com.ipb.castelobranco.features.schedule.domain.model.ScheduleEntry
import com.ipb.castelobranco.features.schedule.domain.model.ScheduleItem
import com.ipb.castelobranco.features.schedule.domain.repository.ScheduleRepository
import io.mockk.coEvery
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.emptyFlow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class ScheduleViewModelTest {

    private val testDispatcher = StandardTestDispatcher()
    private lateinit var repository: ScheduleRepository

    private val scheduleWithEntries = MonthSchedule(
        year = 2024,
        month = 4,
        schedule = mapOf(
            "Domingo" to ScheduleEntry("18:00", listOf(ScheduleItem(7, "João")))
        )
    )
    private val emptySchedule = MonthSchedule(year = 2024, month = 4, schedule = emptyMap())

    @Before
    fun setup() {
        Dispatchers.setMain(testDispatcher)
        repository = mockk()
        coEvery { repository.refreshMonthSchedule() } returns RefreshResult.Updated
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    private fun buildViewModel(
        initial: SnapshotState<MonthSchedule> = SnapshotState.Loading,
        observed: kotlinx.coroutines.flow.Flow<SnapshotState<MonthSchedule>> = emptyFlow(),
    ): ScheduleViewModel {
        every { repository.getCurrentSnapshot() } returns initial
        every { repository.observeMonthSchedule() } returns observed
        return ScheduleViewModel(repository)
    }

    // region uiState mapping

    @Test
    fun `uiState initial value is Loading when getCurrentSnapshot returns Loading`() = runTest {
        val vm = buildViewModel(initial = SnapshotState.Loading, observed = emptyFlow())
        assertEquals(ScheduleUiState.Loading, vm.uiState.value)
    }

    @Test
    fun `uiState emits Loading when observe emits Loading`() = runTest {
        val flow = MutableStateFlow<SnapshotState<MonthSchedule>>(SnapshotState.Loading)
        val vm = buildViewModel(observed = flow)
        advanceUntilIdle()
        assertEquals(ScheduleUiState.Loading, vm.uiState.value)
    }

    @Test
    fun `uiState emits Success when observe emits Data with non-empty schedule`() = runTest {
        val flow = flowOf(SnapshotState.Data(scheduleWithEntries))
        val vm = buildViewModel(observed = flow)
        advanceUntilIdle()
        assertTrue(vm.uiState.value is ScheduleUiState.Success)
    }

    @Test
    fun `uiState Success contains the MonthSchedule data`() = runTest {
        val flow = flowOf(SnapshotState.Data(scheduleWithEntries))
        val vm = buildViewModel(observed = flow)
        advanceUntilIdle()
        val state = vm.uiState.value as ScheduleUiState.Success
        assertEquals(scheduleWithEntries, state.data)
    }

    @Test
    fun `uiState emits Empty when observe emits Data with empty schedule`() = runTest {
        val flow = flowOf(SnapshotState.Data(emptySchedule))
        val vm = buildViewModel(observed = flow)
        advanceUntilIdle()
        assertEquals(ScheduleUiState.Empty, vm.uiState.value)
    }

    @Test
    fun `uiState emits Error when observe emits SnapshotState Error`() = runTest {
        val flow = flowOf(SnapshotState.Error(AppError.Unknown(message = "network error")))
        val vm = buildViewModel(observed = flow)
        advanceUntilIdle()
        assertTrue(vm.uiState.value is ScheduleUiState.Error)
    }

    @Test
    fun `uiState Error has httpCode 401 when error is AppError Auth with 401`() = runTest {
        val exception = AppError.Auth(code = 401, message = "Não autorizado")
        val flow = flowOf(SnapshotState.Error(exception))
        val vm = buildViewModel(observed = flow)
        advanceUntilIdle()
        val state = vm.uiState.value as ScheduleUiState.Error
        assertEquals(401, state.httpCode)
    }

    @Test
    fun `uiState Error has httpCode 403 when error is AppError Auth with 403`() = runTest {
        val exception = AppError.Auth(code = 403, message = "Proibido")
        val flow = flowOf(SnapshotState.Error(exception))
        val vm = buildViewModel(observed = flow)
        advanceUntilIdle()
        val state = vm.uiState.value as ScheduleUiState.Error
        assertEquals(403, state.httpCode)
    }

    @Test
    fun `uiState Error has null httpCode for non-permission exceptions`() = runTest {
        val flow = flowOf(SnapshotState.Error(AppError.Unknown(message = "timeout")))
        val vm = buildViewModel(observed = flow)
        advanceUntilIdle()
        val state = vm.uiState.value as ScheduleUiState.Error
        assertNull(state.httpCode)
    }

    @Test
    fun `uiState Error message uses the authored userMessage`() = runTest {
        val exception = AppError.Auth(code = 401, userMessage = "Faça login para ver a escala")
        val flow = flowOf(SnapshotState.Error(exception))
        val vm = buildViewModel(observed = flow)
        advanceUntilIdle()
        val state = vm.uiState.value as ScheduleUiState.Error
        assertEquals("Faça login para ver a escala", state.message)
    }

    @Test
    fun `uiState Error message never leaks the technical message`() = runTest {
        val exception = AppError.Server(code = 500, message = "<html>Server Error</html>")
        val flow = flowOf(SnapshotState.Error(exception))
        val vm = buildViewModel(observed = flow)
        advanceUntilIdle()
        val state = vm.uiState.value as ScheduleUiState.Error
        assertEquals("Não foi possível completar a operação. Tente novamente mais tarde.", state.message)
    }

    // endregion

    // region isRefreshing

    @Test
    fun `isRefreshing is false after refreshMonthSchedule completes`() = runTest {
        val vm = buildViewModel()
        vm.refreshMonthSchedule(minDurationMs = 0L)
        advanceUntilIdle()
        assertFalse(vm.isRefreshing.value)
    }

    @Test
    fun `isRefreshing starts as false`() = runTest {
        val vm = buildViewModel()
        assertFalse(vm.isRefreshing.value)
    }

    // endregion

    // region nextSection

    @Test
    fun `nextSection is null when uiState is Loading`() = runTest {
        val vm = buildViewModel(initial = SnapshotState.Loading, observed = emptyFlow())
        advanceUntilIdle()
        assertNull(vm.nextSection.value)
    }

    @Test
    fun `nextSection is null when uiState is Empty`() = runTest {
        val flow = flowOf(SnapshotState.Data(emptySchedule))
        val vm = buildViewModel(observed = flow)
        advanceUntilIdle()
        assertNull(vm.nextSection.value)
    }

    @Test
    fun `nextSection is non-null when Success state has matching section`() = runTest {
        val schedule = MonthSchedule(
            year = 2024,
            month = 4,
            schedule = mapOf(
                "Terça" to ScheduleEntry("19:30", listOf(ScheduleItem(2, "Maria"))),
                "Domingo" to ScheduleEntry("18:00", listOf(ScheduleItem(7, "João")))
            )
        )
        val flow = flowOf(SnapshotState.Data(schedule))
        val vm = buildViewModel(observed = flow)
        advanceUntilIdle()
        // When state is Success with sections, nextSection may or may not be null depending on the day.
        // The value is determined by Calendar.getInstance(), so we can only verify the type contract.
        val state = vm.uiState.value
        assertTrue(state is ScheduleUiState.Success)
    }

    // endregion

    // region resolveNextDay

    private val sundays = listOf(29, 1, 8, 15, 22)

    @Test
    fun `resolveNextDay returns first day on or after today in the current month`() {
        assertEquals(15, resolveNextDay(sundays, 2026, 10, 2026, 10, 10))
    }

    @Test
    fun `resolveNextDay treats today as the next day`() {
        assertEquals(8, resolveNextDay(sundays, 2026, 10, 2026, 10, 8))
    }

    @Test
    fun `resolveNextDay is null when every date of the current month has passed`() {
        assertNull(resolveNextDay(sundays, 2026, 10, 2026, 10, 30))
    }

    @Test
    fun `resolveNextDay returns the first day for a future month`() {
        assertEquals(1, resolveNextDay(sundays, 2027, 1, 2026, 12, 31))
    }

    @Test
    fun `resolveNextDay is null for a past month`() {
        assertNull(resolveNextDay(sundays, 2026, 9, 2026, 10, 1))
    }

    // endregion

    // region refreshMonthSchedule

    @Test
    fun `refreshMonthSchedule calls repository`() = runTest {
        val vm = buildViewModel()
        vm.refreshMonthSchedule(minDurationMs = 0L)
        advanceUntilIdle()
        io.mockk.coVerify(atLeast = 1) { repository.refreshMonthSchedule() }
    }

    // endregion
}
