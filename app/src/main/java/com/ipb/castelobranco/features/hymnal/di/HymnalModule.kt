package com.ipb.castelobranco.features.hymnal.di

import com.ipb.castelobranco.core.di.AuthLessRetrofit
import com.ipb.castelobranco.core.domain.repository.HymnCatalogRepository
import com.ipb.castelobranco.core.domain.startup.Preloadable
import com.ipb.castelobranco.core.domain.startup.Refreshable
import com.ipb.castelobranco.features.hymnal.data.api.HymnalApi
import com.ipb.castelobranco.features.hymnal.data.repository.HymnalRepositoryImpl
import com.ipb.castelobranco.features.hymnal.domain.repository.HymnalRepository
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
abstract class HymnalModule {

    @Binds
    @Singleton
    abstract fun bindHymnalRepository(impl: HymnalRepositoryImpl): HymnalRepository

    /**
     * The catalogue as `core/` exposes it, so features that may not import this one — the
     * administration reports, today — can still cross the hymnal with their own data.
     */
    @Binds
    @Singleton
    abstract fun bindHymnCatalogRepository(impl: HymnalRepositoryImpl): HymnCatalogRepository

    companion object {
        @Provides
        @Singleton
        fun provideHymnalApi(
            @AuthLessRetrofit retrofit: Retrofit
        ): HymnalApi = retrofit.create(HymnalApi::class.java)

        @Provides @IntoSet
        fun bindHymnalPreloadable(r: HymnalRepository): Preloadable = Preloadable { r.preload() }

        @Provides @IntoSet
        fun bindHymnalRefreshable(r: HymnalRepository): Refreshable = Refreshable { r.refreshHymnal() }
    }
}