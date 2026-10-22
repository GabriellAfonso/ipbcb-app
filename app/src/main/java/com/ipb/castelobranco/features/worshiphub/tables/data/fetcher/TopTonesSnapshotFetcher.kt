package com.ipb.castelobranco.features.worshiphub.tables.data.fetcher

import com.ipb.castelobranco.core.data.snapshot.RetrofitSnapshotFetcher
import com.ipb.castelobranco.core.domain.snapshot.SnapshotFetcher
import com.ipb.castelobranco.features.worshiphub.tables.data.api.SongsTableApi
import com.ipb.castelobranco.features.worshiphub.tables.data.dto.TopToneDto
import javax.inject.Inject

class TopTonesSnapshotFetcher @Inject constructor(
    api: SongsTableApi
) : SnapshotFetcher<List<TopToneDto>>,
    RetrofitSnapshotFetcher<List<TopToneDto>>(
        call = { etag -> api.getTopTones(etag) }
    )