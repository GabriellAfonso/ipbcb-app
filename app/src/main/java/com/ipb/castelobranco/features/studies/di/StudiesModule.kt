package com.ipb.castelobranco.features.studies.di

import com.ipb.castelobranco.core.di.AuthLessRetrofit
import com.ipb.castelobranco.features.studies.data.api.DoctrineApi
import com.ipb.castelobranco.features.studies.data.local.DoctrinePreferences
import com.ipb.castelobranco.features.studies.data.repository.DefaultDoctrineSnapshotSources
import com.ipb.castelobranco.features.studies.data.repository.DoctrineRepositoryImpl
import com.ipb.castelobranco.features.studies.data.repository.DoctrineSnapshotSources
import com.ipb.castelobranco.features.studies.domain.repository.DoctrinePreferencesRepository
import com.ipb.castelobranco.features.studies.domain.repository.DoctrineRepository
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

    companion object {
        @Provides
        @Singleton
        fun provideDoctrineApi(
            @AuthLessRetrofit retrofit: Retrofit
        ): DoctrineApi = retrofit.create(DoctrineApi::class.java)
    }
}
