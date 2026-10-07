package com.ipb.castelobranco.features.admin.register.data.repository

import com.ipb.castelobranco.core.data.setlist.SetlistDto
import com.ipb.castelobranco.core.data.setlist.SetlistItemDto
import com.ipb.castelobranco.core.domain.error.AppError
import com.ipb.castelobranco.core.testing.errorResponse
import com.ipb.castelobranco.features.admin.register.data.api.SetlistAdminApi
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import retrofit2.Response
import java.io.IOException
import java.time.LocalDate

class SetlistConfirmationRepositoryImplTest {

    private class FakeApi : SetlistAdminApi {
        var byDate: (String) -> Response<SetlistDto> = { errorResponse(404) }
        var pending: () -> Response<List<SetlistDto>> = { Response.success(emptyList()) }
        var delete: (String) -> Response<Unit> = { Response.success(204, Unit) }
        val requestedDates = mutableListOf<String>()
        val deletedDates = mutableListOf<String>()

        override suspend fun byDate(date: String): Response<SetlistDto> {
            requestedDates += date
            return byDate.invoke(date)
        }

        override suspend fun pending(): Response<List<SetlistDto>> = pending.invoke()

        override suspend fun delete(date: String): Response<Unit> {
            deletedDates += date
            return delete.invoke(date)
        }
    }

    private val api = FakeApi()
    private val repository = SetlistConfirmationRepositoryImpl(api)
    private val sunday = LocalDate.of(2026, 10, 4)

    private fun dto(date: String) = SetlistDto(
        date = date,
        items = listOf(SetlistItemDto(1, 12, "Grande é o Senhor", "Adhemar", "G")),
    )

    @Test
    fun `reads the setlist of a date`() = runTest {
        api.byDate = { Response.success(dto(it)) }

        val setlist = repository.byDate(sunday).getOrThrow()

        assertEquals(listOf("2026-10-04"), api.requestedDates)
        assertEquals(sunday, setlist.date)
        assertEquals("G", setlist.items.single().tone)
    }

    @Test
    fun `a missing setlist fails with 404`() = runTest {
        val error = repository.byDate(sunday).exceptionOrNull()

        assertEquals(404, (error as AppError.Server).code)
    }

    @Test
    fun `no network fails as network`() = runTest {
        api.byDate = { throw IOException("offline") }

        assertTrue(repository.byDate(sunday).exceptionOrNull() is AppError.Network)
    }

    @Test
    fun `pending keeps the server order`() = runTest {
        api.pending = { Response.success(listOf(dto("2026-10-04"), dto("2026-09-27"))) }

        val dates = repository.pending().getOrThrow().map { it.date }

        assertEquals(listOf(sunday, LocalDate.of(2026, 9, 27)), dates)
    }

    @Test
    fun `pending refused is a permission error`() = runTest {
        api.pending = { errorResponse(403) }

        assertTrue(repository.pending().exceptionOrNull() is AppError.Auth)
    }

    @Test
    fun `deletes the setlist of a date`() = runTest {
        assertTrue(repository.delete(sunday).isSuccess)
        assertEquals(listOf("2026-10-04"), api.deletedDates)
    }

    @Test
    fun `deleting a missing setlist fails with 404`() = runTest {
        api.delete = { errorResponse(404) }

        assertEquals(404, (repository.delete(sunday).exceptionOrNull() as AppError.Server).code)
    }

    @Test
    fun `deleting with no network fails as network`() = runTest {
        api.delete = { throw IOException("offline") }

        assertTrue(repository.delete(sunday).exceptionOrNull() is AppError.Network)
    }
}
