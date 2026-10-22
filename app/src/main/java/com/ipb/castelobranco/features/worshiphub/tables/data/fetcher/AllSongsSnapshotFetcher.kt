package com.ipb.castelobranco.features.worshiphub.tables.data.fetcher

import com.ipb.castelobranco.core.data.snapshot.RetrofitSnapshotFetcher
import com.ipb.castelobranco.core.domain.snapshot.SnapshotFetcher
import com.ipb.castelobranco.features.worshiphub.tables.data.api.SongsTableApi
import com.ipb.castelobranco.features.worshiphub.tables.data.dto.AllSongDto
import javax.inject.Inject

class AllSongsSnapshotFetcher @Inject constructor(
    api: SongsTableApi
) : SnapshotFetcher<List<AllSongDto>>,
    RetrofitSnapshotFetcher<List<AllSongDto>>(
        call = { etag -> api.getAllSongs(etag) }
    )