package com.ipb.castelobranco.features.worshiphub.lyrics.di

import com.ipb.castelobranco.core.di.AuthLessRetrofit
import com.ipb.castelobranco.core.di.AuthedRetrofit
import com.ipb.castelobranco.core.domain.startup.Preloadable
import com.ipb.castelobranco.core.domain.startup.Refreshable
import com.ipb.castelobranco.features.worshiphub.lyrics.data.api.LyricsApi
import com.ipb.castelobranco.features.worshiphub.lyrics.data.api.LyricsEditApi
import com.ipb.castelobranco.features.worshiphub.lyrics.data.repository.LyricsRepositoryImpl
import com.ipb.castelobranco.features.worshiphub.lyrics.domain.repository.LyricsRepository
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
abstract class LyricsModule {

    @Binds
    @Singleton
    abstract fun bindLyricsRepository(impl: LyricsRepositoryImpl): LyricsRepository

    companion object {
        @Provides
        @Singleton
        fun provideLyricsApi(
            @AuthLessRetrofit retrofit: Retrofit
        ): LyricsApi = retrofit.create(LyricsApi::class.java)

        @Provides
        @Singleton
        fun provideLyricsEditApi(
            @AuthedRetrofit retrofit: Retrofit
        ): LyricsEditApi = retrofit.create(LyricsEditApi::class.java)

        @Provides @IntoSet
        fun bindLyricsPreloadable(r: LyricsRepository): Preloadable = Preloadable { r.preload() }

        @Provides @IntoSet
        fun bindLyricsRefreshable(r: LyricsRepository): Refreshable = Refreshable { r.refresh() }
    }
}