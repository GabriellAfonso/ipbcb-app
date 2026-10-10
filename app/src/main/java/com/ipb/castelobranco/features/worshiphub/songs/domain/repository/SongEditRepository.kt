package com.ipb.castelobranco.features.worshiphub.songs.domain.repository

import com.ipb.castelobranco.features.worshiphub.songs.domain.model.SongEditFields
import com.ipb.castelobranco.features.worshiphub.songs.domain.model.SongWriteResult

interface SongEditRepository {

    /** Saves [fields] (already trimmed) as the song's title, artist and link. */
    suspend fun update(id: Int, fields: SongEditFields): SongWriteResult

    /** Deletes the song with its chord charts and lyrics; refused with [SongWriteResult.InUse] when in use. */
    suspend fun delete(id: Int): SongWriteResult
}
