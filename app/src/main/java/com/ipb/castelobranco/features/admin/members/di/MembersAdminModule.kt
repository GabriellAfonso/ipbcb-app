package com.ipb.castelobranco.features.admin.members.di

import coil.ImageLoader
import com.ipb.castelobranco.core.di.AuthedRetrofit
import com.ipb.castelobranco.core.domain.session.SessionScopedCache
import com.ipb.castelobranco.features.admin.members.data.api.MembersAdminApi
import com.ipb.castelobranco.features.admin.members.data.repository.MembersAdminRepositoryImpl
import com.ipb.castelobranco.features.admin.members.domain.repository.MembersAdminRepository
import dagger.Binds
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import dagger.multibindings.IntoSet
import retrofit2.Retrofit
import javax.inject.Singleton

/**
 * Leader-only members area. The API comes from `@AuthedRetrofit` — every endpoint requires a
 * leader, and the anonymous client would be a silent 401.
 */
@Module
@InstallIn(SingletonComponent::class)
abstract class MembersAdminModule {

    @Binds
    @Singleton
    abstract fun bindMembersAdminRepository(impl: MembersAdminRepositoryImpl): MembersAdminRepository

    companion object {

        @Provides
        @Singleton
        fun provideMembersAdminApi(@AuthedRetrofit retrofit: Retrofit): MembersAdminApi =
            retrofit.create(MembersAdminApi::class.java)

        /** Sign-out wipes the roll held in memory and the photos Coil kept in memory. */
        @Provides
        @IntoSet
        fun provideMembersSessionCache(
            repository: MembersAdminRepository,
            @MemberPhotoLoader imageLoader: ImageLoader,
        ): SessionScopedCache = SessionScopedCache {
            repository.clear()
            imageLoader.memoryCache?.clear()
        }
    }
}
