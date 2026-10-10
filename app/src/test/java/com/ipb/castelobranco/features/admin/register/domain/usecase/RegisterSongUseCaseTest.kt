package com.ipb.castelobranco.features.admin.register.domain.usecase

import com.ipb.castelobranco.core.domain.error.AppError
import com.ipb.castelobranco.features.admin.register.domain.FakeWorshipRegisterRepository
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertSame
import org.junit.Assert.assertTrue
import org.junit.Test

class RegisterSongUseCaseTest {

    private val repository = FakeWorshipRegisterRepository()
    private val useCase = RegisterSongUseCase(repository)

    @Test
    fun `sends trimmed title and artist and returns the created song`() = runTest {
        val result = useCase(title = "  Oceans ", artist = " Hillsong  ")

        assertEquals(listOf("Oceans" to "Hillsong"), repository.registeredSongs)
        assertEquals("Oceans", result.getOrThrow().title)
    }

    @Test
    fun `repository failure is returned as is`() = runTest {
        val conflict = AppError.Server(code = 409)
        repository.registerSongResult = { _, _ -> Result.failure(conflict) }

        val result = useCase(title = "Oceans", artist = "Hillsong")

        assertTrue(result.isFailure)
        assertSame(conflict, result.exceptionOrNull())
    }
}
