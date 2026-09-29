package com.ipb.castelobranco.features.gallery.di

import com.ipb.castelobranco.core.data.snapshot.SnapshotCacheFactory
import com.ipb.castelobranco.core.domain.snapshot.SnapshotCache
import com.ipb.castelobranco.features.gallery.data.snapshot.GalleryIndexSnapshot
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent

@Module
@InstallIn(SingletonComponent::class)
object GallerySnapshotModule {

    private const val GALLERY_INDEX_KEY = "gallery_index"

    @Provides
    fun provideGalleryIndexSnapshotCache(
        factory: SnapshotCacheFactory
    ): SnapshotCache<GalleryIndexSnapshot> =
        factory.create(
            key = GALLERY_INDEX_KEY,
            serializer = GalleryIndexSnapshot.serializer()
        )
}
