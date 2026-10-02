package com.ipb.castelobranco.core.di

import com.ipb.castelobranco.core.data.push.DevicesApi
import com.ipb.castelobranco.core.data.push.DevicesRepositoryImpl
import com.ipb.castelobranco.core.data.push.FcmTokenSource
import com.ipb.castelobranco.core.data.push.PushTokenStore
import com.ipb.castelobranco.core.data.push.SetlistNotifications
import com.ipb.castelobranco.core.data.push.SetlistNotifier
import com.ipb.castelobranco.core.data.push.SetlistRefreshScheduler
import com.ipb.castelobranco.core.data.push.WorkManagerPushRegistrationScheduler
import com.ipb.castelobranco.core.data.push.WorkManagerSetlistRefreshScheduler
import com.ipb.castelobranco.core.domain.push.DevicesRepository
import com.ipb.castelobranco.core.domain.push.PushRegistrationScheduler
import com.ipb.castelobranco.core.domain.push.PushTokenSource
import com.ipb.castelobranco.core.domain.push.RegisteredTokenStore
import dagger.Binds
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import retrofit2.Retrofit
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
abstract class PushModule {

    @Binds
    @Singleton
    abstract fun bindPushTokenSource(impl: FcmTokenSource): PushTokenSource

    @Binds
    @Singleton
    abstract fun bindPushRegistrationScheduler(impl: WorkManagerPushRegistrationScheduler): PushRegistrationScheduler

    @Binds
    @Singleton
    abstract fun bindDevicesRepository(impl: DevicesRepositoryImpl): DevicesRepository

    @Binds
    @Singleton
    abstract fun bindRegisteredTokenStore(impl: PushTokenStore): RegisteredTokenStore

    @Binds
    @Singleton
    abstract fun bindSetlistNotifier(impl: SetlistNotifications): SetlistNotifier

    @Binds
    @Singleton
    abstract fun bindSetlistRefreshScheduler(impl: WorkManagerSetlistRefreshScheduler): SetlistRefreshScheduler

    companion object {
        @Provides
        @Singleton
        fun provideDevicesApi(@AuthedRetrofit retrofit: Retrofit): DevicesApi =
            retrofit.create(DevicesApi::class.java)
    }
}
