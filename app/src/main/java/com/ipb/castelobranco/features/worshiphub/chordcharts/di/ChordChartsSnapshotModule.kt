package com.ipb.castelobranco.features.worshiphub.chordcharts.di

import com.ipb.castelobranco.core.data.snapshot.RetrofitSnapshotFetcher
import com.ipb.castelobranco.core.data.snapshot.SnapshotCacheFactory
import com.ipb.castelobranco.core.domain.snapshot.SnapshotCache
import com.ipb.castelobranco.core.domain.snapshot.SnapshotFetcher
import com.ipb.castelobranco.features.worshiphub.chordcharts.data.api.ChordChartsApi
import com.ipb.castelobranco.features.worshiphub.chordcharts.data.dto.ChordChartDto
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import kotlinx.serialization.builtins.ListSerializer

@Module
@InstallIn(SingletonComponent::class)
object ChordChartsSnapshotModule {

    @Provides
    fun provideChordChartsCache(
        factory: SnapshotCacheFactory,
    ): SnapshotCache<List<ChordChartDto>> = factory.create(
        key = "worshiphub_chord_charts",
        serializer = ListSerializer(ChordChartDto.serializer()),
    )

    @Provides
    fun provideChordChartsFetcher(
        api: ChordChartsApi,
    ): SnapshotFetcher<List<ChordChartDto>> = RetrofitSnapshotFetcher { etag -> api.getChordCharts(etag) }
}
