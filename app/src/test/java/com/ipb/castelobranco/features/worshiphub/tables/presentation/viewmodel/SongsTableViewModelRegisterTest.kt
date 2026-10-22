package com.ipb.castelobranco.features.worshiphub.tables.presentation.viewmodel

import com.ipb.castelobranco.core.domain.access.AccessLevel
import com.ipb.castelobranco.core.domain.access.Role
import com.ipb.castelobranco.core.domain.access.Scope
import com.ipb.castelobranco.core.domain.snapshot.SnapshotState
import com.ipb.castelobranco.core.testing.FakeAccessRepository
import com.ipb.castelobranco.core.testing.accessOf
import com.ipb.castelobranco.features.worshiphub.tables.domain.repository.SongsRepository
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class SongsTableViewModelRegisterTest {

    private val testDispatcher = StandardTestDispatcher()
    private lateinit var repository: SongsRepository

    @Before
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
        repository = mockk(relaxed = true)
        every { repository.observeAllSongs() } returns flowOf(SnapshotState.Loading)
        every { repository.observeSongsBySunday() } returns flowOf(SnapshotState.Loading)
        every { repository.observeTopSongs() } returns flowOf(SnapshotState.Loading)
        every { repository.observeTopTones() } returns flowOf(SnapshotState.Loading)
        every { repository.observeSuggestedSongs() } returns flowOf(SnapshotState.Loading)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun `Registrar domingo shows only with manage on songs`() = runTest {
        val access = FakeAccessRepository()
        val viewModel = songsTableViewModel(repository, access = access)
        advanceUntilIdle()
        assertFalse(viewModel.canRegisterSunday.value)

        access.state.value = accessOf(Role.LEADER, Scope.SONGS to AccessLevel.MANAGE)
        advanceUntilIdle()
        assertTrue(viewModel.canRegisterSunday.value)

        access.state.value = accessOf(Role.MEDIA, Scope.SONGS to AccessLevel.VIEW)
        advanceUntilIdle()
        assertFalse(viewModel.canRegisterSunday.value)
    }
}
