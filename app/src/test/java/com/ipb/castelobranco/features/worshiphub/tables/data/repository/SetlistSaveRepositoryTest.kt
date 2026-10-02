package com.ipb.castelobranco.features.worshiphub.tables.data.repository

import com.ipb.castelobranco.core.data.setlist.SetlistDto
import com.ipb.castelobranco.core.data.setlist.SetlistItemDto
import com.ipb.castelobranco.core.testing.errorResponse
import com.ipb.castelobranco.features.worshiphub.tables.data.api.SaveSetlistBody
import com.ipb.castelobranco.features.worshiphub.tables.data.api.SaveSetlistItemBody
import com.ipb.castelobranco.features.worshiphub.tables.data.api.SetlistSaveApi
import com.ipb.castelobranco.features.worshiphub.tables.domain.model.SaveSetlistFailure
import com.ipb.castelobranco.features.worshiphub.tables.domain.model.SaveSetlistResult
import com.ipb.castelobranco.features.worshiphub.tables.domain.model.SetlistEntry
import kotlinx.coroutines.test.runTest
import kotlinx.serialization.json.Json
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import retrofit2.Response
import java.io.IOException
import java.time.LocalDate

class SetlistSaveRepositoryTest {

    private class FakeApi(var next: () -> Response<SetlistDto>) : SetlistSaveApi {
        val calls = mutableListOf<Pair<String, SaveSetlistBody>>()
        override suspend fun save(date: String, body: SaveSetlistBody): Response<SetlistDto> {
            calls += date to body
            return next()
        }
    }

    private val sunday = LocalDate.of(2026, 10, 4)
    private val entries = listOf(SetlistEntry(1, 12, "G"), SetlistEntry(3, 55, "A#"))
    private val saved = SetlistDto(
        date = "2026-10-04",
        items = listOf(SetlistItemDto(1, 12, "Grande é o Senhor", "Adhemar", "G")),
        savedByName = "Ana",
        savedAt = "2026-10-01T19:42:10-03:00",
    )

    private fun repository(api: FakeApi) = SetlistSaveRepositoryImpl(api, Json { ignoreUnknownKeys = true })

    @Test
    fun `sends the date and the entries, answers with the stored setlist`() = runTest {
        val api = FakeApi { Response.success(saved) }

        val result = repository(api).save(sunday, entries)

        assertEquals("2026-10-04", api.calls.single().first)
        assertEquals(
            listOf(SaveSetlistItemBody(12, 1, "G"), SaveSetlistItemBody(55, 3, "A#")),
            api.calls.single().second.items,
        )
        assertEquals(sunday, (result as SaveSetlistResult.Saved).setlist.date)
    }

    @Test
    fun `403 is no permission`() = runTest {
        val result = repository(FakeApi { errorResponse(403) }).save(sunday, entries)
        assertEquals(SaveSetlistResult.Failed(SaveSetlistFailure.NoPermission), result)
    }

    @Test
    fun `400 is invalid with the server detail`() = runTest {
        val body = """{"error_code":"VALIDATION_ERROR","detail":"2026-10-05 não é domingo."}"""
        val result = repository(FakeApi { errorResponse(400, body) }).save(sunday, entries)

        val failure = (result as SaveSetlistResult.Failed).failure as SaveSetlistFailure.Invalid
        assertEquals("2026-10-05 não é domingo.", failure.error.userMessage)
    }

    @Test
    fun `404 counts the missing songs`() = runTest {
        val body = """{"error_code":"NOT_FOUND","detail":"Músicas não encontradas.","missing_song_ids":[12,55]}"""
        val result = repository(FakeApi { errorResponse(404, body) }).save(sunday, entries)

        assertEquals(SaveSetlistResult.Failed(SaveSetlistFailure.MissingSongs(2)), result)
    }

    @Test
    fun `no network is no connection`() = runTest {
        val result = repository(FakeApi { throw IOException("offline") }).save(sunday, entries)
        assertEquals(SaveSetlistResult.Failed(SaveSetlistFailure.NoConnection), result)
    }

    @Test
    fun `a server error is other`() = runTest {
        val result = repository(FakeApi { errorResponse(500) }).save(sunday, entries)
        assertTrue((result as SaveSetlistResult.Failed).failure is SaveSetlistFailure.Other)
    }
}
