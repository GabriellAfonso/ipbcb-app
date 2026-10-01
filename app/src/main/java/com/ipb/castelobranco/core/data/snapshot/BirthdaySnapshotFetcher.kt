package com.ipb.castelobranco.core.data.snapshot

import com.ipb.castelobranco.core.data.api.MembersApi
import com.ipb.castelobranco.core.data.api.MembersEndpoints
import com.ipb.castelobranco.core.data.dto.BirthdaysResponseDto
import com.ipb.castelobranco.core.domain.snapshot.SnapshotFetcher
import javax.inject.Inject

/** Fetches the whole year: the home card and the worker filter the current month locally. */
class BirthdaySnapshotFetcher @Inject constructor(
    private val api: MembersApi,
) : SnapshotFetcher<BirthdaysResponseDto>,
    RetrofitSnapshotFetcher<BirthdaysResponseDto>(
        call = { etag -> api.getBirthdays(month = MembersEndpoints.WHOLE_YEAR, ifNoneMatch = etag) }
    )
