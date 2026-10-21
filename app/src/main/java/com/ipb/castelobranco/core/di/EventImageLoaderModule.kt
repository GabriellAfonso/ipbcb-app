package com.ipb.castelobranco.core.di

import android.content.Context
import coil.ImageLoader
import coil.annotation.ExperimentalCoilApi
import coil.disk.DiskCache
import com.ipb.castelobranco.core.data.image.ImageMemoryBudget
import com.ipb.castelobranco.core.domain.events.EventImageCache
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import okhttp3.OkHttpClient
import javax.inject.Qualifier
import javax.inject.Singleton

/** The image loader for event flyers, shared by the home carousel, the detail and the management area. */
@Qualifier
@Retention(AnnotationRetention.BINARY)
annotation class EventImageLoader

/**
 * Rides the authenticated client: the token goes along only when there is a session, which is what a
 * members-only flyer needs, and a public flyer loads signed out. The server gives a replaced image a new
 * address, so the disk cache keeps each address for good instead of revalidating on every carousel turn.
 */
@Module
@InstallIn(SingletonComponent::class)
object EventImageLoaderModule {

    private const val CACHE_DIR = "event_images"
    private const val CACHE_MAX_BYTES = 50L * 1024 * 1024

    @Provides
    @Singleton
    @EventImageLoader
    fun provideEventImageLoader(
        @ApplicationContext context: Context,
        @Client client: OkHttpClient,
    ): ImageLoader = ImageLoader.Builder(context)
        .okHttpClient(client)
        .respectCacheHeaders(false)
        .memoryCache { ImageMemoryBudget.memoryCache(context, ImageMemoryBudget.EVENT_IMAGES) }
        .diskCache {
            DiskCache.Builder()
                .directory(context.cacheDir.resolve(CACHE_DIR))
                .maxSizeBytes(CACHE_MAX_BYTES)
                .build()
        }
        .build()

    @OptIn(ExperimentalCoilApi::class)
    @Provides
    @Singleton
    fun provideEventImageCache(
        @EventImageLoader imageLoader: ImageLoader,
    ): EventImageCache = EventImageCache {
        imageLoader.memoryCache?.clear()
        imageLoader.diskCache?.clear()
    }
}
