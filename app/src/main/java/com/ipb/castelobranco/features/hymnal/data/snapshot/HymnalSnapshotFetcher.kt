package com.ipb.castelobranco.features.hymnal.data.snapshot

import com.ipb.castelobranco.core.data.snapshot.RetrofitSnapshotFetcher
import com.ipb.castelobranco.core.domain.snapshot.SnapshotFetcher
import com.ipb.castelobranco.features.hymnal.data.api.HymnalApi
import com.ipb.castelobranco.features.hymnal.data.dto.HymnDto
import com.ipb.castelobranco.features.profile.data.api.ProfileApi
import com.ipb.castelobranco.features.profile.data.dto.MeProfileDto
import javax.inject.Inject

class HymnalSnapshotFetcher @Inject constructor(
    api: HymnalApi
) : SnapshotFetcher<List<HymnDto>>,
    RetrofitSnapshotFetcher<List<HymnDto>>(
        call = { etag -> api.getHymnal(etag) }
    )

