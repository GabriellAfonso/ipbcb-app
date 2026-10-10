package com.ipb.castelobranco.features.worshiphub.songs.domain

import com.ipb.castelobranco.features.worshiphub.songs.domain.model.SongEditFields
import com.ipb.castelobranco.features.worshiphub.songs.domain.model.SongWriteResult
import com.ipb.castelobranco.features.worshiphub.songs.domain.repository.SongEditRepository

class FakeSongEditRepository(
    var updateResult: SongWriteResult = SongWriteResult.Success,
    var deleteResult: SongWriteResult = SongWriteResult.Success,
) : SongEditRepository {

    val updates = mutableListOf<Pair<Int, SongEditFields>>()
    val deletes = mutableListOf<Int>()

    override suspend fun update(id: Int, fields: SongEditFields): SongWriteResult {
        updates += id to fields
        return updateResult
    }

    override suspend fun delete(id: Int): SongWriteResult {
        deletes += id
        return deleteResult
    }
}
