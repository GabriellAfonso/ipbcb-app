package com.ipb.castelobranco.features.admin.register.domain.repository

import com.ipb.castelobranco.core.domain.model.Song
import com.ipb.castelobranco.features.admin.register.domain.model.SundayPlayPushItem

interface WorshipRegisterRepository {
    suspend fun pushSundayPlays(
        date: String,
        plays: List<SundayPlayPushItem>
    ): Result<Unit>

    suspend fun registerSong(title: String, artist: String): Result<Song>
}
