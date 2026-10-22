package com.ipb.castelobranco.features.schedule.data.repository

import com.ipb.castelobranco.core.domain.error.AppError
import com.ipb.castelobranco.core.domain.snapshot.Logger
import com.ipb.castelobranco.core.domain.snapshot.NetworkResult
import com.ipb.castelobranco.core.domain.snapshot.RefreshResult
import com.ipb.castelobranco.core.domain.snapshot.SnapshotCache
import com.ipb.castelobranco.core.domain.snapshot.SnapshotFetcher
import com.ipb.castelobranco.core.domain.snapshot.SnapshotState
import com.ipb.castelobranco.features.schedule.data.dto.MemberDto
import com.ipb.castelobranco.features.schedule.data.dto.MonthScheduleDto
import com.ipb.castelobranco.features.schedule.data.dto.ScheduleEntryDto
import com.ipb.castelobranco.features.schedule.data.dto.ScheduleItemDto
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class ScheduleRepositoryImplTest {

    private lateinit var cache: SnapshotCache<MonthScheduleDto>
    private lateinit var fetcher: SnapshotFetcher<MonthScheduleDto>
    private lateinit var repository: ScheduleRepositoryImpl

    private val sampleDto = MonthScheduleDto(
        year = 2025,
        month = 5,
        schedule = mapOf(
            "Liturgia" to ScheduleEntryDto(
                time = "10h",
                items = listOf(ScheduleItemDto(day = 4, member = MemberDto(1, "João")))
            )
        )
    )

    @Before
    fun setup() {
        cache = mockk(relaxed = true)
        fetcher = mockk()
        repository = ScheduleRepositoryImpl(cache, fetcher, Logger.Noop)
    }

    // region observeMonthSchedule

    @Test
    fun `observeMonthSchedule returns flow starting with Loading`() {
        val state = repository.observeMonthSchedule()
        assertTrue(state is kotlinx.coroutines.flow.StateFlow)
    }

    // endregion

    // region getCurrentSnapshot

    @Test
    fun `getCurrentSnapshot returns Loading initially`() {
        val result = repository.getCurrentSnapshot()
        assertTrue(result is SnapshotState.Loading)
    }

    // endregion

    // region refreshMonthSchedule

    @Test
    fun `refreshMonthSchedule on Success returns Updated`() = runTest {
        coEvery { cache.loadETag() } returns null
        coEvery { fetcher.fetch(any()) } returns NetworkResult.Success(sampleDto, "v1")

        val result = repository.refreshMonthSchedule()

        assertEquals(RefreshResult.Updated, result)
    }

    @Test
    fun `refreshMonthSchedule on Success maps DTO to domain`() = runTest {
        coEvery { cache.loadETag() } returns null
        coEvery { fetcher.fetch(any()) } returns NetworkResult.Success(sampleDto, "v1")

        repository.refreshMonthSchedule()

        val state = repository.getCurrentSnapshot()
        assertTrue(state is SnapshotState.Data)
        val data = (state as SnapshotState.Data).value
        assertEquals(2025, data.year)
        assertEquals(5, data.month)
    }

    @Test
    fun `refreshMonthSchedule on Failure without cache returns Error`() = runTest {
        val error = RuntimeException("fail")
        coEvery { cache.loadETag() } returns null
        coEvery { fetcher.fetch(any()) } returns NetworkResult.Failure(error)
        coEvery { cache.load() } returns null

        val result = repository.refreshMonthSchedule()

        assertTrue(result is RefreshResult.Error)
    }

    // endregion

    // region clearScheduleCache

    @Test
    fun `clearScheduleCache clears cache and emits 401 error`() = runTest {
        coEvery { cache.loadETag() } returns null
        coEvery { fetcher.fetch(any()) } returns NetworkResult.Success(sampleDto, "v1")

        repository.refreshMonthSchedule()
        assertTrue(repository.getCurrentSnapshot() is SnapshotState.Data)

        repository.clearScheduleCache()

        val state = repository.getCurrentSnapshot()
        assertTrue(state is SnapshotState.Error)
        val error = (state as SnapshotState.Error).error
        assertTrue(error is AppError.Auth)
        assertEquals(401, (error as AppError.Auth).code)
        assertEquals("Faça login para ver a escala", error.userMessage)
        coVerify { cache.clear() }
    }

    // endregion
}
