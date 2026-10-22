package com.ipb.castelobranco.features.profile.data.repository

import com.ipb.castelobranco.core.domain.error.AppError
import com.ipb.castelobranco.features.profile.data.api.FakeProfileApi
import com.ipb.castelobranco.features.profile.data.dto.OwnMemberDto
import kotlinx.coroutines.test.runTest
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.ResponseBody.Companion.toResponseBody
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import retrofit2.Response
import java.io.IOException
import java.time.LocalDate

class OwnMemberRepositoryImplTest {

    private val api = FakeProfileApi()
    private val repository = OwnMemberRepositoryImpl(api)

    @Test
    fun `the linked record is read`() = runTest {
        api.ownMemberScript = {
            Response.success(
                OwnMemberDto(
                    id = 12, name = "Ana", role = "Diaconisa", ministries = listOf("Louvor", " "),
                    birthDay = 14, birthMonth = 3, baptismDate = "2001-05-20", gender = "F",
                )
            )
        }

        val member = repository.get().getOrThrow()!!

        assertEquals("Diaconisa", member.role)
        assertEquals(listOf("Louvor"), member.ministries)
        assertEquals(LocalDate.of(2001, 5, 20), member.baptismDate)
        assertEquals("F", member.gender)
    }

    @Test
    fun `not linked is no record, not an error`() = runTest {
        api.ownMemberScript = {
            Response.error(
                404,
                """{"error_code":"MEMBER_NOT_LINKED","detail":"Conta sem vínculo."}"""
                    .toResponseBody("application/json".toMediaType()),
            )
        }

        assertNull(repository.get().getOrThrow())
    }

    @Test
    fun `a network failure is an app error`() = runTest {
        api.ownMemberScript = { throw IOException("offline") }

        assertTrue(repository.get().exceptionOrNull() is AppError.Network)
    }
}
