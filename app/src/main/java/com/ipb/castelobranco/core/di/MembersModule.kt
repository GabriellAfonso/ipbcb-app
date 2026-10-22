package com.ipb.castelobranco.core.di

import com.ipb.castelobranco.core.data.api.MembersApi
import com.ipb.castelobranco.core.data.repository.MembersRepositoryImpl
import com.ipb.castelobranco.core.domain.repository.MembersRepository
import com.ipb.castelobranco.core.domain.startup.Preloadable
import com.ipb.castelobranco.core.domain.startup.Refreshable
import dagger.Binds
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import dagger.multibindings.IntoSet
import retrofit2.Retrofit
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
abstract class MembersModule {

    @Binds
    @Singleton
    abstract fun bindMembersRepository(impl: MembersRepositoryImpl): MembersRepository

    companion object {
        @Provides
        @Singleton
        fun provideMembersApi(
            @AuthedRetrofit retrofit: Retrofit
        ): MembersApi = retrofit.create(MembersApi::class.java)

        @Provides @IntoSet
        fun bindMembersPreloadable(r: MembersRepository): Preloadable =
            Preloadable { r.preload() }

        @Provides @IntoSet
        fun bindMembersRefreshable(r: MembersRepository): Refreshable =
            Refreshable { r.refreshBirthdays() }
    }
}
