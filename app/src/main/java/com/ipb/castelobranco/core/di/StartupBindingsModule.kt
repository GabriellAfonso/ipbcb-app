package com.ipb.castelobranco.core.di

import com.ipb.castelobranco.core.domain.startup.Preloadable
import com.ipb.castelobranco.core.domain.startup.Refreshable
import dagger.Module
import dagger.multibindings.Multibinds
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent

@Module
@InstallIn(SingletonComponent::class)
abstract class StartupBindingsModule {
    @Multibinds
    abstract fun preloadables(): Set<Preloadable>

    @Multibinds
    abstract fun refreshables(): Set<Refreshable>
}
