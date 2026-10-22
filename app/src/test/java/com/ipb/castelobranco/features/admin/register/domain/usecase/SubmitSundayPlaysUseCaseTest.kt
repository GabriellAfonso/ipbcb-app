package com.ipb.castelobranco.features.admin.register.domain.usecase

import com.ipb.castelobranco.core.domain.error.AppError
import com.ipb.castelobranco.features.admin.register.domain.repository.WorshipRegisterRepository
import com.ipb.castelobranco.features.admin.register.presentation.state.SundaySongRowState
import com.ipb.castelobranco.core.domain.model.Song
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import java.time.LocalDate

class SubmitSundayPlaysUseCaseTest {

    private lateinit var repository: WorshipRegisterRepository
    private lateinit var useCase: SubmitSundayPlaysUseCase

    private fun song(id: Int) = Song(id = id, title = "Song $id", artist = "", categoryName = "")

    private fun row(
        position: Int,
        songQuery: String = "",
        selectedSongId: Int? = null,
        tone: String = ""
    ) = SundaySongRowState(position = position, songQuery = songQuery, selectedSongId = selectedSongId, tone = tone)

    @Before
    fun setup() {
        repository = mockk()
        useCase = SubmitSundayPlaysUseCase(repository)
    }

    // region validation errors

    @Test
    fun `incomplete row returns ValidationError without calling repository`() = runTest {
        val rows = listOf(row(1, songQuery = "Song", tone = ""))
        val songs = listOf(song(1))

        val result = useCase(rows, songs, LocalDate.of(2025, 6, 15))

        assertTrue(result is SubmitSundayPlaysUseCase.Result.ValidationError)
        coVerify(exactly = 0) { repository.pushSundayPlays(any(), any()) }
    }

    @Test
    fun `ValidationError contains errors by position`() = runTest {
        val rows = listOf(row(1, songQuery = "Song"))
        val songs = listOf(song(1))

        val result = useCase(rows, songs, LocalDate.of(2025, 6, 15))

        val error = result as SubmitSundayPlaysUseCase.Result.ValidationError
        assertTrue(error.errorsByPosition.containsKey(1))
    }

    // endregion

    // region success

    @Test
    fun `valid rows with successful repository returns Success`() = runTest {
        val songs = listOf(song(1))
        val rows = listOf(row(1, selectedSongId = 1, tone = "C"))
        coEvery { repository.pushSundayPlays(any(), any()) } returns Result.success(Unit)

        val result = useCase(rows, songs, LocalDate.of(2025, 6, 15))

        assertEquals(SubmitSundayPlaysUseCase.Result.Success, result)
    }

    @Test
    fun `valid rows call repository with correct date`() = runTest {
        val songs = listOf(song(1))
        val rows = listOf(row(1, selectedSongId = 1, tone = "C"))
        coEvery { repository.pushSundayPlays(any(), any()) } returns Result.success(Unit)

        useCase(rows, songs, LocalDate.of(2025, 6, 15))

        coVerify { repository.pushSundayPlays(date = "2025-06-15", any()) }
    }

    @Test
    fun `null selectedDate calls repository with empty date string`() = runTest {
        val songs = listOf(song(1))
        val rows = listOf(row(1, selectedSongId = 1, tone = "C"))
        coEvery { repository.pushSundayPlays(any(), any()) } returns Result.success(Unit)

        useCase(rows, songs, selectedDate = null)

        coVerify { repository.pushSundayPlays(date = "", any()) }
    }

    // endregion

    // region failure

    @Test
    fun `repository throws AppError with userMessage returns Failure with that message`() = runTest {
        val songs = listOf(song(1))
        val rows = listOf(row(1, selectedSongId = 1, tone = "C"))
        coEvery { repository.pushSundayPlays(any(), any()) } throws
            AppError.Server(code = 400, message = "raw body", userMessage = "Servidor indisponível")

        val result = useCase(rows, songs, LocalDate.of(2025, 6, 15))

        val failure = result as SubmitSundayPlaysUseCase.Result.Failure
        assertEquals("Servidor indisponível", failure.message)
    }

    @Test
    fun `userMessage with surrounding spaces is trimmed`() = runTest {
        val songs = listOf(song(1))
        val rows = listOf(row(1, selectedSongId = 1, tone = "C"))
        coEvery { repository.pushSundayPlays(any(), any()) } throws
            AppError.Server(code = 400, userMessage = "  erro  ")

        val result = useCase(rows, songs, LocalDate.of(2025, 6, 15))

        val failure = result as SubmitSundayPlaysUseCase.Result.Failure
        assertEquals("erro", failure.message)
    }

    @Test
    fun `exception with null message returns fallback message`() = runTest {
        val songs = listOf(song(1))
        val rows = listOf(row(1, selectedSongId = 1, tone = "C"))
        coEvery { repository.pushSundayPlays(any(), any()) } throws RuntimeException(null as String?)

        val result = useCase(rows, songs, LocalDate.of(2025, 6, 15))

        val failure = result as SubmitSundayPlaysUseCase.Result.Failure
        assertEquals("Erro inesperado ao enviar.", failure.message)
    }

    @Test
    fun `exception with blank message returns fallback message`() = runTest {
        val songs = listOf(song(1))
        val rows = listOf(row(1, selectedSongId = 1, tone = "C"))
        coEvery { repository.pushSundayPlays(any(), any()) } throws RuntimeException("   ")

        val result = useCase(rows, songs, LocalDate.of(2025, 6, 15))

        val failure = result as SubmitSundayPlaysUseCase.Result.Failure
        assertEquals("Erro inesperado ao enviar.", failure.message)
    }

    // endregion
}
