package com.ipb.castelobranco.features.admin.schedule.data.repository

import com.ipb.castelobranco.core.domain.error.AppError
import com.ipb.castelobranco.features.admin.schedule.data.api.AdminScheduleApi
import com.ipb.castelobranco.features.admin.schedule.data.dto.GenerateScheduleResponseDto
import com.ipb.castelobranco.features.admin.schedule.data.dto.GeneratedItemDto
import com.ipb.castelobranco.features.admin.schedule.data.dto.GeneratedScheduleTypeDto
import com.ipb.castelobranco.features.admin.schedule.data.dto.MemberItemDto
import com.ipb.castelobranco.features.admin.schedule.data.dto.MemberListDto
import com.ipb.castelobranco.features.admin.schedule.data.dto.SaveScheduleRequestDto
import com.ipb.castelobranco.features.admin.schedule.domain.model.Member
import com.ipb.castelobranco.features.admin.schedule.domain.model.ScheduleItem
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import io.mockk.slot
import kotlinx.coroutines.test.runTest
import okhttp3.ResponseBody
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import retrofit2.Response
import java.io.IOException

class AdminScheduleRepositoryImplTest {

    private lateinit var api: AdminScheduleApi
    private lateinit var repository: AdminScheduleRepositoryImpl

    @Before
    fun setup() {
        api = mockk()
        repository = AdminScheduleRepositoryImpl(api)
    }

    // region getMembers

    @Test
    fun `getMembers success maps MemberItemDto to Member correctly`() = runTest {
        coEvery { api.getMembers() } returns MemberListDto(
            members = listOf(MemberItemDto(id = 1, name = "João Silva"))
        )

        val result = repository.getMembers()

        assertTrue(result.isSuccess)
        assertEquals(listOf(Member(id = 1, name = "João Silva")), result.getOrNull())
    }

    @Test
    fun `getMembers maps multiple members preserving order`() = runTest {
        coEvery { api.getMembers() } returns MemberListDto(
            members = listOf(
                MemberItemDto(id = 1, name = "João"),
                MemberItemDto(id = 2, name = "Maria"),
                MemberItemDto(id = 3, name = "Pedro")
            )
        )

        val result = repository.getMembers()

        val members = result.getOrNull()!!
        assertEquals(3, members.size)
        assertEquals(Member(1, "João"), members[0])
        assertEquals(Member(2, "Maria"), members[1])
        assertEquals(Member(3, "Pedro"), members[2])
    }

    @Test
    fun `getMembers network failure returns AppError Network`() = runTest {
        coEvery { api.getMembers() } throws IOException("timeout")

        val result = repository.getMembers()

        assertTrue(result.isFailure)
        assertTrue(result.exceptionOrNull() is AppError.Network)
    }

    @Test
    fun `getMembers unknown failure returns AppError Unknown`() = runTest {
        coEvery { api.getMembers() } throws RuntimeException("unexpected")

        val result = repository.getMembers()

        assertTrue(result.isFailure)
        assertTrue(result.exceptionOrNull() is AppError.Unknown)
    }

    // endregion

    // region generateSchedule

    @Test
    fun `generateSchedule success maps all fields of GeneratedItemDto to ScheduleItem`() = runTest {
        coEvery { api.generateSchedule(any()) } returns GenerateScheduleResponseDto(
            year = 2026,
            month = 4,
            items = listOf(
                GeneratedItemDto(
                    date = "2026-04-01",
                    day = 1,
                    scheduleType = GeneratedScheduleTypeDto(id = 1, name = "Terça de Oração", time = "19:30"),
                    member = MemberItemDto(id = 5, name = "Maria")
                )
            )
        )

        val result = repository.generateSchedule(2026, 4)

        assertTrue(result.isSuccess)
        val items = result.getOrNull()!!
        assertEquals(1, items.size)
        with(items[0]) {
            assertEquals("2026-04-01", date)
            assertEquals(1, day)
            assertEquals("Terça de Oração", scheduleTypeName)
            assertEquals(1, scheduleTypeId)
            assertEquals(Member(id = 5, name = "Maria"), selectedMember)
        }
    }

