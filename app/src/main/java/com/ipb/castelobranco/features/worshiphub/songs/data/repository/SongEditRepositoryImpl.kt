package com.ipb.castelobranco.features.worshiphub.songs.data.repository

import com.ipb.castelobranco.core.domain.error.AppError
import com.ipb.castelobranco.core.domain.error.toAppError
import com.ipb.castelobranco.core.network.error.toAppError
import com.ipb.castelobranco.features.worshiphub.chordcharts.domain.repository.ChordChartRepository
import com.ipb.castelobranco.features.worshiphub.lyrics.domain.repository.LyricsRepository
import com.ipb.castelobranco.features.worshiphub.songs.data.api.SongsEditApi
import com.ipb.castelobranco.features.worshiphub.songs.data.api.UpdateSongBody
import com.ipb.castelobranco.features.worshiphub.songs.domain.model.SongEditFields
import com.ipb.castelobranco.features.worshiphub.songs.domain.model.SongFieldErrors
import com.ipb.castelobranco.features.worshiphub.songs.domain.model.SongWriteResult
import com.ipb.castelobranco.features.worshiphub.songs.domain.repository.SongEditRepository
import com.ipb.castelobranco.features.worshiphub.songs.domain.validation.SongEditValidator
import com.ipb.castelobranco.features.worshiphub.tables.domain.repository.SongsRepository
import kotlinx.coroutines.CancellationException
import retrofit2.Response
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class SongEditRepositoryImpl @Inject constructor(
    private val api: SongsEditApi,
    private val songsRepository: SongsRepository,
    private val chordChartRepository: ChordChartRepository,
    private val lyricsRepository: LyricsRepository,
) : SongEditRepository {

    override suspend fun update(id: Int, fields: SongEditFields): SongWriteResult =
        write(
            call = { api.update(id, UpdateSongBody(fields.title, fields.artist, fields.youtubeLink)) },
            onConflict = { SongWriteResult.Duplicate },
        ) {
            // The title is repeated in the Sunday history and the ranking, not only in the catalogue.
            songsRepository.refreshAllSongs()
            songsRepository.refreshSongsBySunday()
            songsRepository.refreshTopSongs()
        }

    override suspend fun delete(id: Int): SongWriteResult =
        // The API explains in `detail` why the song is in use (played on Sundays, on a setlist).
        write(call = { api.delete(id) }, onConflict = { SongWriteResult.InUse(it.userMessage) }) {
            // The API deletes the song's chord charts and lyrics with it.
            songsRepository.refreshAllSongs()
            chordChartRepository.refresh()
            lyricsRepository.refresh()
        }

    private suspend fun write(
        call: suspend () -> Response<*>,
        onConflict: (AppError.Server) -> SongWriteResult,
        onSuccess: suspend () -> Unit,
    ): SongWriteResult = try {
        val response = call()
        if (response.isSuccessful) {
            onSuccess()
            SongWriteResult.Success
        } else {
            response.toAppError().toResult(onConflict)
        }
    } catch (e: CancellationException) {
        throw e
    } catch (e: Throwable) {
        SongWriteResult.Failed(e.toAppError())
    }

    private fun AppError.toResult(onConflict: (AppError.Server) -> SongWriteResult): SongWriteResult = when {
        this is AppError.Auth && code == HTTP_FORBIDDEN -> SongWriteResult.NoPermission
        this is AppError.Server && code == HTTP_NOT_FOUND -> SongWriteResult.NotFound
        this is AppError.Server && code == HTTP_CONFLICT -> onConflict(this)
        this is AppError.Server && code == HTTP_BAD_REQUEST ->
            fieldErrors?.toInvalid() ?: SongWriteResult.Failed(this)
        else -> SongWriteResult.Failed(this)
    }

    private fun Map<String, List<String>>.toInvalid(): SongWriteResult.Invalid? {
        val errors = SongFieldErrors(
            title = SongEditValidator.TITLE_INVALID.takeIf { TITLE in this },
            artist = SongEditValidator.ARTIST_INVALID.takeIf { ARTIST in this },
            youtubeLink = SongEditValidator.LINK_INVALID.takeIf { YOUTUBE_LINK in this },
        )
        return if (errors.hasAny) SongWriteResult.Invalid(errors) else null
    }

    private companion object {
        const val HTTP_BAD_REQUEST = 400
        const val HTTP_FORBIDDEN = 403
        const val HTTP_NOT_FOUND = 404
        const val HTTP_CONFLICT = 409
        const val TITLE = "title"
        const val ARTIST = "artist"
        const val YOUTUBE_LINK = "youtube_link"
    }
}
