package com.ipb.castelobranco.core.di

import android.os.SystemClock
import com.ipb.castelobranco.BuildConfig
import com.ipb.castelobranco.core.data.auth.AuthSessionStatusProvider
import com.ipb.castelobranco.core.data.local.DataStoreDeviceIdProvider
import com.ipb.castelobranco.core.data.local.DeviceIdProvider
import com.ipb.castelobranco.core.domain.auth.AuthStatusProvider
import com.ipb.castelobranco.core.domain.auth.SessionPresenceProvider
import com.ipb.castelobranco.core.domain.util.DateProvider
import com.ipb.castelobranco.core.domain.util.MonotonicClock
import com.ipb.castelobranco.core.domain.util.WallClock
import dagger.Binds
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import java.time.LocalDate
import java.time.ZoneId
import javax.inject.Qualifier
import javax.inject.Singleton

@Qualifier
@Retention(AnnotationRetention.BINARY)
annotation class AppVersionName

@Qualifier
@Retention(AnnotationRetention.BINARY)
annotation class PlatformName

/**
 * Ambient app facts and small cross-cutting primitives: the running version, the platform name,
 * a monotonic clock, the device identifier, and a read-only view of the session.
 *
 * These live here so `domain/` code can depend on plain values and interfaces instead of on
 * `BuildConfig`, `SystemClock`, or another feature's session class.
 */
@Module
@InstallIn(SingletonComponent::class)
abstract class AppInfoModule {

    @Binds
    @Singleton
    abstract fun bindDeviceIdProvider(impl: DataStoreDeviceIdProvider): DeviceIdProvider

    @Binds
    @Singleton
    abstract fun bindAuthStatusProvider(impl: AuthSessionStatusProvider): AuthStatusProvider

    @Binds
    @Singleton
    abstract fun bindSessionPresenceProvider(impl: AuthSessionStatusProvider): SessionPresenceProvider

    companion object {

        @Provides
        @Singleton
        @AppVersionName
        fun provideAppVersionName(): String = BuildConfig.VERSION_NAME

        @Provides
        @Singleton
        @PlatformName
        fun providePlatformName(): String = PLATFORM_ANDROID

        @Provides
        @Singleton
        fun provideMonotonicClock(): MonotonicClock =
            MonotonicClock { SystemClock.elapsedRealtime() }

        @Provides
        @Singleton
        fun provideWallClock(): WallClock =
            WallClock { System.currentTimeMillis() }

        /**
         * Today in the church's zone, not the device's. A leader travelling must not see the
         * report shift by a day, and the collection service resolves its inclusive day
         * boundaries in this same zone.
         */
        @Provides
        @Singleton
        fun provideDateProvider(): DateProvider =
            DateProvider { LocalDate.now(ZoneId.of(CHURCH_ZONE_ID)) }

        private const val PLATFORM_ANDROID = "android"

        private const val CHURCH_ZONE_ID = "America/Sao_Paulo"
    }
}
