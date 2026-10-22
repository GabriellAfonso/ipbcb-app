package com.ipb.castelobranco.core.di

import com.ipb.castelobranco.core.data.api.MembersApi
import com.ipb.castelobranco.core.data.dto.BirthdaysResponseDto
import com.ipb.castelobranco.core.data.snapshot.BirthdaySnapshotFetcher
import com.ipb.castelobranco.core.data.snapshot.SnapshotCacheFactory
import com.ipb.castelobranco.core.domain.snapshot.SnapshotCache
import com.ipb.castelobranco.core.domain.snapshot.SnapshotFetcher
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent

@Module
@InstallIn(SingletonComponent::class)
object BirthdaySnapshotModule {

    @Provides
    fun provideBirthdaySnapshotCache(
        factory: SnapshotCacheFactory
    ): SnapshotCache<BirthdaysResponseDto> =
        factory.create(
            key = "birthdays",
            serializer = BirthdaysResponseDto.serializer()
        )

    @Provides
    fun provideBirthdaySnapshotFetcher(
        api: MembersApi
    ): SnapshotFetcher<BirthdaysResponseDto> =
        BirthdaySnapshotFetcher(api)
}
