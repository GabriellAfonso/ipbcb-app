package com.ipb.castelobranco.core.di

import com.ipb.castelobranco.BuildConfig
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton
import kotlinx.serialization.json.Json
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import retrofit2.Retrofit
import retrofit2.converter.kotlinx.serialization.asConverterFactory

@Module
@InstallIn(SingletonComponent::class)
object RetrofitModule {

    @Provides
    @Singleton
    @ApiBaseUrl
    fun provideBaseUrl(): String = BuildConfig.API_BASE_URL

    @Provides
    @Singleton
    @AuthedRetrofit
    fun provideAuthedRetrofit(
        json: Json,
        @Client client: OkHttpClient,
        @ApiBaseUrl baseUrl: String,
    ): Retrofit =
        Retrofit.Builder()
            .baseUrl(baseUrl)
            .addConverterFactory(json.asConverterFactory("application/json".toMediaType()))
            .client(client)
            .build()

    @Provides
    @Singleton
    @AuthLessRetrofit
    fun provideAuthLessRetrofit(
        json: Json,
        @AuthLessClient client: OkHttpClient,
        @ApiBaseUrl baseUrl: String,
    ): Retrofit =
        Retrofit.Builder()
            .baseUrl(baseUrl)
            .addConverterFactory(json.asConverterFactory("application/json".toMediaType()))
            .client(client)
            .build()
}