package com.ipb.castelobranco.features.worshiphub.tables.data.fetcher

import com.ipb.castelobranco.core.data.snapshot.RetrofitSnapshotFetcher
import com.ipb.castelobranco.core.domain.snapshot.SnapshotFetcher
import com.ipb.castelobranco.features.worshiphub.tables.data.api.SongsTableApi
import com.ipb.castelobranco.features.worshiphub.tables.data.dto.SuggestedSongDto
import javax.inject.Inject

class SuggestedSongsSnapshotFetcher @Inject constructor(
    api: SongsTableApi
) : SnapshotFetcher<List<SuggestedSongDto>>,
    RetrofitSnapshotFetcher<List<SuggestedSongDto>>(
        call = { etag ->
            api.getSuggestedSongs(
                ifNoneMatch = etag,
                fixed = null
            )
        }
    )