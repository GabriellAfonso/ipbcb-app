package com.ipb.castelobranco.features.admin.register.domain.usecase

import com.ipb.castelobranco.core.domain.model.Song
import com.ipb.castelobranco.features.admin.register.domain.repository.WorshipRegisterRepository
import javax.inject.Inject

/** Adds a song to the catalogue. Failures arrive as [com.ipb.castelobranco.core.domain.error.AppError]. */
class RegisterSongUseCase @Inject constructor(
    private val repository: WorshipRegisterRepository
) {
    suspend operator fun invoke(title: String, artist: String): Result<Song> =
        repository.registerSong(title = title.trim(), artist = artist.trim())
}
