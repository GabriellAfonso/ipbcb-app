package com.ipb.castelobranco.features.events.di

import com.ipb.castelobranco.core.data.snapshot.RetrofitSnapshotFetcher
import com.ipb.castelobranco.core.data.snapshot.SnapshotCacheFactory
import com.ipb.castelobranco.core.domain.snapshot.SnapshotCache
import com.ipb.castelobranco.core.domain.snapshot.SnapshotFetcher
import com.ipb.castelobranco.features.events.data.api.EventsApi
import com.ipb.castelobranco.features.events.data.dto.EventPublicDto
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import kotlinx.serialization.builtins.ListSerializer

@Module
@InstallIn(SingletonComponent::class)
object EventsSnapshotModule {

    private const val CACHE_KEY = "events"

    @Provides
    fun provideEventsSnapshotCache(
        factory: SnapshotCacheFactory,
    ): SnapshotCache<List<EventPublicDto>> =
        factory.create(
            key = CACHE_KEY,
            serializer = ListSerializer(EventPublicDto.serializer()),
        )

    @Provides
    fun provideEventsSnapshotFetcher(
        api: EventsApi,
    ): SnapshotFetcher<List<EventPublicDto>> =
        RetrofitSnapshotFetcher(call = { etag -> api.getEvents(etag) })
}
