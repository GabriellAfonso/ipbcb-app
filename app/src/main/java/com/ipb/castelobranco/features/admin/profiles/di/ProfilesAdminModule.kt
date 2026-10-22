package com.ipb.castelobranco.features.admin.profiles.di

import android.content.Context
import coil.ImageLoader
import com.ipb.castelobranco.core.data.image.ImageMemoryBudget
import com.ipb.castelobranco.core.di.AuthedRetrofit
import com.ipb.castelobranco.core.di.Client
import com.ipb.castelobranco.core.domain.session.SessionScopedCache
import com.ipb.castelobranco.features.admin.profiles.data.api.ProfilesAdminApi
import com.ipb.castelobranco.features.admin.profiles.data.repository.ProfilesAdminRepositoryImpl
import com.ipb.castelobranco.features.admin.profiles.domain.repository.ProfilesAdminRepository
import dagger.Binds
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import dagger.multibindings.IntoSet
import okhttp3.OkHttpClient
import retrofit2.Retrofit
import javax.inject.Qualifier
import javax.inject.Singleton

/** Account photos in "Perfis": protected media, kept in memory only — never in a disk cache. */
@Qualifier
@Retention(AnnotationRetention.BINARY)
annotation class ProfilePhotoLoader

@Module
@InstallIn(SingletonComponent::class)
abstract class ProfilesAdminModule {

    @Binds
    @Singleton
    abstract fun bindProfilesAdminRepository(impl: ProfilesAdminRepositoryImpl): ProfilesAdminRepository

    companion object {

        @Provides
        @Singleton
        fun provideProfilesAdminApi(@AuthedRetrofit retrofit: Retrofit): ProfilesAdminApi =
            retrofit.create(ProfilesAdminApi::class.java)

        @Provides
        @Singleton
        @ProfilePhotoLoader
        fun provideProfilePhotoLoader(
            @ApplicationContext context: Context,
            @Client client: OkHttpClient,
        ): ImageLoader = ImageLoader.Builder(context)
            .okHttpClient(client)
            .memoryCache { ImageMemoryBudget.memoryCache(context, ImageMemoryBudget.PROFILE_PHOTOS) }
            .diskCache(null)
            .build()

        /** Sign-out forgets the lists and the photos held in memory. */
        @Provides
        @IntoSet
        fun provideProfilesSessionCache(
            repository: ProfilesAdminRepository,
            @ProfilePhotoLoader imageLoader: ImageLoader,
        ): SessionScopedCache = SessionScopedCache {
            repository.clear()
            imageLoader.memoryCache?.clear()
        }
    }
}
