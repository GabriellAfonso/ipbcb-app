package com.ipb.castelobranco.features.profile.di

import com.ipb.castelobranco.core.di.AuthedRetrofit
import com.ipb.castelobranco.core.domain.startup.Preloadable
import com.ipb.castelobranco.features.profile.data.api.ProfileApi
import com.ipb.castelobranco.features.profile.data.repository.ProfileRepositoryImpl
import com.ipb.castelobranco.features.profile.data.snapshot.ProfileSnapshotRepository
import com.ipb.castelobranco.features.profile.domain.repository.ProfileRepository
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
abstract class ProfileModule {

    @Binds
    @Singleton
    abstract fun bindProfileRepository(impl: ProfileRepositoryImpl): ProfileRepository

    companion object {
        @Provides
        @Singleton
        fun provideProfileApi(
            @AuthedRetrofit retrofit: Retrofit
        ): ProfileApi = retrofit.create(ProfileApi::class.java)

        // Cached /me must be in memory at boot: isAdmin gates the admin entry on the home screen.
        @Provides @IntoSet
        fun bindProfilePreloadable(snapshot: ProfileSnapshotRepository): Preloadable =
            Preloadable { snapshot.preload() }
    }
}