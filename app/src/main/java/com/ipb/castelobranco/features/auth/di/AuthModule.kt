package com.ipb.castelobranco.features.auth.di

import com.ipb.castelobranco.core.di.AuthLessRetrofit
import com.ipb.castelobranco.features.auth.data.api.AuthApi
import com.ipb.castelobranco.features.auth.data.repository.AuthRepositoryImpl
import com.ipb.castelobranco.features.auth.domain.repository.AuthRepository
import dagger.Binds
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import retrofit2.Retrofit
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
abstract class AuthModule {

    @Binds
    @Singleton
    abstract fun bindAuthRepository(
        impl: AuthRepositoryImpl
    ): AuthRepository

    companion object {
        @Provides
        @Singleton
        fun provideAuthApi(
            @AuthLessRetrofit retrofit: Retrofit
        ): AuthApi = retrofit.create(AuthApi::class.java)
    }
}