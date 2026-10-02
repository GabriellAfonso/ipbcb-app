package com.ipb.castelobranco.features.admin.register.di

import com.ipb.castelobranco.core.di.AuthedRetrofit
import com.ipb.castelobranco.features.admin.register.data.api.SetlistAdminApi
import com.ipb.castelobranco.features.admin.register.data.api.WorshipRegisterApi
import com.ipb.castelobranco.features.admin.register.data.repository.SetlistConfirmationRepositoryImpl
import com.ipb.castelobranco.features.admin.register.domain.repository.SetlistConfirmationRepository
import com.ipb.castelobranco.features.admin.register.data.repository.WorshipRegisterRepositoryImpl
import com.ipb.castelobranco.features.admin.register.domain.repository.WorshipRegisterRepository
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

    @Binds
    @Singleton
    abstract fun bindSetlistConfirmationRepository(
        impl: SetlistConfirmationRepositoryImpl
    ): SetlistConfirmationRepository

    companion object {
        @Provides
        @Singleton
        fun provideWorshipRegisterApi(
            @AuthedRetrofit retrofit: Retrofit
        ): WorshipRegisterApi = retrofit.create(WorshipRegisterApi::class.java)

        @Provides
        @Singleton
        fun provideSetlistAdminApi(
            @AuthedRetrofit retrofit: Retrofit
        ): SetlistAdminApi = retrofit.create(SetlistAdminApi::class.java)
    }
}