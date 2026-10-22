package com.ipb.castelobranco.features.schedule.di

import com.ipb.castelobranco.core.data.snapshot.SnapshotCacheFactory
import com.ipb.castelobranco.core.domain.snapshot.SnapshotCache
import com.ipb.castelobranco.core.domain.snapshot.SnapshotFetcher
import com.ipb.castelobranco.features.schedule.data.api.ScheduleApi
import com.ipb.castelobranco.features.schedule.data.dto.MonthScheduleDto
import com.ipb.castelobranco.features.schedule.data.snapshot.ScheduleSnapshotFetcher
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent

@Module
@InstallIn(SingletonComponent::class)
object ScheduleSnapshotModule {

    @Provides
    fun provideScheduleSnapshotCache(
        factory: SnapshotCacheFactory
    ): SnapshotCache<MonthScheduleDto> =
        factory.create(
            key = "Month_Schedule",
            serializer = MonthScheduleDto.serializer()
        )

    @Provides
    fun provideProfileSnapshotFetcher(
        api: ScheduleApi
    ): SnapshotFetcher<MonthScheduleDto> =
        ScheduleSnapshotFetcher(api)
}
