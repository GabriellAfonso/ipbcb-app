package com.ipb.castelobranco.features.admin.schedule.di

import com.ipb.castelobranco.core.di.AuthedRetrofit
import com.ipb.castelobranco.features.admin.schedule.data.api.AdminScheduleApi
import com.ipb.castelobranco.features.admin.schedule.data.repository.AdminScheduleRepositoryImpl
import com.ipb.castelobranco.features.admin.schedule.domain.repository.AdminScheduleRepository
import dagger.Binds
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton
import retrofit2.Retrofit

@Module
@InstallIn(SingletonComponent::class)
abstract class AdminScheduleModule {

    @Binds
    @Singleton
    abstract fun bindAdminScheduleRepository(
        impl: AdminScheduleRepositoryImpl
    ): AdminScheduleRepository

    companion object {
        @Provides
        @Singleton
        fun provideAdminScheduleApi(
            @AuthedRetrofit retrofit: Retrofit
        ): AdminScheduleApi = retrofit.create(AdminScheduleApi::class.java)
    }
}