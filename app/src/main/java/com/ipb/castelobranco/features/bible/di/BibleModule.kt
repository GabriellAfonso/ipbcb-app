package com.ipb.castelobranco.features.bible.di

import com.ipb.castelobranco.core.di.AuthLessRetrofit
import com.ipb.castelobranco.features.bible.data.api.BibleApi
import com.ipb.castelobranco.features.bible.data.repository.BibleRepositoryImpl
import com.ipb.castelobranco.features.bible.data.work.WorkManagerBibleDownloadScheduler
import com.ipb.castelobranco.features.bible.domain.download.BibleDownloadScheduler
import com.ipb.castelobranco.features.bible.domain.repository.BibleRepository
import dagger.Binds
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import retrofit2.Retrofit
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
abstract class BibleModule {

    @Binds
    @Singleton
    abstract fun bindBibleRepository(impl: BibleRepositoryImpl): BibleRepository

    @Binds
    @Singleton
    abstract fun bindBibleDownloadScheduler(
        impl: WorkManagerBibleDownloadScheduler
    ): BibleDownloadScheduler

    companion object {
        @Provides
        @Singleton
        fun provideBibleApi(
            @AuthLessRetrofit retrofit: Retrofit
        ): BibleApi = retrofit.create(BibleApi::class.java)
    }
}