    @Test
    fun `generateSchedule maps Terca de Oracao to scheduleTypeId 1`() = runTest {
        coEvery { api.generateSchedule(any()) } returns generateResponseWithType("Terça de Oração")

        val result = repository.generateSchedule(2026, 4)

        assertEquals(1, result.getOrNull()!![0].scheduleTypeId)
    }

    @Test
    fun `generateSchedule maps Quinta de Oracao to scheduleTypeId 2`() = runTest {
        coEvery { api.generateSchedule(any()) } returns generateResponseWithType("Quinta de Oração")

        val result = repository.generateSchedule(2026, 4)

        assertEquals(2, result.getOrNull()!![0].scheduleTypeId)
    }

    @Test
    fun `generateSchedule maps Domingo Liturgia de Adoracao to scheduleTypeId 3`() = runTest {
        coEvery { api.generateSchedule(any()) } returns
            generateResponseWithType("Domingo Liturgia de Adoração")

        val result = repository.generateSchedule(2026, 4)

        assertEquals(3, result.getOrNull()!![0].scheduleTypeId)
    }

    @Test
    fun `generateSchedule maps unknown schedule type name to scheduleTypeId 0`() = runTest {
        coEvery { api.generateSchedule(any()) } returns generateResponseWithType("Tipo Desconhecido")

        val result = repository.generateSchedule(2026, 4)

        assertEquals(0, result.getOrNull()!![0].scheduleTypeId)
    }

    @Test
    fun `generateSchedule returns empty list when API returns no items`() = runTest {
        coEvery { api.generateSchedule(any()) } returns
            GenerateScheduleResponseDto(year = 2026, month = 4, items = emptyList())

        val result = repository.generateSchedule(2026, 4)

        assertTrue(result.isSuccess)
        assertTrue(result.getOrNull()!!.isEmpty())
    }

    @Test
    fun `generateSchedule passes year and month to API`() = runTest {
        coEvery { api.generateSchedule(any()) } returns
            GenerateScheduleResponseDto(year = 2027, month = 7, items = emptyList())

        repository.generateSchedule(2027, 7)

        coVerify { api.generateSchedule(match { it.year == 2027 && it.month == 7 }) }
    }

    @Test
    fun `generateSchedule network failure returns AppError Network`() = runTest {
        coEvery { api.generateSchedule(any()) } throws IOException("no connection")

        val result = repository.generateSchedule(2026, 4)

        assertTrue(result.isFailure)
        assertTrue(result.exceptionOrNull() is AppError.Network)
    }

    @Test
    fun `generateSchedule unknown failure returns AppError Unknown`() = runTest {
        coEvery { api.generateSchedule(any()) } throws RuntimeException("unexpected")

        val result = repository.generateSchedule(2026, 4)

        assertTrue(result.isFailure)
        assertTrue(result.exceptionOrNull() is AppError.Unknown)
    }

    // endregion

    // region saveSchedule

    @Test
    fun `saveSchedule success returns Result success`() = runTest {
        val response = mockk<Response<Unit>> { every { isSuccessful } returns true }
        coEvery { api.saveSchedule(any()) } returns response

        val result = repository.saveSchedule(
            year = 2026,
            month = 4,
            items = listOf(ScheduleItem("2026-04-01", 1, "Terça de Oração", 1, Member(1, "João")))
        )

        assertTrue(result.isSuccess)
    }

