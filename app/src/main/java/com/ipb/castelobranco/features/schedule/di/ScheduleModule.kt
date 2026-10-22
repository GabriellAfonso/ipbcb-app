package com.ipb.castelobranco.features.schedule.di

import com.ipb.castelobranco.core.di.AuthedRetrofit
import com.ipb.castelobranco.core.domain.startup.Preloadable
import com.ipb.castelobranco.core.domain.startup.Refreshable
import com.ipb.castelobranco.features.schedule.data.api.ScheduleApi
import com.ipb.castelobranco.features.schedule.data.repository.ScheduleRepositoryImpl
import com.ipb.castelobranco.features.schedule.domain.repository.ScheduleRepository
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
abstract class ScheduleModule {

    @Binds
    @Singleton
    abstract fun bindScheduleRepository(impl: ScheduleRepositoryImpl): ScheduleRepository

    companion object {
        @Provides
        @Singleton
        fun provideScheduleApi(
            @AuthedRetrofit retrofit: Retrofit
        ): ScheduleApi = retrofit.create(ScheduleApi::class.java)

        @Provides @IntoSet
        fun bindSchedulePreloadable(r: ScheduleRepository): Preloadable = Preloadable { r.preload() }

        @Provides @IntoSet
        fun bindScheduleRefreshable(r: ScheduleRepository): Refreshable = Refreshable { r.refreshMonthSchedule() }
    }
}