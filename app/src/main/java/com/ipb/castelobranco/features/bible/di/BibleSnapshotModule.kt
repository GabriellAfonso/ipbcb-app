package com.ipb.castelobranco.features.bible.di

import com.ipb.castelobranco.core.data.snapshot.SnapshotCacheFactory
import com.ipb.castelobranco.core.domain.snapshot.SnapshotCache
import com.ipb.castelobranco.core.domain.snapshot.SnapshotFetcher
import com.ipb.castelobranco.features.bible.data.api.BibleApi
import com.ipb.castelobranco.features.bible.data.dto.BibleBookDto
import com.ipb.castelobranco.features.bible.data.snapshot.BibleSnapshotFetcher
import com.ipb.castelobranco.features.bible.domain.model.BibleTranslation
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import kotlinx.serialization.builtins.ListSerializer
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object BibleSnapshotModule {

    /**
     * Mapa imutável de tradução → cache. Injetado no Repository e no Worker.
     * Cada cache aponta para `filesDir/snapshots/bible_{naa,ara}.json`.
     */
    @Provides
    @Singleton
    fun provideBibleSnapshotCaches(
        factory: SnapshotCacheFactory
    ): Map<BibleTranslation, @JvmSuppressWildcards SnapshotCache<List<BibleBookDto>>> =
        BibleTranslation.entries.associateWith { translation ->
            factory.create(
                key = translation.snapshotKey,
                serializer = ListSerializer(BibleBookDto.serializer()),
            )
        }

    @Provides
    @Singleton
    fun provideBibleSnapshotFetchers(
        api: BibleApi,
    ): Map<BibleTranslation, @JvmSuppressWildcards SnapshotFetcher<List<BibleBookDto>>> =
        BibleTranslation.entries.associateWith { translation ->
            BibleSnapshotFetcher(api, translation)
        }
}
