package com.ipb.castelobranco.core.presentation.viewmodel

import com.ipb.castelobranco.core.domain.error.AppError
import com.ipb.castelobranco.core.domain.model.Birthday
import com.ipb.castelobranco.core.domain.snapshot.SnapshotState
import com.ipb.castelobranco.core.domain.usecase.GetYearBirthdaysUseCase
import com.ipb.castelobranco.core.testing.FakeMembersRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.UnconfinedTestDispatcher
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
import java.time.LocalDate

@OptIn(ExperimentalCoroutinesApi::class)
class BirthdaysViewModelTest {

    private val testDispatcher = UnconfinedTestDispatcher()

    @Before
    fun setup() {
        Dispatchers.setMain(testDispatcher)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    private fun buildViewModel(repository: FakeMembersRepository) =
        BirthdaysViewModel(GetYearBirthdaysUseCase(repository))

    @Test
    fun `groups the year into twelve months ordered by day`() = runTest(testDispatcher) {
        val repository = FakeMembersRepository(
            SnapshotState.Data(
                listOf(
                    Birthday(name = "Bob", month = 1, day = 20),
                    Birthday(name = "Alice", month = 1, day = 5),
                    Birthday(name = "Carlos", month = 12, day = 31),
                )
            )
        )
        val viewModel = buildViewModel(repository)
        backgroundScope.launch { viewModel.uiState.collect() }

        val state = viewModel.uiState.value

        assertNull(state.error)
        assertFalse(state.isLoading)
        assertEquals((1..12).toList(), state.months.map { it.month })
        assertEquals("Janeiro", state.months.first().name)
        assertEquals(listOf("Alice", "Bob"), state.months.first().birthdays.map { it.name })
        assertEquals(listOf("Carlos"), state.months.last().birthdays.map { it.name })
        assertTrue(state.months.subList(1, 11).all { it.birthdays.isEmpty() })
    }

    @Test
    fun `marks the current month`() = runTest(testDispatcher) {
        val viewModel = buildViewModel(FakeMembersRepository(SnapshotState.Data(emptyList())))
        backgroundScope.launch { viewModel.uiState.collect() }

        val current = LocalDate.now().monthValue
        val state = viewModel.uiState.value

        assertEquals(listOf(current), state.months.filter { it.isCurrent }.map { it.month })
        assertEquals(current, state.currentMonth)
    }

    @Test
    fun `loading snapshot shows loading`() = runTest(testDispatcher) {
        val viewModel = buildViewModel(FakeMembersRepository(SnapshotState.Loading))
        backgroundScope.launch { viewModel.uiState.collect() }

        assertTrue(viewModel.uiState.value.isLoading)
    }

    @Test
    fun `auth error offers login`() = runTest(testDispatcher) {
        val viewModel = buildViewModel(FakeMembersRepository(SnapshotState.Error(AppError.Auth(code = 401))))
        backgroundScope.launch { viewModel.uiState.collect() }

        val state = viewModel.uiState.value

        assertEquals("Faça login para continuar.", state.error)
        assertTrue(state.showLoginButton)
    }

    @Test
    fun `network error does not offer login`() = runTest(testDispatcher) {
        val viewModel = buildViewModel(FakeMembersRepository(SnapshotState.Error(AppError.Network())))
        backgroundScope.launch { viewModel.uiState.collect() }

        assertFalse(viewModel.uiState.value.showLoginButton)
    }

    @Test
    fun `refresh asks the repository`() = runTest(testDispatcher) {
        val repository = FakeMembersRepository(SnapshotState.Data(emptyList()))
        val viewModel = buildViewModel(repository)

        viewModel.refresh()
        advanceUntilIdle()

        assertEquals(1, repository.refreshCalls)
        assertFalse(viewModel.isRefreshing.value)
    }
}
