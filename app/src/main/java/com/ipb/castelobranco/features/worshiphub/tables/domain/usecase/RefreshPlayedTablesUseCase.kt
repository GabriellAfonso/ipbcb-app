package com.ipb.castelobranco.features.worshiphub.tables.domain.usecase

import com.ipb.castelobranco.features.worshiphub.tables.domain.repository.SongsRepository
import javax.inject.Inject

/**
 * Re-reads the tables built from the played songs (Últimos Domingos, Mais tocadas, Top tons) after a Sunday is
 * registered, so they show it on return. Each one keeps what it had when its refresh fails.
 */
class RefreshPlayedTablesUseCase @Inject constructor(
    private val repository: SongsRepository,
) {
    suspend operator fun invoke() {
        repository.refreshSongsBySunday()
        repository.refreshTopSongs()
        repository.refreshTopTones()
    }
}
