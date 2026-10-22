package com.ipb.castelobranco.features.schedule.data.snapshot

import com.ipb.castelobranco.core.data.snapshot.RetrofitSnapshotFetcher
import com.ipb.castelobranco.features.profile.data.api.ProfileApi
import com.ipb.castelobranco.core.domain.snapshot.SnapshotFetcher
import com.ipb.castelobranco.features.profile.data.dto.MeProfileDto
import com.ipb.castelobranco.features.schedule.data.api.ScheduleApi
import com.ipb.castelobranco.features.schedule.data.dto.MonthScheduleDto
import javax.inject.Inject

class ScheduleSnapshotFetcher @Inject constructor(
    api: ScheduleApi
) : SnapshotFetcher<MonthScheduleDto>,
    RetrofitSnapshotFetcher<MonthScheduleDto>(
        call = { etag -> api.getMonthSchedule(etag) }
    )
