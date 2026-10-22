package com.ipb.castelobranco.features.worshiphub.register.data.repository

import com.ipb.castelobranco.core.domain.error.AppError
import com.ipb.castelobranco.features.worshiphub.register.data.api.WorshipRegisterApi
import com.ipb.castelobranco.features.worshiphub.register.data.dto.RegisterSundayPlaysResponseDto
import com.ipb.castelobranco.features.worshiphub.register.domain.model.SundayPlayPushItem
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import okhttp3.ResponseBody.Companion.toResponseBody
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import retrofit2.Response
import java.io.IOException

class WorshipRegisterRepositoryImplTest {

    private lateinit var api: WorshipRegisterApi
    private lateinit var repository: WorshipRegisterRepositoryImpl

    private val plays = listOf(
        SundayPlayPushItem(songId = 1, position = 1, tone = "G"),
        SundayPlayPushItem(songId = 2, position = 2, tone = "D"),
    )

    @Before
    fun setup() {
        api = mockk()
        repository = WorshipRegisterRepositoryImpl(api)
    }

    // region success

    @Test
    fun `pushSundayPlays success returns Result success`() = runTest {
        coEvery { api.registerSundayPlays(any()) } returns
            Response.success(RegisterSundayPlaysResponseDto(created = 2))

        val result = repository.pushSundayPlays("04/05/2025", plays)

        assertTrue(result.isSuccess)
    }

    @Test
    fun `pushSundayPlays sends correct request to api`() = runTest {
        coEvery { api.registerSundayPlays(any()) } returns
            Response.success(RegisterSundayPlaysResponseDto(created = 2))

        repository.pushSundayPlays("04/05/2025", plays)

        coVerify { api.registerSundayPlays(match { it.date == "04/05/2025" && it.plays.size == 2 }) }
    }

    // endregion

    // region server error

    @Test
    fun `pushSundayPlays server error returns AppError Server`() = runTest {
        coEvery { api.registerSundayPlays(any()) } returns
            Response.error(500, "error".toResponseBody())

        val result = repository.pushSundayPlays("04/05/2025", plays)

        assertTrue(result.isFailure)
        assertTrue(result.exceptionOrNull() is AppError.Server)
        assertEquals(500, (result.exceptionOrNull() as AppError.Server).code)
    }

    // endregion

    // region network error

    @Test
    fun `pushSundayPlays IOException maps to AppError Network`() = runTest {
        coEvery { api.registerSundayPlays(any()) } throws IOException("no connection")

        val result = repository.pushSundayPlays("04/05/2025", plays)

        assertTrue(result.isFailure)
        assertTrue(result.exceptionOrNull() is AppError.Network)
    }

    // endregion

    // region unknown error

    @Test
    fun `pushSundayPlays unexpected exception maps to AppError Unknown`() = runTest {
        coEvery { api.registerSundayPlays(any()) } throws IllegalStateException("unexpected")

        val result = repository.pushSundayPlays("04/05/2025", plays)

        assertTrue(result.isFailure)
        assertTrue(result.exceptionOrNull() is AppError.Unknown)
    }

    // endregion
}
