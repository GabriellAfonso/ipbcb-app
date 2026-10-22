package com.ipb.castelobranco.features.worshiphub.register.di

import com.ipb.castelobranco.core.di.AuthedRetrofit
import com.ipb.castelobranco.features.worshiphub.register.data.api.WorshipRegisterApi
import com.ipb.castelobranco.features.worshiphub.register.data.repository.WorshipRegisterRepositoryImpl
import com.ipb.castelobranco.features.worshiphub.register.domain.repository.WorshipRegisterRepository
import dagger.Binds
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton
import retrofit2.Retrofit

@Module
@InstallIn(SingletonComponent::class)
abstract class WorshipRegisterModule {

    @Binds
    @Singleton
    abstract fun bindWorshipRegisterRepository(
        impl: WorshipRegisterRepositoryImpl
    ): WorshipRegisterRepository

    companion object {
        @Provides
        @Singleton
        fun provideWorshipRegisterApi(
            @AuthedRetrofit retrofit: Retrofit
        ): WorshipRegisterApi = retrofit.create(WorshipRegisterApi::class.java)
    }
}
