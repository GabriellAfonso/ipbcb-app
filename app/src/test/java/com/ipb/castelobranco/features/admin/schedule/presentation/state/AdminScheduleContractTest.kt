package com.ipb.castelobranco.features.admin.schedule.presentation.state

import org.junit.Assert.assertEquals
import org.junit.Test
import java.time.LocalDate

class AdminScheduleContractTest {

    @Test
    fun `returns next month when 1 day remaining`() {
        val today = LocalDate.of(2025, 4, 30)
        val result = AdminScheduleUiState.defaultDate(today)
        assertEquals(LocalDate.of(2025, 5, 1), result.withDayOfMonth(1))
        assertEquals(5, result.monthValue)
    }

    @Test
    fun `returns next month when 9 days remaining`() {
        val today = LocalDate.of(2025, 4, 22)
        val result = AdminScheduleUiState.defaultDate(today)
        assertEquals(5, result.monthValue)
    }

    @Test
    fun `returns current month when exactly 10 days remaining`() {
        val today = LocalDate.of(2025, 4, 21)
        val result = AdminScheduleUiState.defaultDate(today)
        assertEquals(4, result.monthValue)
    }

    @Test
    fun `returns current month when 23 days remaining`() {
        val today = LocalDate.of(2025, 5, 8)
        val result = AdminScheduleUiState.defaultDate(today)
        assertEquals(5, result.monthValue)
    }

    @Test
    fun `returns current month on first day of month`() {
        val today = LocalDate.of(2025, 5, 1)
        val result = AdminScheduleUiState.defaultDate(today)
        assertEquals(5, result.monthValue)
    }

    @Test
    fun `year advances correctly when next month crosses year boundary`() {
        val today = LocalDate.of(2025, 12, 25)
        val result = AdminScheduleUiState.defaultDate(today)
        assertEquals(1, result.monthValue)
        assertEquals(2026, result.year)
    }

    @Test
    fun `returns current month when 25 days remaining`() {
        val today = LocalDate.of(2025, 4, 5)
        val result = AdminScheduleUiState.defaultDate(today)
        assertEquals(4, result.monthValue)
    }
}
