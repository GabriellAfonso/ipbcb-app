package com.ipb.castelobranco.features.events.di

import com.ipb.castelobranco.core.di.AuthedRetrofit
import com.ipb.castelobranco.core.domain.events.EventImageCache
import com.ipb.castelobranco.core.domain.session.SessionScopedCache
import com.ipb.castelobranco.core.domain.startup.Preloadable
import com.ipb.castelobranco.core.domain.startup.Refreshable
import com.ipb.castelobranco.features.events.data.api.EventsApi
import com.ipb.castelobranco.features.events.data.repository.EventsRepositoryImpl
import com.ipb.castelobranco.features.events.domain.repository.EventsRepository
import dagger.Binds
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import dagger.multibindings.IntoSet
import retrofit2.Retrofit
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
abstract class EventsModule {

    @Binds
    @Singleton
    abstract fun bindEventsRepository(impl: EventsRepositoryImpl): EventsRepository

    companion object {
        @Provides
        @Singleton
        fun provideEventsApi(@AuthedRetrofit retrofit: Retrofit): EventsApi =
            retrofit.create(EventsApi::class.java)

        // Offline from boot: the carousel shows the saved events before the network answers.
        @Provides @IntoSet
        fun bindEventsPreloadable(repository: EventsRepository): Preloadable =
            Preloadable { repository.preload() }

        // Also runs right after login, so the members-only events arrive.
        @Provides @IntoSet
        fun bindEventsRefreshable(repository: EventsRepository): Refreshable =
            Refreshable { repository.refreshEvents() }

        // A member's list may hold members-only events and their flyers: none of it outlives the session.
        @Provides @IntoSet
        fun bindEventsSessionCache(
            repository: EventsRepository,
            imageCache: EventImageCache,
        ): SessionScopedCache = eventsSessionCache(repository, imageCache)
    }
}

/**
 * Kept apart from the module so it can be tested with fakes. It does not fetch again: on an explicit
 * logout it runs while the token is still stored, so the public list is fetched once the home sees the
 * user signed out.
 */
internal fun eventsSessionCache(
    repository: EventsRepository,
    imageCache: EventImageCache,
): SessionScopedCache = SessionScopedCache {
    repository.clear()
    imageCache.clear()
}