    @Test
    fun `saveSchedule maps ScheduleItem to SaveScheduleItemDto with correct fields`() = runTest {
        val captured = slot<SaveScheduleRequestDto>()
        val response = mockk<Response<Unit>> { every { isSuccessful } returns true }
        coEvery { api.saveSchedule(capture(captured)) } returns response

        repository.saveSchedule(
            year = 2026,
            month = 4,
            items = listOf(
                ScheduleItem(
                    date = "2026-04-01",
                    day = 1,
                    scheduleTypeName = "Terça de Oração",
                    scheduleTypeId = 1,
                    selectedMember = Member(id = 7, name = "Ana")
                )
            )
        )

        with(captured.captured) {
            assertEquals(2026, year)
            assertEquals(4, month)
            assertEquals(1, items.size)
            assertEquals("2026-04-01", items[0].date)
            assertEquals(1, items[0].scheduleTypeId)
            assertEquals(7, items[0].memberId)
        }
    }

    @Test
    fun `saveSchedule server error with new API format returns detail as message`() = runTest {
        val errorBody = mockk<ResponseBody> {
            every { string() } returns """{"error_code":"DUPLICATE","detail":"Escala já existe para este mês"}"""
        }
        val response = mockk<Response<Unit>> {
            every { isSuccessful } returns false
            every { code() } returns 422
            every { errorBody() } returns errorBody
        }
        coEvery { api.saveSchedule(any()) } returns response

        val result = repository.saveSchedule(2026, 4, emptyList())

        assertTrue(result.isFailure)
        val error = result.exceptionOrNull() as AppError.Server
        assertEquals(422, error.code)
        assertEquals("Escala já existe para este mês", error.message)
        assertEquals("DUPLICATE", error.errorCode)
    }

    @Test
    fun `saveSchedule server error with legacy format uses raw body as message`() = runTest {
        val errorBody = mockk<ResponseBody> {
            every { string() } returns """{"error":"Escala já existe para este mês"}"""
        }
        val response = mockk<Response<Unit>> {
            every { isSuccessful } returns false
            every { code() } returns 422
            every { errorBody() } returns errorBody
        }
        coEvery { api.saveSchedule(any()) } returns response

        val result = repository.saveSchedule(2026, 4, emptyList())

        assertTrue(result.isFailure)
        val error = result.exceptionOrNull() as AppError.Server
        assertEquals(422, error.code)
        assertEquals("""{"error":"Escala já existe para este mês"}""", error.message)
    }

    @Test
    fun `saveSchedule server error with non-JSON body uses raw string as message`() = runTest {
        val errorBody = mockk<ResponseBody> {
            every { string() } returns "Internal Server Error"
        }
        val response = mockk<Response<Unit>> {
            every { isSuccessful } returns false
            every { code() } returns 500
            every { errorBody() } returns errorBody
        }
        coEvery { api.saveSchedule(any()) } returns response

        val result = repository.saveSchedule(2026, 4, emptyList())

        val error = result.exceptionOrNull() as AppError.Server
        assertEquals(500, error.code)
        assertEquals("Internal Server Error", error.message)
    }

    @Test
    fun `saveSchedule server error with null body uses HTTP code as message`() = runTest {
        val response = mockk<Response<Unit>> {
            every { isSuccessful } returns false
            every { code() } returns 500
            every { errorBody() } returns null
        }
        coEvery { api.saveSchedule(any()) } returns response

        val result = repository.saveSchedule(2026, 4, emptyList())

        val error = result.exceptionOrNull() as AppError.Server
        assertEquals("HTTP 500", error.message)
    }

    @Test
    fun `saveSchedule network failure returns AppError Network`() = runTest {
        coEvery { api.saveSchedule(any()) } throws IOException("connection reset")

        val result = repository.saveSchedule(2026, 4, emptyList())

        assertTrue(result.isFailure)
        assertTrue(result.exceptionOrNull() is AppError.Network)
    }

    // endregion

    // region helpers

    private fun generateResponseWithType(typeName: String) = GenerateScheduleResponseDto(
        year = 2026,
        month = 4,
        items = listOf(
            GeneratedItemDto(
                date = "2026-04-01",
                day = 1,
                scheduleType = GeneratedScheduleTypeDto(id = 0, name = typeName, time = "19:30"),
                member = MemberItemDto(id = 1, name = "A")
            )
        )
    )

    // endregion
}
