package com.ipb.castelobranco.core.di

import com.ipb.castelobranco.core.domain.session.SessionScopedCache
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import dagger.multibindings.Multibinds

/** Declares the set so it exists (empty) even when no feature contributes a cache. */
@Module
@InstallIn(SingletonComponent::class)
abstract class SessionModule {

    @Multibinds
    abstract fun sessionScopedCaches(): Set<SessionScopedCache>
}
