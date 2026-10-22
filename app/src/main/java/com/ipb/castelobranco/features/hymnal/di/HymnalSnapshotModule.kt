package com.ipb.castelobranco.features.hymnal.di

import com.ipb.castelobranco.core.data.snapshot.SnapshotCacheFactory
import com.ipb.castelobranco.core.domain.snapshot.SnapshotCache
import com.ipb.castelobranco.core.domain.snapshot.SnapshotFetcher
import com.ipb.castelobranco.features.hymnal.data.api.HymnalApi
import com.ipb.castelobranco.features.hymnal.data.dto.HymnDto
import com.ipb.castelobranco.features.hymnal.data.snapshot.HymnalSnapshotFetcher
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import kotlinx.serialization.builtins.ListSerializer

@Module
@InstallIn(SingletonComponent::class)
object HymnalSnapshotModule {

    @Provides
    fun provideHymnalSnapshotCache(
        factory: SnapshotCacheFactory
    ): SnapshotCache<List<HymnDto>> =
        factory.create(
            key = "hymnal",
            serializer = ListSerializer(HymnDto.serializer())
        )

    @Provides
    fun provideHymnalSnapshotFetcher(
        api: HymnalApi
    ): SnapshotFetcher<List<HymnDto>> =
        HymnalSnapshotFetcher(api)
}
