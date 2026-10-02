package com.ipb.castelobranco.core.di

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.preferencesDataStoreFile
import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton


import javax.inject.Qualifier

@Qualifier
@Retention(AnnotationRetention.BINARY)
annotation class AuthPrefs

@Qualifier
@Retention(AnnotationRetention.BINARY)
annotation class SettingsPrefs

@Qualifier
@Retention(AnnotationRetention.BINARY)
annotation class SetlistPrefs

@Qualifier
@Retention(AnnotationRetention.BINARY)
annotation class PushPrefs

@Module
@InstallIn(SingletonComponent::class)
object DataStoreModule {

    @Provides
    @Singleton
    @AuthPrefs
    fun provideAuthPreferencesDataStore(
        @ApplicationContext context: Context,
    ): DataStore<Preferences> =
        PreferenceDataStoreFactory.create(
            produceFile = { context.preferencesDataStoreFile("auth_prefs") }
        )

    @Provides
    @Singleton
    @SettingsPrefs
    fun provideSettingsPreferencesDataStore(
        @ApplicationContext context: Context,
    ): DataStore<Preferences> =
        PreferenceDataStoreFactory.create(
            produceFile = { context.preferencesDataStoreFile("settings_prefs") }
        )

    @Provides
    @Singleton
    @SetlistPrefs
    fun provideSetlistPreferencesDataStore(
        @ApplicationContext context: Context,
    ): DataStore<Preferences> =
        PreferenceDataStoreFactory.create(
            produceFile = { context.preferencesDataStoreFile("setlist_prefs") }
        )

    @Provides
    @Singleton
    @PushPrefs
    fun providePushPreferencesDataStore(
        @ApplicationContext context: Context,
    ): DataStore<Preferences> =
        PreferenceDataStoreFactory.create(
            produceFile = { context.preferencesDataStoreFile("push_prefs") }
        )
}
