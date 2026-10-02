package com.ipb.castelobranco.core.di

import com.ipb.castelobranco.core.data.setlist.SetlistApi
import com.ipb.castelobranco.core.data.setlist.SundaySetlistRepositoryImpl
import com.ipb.castelobranco.core.domain.session.SessionScopedCache
import com.ipb.castelobranco.core.domain.setlist.SundaySetlistRepository
import com.ipb.castelobranco.core.domain.startup.Preloadable
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
abstract class SetlistModule {

    @Binds
    @Singleton
    abstract fun bindSundaySetlistRepository(impl: SundaySetlistRepositoryImpl): SundaySetlistRepository

    companion object {
        @Provides
        @Singleton
        fun provideSetlistApi(@AuthedRetrofit retrofit: Retrofit): SetlistApi =
            retrofit.create(SetlistApi::class.java)

        // Offline from boot: the worship hub lists read it before any network call.
        @Provides @IntoSet
        fun bindSundaySetlistPreloadable(repository: SundaySetlistRepositoryImpl): Preloadable =
            Preloadable { repository.preload() }

        // The setlist came from a members-only read: it must not outlive the session.
        @Provides @IntoSet
        fun bindSundaySetlistSessionCache(repository: SundaySetlistRepositoryImpl): SessionScopedCache =
            SessionScopedCache { repository.clear() }
    }
}
