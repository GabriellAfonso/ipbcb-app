package com.ipb.castelobranco.features.studies.di

import com.ipb.castelobranco.core.di.AuthLessRetrofit
import com.ipb.castelobranco.core.di.AuthedRetrofit
import com.ipb.castelobranco.features.studies.data.api.DoctrineApi
import com.ipb.castelobranco.features.studies.data.api.SundaySchoolAdminApi
import com.ipb.castelobranco.features.studies.data.api.SundaySchoolApi
import com.ipb.castelobranco.features.studies.data.local.DoctrinePreferences
import com.ipb.castelobranco.features.studies.data.repository.DefaultDoctrineSnapshotSources
import com.ipb.castelobranco.features.studies.data.repository.DoctrineRepositoryImpl
import com.ipb.castelobranco.features.studies.data.repository.DoctrineSnapshotSources
import com.ipb.castelobranco.features.studies.data.repository.DefaultSundaySchoolSnapshotSources
import com.ipb.castelobranco.features.studies.data.repository.SundaySchoolAdminRepositoryImpl
import com.ipb.castelobranco.features.studies.data.repository.SundaySchoolRepositoryImpl
import com.ipb.castelobranco.features.studies.data.repository.SundaySchoolSnapshotSources
import com.ipb.castelobranco.features.studies.domain.repository.DoctrinePreferencesRepository
import com.ipb.castelobranco.features.studies.domain.repository.DoctrineRepository
import com.ipb.castelobranco.features.studies.domain.repository.SundaySchoolAdminRepository
import com.ipb.castelobranco.features.studies.domain.repository.SundaySchoolRepository
import dagger.Binds
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import retrofit2.Retrofit
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
abstract class StudiesModule {

    @Binds
    @Singleton
    abstract fun bindDoctrineRepository(impl: DoctrineRepositoryImpl): DoctrineRepository

    @Binds
    abstract fun bindDoctrineSnapshotSources(impl: DefaultDoctrineSnapshotSources): DoctrineSnapshotSources

    @Binds
    abstract fun bindDoctrinePreferences(impl: DoctrinePreferences): DoctrinePreferencesRepository

    @Binds
    @Singleton
    abstract fun bindSundaySchoolRepository(impl: SundaySchoolRepositoryImpl): SundaySchoolRepository

    @Binds
    abstract fun bindSundaySchoolSnapshotSources(
        impl: DefaultSundaySchoolSnapshotSources,
    ): SundaySchoolSnapshotSources

    @Binds
    @Singleton
    abstract fun bindSundaySchoolAdminRepository(impl: SundaySchoolAdminRepositoryImpl): SundaySchoolAdminRepository

    companion object {
        @Provides
        @Singleton
        fun provideDoctrineApi(
            @AuthLessRetrofit retrofit: Retrofit
        ): DoctrineApi = retrofit.create(DoctrineApi::class.java)

        @Provides
        @Singleton
        fun provideSundaySchoolApi(
            @AuthLessRetrofit retrofit: Retrofit
        ): SundaySchoolApi = retrofit.create(SundaySchoolApi::class.java)

        @Provides
        @Singleton
        fun provideSundaySchoolAdminApi(
            @AuthedRetrofit retrofit: Retrofit
        ): SundaySchoolAdminApi = retrofit.create(SundaySchoolAdminApi::class.java)
    }
}
