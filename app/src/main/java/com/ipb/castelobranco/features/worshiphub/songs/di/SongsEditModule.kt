package com.ipb.castelobranco.features.worshiphub.songs.di

import com.ipb.castelobranco.core.di.AuthedRetrofit
import com.ipb.castelobranco.features.worshiphub.songs.data.api.SongsEditApi
import com.ipb.castelobranco.features.worshiphub.songs.data.repository.SongEditRepositoryImpl
import com.ipb.castelobranco.features.worshiphub.songs.domain.repository.SongEditRepository
import dagger.Binds
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton
import retrofit2.Retrofit

@Module
@InstallIn(SingletonComponent::class)
abstract class SongsEditModule {

    @Binds
    @Singleton
    abstract fun bindSongEditRepository(impl: SongEditRepositoryImpl): SongEditRepository

    companion object {
        @Provides
        @Singleton
        fun provideSongsEditApi(
            @AuthedRetrofit retrofit: Retrofit
        ): SongsEditApi = retrofit.create(SongsEditApi::class.java)
    }
}
