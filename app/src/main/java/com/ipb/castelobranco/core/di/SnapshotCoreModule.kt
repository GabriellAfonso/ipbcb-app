package com.ipb.castelobranco.core.di

import android.content.Context
import com.ipb.castelobranco.core.data.local.JsonSnapshotStorage
import com.ipb.castelobranco.core.data.local.SnapshotStorage
import com.ipb.castelobranco.core.data.logger.AndroidLogger
import com.ipb.castelobranco.core.data.snapshot.SnapshotCacheFactory
import com.ipb.castelobranco.core.domain.snapshot.Logger
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.serialization.json.Json

import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object SnapshotCoreModule {


    @Provides
    @Singleton
    fun provideLogger(): Logger =
        AndroidLogger()

    @Provides
    @Singleton
    fun provideSnapshotStorage(
        @ApplicationContext context: Context,
        @IoDispatcher ioDispatcher: CoroutineDispatcher,
    ): SnapshotStorage =
        JsonSnapshotStorage(context, ioDispatcher)

    @Provides
    @Singleton
    fun provideSnapshotCacheFactory(
        storage: SnapshotStorage,
        json: Json
    ): SnapshotCacheFactory =
        SnapshotCacheFactory(storage, json)

}
