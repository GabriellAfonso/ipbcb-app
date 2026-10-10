package com.ipb.castelobranco.features.gallery.di

import android.content.Context
import coil.ImageLoader
import coil.annotation.ExperimentalCoilApi
import coil.disk.DiskCache
import com.ipb.castelobranco.core.data.image.ImageMemoryBudget
import com.ipb.castelobranco.core.di.Client
import com.ipb.castelobranco.features.gallery.domain.repository.GalleryPreviewCache
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import okhttp3.OkHttpClient
import javax.inject.Qualifier
import javax.inject.Singleton

/** The image loader for gallery previews (`thumbnail_url`) — authenticated, with a disk cache. */
@Qualifier
@Retention(AnnotationRetention.BINARY)
annotation class GalleryThumbnailLoader

/**
 * Previews are member content, like the originals already stored in full on the device. So this loader
 * rides the authenticated client (`AuthInterceptor` + `TokenAuthenticator`) and keeps a bounded disk
 * cache, which lets an album seen online show its previews offline. The cache is emptied on sign-out.
 */
@Module
@InstallIn(SingletonComponent::class)
object GalleryThumbnailLoaderModule {

    private const val CACHE_DIR = "gallery_thumbs"
    private const val CACHE_MAX_BYTES = 100L * 1024 * 1024

    @Provides
    @Singleton
    @GalleryThumbnailLoader
    fun provideGalleryThumbnailLoader(
        @ApplicationContext context: Context,
        @Client client: OkHttpClient,
    ): ImageLoader = ImageLoader.Builder(context)
        .okHttpClient(client)
        .respectCacheHeaders(false)
        .memoryCache { ImageMemoryBudget.memoryCache(context, ImageMemoryBudget.GALLERY_PREVIEWS) }
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
    fun provideGalleryPreviewCache(
        @GalleryThumbnailLoader imageLoader: ImageLoader,
    ): GalleryPreviewCache = GalleryPreviewCache {
        imageLoader.memoryCache?.clear()
        imageLoader.diskCache?.clear()
    }
}
