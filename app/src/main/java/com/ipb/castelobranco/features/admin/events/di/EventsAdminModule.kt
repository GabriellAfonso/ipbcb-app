package com.ipb.castelobranco.features.admin.events.di

import com.ipb.castelobranco.core.di.AuthedRetrofit
import com.ipb.castelobranco.features.admin.events.data.api.EventsAdminApi
import com.ipb.castelobranco.features.admin.events.data.image.AndroidImageBoundsReader
import com.ipb.castelobranco.features.admin.events.data.repository.EventsAdminRepositoryImpl
import com.ipb.castelobranco.features.admin.events.domain.repository.EventsAdminRepository
import com.ipb.castelobranco.features.admin.events.domain.repository.ImageBoundsReader
import dagger.Binds
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import retrofit2.Retrofit
import javax.inject.Singleton

/** Events management. The API comes from `@AuthedRetrofit`: every endpoint needs a level on `events`. */
@Module
@InstallIn(SingletonComponent::class)
abstract class EventsAdminModule {

    @Binds
    @Singleton
    abstract fun bindEventsAdminRepository(impl: EventsAdminRepositoryImpl): EventsAdminRepository

    @Binds
    abstract fun bindImageBoundsReader(impl: AndroidImageBoundsReader): ImageBoundsReader

    companion object {
        @Provides
        @Singleton
        fun provideEventsAdminApi(@AuthedRetrofit retrofit: Retrofit): EventsAdminApi =
            retrofit.create(EventsAdminApi::class.java)
    }
}
