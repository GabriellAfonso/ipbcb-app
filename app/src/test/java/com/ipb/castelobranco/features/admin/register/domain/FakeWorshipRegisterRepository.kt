package com.ipb.castelobranco.features.admin.register.domain

import com.ipb.castelobranco.core.domain.model.Song
import com.ipb.castelobranco.features.admin.register.domain.model.SundayPlayPushItem
import com.ipb.castelobranco.features.admin.register.domain.repository.WorshipRegisterRepository

class FakeWorshipRegisterRepository(
    var registerSongResult: (title: String, artist: String) -> Result<Song> = { title, artist ->
        Result.success(Song(id = 99, title = title, artist = artist, categoryName = ""))
    },
) : WorshipRegisterRepository {

    val registeredSongs = mutableListOf<Pair<String, String>>()

    override suspend fun pushSundayPlays(date: String, plays: List<SundayPlayPushItem>): Result<Unit> =
        Result.success(Unit)

    override suspend fun registerSong(title: String, artist: String): Result<Song> {
        registeredSongs += title to artist
        return registerSongResult(title, artist)
    }
}
