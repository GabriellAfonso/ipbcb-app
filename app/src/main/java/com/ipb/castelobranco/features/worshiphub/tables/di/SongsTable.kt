package com.ipb.castelobranco.features.worshiphub.tables.di

import com.ipb.castelobranco.core.di.AuthLessRetrofit
import com.ipb.castelobranco.core.di.AuthedRetrofit
import com.ipb.castelobranco.core.domain.repository.AllSongsRepository
import com.ipb.castelobranco.core.domain.startup.Preloadable
import com.ipb.castelobranco.core.domain.startup.Refreshable
import com.ipb.castelobranco.features.worshiphub.tables.data.api.SetlistSaveApi
import com.ipb.castelobranco.features.worshiphub.tables.data.api.SongsTableApi
import com.ipb.castelobranco.features.worshiphub.tables.data.local.RepertoireDraftStorage
import com.ipb.castelobranco.features.worshiphub.tables.data.repository.SetlistSaveRepositoryImpl
import com.ipb.castelobranco.features.worshiphub.tables.data.repository.SongsRepositoryImpl
import com.ipb.castelobranco.features.worshiphub.tables.domain.repository.RepertoireDraftRepository
import com.ipb.castelobranco.features.worshiphub.tables.domain.repository.SetlistSaveRepository
import com.ipb.castelobranco.features.worshiphub.tables.domain.repository.SongsRepository
import dagger.Binds
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import dagger.multibindings.IntoSet
import javax.inject.Singleton
import retrofit2.Retrofit

@Module
@InstallIn(SingletonComponent::class)
abstract class SongsTable {

    @Binds
    @Singleton
    abstract fun bindSongsRepository(impl: SongsRepositoryImpl): SongsRepository

    @Binds
    @Singleton
    abstract fun bindAllSongsRepository(impl: SongsRepositoryImpl): AllSongsRepository

    @Binds
    @Singleton
    abstract fun bindRepertoireDraftRepository(impl: RepertoireDraftStorage): RepertoireDraftRepository

    @Binds
    @Singleton
    abstract fun bindSetlistSaveRepository(impl: SetlistSaveRepositoryImpl): SetlistSaveRepository

    companion object {
        @Provides
        @Singleton
        fun provideSongsTableApi(
            @AuthLessRetrofit retrofit: Retrofit
        ): SongsTableApi = retrofit.create(SongsTableApi::class.java)

        @Provides
        @Singleton
        fun provideSetlistSaveApi(
            @AuthedRetrofit retrofit: Retrofit
        ): SetlistSaveApi = retrofit.create(SetlistSaveApi::class.java)

        @Provides @IntoSet
        fun bindSongsPreloadable(r: SongsRepository): Preloadable = Preloadable { r.preload() }

        @Provides @IntoSet
        fun bindAllSongsRefreshable(r: SongsRepository): Refreshable = Refreshable { r.refreshAllSongs() }

        @Provides @IntoSet
        fun bindSongsBySundayRefreshable(r: SongsRepository): Refreshable = Refreshable { r.refreshSongsBySunday() }

        @Provides @IntoSet
        fun bindTopSongsRefreshable(r: SongsRepository): Refreshable = Refreshable { r.refreshTopSongs() }

        @Provides @IntoSet
        fun bindTopTonesRefreshable(r: SongsRepository): Refreshable = Refreshable { r.refreshTopTones() }

        @Provides @IntoSet
        fun bindSuggestedSongsRefreshable(r: SongsRepository): Refreshable = Refreshable { r.refreshSuggestedSongs() }
    }
}