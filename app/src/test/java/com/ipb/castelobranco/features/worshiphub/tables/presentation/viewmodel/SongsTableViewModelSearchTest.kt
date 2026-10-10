package com.ipb.castelobranco.features.worshiphub.tables.presentation.viewmodel

import com.ipb.castelobranco.core.domain.error.AppError
import com.ipb.castelobranco.core.domain.snapshot.SnapshotState
import com.ipb.castelobranco.features.worshiphub.tables.domain.model.SundaySet
import com.ipb.castelobranco.features.worshiphub.tables.domain.model.SundaySetItem
import com.ipb.castelobranco.features.worshiphub.tables.domain.repository.SongsRepository
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class SongsTableViewModelSearchTest {

    private val testDispatcher = StandardTestDispatcher()
    private lateinit var repository: SongsRepository

    private val april = SundaySet("07/04/2024", listOf(SundaySetItem(1, "Grandioso És Tu", "Cantor Cristão", "C")))
    private val may = SundaySet("12/05/2024", listOf(SundaySetItem(1, "Oceans", "Hillsong", "Bb")))
    private val sundays = listOf(april, may)

    @Before
    fun setup() {
        Dispatchers.setMain(testDispatcher)
        repository = mockk(relaxed = true)
        every { repository.observeAllSongs() } returns flowOf(SnapshotState.Loading)
        every { repository.observeSongsBySunday() } returns flowOf(SnapshotState.Data(sundays))
        every { repository.observeTopSongs() } returns flowOf(SnapshotState.Loading)
        every { repository.observeTopTones() } returns flowOf(SnapshotState.Loading)
        every { repository.observeSuggestedSongs() } returns flowOf(SnapshotState.Loading)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun `empty query shows every sunday`() = runTest {
        val viewModel = songsTableViewModel(repository)
        advanceUntilIdle()

        assertEquals(SnapshotState.Data(sundays), viewModel.filteredSundays.value)
    }

    @Test
    fun `query filters after the debounce and keeps lastSundays unfiltered`() = runTest {
        val viewModel = songsTableViewModel(repository)
        advanceUntilIdle()

        viewModel.onSearchQueryChange("grandioso es")
        assertEquals("grandioso es", viewModel.searchQuery.value)
        advanceTimeBy(100)
        runCurrent()
        assertEquals(SnapshotState.Data(sundays), viewModel.filteredSundays.value)

        advanceUntilIdle()
        assertEquals(SnapshotState.Data(listOf(april)), viewModel.filteredSundays.value)
        assertEquals(SnapshotState.Data(sundays), viewModel.lastSundays.value)
    }

    @Test
    fun `clearing the query restores the full list without waiting`() = runTest {
        val viewModel = songsTableViewModel(repository)
        viewModel.onSearchQueryChange("hillsong")
        advanceUntilIdle()
        assertEquals(SnapshotState.Data(listOf(may)), viewModel.filteredSundays.value)

        viewModel.onSearchQueryChange("")
        runCurrent()
        assertEquals(SnapshotState.Data(sundays), viewModel.filteredSundays.value)
    }

    @Test
    fun `error passes through regardless of query`() = runTest {
        val error = SnapshotState.Error(AppError.Unknown())
        every { repository.observeSongsBySunday() } returns flowOf(error)
        val viewModel = songsTableViewModel(repository)
        viewModel.onSearchQueryChange("oceans")
        advanceUntilIdle()

        assertEquals(error, viewModel.filteredSundays.value)
    }
}
