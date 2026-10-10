package com.ipb.castelobranco.features.worshiphub.songs.data.repository

import com.ipb.castelobranco.core.domain.snapshot.RefreshResult
import com.ipb.castelobranco.core.testing.errorResponse
import com.ipb.castelobranco.features.worshiphub.chordcharts.domain.repository.ChordChartRepository
import com.ipb.castelobranco.features.worshiphub.lyrics.domain.repository.LyricsRepository
import com.ipb.castelobranco.features.worshiphub.songs.data.api.SongsEditApi
import com.ipb.castelobranco.features.worshiphub.songs.data.api.UpdateSongBody
import com.ipb.castelobranco.features.worshiphub.songs.domain.model.SongEditFields
import com.ipb.castelobranco.features.worshiphub.songs.domain.model.SongFieldErrors
import com.ipb.castelobranco.features.worshiphub.songs.domain.model.SongWriteResult
import com.ipb.castelobranco.features.worshiphub.songs.domain.validation.SongEditValidator
import com.ipb.castelobranco.features.worshiphub.tables.data.dto.AllSongDto
import com.ipb.castelobranco.features.worshiphub.tables.domain.repository.SongsRepository
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import retrofit2.Response
import java.io.IOException

class SongEditRepositoryImplTest {

    private class FakeApi : SongsEditApi {
        var updateResponse: () -> Response<AllSongDto> = { Response.success(AllSongDto(7, "Oceans", "Hillsong")) }
        var deleteResponse: () -> Response<Unit> = { Response.success(204, Unit) }
        val updates = mutableListOf<Pair<Int, UpdateSongBody>>()

        override suspend fun update(id: Int, body: UpdateSongBody): Response<AllSongDto> {
            updates += id to body
            return updateResponse()
        }

        override suspend fun delete(id: Int): Response<Unit> = deleteResponse()
    }

    private val api = FakeApi()
    private val songsRepository: SongsRepository = mockk {
        coEvery { refreshAllSongs() } returns RefreshResult.Updated
        coEvery { refreshSongsBySunday() } returns RefreshResult.Updated
        coEvery { refreshTopSongs() } returns RefreshResult.Updated
    }
    private val chordChartRepository: ChordChartRepository = mockk {
        coEvery { refresh() } returns RefreshResult.Updated
    }
    private val lyricsRepository: LyricsRepository = mockk {
        coEvery { refresh() } returns RefreshResult.Updated
    }
    private val repository = SongEditRepositoryImpl(api, songsRepository, chordChartRepository, lyricsRepository)

    private val fields = SongEditFields("Oceans", "Hillsong", "")

    @Test
    fun `update sends every field and refreshes the lists that carry the title`() = runTest {
        val result = repository.update(7, fields)

        assertEquals(SongWriteResult.Success, result)
        assertEquals(7 to UpdateSongBody("Oceans", "Hillsong", ""), api.updates.single())
        coVerify {
            songsRepository.refreshAllSongs()
            songsRepository.refreshSongsBySunday()
            songsRepository.refreshTopSongs()
        }
    }

    @Test
    fun `update 409 is a duplicate`() = runTest {
        api.updateResponse = { errorResponse(409) }

        assertEquals(SongWriteResult.Duplicate, repository.update(7, fields))
        coVerify(exactly = 0) { songsRepository.refreshAllSongs() }
    }

    @Test
    fun `update 400 maps field errors to the app's messages`() = runTest {
        val body = """{"error_code":"VALIDATION_ERROR","detail":"Invalid.","field_errors":{"youtube_link":["bad"]}}"""
        api.updateResponse = { errorResponse(400, body) }

        val result = repository.update(7, fields)

        assertEquals(
            SongWriteResult.Invalid(SongFieldErrors(youtubeLink = SongEditValidator.LINK_INVALID)),
            result,
        )
    }

    @Test
    fun `update 403 is no permission and 404 is not found`() = runTest {
        api.updateResponse = { errorResponse(403) }
        assertEquals(SongWriteResult.NoPermission, repository.update(7, fields))

        api.updateResponse = { errorResponse(404) }
        assertEquals(SongWriteResult.NotFound, repository.update(7, fields))
    }

    @Test
    fun `no network is a failure`() = runTest {
        api.updateResponse = { throw IOException("offline") }

        assertTrue(repository.update(7, fields) is SongWriteResult.Failed)
    }

    @Test
    fun `delete refreshes the catalogue, chord charts and lyrics`() = runTest {
        assertEquals(SongWriteResult.Success, repository.delete(7))

        coVerify {
            songsRepository.refreshAllSongs()
            chordChartRepository.refresh()
            lyricsRepository.refresh()
        }
    }

    @Test
    fun `delete 409 is in use with the API's explanation`() = runTest {
        val detail = "A música \"Oceans\" não pode ser excluída: foi tocada em 7 domingos."
        val body = """{"error_code":"CONFLICT","detail":"${detail.replace("\"", "\\\"")}","plays":7,"setlist_dates":[]}"""
        api.deleteResponse = { errorResponse(409, body) }

        assertEquals(SongWriteResult.InUse(detail), repository.delete(7))
        coVerify(exactly = 0) { songsRepository.refreshAllSongs() }
    }
}
