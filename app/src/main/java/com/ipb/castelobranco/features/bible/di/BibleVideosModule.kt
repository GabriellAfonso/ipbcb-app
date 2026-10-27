package com.ipb.castelobranco.features.bible.di

import com.ipb.castelobranco.core.data.snapshot.RetrofitSnapshotFetcher
import com.ipb.castelobranco.core.data.snapshot.SnapshotCacheFactory
import com.ipb.castelobranco.core.di.AuthLessRetrofit
import com.ipb.castelobranco.core.di.AuthedRetrofit
import com.ipb.castelobranco.core.domain.session.SessionScopedCache
import com.ipb.castelobranco.core.domain.snapshot.SnapshotCache
import com.ipb.castelobranco.core.domain.snapshot.SnapshotFetcher
import com.ipb.castelobranco.core.domain.startup.Preloadable
import com.ipb.castelobranco.core.domain.startup.Refreshable
import com.ipb.castelobranco.features.bible.data.api.BibleProgressApi
import com.ipb.castelobranco.features.bible.data.api.BibleVideosManageApi
import com.ipb.castelobranco.features.bible.data.api.BibleVideosPublicApi
import com.ipb.castelobranco.features.bible.data.dto.BibleProgressDto
import com.ipb.castelobranco.features.bible.data.dto.BibleVideoDto
import com.ipb.castelobranco.features.bible.data.repository.BibleProgressRepositoryImpl
import com.ipb.castelobranco.features.bible.data.repository.BibleVideoRepositoryImpl
import com.ipb.castelobranco.features.bible.data.work.WorkManagerBibleProgressSyncScheduler
import com.ipb.castelobranco.features.bible.domain.repository.BibleProgressRepository
import com.ipb.castelobranco.features.bible.domain.repository.BibleVideoRepository
import com.ipb.castelobranco.features.bible.domain.sync.BibleProgressSyncScheduler
import dagger.Binds
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import dagger.multibindings.IntoSet
import kotlinx.serialization.builtins.ListSerializer
import retrofit2.Retrofit
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
abstract class BibleVideosModule {

    @Binds
    @Singleton
    abstract fun bindBibleVideoRepository(impl: BibleVideoRepositoryImpl): BibleVideoRepository

    @Binds
    @Singleton
    abstract fun bindBibleProgressRepository(impl: BibleProgressRepositoryImpl): BibleProgressRepository

    @Binds
    @Singleton
    abstract fun bindBibleProgressSyncScheduler(
        impl: WorkManagerBibleProgressSyncScheduler
    ): BibleProgressSyncScheduler

    companion object {
        private const val VIDEOS_SNAPSHOT_KEY = "bible_videos"
        private const val PROGRESS_SNAPSHOT_KEY = "bible_progress"

        /** Public list: no token, so signed-out users see it and a stale token never interferes. */
        @Provides
        @Singleton
        fun provideBibleVideosPublicApi(@AuthLessRetrofit retrofit: Retrofit): BibleVideosPublicApi =
            retrofit.create(BibleVideosPublicApi::class.java)

        @Provides
        @Singleton
        fun provideBibleVideosManageApi(@AuthedRetrofit retrofit: Retrofit): BibleVideosManageApi =
            retrofit.create(BibleVideosManageApi::class.java)

        @Provides
        @Singleton
        fun provideBibleProgressApi(@AuthedRetrofit retrofit: Retrofit): BibleProgressApi =
            retrofit.create(BibleProgressApi::class.java)

        @Provides
        fun provideBibleVideosCache(factory: SnapshotCacheFactory): SnapshotCache<List<BibleVideoDto>> =
            factory.create(key = VIDEOS_SNAPSHOT_KEY, serializer = ListSerializer(BibleVideoDto.serializer()))

        @Provides
        fun provideBibleVideosFetcher(api: BibleVideosPublicApi): SnapshotFetcher<List<BibleVideoDto>> =
            RetrofitSnapshotFetcher(call = { etag -> api.getVideos(etag) })

        @Provides
        fun provideBibleProgressCache(factory: SnapshotCacheFactory): SnapshotCache<BibleProgressDto> =
            factory.create(key = PROGRESS_SNAPSHOT_KEY, serializer = BibleProgressDto.serializer())

        /** The list in memory at boot: the play icon needs no wait. */
        @Provides
        @IntoSet
        fun bindBibleVideosPreloadable(repository: BibleVideoRepository): Preloadable =
            Preloadable { repository.preload() }

        /** Boot network phase and again right after login. */
        @Provides
        @IntoSet
        fun bindBibleProgressRefreshable(repository: BibleProgressRepository): Refreshable =
            Refreshable { repository.refresh() }

        /** Another account may sign in on this device: its marks must start clean. */
        @Provides
        @IntoSet
        fun bindBibleProgressSessionCache(repository: BibleProgressRepository): SessionScopedCache =
            SessionScopedCache { repository.flushAndClear() }
    }
}
