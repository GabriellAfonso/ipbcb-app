package com.ipb.castelobranco.features.admin.register.data.repository

import com.ipb.castelobranco.core.domain.model.Song
import com.ipb.castelobranco.features.admin.register.data.api.WorshipRegisterApi
import com.ipb.castelobranco.features.admin.register.data.dto.RegisterSongRequestDto
import com.ipb.castelobranco.features.admin.register.data.mapper.buildRegisterRequest
import com.ipb.castelobranco.features.admin.register.domain.repository.WorshipRegisterRepository
import com.ipb.castelobranco.features.admin.register.domain.model.SundayPlayPushItem
import com.ipb.castelobranco.core.domain.error.mapError
import com.ipb.castelobranco.core.network.error.toAppError
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class WorshipRegisterRepositoryImpl @Inject constructor(
    private val api: WorshipRegisterApi,
) : WorshipRegisterRepository {

    override suspend fun pushSundayPlays(
        date: String,
        plays: List<SundayPlayPushItem>
    ): Result<Unit> = runCatching {
        val body = buildRegisterRequest(date = date, plays = plays)
        val response = api.registerSundayPlays(body)
        if (!response.isSuccessful) throw response.toAppError()
        Unit
    }.mapError()

    override suspend fun registerSong(title: String, artist: String): Result<Song> = runCatching {
        val response = api.registerSong(RegisterSongRequestDto(title = title, artist = artist))
        if (!response.isSuccessful) throw response.toAppError()
        val body = response.body() ?: error("Empty body on song registration")
        Song(
            id = body.id,
            title = body.title,
            artist = body.artist,
            categoryName = body.categoryName,
            youtubeLink = body.youtubeLink,
        )
    }.mapError()
}
