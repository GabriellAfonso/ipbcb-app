package com.ipb.castelobranco.features.admin.members.di

import android.content.Context
import coil.ImageLoader
import com.ipb.castelobranco.core.di.Client
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import okhttp3.OkHttpClient
import javax.inject.Qualifier
import javax.inject.Singleton

/** The image loader for member photos — authenticated and memory-only. */
@Qualifier
@Retention(AnnotationRetention.BINARY)
annotation class MemberPhotoLoader

/**
 * Member photos open only with a leader's token and must never reach the disk (LGPD art. 11).
 * So this loader rides the authenticated client (`AuthInterceptor` + `TokenAuthenticator`) and has
 * no disk cache; the memory cache keeps a photo already shown in this session from downloading
 * again, and it is cleared on sign-out with the rest of the members data.
 */
@Module
@InstallIn(SingletonComponent::class)
object MemberPhotoLoaderModule {

    @Provides
    @Singleton
    @MemberPhotoLoader
    fun provideMemberPhotoLoader(
        @ApplicationContext context: Context,
        @Client client: OkHttpClient,
    ): ImageLoader = ImageLoader.Builder(context)
        .okHttpClient(client)
        .diskCache(null)
        .respectCacheHeaders(false)
        .build()
}
