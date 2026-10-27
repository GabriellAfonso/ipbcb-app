package com.ipb.castelobranco.features.gallery.di

import android.content.Context
import android.net.ConnectivityManager
import com.ipb.castelobranco.core.di.AuthedRetrofit
import com.ipb.castelobranco.core.domain.gallery.GalleryStoragePort
import com.ipb.castelobranco.core.domain.startup.Preloadable
import com.ipb.castelobranco.features.gallery.data.GalleryStoragePortImpl
import com.ipb.castelobranco.features.gallery.data.api.GalleryApi
import com.ipb.castelobranco.features.gallery.data.download.DeviceSpace
import com.ipb.castelobranco.features.gallery.data.local.GalleryMediaStore
import com.ipb.castelobranco.features.gallery.data.local.GalleryPreferences
import com.ipb.castelobranco.features.gallery.data.manage.GalleryManageRepositoryImpl
import com.ipb.castelobranco.features.gallery.data.upload.AndroidImageCodec
import com.ipb.castelobranco.features.gallery.data.upload.AndroidVideoProbe
import com.ipb.castelobranco.features.gallery.data.upload.NetworkStatus
import com.ipb.castelobranco.features.gallery.data.upload.VideoProbe
import com.ipb.castelobranco.features.gallery.data.upload.GalleryUploadQueueStore
import com.ipb.castelobranco.features.gallery.data.upload.GalleryUploadRepositoryImpl
import com.ipb.castelobranco.features.gallery.data.upload.ImageCodec
import com.ipb.castelobranco.features.gallery.data.work.WorkManagerGalleryUploadScheduler
import com.ipb.castelobranco.features.gallery.data.repository.GalleryRepositoryImpl
import com.ipb.castelobranco.features.gallery.data.work.WorkManagerGalleryDownloadScheduler
import com.ipb.castelobranco.features.gallery.data.work.WorkManagerGallerySyncScheduler
import com.ipb.castelobranco.features.gallery.data.work.WorkManagerGalleryVideoDownloadScheduler
import com.ipb.castelobranco.features.gallery.domain.download.GalleryDownloadScheduler
import com.ipb.castelobranco.features.gallery.domain.download.GalleryVideoDownloadScheduler
import com.ipb.castelobranco.features.gallery.domain.manage.GalleryManageRepository
import com.ipb.castelobranco.features.gallery.domain.repository.GalleryMediaSettings
import com.ipb.castelobranco.features.gallery.domain.repository.GalleryRepository
import com.ipb.castelobranco.features.gallery.domain.sync.GallerySyncScheduler
import com.ipb.castelobranco.features.gallery.domain.upload.GalleryUploadRepository
import com.ipb.castelobranco.features.gallery.domain.upload.GalleryUploadScheduler
import dagger.Binds
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import dagger.multibindings.IntoSet
import retrofit2.Retrofit
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
abstract class GalleryModule {

    @Binds
    @Singleton
    abstract fun bindGalleryRepository(impl: GalleryRepositoryImpl): GalleryRepository

    @Binds
    @Singleton
    abstract fun bindGalleryManageRepository(impl: GalleryManageRepositoryImpl): GalleryManageRepository

    @Binds
    @Singleton
    abstract fun bindGalleryUploadRepository(impl: GalleryUploadRepositoryImpl): GalleryUploadRepository

    @Binds
    @Singleton
    abstract fun bindGalleryUploadScheduler(impl: WorkManagerGalleryUploadScheduler): GalleryUploadScheduler

    @Binds
    abstract fun bindGalleryStoragePort(impl: GalleryStoragePortImpl): GalleryStoragePort

    @Binds
    abstract fun bindGalleryMediaSettings(impl: GalleryPreferences): GalleryMediaSettings

    @Binds
    abstract fun bindImageCodec(impl: AndroidImageCodec): ImageCodec

    @Binds
    abstract fun bindVideoProbe(impl: AndroidVideoProbe): VideoProbe

    @Binds
    @Singleton
    abstract fun bindGalleryDownloadScheduler(
        impl: WorkManagerGalleryDownloadScheduler
    ): GalleryDownloadScheduler

    @Binds
    @Singleton
    abstract fun bindGalleryVideoDownloadScheduler(
        impl: WorkManagerGalleryVideoDownloadScheduler
    ): GalleryVideoDownloadScheduler

    @Binds
    @Singleton
    abstract fun bindGallerySyncScheduler(
        impl: WorkManagerGallerySyncScheduler
    ): GallerySyncScheduler

    companion object {
        @Provides
        @Singleton
        fun provideGalleryMediaStore(
            @ApplicationContext context: Context
        ): GalleryMediaStore = GalleryMediaStore(context)

        @Provides
        fun provideNetworkStatus(@ApplicationContext context: Context): NetworkStatus = NetworkStatus {
            context.getSystemService(ConnectivityManager::class.java)?.isActiveNetworkMetered == false
        }

        @Provides
        fun provideDeviceSpace(@ApplicationContext context: Context): DeviceSpace =
            DeviceSpace { context.filesDir.usableSpace }

        @Provides
        @Singleton
        fun provideGalleryApi(
            @AuthedRetrofit retrofit: Retrofit
        ): GalleryApi = retrofit.create(GalleryApi::class.java)

        @Provides @IntoSet
        fun bindGalleryPreloadable(r: GalleryRepository): Preloadable = Preloadable { r.preload() }

        @Provides @IntoSet
        fun bindGalleryUploadQueuePreloadable(queue: GalleryUploadQueueStore): Preloadable =
            Preloadable { queue.load() }
    }
}
